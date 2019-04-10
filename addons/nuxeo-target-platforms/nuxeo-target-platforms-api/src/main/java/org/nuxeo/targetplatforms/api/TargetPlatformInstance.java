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
