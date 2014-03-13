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
 * Main interface for target platform instance, holding resources that need to
 * be available on client sides.
 *
 * @author Anahide Tchertchian
 * @since 2.18
 */
public interface TargetPlatformInstance extends Target {

    /**
     * Returns true if given target platform is a fast track.
     */
    boolean isFastTrack();

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
