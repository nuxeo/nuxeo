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
 * Main interface for target platform instance, holding resources that need to be available on client sides.
 *
 * @author Anahide Tchertchian
 * @since 5.7.1
 */
public interface TargetPlatformInstance extends Target {

    /**
     * Return the list of ids of packages enabled on this target platform.
     */
    List<String> getEnabledPackagesIds();

    /**
     * Return the list of packages enabled on this target platform.
     */
    Map<String, TargetPackage> getEnabledPackages();

    /**
     * Checks if there is any enabled package with the name packageName.
     */
    boolean hasEnabledPackageWithName(String packageName);

    /**
     * Returns this target platform parent or null if there is no inheritance.
     */
    TargetPlatform getParent();

}
