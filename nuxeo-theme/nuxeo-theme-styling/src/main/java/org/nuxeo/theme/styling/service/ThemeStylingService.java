package org.nuxeo.theme.styling.service;

import org.nuxeo.theme.services.ThemeService;

/**
 * Service handling the mapping between a theme page and its resources (styling
 * and flavors)
 * <p>
 * Registers corresponding contributions to the {@link ThemeService} so that
 * styling of the page is handled as if styling was provided by the theme
 * definition. Also handles related flavors as theme collections.
 *
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
    String getDefaultFlavor(String themePage);

    /**
     * Hook to notify the service that a theme has been registered
     *
     * @param themeName
     */
    void themeRegistered(String themeName);

}
