/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.descriptors;

import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to associate resources to a theme page
 *
 * @since 5.5
 * @deprecated since 7.4, use {@link PageDescriptor} instead.
 */
@Deprecated
@XObject("themePage")
public class ThemePage extends PageDescriptor {

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
