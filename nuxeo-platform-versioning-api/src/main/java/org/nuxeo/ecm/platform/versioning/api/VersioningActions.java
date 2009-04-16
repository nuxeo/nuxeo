/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.api;

import java.io.Serializable;

/**
 * Defines actions to be taken in a document versioning incrementation process.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public enum VersioningActions implements Serializable {

    ACTION_UNDEFINED("undefined"),
    ACTION_AUTO_INCREMENT("auto_inc"),
    ACTION_NO_INCREMENT("no_inc"),
    ACTION_INCREMENT_MINOR("inc_minor"),
    ACTION_INCREMENT_MAJOR("inc_major"),
    ACTION_INCREMENT_DEFAULT("inc_default"),
    ACTION_CASE_DEPENDENT("ask_user"),
    NO_VERSIONING("no_ver_doctype");

    public static final String KEY_FOR_INC_OPTION = "VersioningOption";

    public static final String SKIP_VERSIONING = "SKIP_VERSIONING";

    private final String name;

    /**
     * @deprecated an enum should not hold a state value: it is reset when
     *             serializing it.
     */
    @Deprecated
    private boolean isDefault;

    VersioningActions(String name) {
        this.name = name;
        this.isDefault = false;
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
     * @deprecated an enum should not hold a state value: it is reset when
     *             serializing it, see NXP-2516.
     */
    @Deprecated
    public boolean isDefault() {
        return this.isDefault;
    }

    /**
     * @deprecated an enum should not hold a state value: it is reset when
     *             serializing it, see NXP-2516.
     */
    @Deprecated
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return name;
    }

}
