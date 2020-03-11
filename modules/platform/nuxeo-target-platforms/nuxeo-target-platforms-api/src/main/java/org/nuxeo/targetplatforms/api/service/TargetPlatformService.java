/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.service;

import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformFilter;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

/**
 * Service for target platforms and packages management.
 *
 * @since 5.7.1
 */
public interface TargetPlatformService {

    /**
     * Returns the override directory name.
     */
    String getOverrideDirectory();

    /**
     * Returns the default target platform.
     * <p>
     * If several target platforms are found for given filter, the first one is returned (ordered alphabetically on id).
     */
    TargetPlatform getDefaultTargetPlatform(TargetPlatformFilter filter);

    /**
     * Returns the target platform with given id, or null if not found.
     */
    TargetPlatform getTargetPlatform(String id);

    /**
     * Returns the target platform info for given id, or null if not found.
     */
    TargetPlatformInfo getTargetPlatformInfo(String id);

    /**
     * Returns the target package with given id, or null if not found.
     */
    TargetPackage getTargetPackage(String id);

    /**
     * Returns the target package info for given id, or null if not found.
     */
    TargetPackageInfo getTargetPackageInfo(String id);

    /**
     * Returns a target platform instance with given id and given enabled packages, or null if not found.
     * <p>
     * Ignore target packages that would not be found.
     */
    TargetPlatformInstance getTargetPlatformInstance(String id, List<String> packages);

    /**
     * Returns all target platforms matching given criteria.
     *
     * @param filter the filter to apply, can be null if no filtering is needed.
     * @see TargetPlatformFilter
     */
    List<TargetPlatform> getAvailableTargetPlatforms(TargetPlatformFilter filter);

    /**
     * Returns all target platforms info matching given criteria.
     *
     * @param filter the filter to apply, can be null if no filtering is needed.
     * @see TargetPlatformFilter
     */
    List<TargetPlatformInfo> getAvailableTargetPlatformsInfo(TargetPlatformFilter filter);

    /**
     * Deprecates the target platform if given boolean is true (or un-deprecates it if boolean is false), overriding the
     * default value from extension points and adding an entry in the override directory.
     */
    void deprecateTargetPlatform(boolean deprecate, String id);

    /**
     * Enables the target platform if given boolean is true (or disables it boolean is false), overriding the default
     * value from extension points and adding an entry in the override directory.
     */
    void enableTargetPlatform(boolean enable, String id);

    /**
     * Restricts the target platform if given boolean is true (or un-restricts it if boolean is false), overriding the
     * default value from extension points and adding an entry in the override directory.
     */
    void restrictTargetPlatform(boolean restrict, String id);

    /**
     * Set the target platform as trial if given boolean is true (or unset it as trial if boolean is false), overriding
     * the default value from extension points and adding an entry in the override directory.
     */
    void setTrialTargetPlatform(boolean trial, String id);

    /**
     * Set the target platform as default if given boolean is true (or unset it as default if boolean is false),
     * overriding the default value from extension points and adding an entry in the override directory.
     */
    void setDefaultTargetPlatform(boolean isDefault, String id);

    /**
     * Removes overrides for this target platform.
     */
    void restoreTargetPlatform(String id);

    /**
     * Removes overrides for all target platform.
     */
    void restoreAllTargetPlatforms();

    /**
     * @return the default target platform instance and enabled if not found.
     * @since 5.9.3-NXP-15602
     */
    TargetPlatformInstance getDefaultTargetPlatformInstance(boolean restricted);

}
