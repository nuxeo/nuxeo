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

/**
 * Represents a target (platform or package).
 *
 * @since 5.7.1
 */
public interface Target extends TargetInfo {

    /**
     * Returns true if this target reference version is strictly before given version.
     */
    boolean isStrictlyBeforeVersion(String version);

    /**
     * Returns true if this target reference version is or is after given version.
     */
    boolean isAfterVersion(String version);

    /**
     * Returns true if this target reference version matches given version.
     */
    boolean isVersion(String version);

    /**
     * Returns true if this target reference version is strictly before given version.
     */
    boolean isStrictlyBeforeVersion(Target version);

    /**
     * Returns true if this target reference version is or is after given version.
     */
    boolean isAfterVersion(Target version);

    /**
     * Returns true if this target reference version matches given version.
     */
    boolean isVersion(Target version);

}
