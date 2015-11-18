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

package com.android.sdklib.repositoryv2.meta;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.Revision;
import com.android.repository.impl.meta.TypeDetails;
import com.android.sdklib.repositoryv2.IdDisplay;
import com.android.repository.impl.meta.RevisionType;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Container for the subclasses of {@link TypeDetails} used by the android SDK.
 * Concrete classes are generated by xjc.
 */
public final class DetailsTypes {

    private DetailsTypes() {}

    /**
     * Common methods shared by all android version-specific details types.
     */
    public interface ApiDetailsType {

        /**
         * Sets the api level this package corresponds to.
         */
        void setApiLevel(int apiLevel);

        /**
         * Gets the api level of this package.
         */
        int getApiLevel();

        /**
         * If this is a preview release the api is identified by a codename in addition to the api
         * level. In this case {@code codename} should be non-null.
         */
        void setCodename(@Nullable String codename);

        /**
         * Gets the codename of this release. Should be {@code null} for regular releases, and non-
         * null for preview releases.
         */
        String getCodename();
    }

    /**
     * Trivial details type for source packages.
     */
    @XmlTransient
    public interface SourceDetailsType extends ApiDetailsType {}

    /**
     * Trivial details type for build tools packages.
     */
    @XmlTransient
    public interface BuildToolDetailsType {}

    /**
     * Trivial details type for doc packages.
     */
    @XmlTransient
    public interface DocDetailsType {}

    /**
     * Trivial details type for ndk packages.
     */
    @XmlTransient
    public interface NdkDetailsType {}

    /**
     * Details type for platform packages. Contains info on the layout lib version provided.
     */
    @XmlTransient
    public interface PlatformDetailsType extends ApiDetailsType {

        void setLayoutlib(@NonNull LayoutlibType layoutLib);

        @NonNull
        LayoutlibType getLayoutlib();

        /**
         * Parent class for xjc-generated classes containing info on the layout lib version.
         */
        @XmlTransient
        abstract class LayoutlibType {

            public abstract void setApi(int api);

            public abstract int getApi();

            public void setRevision(@Nullable RevisionType revision) {
                // Stub
            }

            /**
             * Convenience method to get the revision as a {@link Revision}.
             */
            @Nullable
            public Revision getPreciseRevision() {
                return getRevision() != null ? getRevision().toRevision() : null;
            }

            @Nullable
            protected RevisionType getRevision() {
                // Stub
                return null;
            }
        }
    }

    /**
     * Trivial details type for platform-tool packages.
     */
    @XmlTransient
    public interface PlatformToolDetailsType {}

    /**
     * Trivial details type for tool packages.
     */
    @XmlTransient
    public interface ToolDetailsType {}

    /**
     * Details type for extra packages. Includes a {@link IdDisplay} for the vendor.
     */
    @XmlTransient
    public interface ExtraDetailsType {
        void setVendor(@NonNull IdDisplay vendor);

        @NonNull
        IdDisplay getVendor();
    }

    /**
     * Details type for addon packages. Includes a {@link IdDisplay} for the vendor.
     */
    @XmlTransient
    public interface AddonDetailsType extends ApiDetailsType {
        void setVendor(@NonNull IdDisplay vendor);

        @NonNull
        IdDisplay getVendor();
    }

    /**
     * Details type for system images packages. Includes information on the abi (architecture),
     * tag (device type), and vendor.
     */
    @XmlTransient
    public interface SysImgDetailsType extends ApiDetailsType {
        void setAbi(@NonNull String abi);

        void setTag(@NonNull IdDisplay tag);

        void setVendor(@Nullable IdDisplay vendor);
    }
}
