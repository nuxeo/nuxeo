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
package org.nuxeo.theme.styling.service;

import java.net.URL;
import java.util.List;

import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.Logo;

/**
 * Service handling the mapping between a theme page and its resources (styling
 * and flavors)
 * <p>
 * Registers corresponding contributions to the {@link ThemeService} so that
 * styling of the page is handled as if styling was provided by the theme
 * definition. Also handles related flavors as theme collections.
 *
 * @see Flavor
 * @since 5.5
 */
public interface ThemeStylingService {

    public static final String FLAVOR_MARKER = "__FLAVOR__";

    public static final String PAGE_STYLE_CLASS_NAME_PREFIX = " CSS";

    public static final String PAGE_STYLE_NAME_SUFFIX = " Page Styles";

    public static enum PRESET_CATEGORY {
        background, border, color, font;
    }

    /**
     * Returns the default flavor for a given theme page
     */
    String getDefaultFlavorName(String themePage);

    /**
     * Returns the flavor names for a given theme page
     */
    List<String> getFlavorNames(String themePage);

    /**
     * Returns the flavors for a given theme page
     */
    List<Flavor> getFlavors(String themePage);

    /**
     * Returns the flavor for given name, or null if not found.
     * <p>
     * If not defined on the local flavor, flavor attributes will be resolved
     * from the extended flavor if any (except presets that just need to be
     * resolved at registration to the {@link ThemeService}.
     * </p>
     *
     * @param flavorName
     */
    Flavor getFlavor(String flavorName);

    /**
     * Returns the logo configured for given flavor name, and fallbacks on the
     * extends flavor logo if not set.
     *
     * @param flavor
     */
    Logo getLogo(String flavor);

    /**
     * Hook to notify the service that a theme has been registered
     *
     * @param themeName
     */
    void themeRegistered(String themeName);

    /**
     * Hook to notify the service that global resources for a theme need to be
     * registered
     *
     * @param themeUrl
     */
    void themeGlobalResourcesRegistered(URL themeUrl);

}