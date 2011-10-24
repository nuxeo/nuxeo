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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.presets.PaletteType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.styling.service.descriptors.Flavour;
import org.nuxeo.theme.styling.service.descriptors.FlavourPresets;
import org.nuxeo.theme.styling.service.descriptors.SimpleStyle;
import org.nuxeo.theme.styling.service.descriptors.ThemePage;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

/**
 * Default implementation for the {@link ThemeStylingService}
 *
 * @since 5.4.3
 */
public class ThemeStylingServiceImpl extends DefaultComponent implements
        ThemeStylingService {

    private static final Log log = LogFactory.getLog(ThemeStylingServiceImpl.class);

    // TODO: use ContributionFragmentRegistry instances for hot reload support
    protected Map<String, ThemePage> themePageResources = new HashMap<String, ThemePage>();

    protected Map<String, Flavour> themePageFlavours = new HashMap<String, Flavour>();

    protected Map<String, SimpleStyle> themePageStyles = new HashMap<String, SimpleStyle>();

    // Runtime Component API

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        /*
         * Register flavours
         */
        if (contribution instanceof Flavour) {
            Flavour flavour = (Flavour) contribution;
            String flavourName = flavour.getName();
            if (themePageFlavours.containsKey(flavourName)
                    && flavour.getAppendPresets()) {
                Flavour existing = themePageFlavours.get(flavourName);
                // TODO: merge
                themePageFlavours.put(flavourName, existing);
            } else {
                themePageFlavours.put(flavourName, flavour);
            }
            // Register flavour and presets as palettes
            registerPaletteToThemeServiceFor(contributor.getContext(), flavour);
        }

        /*
         * Register styles
         */
        else if (contribution instanceof SimpleStyle) {
            SimpleStyle style = (SimpleStyle) contribution;
            themePageStyles.put(style.getName(), style);
            // load the style content
            String src = style.getPath();
            URL url = null;
            RuntimeContext extensionContext = contributor.getContext();
            try {
                url = new URL(src);
            } catch (MalformedURLException e) {
                url = extensionContext.getLocalResource(src);
                if (url == null) {
                    url = extensionContext.getResource(src);
                }
            }
            if (url == null) {
                log.error(String.format("Could not find resource at '%s'", src));
            } else {
                String cssSource = new String(FileUtils.readBytes(url));
                style.setContent(cssSource);
            }
            // FIXME: reload theme styles in case style content changed
        }
        /*
         * Register pages
         */
        else if (contribution instanceof ThemePage) {
            ThemePage item = (ThemePage) contribution;
            String themePage = item.getName();

            // Merge
            if (themePageResources.containsKey(themePage)
                    && (item.getAppendStyles() || item.getAppendFlavours())) {

                List<String> newStyles = new ArrayList<String>();
                ThemePage existing = themePageResources.get(themePage);
                if (item.getAppendStyles()) {
                    List<String> existingStyles = existing.getStyles();
                    if (existingStyles != null) {
                        newStyles.addAll(existingStyles);
                    }
                }
                newStyles.addAll(item.getStyles());
                List<String> newFlavours = new ArrayList<String>();
                if (item.getAppendFlavours()) {
                    List<String> existingFlavours = existing.getFlavours();
                    if (existingFlavours != null) {
                        newFlavours.addAll(existingFlavours);
                    }
                }
                newFlavours.addAll(item.getFlavours());
                existing.setStyles(newStyles);
                existing.setFlavours(newFlavours);
                themePageResources.put(themePage, existing);
                if (existing.isLoaded()) {
                    // reload
                    postRegisterThemePageResources(existing);
                }

            }

            // New page or override existing page
            else {
                themePageResources.put(themePage, item);
                postRegisterThemePageResources(item);
            }

        } else {
            log.error("Unknown contribution to the theme service, extension"
                    + " point 'themePageResources': " + contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // TODO
    }

    protected void registerPaletteToThemeServiceFor(
            RuntimeContext extensionContext, Flavour flavour) {
        // register all presets to the standard registries
        List<FlavourPresets> presets = flavour.getPresets();
        if (presets != null) {
            for (FlavourPresets myPreset : presets) {
                PaletteType palette = new PaletteType(flavour.getName(),
                        myPreset.getSrc(), myPreset.getCategory());
                TypeRegistry typeRegistry = Manager.getTypeRegistry();
                String paletteName = palette.getName() + " "
                        + palette.getCategory();
                String src = palette.getSrc();
                String category = palette.getCategory();
                URL url = null;
                try {
                    url = new URL(src);
                } catch (MalformedURLException e) {
                    url = extensionContext.getLocalResource(src);
                    if (url == null) {
                        url = extensionContext.getResource(src);
                    }
                }

                if (url != null) {
                    typeRegistry.register(palette);
                    Map<String, String> entries = PaletteParser.parse(url);
                    for (Map.Entry<String, String> entry : entries.entrySet()) {
                        PresetType preset = new PresetType(entry.getKey(),
                                entry.getValue(), paletteName, category, "", "");
                        typeRegistry.register(preset);
                    }
                }
            }
        }
    }

    /**
     * Reload theme page resources conf according to new style
     *
     * @since 5.4.3
     */

    protected void postRegisterThemePageResources(ThemePage page)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        String themeName = page.getThemeName();

        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            log.error(String.format(
                    "Could not resolve theme descriptor for name '%s'",
                    themeName));
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }
        PageElement pageElement = themeManager.getPageByPath(page.getName());
        if (pageElement != null) {
            List<String> styleNames = page.getStyles();

            if (styleNames != null) {
                for (String styleName : page.getStyles()) {
                    SimpleStyle simpleStyle = themePageStyles.get(styleName);
                    if (simpleStyle == null) {
                        log.warn("Style unknown: " + styleName);
                    } else {

                        // Check whether the style is already registered
                        Style style = (Style) themeManager.getNamedObject(
                                themeName, "style", styleName);

                        // Register new style
                        if (style == null) {
                            style = themeManager.createStyle();
                            style.setName(styleName);
                            style.setExternal(true);
                            themeManager.setNamedObject(themeName, "style",
                                    style);
                            String cssSource = simpleStyle.getContent();
                            if (cssSource != null) {
                                cssSource = cssSource.replaceAll("__FLAVOUR__",
                                        ThemeManager.getCollectionCssMarker());
                            }
                            Utils.loadCss(style, cssSource, "*");
                        }

                        // link page and style
                        Style existingPageStyle = (Style) ElementFormatter.getFormatFor(
                                pageElement, "style");
                        if (existingPageStyle == null) {
                            existingPageStyle = (Style) FormatFactory.create("style");
                            ElementFormatter.setFormat(pageElement,
                                    existingPageStyle);
                        }
                        themeManager.makeFormatInherit(existingPageStyle, style);
                    }
                }
            }

            // mark page as loaded
            page.setLoaded(true);
            // reset cache
            themeManager.stylesModified(themeName);
            themeManager.themeModified(themeName);
        } else {
            log.error(String.format("Unknown theme page '%s'", page.getName()));
        }
    }

    // service API

    @Override
    public String getDefaultFlavour(String themePageName) {
        if (themePageResources != null) {
            ThemePage themePage = themePageResources.get(themePageName);
            if (themePage != null) {
                return themePage.getDefaultFlavour();
            }
        }
        return null;
    }

    @Override
    public void themeRegistered(String themeName) {
        if (themeName == null) {
            return;
        }
        if (themePageResources != null) {
            for (ThemePage res : themePageResources.values()) {
                if (themeName.equals(res.getThemeName())) {
                    try {
                        postRegisterThemePageResources(res);
                        log.info("Registered theme page resources for "
                                + res.getName());
                    } catch (ThemeException e) {
                        log.error("Could not load theme page "
                                + "resources for theme " + themeName, e);
                    }
                }
            }
        }
    }

}
