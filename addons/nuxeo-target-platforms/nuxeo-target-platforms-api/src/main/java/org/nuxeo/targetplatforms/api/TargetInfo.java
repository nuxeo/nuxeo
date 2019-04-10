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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Represents a target info (platform or package).
 * <p>
 * This is a lighter version of the target platform or package, useful for listing targets in select inputs for
 * instance.
 *
 * @since 5.7.1
 */
public interface TargetInfo extends Serializable {

    /**
     * This target unique identifier (usually some kind of concatenation of name and version).
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
     * Returns true if access to the corresponding target platform is restricted.
     * <p>
     * Criteria for which access should be granted or not are left to the caller.
     */
    boolean isRestricted();

    /**
     * Returns true if given target is deprecated.
     */
    boolean isDeprecated();

    /**
     * Returns true if given target information is available for trials.
     */
    boolean isTrial();

    /**
     * Returns true if given target information is marked as default.
     */
    boolean isDefault();

    /**
     * Returns true if given target platform is a fast track
     */
    boolean isFastTrack();

    /**
     * Returns true if given target information is overridden by directory information.
     */
    boolean isOverridden();

    /**
     * Returns this target release date.
     */
    Date getReleaseDate();

    /**
     * Returns this target end of availability date.
     */
    Date getEndOfAvailability();

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

    /**
     * String markers for feature/behaviour checks on this instance.
     */
    List<String> getTypes();

    /**
     * Returns true if given type is in the list of this target types.
     */
    boolean matchesType(String type);

}