/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.internal.incremental;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.AsmUtils;
import com.google.common.base.Objects;
import com.google.common.io.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Instant Run Verifier responsible for checking that a class change (between two developers
 * iteration) can be safely hot swapped on the device or not.
 *
 * ThreadSafe
 */
public class InstantRunVerifier {

    private static final Comparator<MethodNode> METHOD_COMPARATOR = new MethodNodeComparator();
    private static final Comparator<AnnotationNode> ANNOTATION_COMPARATOR =
            new AnnotationNodeComparator();
    private static final Comparator<Object> OBJECT_COMPARATOR = new Comparator<Object>() {
        @Override
        public boolean areEqual(Object first, Object second) {
            return Objects.equal(first, second);
        }
    };

    /**
     * describe the difference between two collections of the same elements.
     */
    private enum Diff {
        /**
         * no change, the collections are equals
         */
        NONE,
        /**
         * an element was added to the first collection.
         */
        ADDITION,
        /**
         * an element was removed from the first collection.
         */
        REMOVAL,
        /**
         * an element was changed.
         */
        CHANGE
    }

    private InstantRunVerifier() {
    }

    // ASM API not generified.
    @SuppressWarnings("unchecked")
    @Nullable
    public static IncompatibleChange run(File original, File updated) throws IOException {

        ClassNode originalClass = loadClass(original);
        ClassNode updatedClass = loadClass(updated);

        if (!originalClass.superName.equals(updatedClass.superName)) {
            return IncompatibleChange.PARENT_CLASS_CHANGED;
        }

        if (diffList(originalClass.interfaces, updatedClass.interfaces,
                OBJECT_COMPARATOR) != Diff.NONE) {
            return IncompatibleChange.IMPLEMENTED_INTERFACES_CHANGE;
        }

        // ASM API here and below.
        //noinspection unchecked
        if (diffList(originalClass.visibleAnnotations,
                updatedClass.visibleAnnotations,
                ANNOTATION_COMPARATOR) != Diff.NONE) {
            return IncompatibleChange.CLASS_ANNOTATION_CHANGE;
        }

        IncompatibleChange fieldChange = verifyFields(originalClass, updatedClass);
        if (fieldChange != null) {
            return fieldChange;
        }

        return verifyMethods(originalClass, updatedClass);
    }

    @Nullable
    private static IncompatibleChange verifyFields(
            @NonNull ClassNode originalClass,
            @NonNull ClassNode updatedClass) {

        Diff diff = diffList(originalClass.fields, updatedClass.fields, new Comparator<FieldNode>() {

            @Override
            public boolean areEqual(@Nullable FieldNode first, @Nullable FieldNode second) {
                if ((first == null) && (second == null)) {
                    return true;
                }
                if (first == null || second == null) {
                    return true;
                }
                return first.name.equals(second.name) && first.desc.equals(second.desc)
                        && first.access == second.access;
            }
        });
        switch (diff) {
            case NONE:
                return null;
            case ADDITION:
                return IncompatibleChange.FIELD_ADDED;
            case REMOVAL:
                return IncompatibleChange.FIELD_REMOVED;
            case CHANGE:
                return IncompatibleChange.FIELD_TYPE_CHANGE;
            default:
                throw new RuntimeException("Unhandled action : " + diff);
        }
    }

    @Nullable
    private static IncompatibleChange verifyMethods(
            @NonNull ClassNode originalClass, @NonNull ClassNode updatedClass) {

        @SuppressWarnings("unchecked") // ASM API.
        List<MethodNode> nonVisitedMethodsOnUpdatedClass =
                new ArrayList<MethodNode>(updatedClass.methods);

        //noinspection unchecked
        for(MethodNode methodNode : (List<MethodNode>) originalClass.methods) {

            MethodNode updatedMethod = findMethod(updatedClass, methodNode.name, methodNode.desc);
            if (updatedMethod == null) {
                // although it's probably ok if a method got deleted since nobody should be calling
                // it anymore BUT the application might be using reflection to get the list of
                // methods and would still see the deleted methods. To be prudent, restart.
                // However, if the class initializer got removed, it's always fine.
                return methodNode.name.equals(AsmUtils.CLASS_INITIALIZER)
                        ? null
                        : IncompatibleChange.METHOD_DELETED;
            }

            // remove the method from the visited ones on the updated class.
            nonVisitedMethodsOnUpdatedClass.remove(updatedMethod);

            IncompatibleChange change = methodNode.name.equals(AsmUtils.CLASS_INITIALIZER)
                    ? visitClassInitializer(methodNode, updatedMethod)
                    : verifyMethod(methodNode, updatedMethod);

            if (change!=null) {
                return change;
            }
        }

        if (!nonVisitedMethodsOnUpdatedClass.isEmpty()) {
            return IncompatibleChange.METHOD_ADDED;
        }
        return null;
    }

    @Nullable
    private static IncompatibleChange visitClassInitializer(MethodNode originalClassInitializer,
            MethodNode updateClassInitializer) {

        return METHOD_COMPARATOR.areEqual(originalClassInitializer, updateClassInitializer)
                ? null
                : IncompatibleChange.STATIC_INITIALIZER_CHANGE;
    }

    @Nullable
    private static IncompatibleChange verifyMethod(
            MethodNode methodNode,
            MethodNode updatedMethod) {

        //noinspection unchecked
        if (diffList(methodNode.visibleAnnotations,
                updatedMethod.visibleAnnotations,
                new AnnotationNodeComparator()) != Diff.NONE) {
            return IncompatibleChange.METHOD_ANNOTATION_CHANGE;
        }

        // if the method content has changed, better not used any of our black listed APIs.
        return METHOD_COMPARATOR.areEqual(methodNode, updatedMethod)
                ? null
                : InstantRunMethodVerifier.verifyMethod(updatedMethod);
    }

    @Nullable
    private static MethodNode findMethod(@NonNull ClassNode classNode,
            @NonNull  String name,
            @Nullable String desc) {

        //noinspection unchecked
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {

            if (methodNode.name.equals(name) &&
                    ((desc == null && methodNode.desc == null) || (methodNode.desc.equals(desc)))) {
                return methodNode;
            }
        }
        return null;
    }

    private interface Comparator<T> {
        boolean areEqual(@Nullable T first, @Nullable T second);
    }

    private static class MethodNodeComparator implements Comparator<MethodNode> {

        @Override
        public boolean areEqual(@Nullable  MethodNode first, @Nullable MethodNode second) {
            if (first==null && second==null) {
                return true;
            }
            if (first==null || second==null) {
                return false;
            }
            if (!first.name.equals(second.name) || !first.desc.equals(second.desc)) {
                return false;
            }
            VerifierTextifier firstMethodTextifier = new VerifierTextifier();
            VerifierTextifier secondMethodTextifier = new VerifierTextifier();
            first.accept(new TraceMethodVisitor(firstMethodTextifier));
            second.accept(new TraceMethodVisitor(secondMethodTextifier));

            StringWriter firstText = new StringWriter();
            StringWriter secondText = new StringWriter();
            firstMethodTextifier.print(new PrintWriter(firstText));
            secondMethodTextifier.print(new PrintWriter(secondText));

            return firstText.toString().equals(secondText.toString());
        }
    }

    /**
     * Subclass of {@link Textifier} that will pretty print method bytecodes but will swallow the
     * line numbers notification as it is not pertinent for the InstantRun hot swapping.
     */
    private static class VerifierTextifier extends Textifier {

        protected VerifierTextifier() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visitLineNumber(int i, Label label) {
            // don't care about line numbers
        }
    }

    public static class AnnotationNodeComparator implements Comparator<AnnotationNode> {

        @Override
        public boolean areEqual(@Nullable AnnotationNode first, @Nullable  AnnotationNode second) {
            // probably deep compare for values...
            //noinspection unchecked
            return (first == null && second == null) || (first != null && second != null)
                && (OBJECT_COMPARATOR.areEqual(first.desc, second.desc) &&
                    diffList(first.values, second.values, OBJECT_COMPARATOR) == Diff.NONE);
        }
    }

    @NonNull
    public static <T> Diff diffList(
            @Nullable List<T> one,
            @Nullable List<T> two,
            @NonNull  Comparator<T> comparator) {

        if (one == null && two == null) {
            return Diff.NONE;
        }
        if (one == null) {
            return Diff.ADDITION;
        }
        if (two == null) {
            return Diff.REMOVAL;
        }
        List<T> copyOfOne = new ArrayList<T>(one);
        for (T elementOfTwo : two) {
            T elementOfCopyOfOne = getElementOf(copyOfOne, elementOfTwo, comparator);
            if (elementOfCopyOfOne != null) {
                copyOfOne.remove(elementOfCopyOfOne);
            }
        }

        for (T elementOfOne : one) {
            T elementOfTwo = getElementOf(two, elementOfOne, comparator);
            if (elementOfTwo != null) {
                two.remove(elementOfTwo);
            }
        }
        if ((!copyOfOne.isEmpty()) && (copyOfOne.size() == two.size())) {
            return Diff.CHANGE;
        }
        if (!copyOfOne.isEmpty()) {
            return Diff.REMOVAL;
        }
        return two.isEmpty() ? Diff.NONE : Diff.ADDITION;
    }

    @Nullable
    public static <T> T getElementOf(List<T> list, T element, Comparator<T> comparator) {
        for (T elementOfList : list) {
            if (comparator.areEqual(elementOfList, element)) {
                return elementOfList;
            }
        }
        return null;
    }

    static ClassNode loadClass(File classFile) throws IOException {
        byte[] classBytes;
        classBytes = Files.toByteArray(classFile);
        ClassReader classReader = new ClassReader(classBytes);

        org.objectweb.asm.tree.ClassNode classNode = new org.objectweb.asm.tree.ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }
}
