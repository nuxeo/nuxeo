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
package org.nuxeo.targetplatforms.api;

import java.util.List;
import java.util.Map;

/**
 * Represents a target platform info, useful for listing of available target
 * platforms.
 *
 * @since 5.7.1
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
