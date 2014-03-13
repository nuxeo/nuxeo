/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api;

import java.util.List;
import java.util.Map;

/**
 * Represents a target platform info, useful for listing of available target
 * platforms.
 *
 * @since 2.18
 */
public interface TargetPlatformInfo extends TargetInfo,
        Comparable<TargetPlatformInfo> {

    /**
     * Return the list of ids of packages available on this target platform.
     */
    List<String> getAvailablePackagesIds();

    /**
     * Return the map of packages available on this target platform.
     */
    Map<String, TargetPackageInfo> getAvailablePackagesInfo();

    /**
     * Add a package to the list of available packages.
     */
    void addAvailablePackageInfo(TargetPackageInfo pack);

    /**
     * Sets available packages on this target platform.
     */
    void setAvailablePackagesInfo(Map<String, TargetPackageInfo> packages);

}
