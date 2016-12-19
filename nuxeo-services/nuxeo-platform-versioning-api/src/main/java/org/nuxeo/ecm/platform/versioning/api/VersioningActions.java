/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.versioning.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Defines actions to be taken in a document versioning increment process.
 * <p>
 * Used by UI.
 */
public enum VersioningActions implements Serializable {

    ACTION_NO_INCREMENT("no_inc", VersioningOption.NONE), //
    ACTION_INCREMENT_MINOR("inc_minor", VersioningOption.MINOR), //
    ACTION_INCREMENT_MAJOR("inc_major", VersioningOption.MAJOR);

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
     * Returns the corresponding core versioning option for this UI versioning action.
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
