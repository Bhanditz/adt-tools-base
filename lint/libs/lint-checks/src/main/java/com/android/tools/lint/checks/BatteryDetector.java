/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_ACTION;
import static com.android.SdkConstants.TAG_RECEIVER;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Checks looking for issues that negatively affect battery life
 */
public class BatteryDetector extends ResourceXmlDetector implements
        JavaPsiScanner {

    @SuppressWarnings("unchecked")
    public static final Implementation IMPLEMENTATION = new Implementation(
            BatteryDetector.class,
            EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE),
            Scope.MANIFEST_SCOPE,
            Scope.JAVA_FILE_SCOPE);

    /** Issues that negatively affect battery life */
    public static final Issue ISSUE = Issue.create(
            "BatteryLife", //$NON-NLS-1$
            "Battery Life Issues",

            "This issue flags code that either\n" +
            "* negatively affects battery life, or\n" +
            "* uses APIs that have recently changed behavior to prevent background tasks " +
            "from consuming memory and battery excessively.\n" +
            "\n" +
            "Generally, you should be using `JobScheduler` or `GcmNetworkManager` instead.\n" +
            "\n" +
            "For more details on how to update your code, please see" +
            "http://developer.android.com/preview/features/background-optimization.html",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo("http://developer.android.com/preview/features/background-optimization.html");

    /** Constructs a new {@link BatteryDetector} */
    public BatteryDetector() {
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_ACTION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        assert element.getTagName().equals(TAG_ACTION);
        Attr attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
        if (attr == null) {
            return;
        }
        String name = attr.getValue();
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(name)
                && element.getParentNode() != null
                && element.getParentNode().getParentNode() != null
                && TAG_RECEIVER.equals(element.getParentNode().getParentNode().getNodeName())
                && context.getMainProject().getTargetSdkVersion().getFeatureLevel() >= 24) {
            String message = "Declaring a broadcastreceiver for "
                + "`android.net.conn.CONNECTIVITY_CHANGE` is deprecated for apps targeting "
                + "N and higher. In general, apps should not rely on this broadcast and "
                + "instead use `JobScheduler` or `GCMNetworkManager`.";
            context.report(ISSUE, element, context.getValueLocation(attr), message);
        }

        if ("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS".equals(name)
                && context.getMainProject().getTargetSdkVersion().getFeatureLevel() >= 23) {
            String message = getBatteryOptimizationsErrorMessage();
            context.report(ISSUE, element, context.getValueLocation(attr), message);
        }

        if ("android.hardware.action.NEW_PICTURE".equals(name)
                || "android.hardware.action.NEW_VIDEO".equals(name)
                || "com.android.camera.NEW_PICTURE".equals(name)) {
            String message = String.format("Use of %1$s is deprecated for all apps starting "
                    + "with the N release independent of the target SDK. Apps should not "
                    + "rely on these broadcasts and instead use `JobScheduler`", name);
            context.report(ISSUE, element, context.getValueLocation(attr), message);
        }
    }

    @Nullable
    @Override
    public List<String> getApplicableReferenceNames() {
        return Collections.singletonList("ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
    }

    @Override
    public void visitReference(@NonNull JavaContext context, @Nullable JavaElementVisitor visitor,
            @NonNull PsiJavaCodeReferenceElement reference, @NonNull PsiElement resolved) {
        if (resolved instanceof PsiField &&
                context.getEvaluator().isMemberInSubClassOf((PsiField)resolved,
                        "android.provider.Settings", false)
                && context.getMainProject().getTargetSdkVersion().getFeatureLevel() >= 23) {
            String message = getBatteryOptimizationsErrorMessage();
            context.report(ISSUE, reference, context.getNameLocation(reference), message);
        }
    }

    @NonNull
    private static String getBatteryOptimizationsErrorMessage() {
        return "Use of `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` violates the "
                + "Play Store Content Policy regarding acceptable use cases, as described in "
                + "http://developer.android.com/training/monitoring-device-state/doze-standby.html";
    }
}
