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

/**
 * Represents a target (platform or package).
 *
 * @since 2.18
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

    /**
     * String markers for feature/behaviour checks on this instance.
     */
    List<String> getTypes();

    /**
     * Returns true if given type is in the list of this target types.
     */
    boolean matchesType(String type);

}