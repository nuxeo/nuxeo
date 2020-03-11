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
package org.nuxeo.targetplatforms.api;

import java.util.List;
import java.util.Map;

/**
 * Represents a target platform info, useful for listing of available target platforms.
 *
 * @since 5.7.1
 */
public interface TargetPlatformInfo extends TargetInfo, Comparable<TargetPlatformInfo> {

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
