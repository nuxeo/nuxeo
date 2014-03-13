/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.service;

import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;


/**
 * Service for target platforms and packages management.
 *
 * @since 2.18
 */
public interface TargetPlatformService {

    /**
     * Returns the default target platform.
     */
    TargetPlatform getDefaultTargetPlatform();

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
     * Returns a target platform instance with given id and given enabled
     * packages, or null if not found.
     * <p>
     * Ignore target packages that would not be found.
     */
    TargetPlatformInstance getTargetPlatformInstance(String id,
            List<String> packages);

    /**
     * Returns all target platforms matching given criteria.
     *
     * @param filterDeprecated true if deprecated target platforms should be
     *            filtered from the resulting list.
     * @param filterRestricted true if restricted target platforms should be
     *            filtered from the resulting list.
     * @param type null if no filtering should be done, otherwise filters all
     *            target platforms that would not hold this type.
     */
    List<TargetPlatform> getAvailableTargetPlatforms(boolean filterDeprecated,
            boolean filterRestricted, String type);

    /**
     * Returns all target platforms info matching given criteria.
     */
    List<TargetPlatformInfo> getAvailableTargetPlatformsInfo(
            boolean filterDeprecated, boolean filterRestricted, String type);

}
