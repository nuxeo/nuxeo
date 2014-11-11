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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: BuiltinModes.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

/**
 * List of built-in modes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class BuiltinModes {

    public static final String ANY = "any";

    public static final String VIEW = "view";

    public static final String EDIT = "edit";

    public static final String BULK_EDIT = "bulkEdit";

    public static final String CREATE = "create";

    public static final String SEARCH = "search";

    public static final String LISTING = "listing";

    public static final String SUMMARY = "summary";

    private BuiltinModes() {
    }

    /**
     * Returns true if given layout mode is mapped by default to the edit
     * widget mode.
     */
    public static final boolean isBoundToEditMode(String layoutMode) {
        if (layoutMode != null
                && (layoutMode.startsWith(BuiltinModes.CREATE)
                        || layoutMode.startsWith(BuiltinModes.EDIT)
                        || layoutMode.startsWith(BuiltinModes.SEARCH) || layoutMode.startsWith(BuiltinModes.BULK_EDIT))) {
            return true;
        }
        return false;
    }

}
