/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.service;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
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
     * If several target platforms are found for given filter, the first one is
     * returned (ordered alphabetically on id).
     */
    TargetPlatform getDefaultTargetPlatform(TargetPlatformFilter filter)
            throws ClientException;

    /**
     * Returns the target platform with given id, or null if not found.
     */
    TargetPlatform getTargetPlatform(String id) throws ClientException;

    /**
     * Returns the target platform info for given id, or null if not found.
     */
    TargetPlatformInfo getTargetPlatformInfo(String id) throws ClientException;

    /**
     * Returns the target package with given id, or null if not found.
     */
    TargetPackage getTargetPackage(String id);

    /**
     * Returns the target package info for given id, or null if not found.
     */
    TargetPackageInfo getTargetPackageInfo(String id);

    /**
     * Returns a target platform instance with given id and given enabled
     * packages, or null if not found.
     * <p>
     * Ignore target packages that would not be found.
     */
    TargetPlatformInstance getTargetPlatformInstance(String id,
            List<String> packages) throws ClientException;

    /**
     * Returns all target platforms matching given criteria.
     *
     * @param filter the filter to apply, can be null if no filtering is
     *            needed.
     * @see TargetPlatformFilter
     */
    List<TargetPlatform> getAvailableTargetPlatforms(TargetPlatformFilter filter)
            throws ClientException;

    /**
     * Returns all target platforms info matching given criteria.
     *
     * @param filter the filter to apply, can be null if no filtering is
     *            needed.
     * @see TargetPlatformFilter
     */
    List<TargetPlatformInfo> getAvailableTargetPlatformsInfo(
            TargetPlatformFilter filter) throws ClientException;

    /**
     * Deprecates the target platform if given boolean is true (or
     * un-deprecates it if boolean is false), overriding the default value from
     * extension points and adding an entry in the override directory.
     */
    void deprecateTargetPlatform(boolean deprecate, String id)
            throws ClientException;

    /**
     * Enables the target platform if given boolean is true (or disables it
     * boolean is false), overriding the default value from extension points
     * and adding an entry in the override directory.
     */
    void enableTargetPlatform(boolean enable, String id) throws ClientException;

    /**
     * Restricts the target platform if given boolean is true (or un-restricts
     * it if boolean is false), overriding the default value from extension
     * points and adding an entry in the override directory.
     */
    void restrictTargetPlatform(boolean restrict, String id)
            throws ClientException;

    /**
     * Set the target platform as trial if given boolean is true (or unset it
     * as trial if boolean is false), overriding the default value from
     * extension points and adding an entry in the override directory.
     */
    void setTrialTargetPlatform(boolean trial, String id)
            throws ClientException;

    /**
     * Set the target platform as default if given boolean is true (or unset it
     * as default if boolean is false), overriding the default value from
     * extension points and adding an entry in the override directory.
     */
    void setDefaultTargetPlatform(boolean isDefault, String id)
            throws ClientException;

    /**
     * Removes overrides for this target platform.
     */
    void restoreTargetPlatform(String id) throws ClientException;

    /**
     * Removes overrides for all target platform.
     */
    void restoreAllTargetPlatforms() throws ClientException;

    /**
     * @return the default target platform instance and enabled if not found.
     *
     * @since 5.9.3-NXP-15602
     */
    TargetPlatformInstance getDefaultTargetPlatformInstance(boolean restricted)
            throws ClientException;

}