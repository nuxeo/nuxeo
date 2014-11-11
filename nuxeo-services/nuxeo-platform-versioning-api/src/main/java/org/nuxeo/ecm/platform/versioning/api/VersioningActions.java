/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;

/**
 * Defines actions to be taken in a document versioning increment process.
 * <p>
 * Used by UI.
 */
public enum VersioningActions implements Serializable {

    ACTION_NO_INCREMENT("no_inc", VersioningOption.NONE), //
    ACTION_INCREMENT_MINOR("inc_minor", VersioningOption.MINOR), //
    ACTION_INCREMENT_MAJOR("inc_major", VersioningOption.MAJOR);

    public static final String KEY_FOR_INC_OPTION = VersioningDocument.KEY_FOR_INC_OPTION;

    /**
     * @deprecated use
     *             {@link org.nuxeo.ecm.core.versioning.VersioningService#SKIP_VERSIONING}
     *             instead
     */
    @Deprecated
    public static final String SKIP_VERSIONING = "SKIP_VERSIONING";

    private final String name;

    /**
     * Equivalent core increment option.
     *
     * @since 5.7.3
     */
    private final VersioningOption vo;

    VersioningActions(String name, VersioningOption vo) {
        this.name = name;
        this.vo = vo;
    }

    @Override
    public String toString() {
        return name;
    }

    public VersioningOption getVersioningOption() {
        return vo;
    }

    public static VersioningActions getByActionName(String actionName) {
        for (VersioningActions va : VersioningActions.values()) {
            if (va.toString().equals(actionName)) {
                return va;
            }
        }
        return null;
    }

    /**
     * Returns the corresponding core versioning option for this UI versioning
     * action.
     *
     * @since 5.7.3
     */
    public static VersioningActions getByVersioningOption(VersioningOption vo) {
        for (VersioningActions va : VersioningActions.values()) {
            if (va.getVersioningOption().equals(vo)) {
                return va;
            }
        }
        return null;
    }

}
