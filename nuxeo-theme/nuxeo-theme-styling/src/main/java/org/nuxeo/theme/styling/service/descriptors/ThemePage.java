/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.descriptors;

import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to associate resources to a theme page
 *
 * @since 5.5
 * @deprecated since 7.3, use {@link Page} instead.
 */
@XObject("themePage")
public class ThemePage extends Page {

    public static String getPageName(String themePage) {
        if ("*".equals(themePage)) {
            return "*";
        }
        try {
            String[] nameEl = themePage.split("/");
            return nameEl[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(String.format("Invalid theme page name '%s': cannot retrieve page name",
                    themePage));
        }
    }

    public static String getThemeName(String themePage) {
        if ("*".equals(themePage)) {
            return "*";
        }
        try {
            String[] nameEl = themePage.split("/");
            return nameEl[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(String.format("Invalid theme page name '%s': cannot retrieve theme name",
                    themePage));
        }
    }

}
