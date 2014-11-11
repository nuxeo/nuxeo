package org.nuxeo.theme.styling.service;

import org.nuxeo.theme.services.ThemeService;

/**
 * Service handling the mapping between a theme page and its resources (styling
 * and flavours)
 * <p>
 * Registers corresponding contributions to the {@link ThemeService} so that
 * styling of the page is handled as if styling was provided by the theme
 * definition. Also handles related flavours as theme collections.
 *
 * @since 5.4.3
 */
public interface ThemeStylingService {

    /**
     * Returns the default flavour for a given theme page
     */
    String getDefaultFlavour(String themePage);

    /**
     * Hook to notify the service that a theme has been registered
     *
     * @param themeName
     */
    void themeRegistered(String themeName);

}