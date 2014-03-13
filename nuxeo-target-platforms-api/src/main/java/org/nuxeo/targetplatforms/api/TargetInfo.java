/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Represents a target info (platform or package).
 * <p>
 * This is a lighter version of the target platform or package, useful for
 * listing targets in select inputs for instance.
 *
 * @since 2.18
 */
public interface TargetInfo extends Serializable {

    /**
     * This target unique identifier (usually some kind of concatenation of
     * name and version).
     */
    String getId();

    /**
     * The target platform name, for instance "cap".
     */
    String getName();

    /**
     * The target platform version, for instance "5.8".
     */
    String getVersion();

    /**
     * The target platform reference version used for behaviour checks.
     * <p>
     * Defaults to {@link #getVersion()} when not set.
     */
    String getRefVersion();

    /**
     * A user-friendly label for this platform, like "Nuxeo Platform 5.8".
     */
    String getLabel();

    /**
     * String marker for a dev/deprecated/new status.
     */
    String getStatus();

    /**
     * Returns true if the corresponding target platform is enabled.
     */
    boolean isEnabled();

    /**
     * Returns true if access to the corresponding target platform is
     * restricted.
     * <p>
     * Criteria for which access should be granted or not are left to the
     * caller.
     */
    boolean isRestricted();

    /**
     * Returns true if given target is deprecated.
     */
    boolean isDeprecated();

    /**
     * Returns this target release date.
     */
    Calendar getReleaseDate();

    /**
     * Returns this target end of availability date.
     */
    Calendar getEndOfAvailability();

    /**
     * Returns this target download link.
     */
    String getDownloadLink();

    /**
     * Returns a description for this target.
     * <p>
     * Can contain HTML code.
     */
    String getDescription();

}