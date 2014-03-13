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


/**
 * Represents a target (platform or package).
 *
 * @since 5.7.1
 */
public interface Target extends TargetInfo {

    /**
     * Returns true if this target reference version is strictly before given
     * version.
     */
    boolean isStrictlyBeforeVersion(String version);

    /**
     * Returns true if this target reference version is or is after given
     * version.
     */
    boolean isAfterVersion(String version);

    /**
     * Returns true if this target reference version matches given version.
     */
    boolean isVersion(String version);

    /**
     * Returns true if this target reference version is strictly before given
     * version.
     */
    boolean isStrictlyBeforeVersion(Target version);

    /**
     * Returns true if this target reference version is or is after given
     * version.
     */
    boolean isAfterVersion(Target version);

    /**
     * Returns true if this target reference version matches given version.
     */
    boolean isVersion(Target version);

}