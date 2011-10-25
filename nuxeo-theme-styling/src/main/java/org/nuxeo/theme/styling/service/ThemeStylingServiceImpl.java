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
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;
import org.nuxeo.theme.styling.service.descriptors.SimpleStyle;
import org.nuxeo.theme.styling.service.descriptors.ThemePage;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
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

    protected Map<String, Flavor> themePageFlavors = new HashMap<String, Flavor>();

    protected Map<String, SimpleStyle> themePageStyles = new HashMap<String, SimpleStyle>();

    // Runtime Component API

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        // Register flavors
        if (contribution instanceof Flavor) {
            Flavor flavor = (Flavor) contribution;
            String flavorName = flavor.getName();
            if (themePageFlavors.containsKey(flavorName)
                    && flavor.getAppendPresets()) {
                Flavor existing = themePageFlavors.get(flavorName);
                List<FlavorPresets> newPresets = existing.getPresets();
                if (newPresets == null) {
                    newPresets = new ArrayList<FlavorPresets>();
                }
                // merge
                List<FlavorPresets> presets = flavor.getPresets();
                if (presets != null) {
                    newPresets.addAll(presets);
                }
                flavor.setPresets(newPresets);
                themePageFlavors.put(flavorName, flavor);
            } else {
                themePageFlavors.put(flavorName, flavor);
            }
            // Register flavor and presets as palettes
            registerPaletteToThemeServiceFor(contributor.getContext(), flavor);
        }

        // Register styles
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
            // reload theme styles in case style content changed
            postRegisterThemePageResourcesForStyle(extensionContext, style);
        }

        // Register pages
        else if (contribution instanceof ThemePage) {
            ThemePage themePage = (ThemePage) contribution;
            String themePageName = themePage.getName();

            ThemePage existingPage = themePageResources.get(themePageName);
            // Merge
            if (existingPage != null) {
                String defaultFlavor = existingPage.getDefaultFlavor();
                if (themePage.getDefaultFlavor() != null) {
                    // override
                    defaultFlavor = themePage.getDefaultFlavor();
                }

                List<String> newStyles = themePage.getStyles();
                if (newStyles.isEmpty() || themePage.getAppendStyles()) {
                    List<String> existingStyles = existingPage.getStyles();
                    if (existingStyles != null) {
                        newStyles.addAll(0, existingStyles);
                    }
                }

                List<String> newFlavors = themePage.getFlavors();
                if (newFlavors.isEmpty() || themePage.getAppendFlavors()) {
                    List<String> existingFlavors = existingPage.getFlavors();
                    if (existingFlavors != null) {
                        newFlavors.addAll(0, existingFlavors);
                    }
                }

                ThemePage newPage = new ThemePage();
                newPage.setName(themePageName);
                newPage.setDefaultFlavor(defaultFlavor);
                newPage.setStyles(newStyles);
                newPage.setFlavors(newFlavors);
                themePageResources.put(themePageName, newPage);
                if (existingPage.isLoaded()) {
                    // reload
                    postRegisterThemePageResources(newPage);
                }
            } else {
                // register new page or override existing page
                themePageResources.put(themePageName, themePage);
                // wait for the theme to be loaded
                // postRegisterThemePageResources(item);
            }
        } else {
            log.error(String.format("Unknown contribution to the theme "
                    + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // TODO
    }

    protected String getPaletteName(String name, String category) {
        return name + " " + category;
    }

    protected void registerPaletteToThemeServiceFor(
            RuntimeContext extensionContext, Flavor flavor) {
        // register all presets to the standard registries
        List<FlavorPresets> presets = flavor.getPresets();
        Map<String, Map<String, String>> presetsByCat = new HashMap<String, Map<String, String>>();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String src = myPreset.getSrc();
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
                    String cat = myPreset.getCategory();
                    Map<String, String> allEntries;
                    if (presetsByCat.containsKey(cat)) {
                        allEntries = presetsByCat.get(cat);
                    } else {
                        allEntries = new HashMap<String, String>();
                    }
                    Map<String, String> newEntries = PaletteParser.parse(url);
                    if (newEntries != null) {
                        allEntries.putAll(newEntries);
                    }
                    if (allEntries.isEmpty()) {
                        presetsByCat.remove(cat);
                    } else {
                        presetsByCat.put(cat, allEntries);
                    }
                }
            }
        }

        String flavorName = flavor.getName();

        // cleanup already registered presets
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        List<String> groupNames = new ArrayList<String>();
        for (PRESET_CATEGORY cat : PRESET_CATEGORY.values()) {
            String paletteName = getPaletteName(flavorName, cat.name());
            groupNames.add(paletteName);
        }
        List<Type> registeredPresets = typeRegistry.getTypes(TypeFamily.PRESET);
        for (Type type : registeredPresets) {
            PresetType preset = (PresetType) type;
            if (groupNames.contains(preset.getGroup())) {
                typeRegistry.unregister(type);
            }
        }

        // register all presets
        for (String cat : presetsByCat.keySet()) {
            String paletteName = getPaletteName(flavorName, cat);
            Map<String, String> entries = presetsByCat.get(cat);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                PresetType preset = new PresetType(entry.getKey(),
                        entry.getValue(), paletteName, cat, "", "");
                typeRegistry.register(preset);
            }
        }
    }

    /**
     * Reload theme page resources conf according to new style
     */
    protected void postRegisterThemePageResourcesForStyle(
            RuntimeContext extensionContext, SimpleStyle style) {
        if (themePageResources != null) {
            String styleName = style.getName();
            for (ThemePage res : themePageResources.values()) {
                List<String> styleNames = res.getStyles();
                if (styleNames != null && styleNames.contains(styleName)) {
                    // only register resources again if it's already been
                    // loaded
                    if (res.isLoaded()) {
                        try {
                            postRegisterThemePageResources(res);
                        } catch (ThemeException e) {
                            log.error(
                                    String.format("Could not load theme page "
                                            + "resources for page '%s' "
                                            + "and style '%s",
                                            res.getThemeName(), style), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Register link between page and style after theme has been registered
     */
    protected void postRegisterThemePageResources(ThemePage page)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        String themeName = page.getThemeName();
        String pageName = page.getName();

        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            log.error(String.format(
                    "Could not resolve theme descriptor for name '%s'",
                    themeName));
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }
        PageElement pageElement = themeManager.getPageByPath(pageName);
        if (pageElement != null) {
            List<String> styleNames = page.getStyles();
            if (styleNames != null) {
                Style style = themeManager.createStyle();
                style.setExternal(true);
                for (String styleName : styleNames) {
                    SimpleStyle simpleStyle = themePageStyles.get(styleName);
                    if (simpleStyle == null) {
                        log.warn("Style unknown: " + styleName);
                    } else {
                        String cssSource = simpleStyle.getContent();
                        if (cssSource != null) {
                            cssSource = cssSource.replaceAll(FLAVOR_MARKER,
                                    ThemeManager.getCollectionCssMarker());
                        }
                        // merge all properties into new style
                        Utils.loadCss(style, cssSource, "*", true);
                    }
                }
                // link page and style
                style.setName(pageName + PAGE_STYLE_NAME_SUFFIX);
                themeManager.setNamedObject(themeName, "style", style);
                Style existingPageStyle = (Style) ElementFormatter.getFormatFor(
                        pageElement, "style");
                if (existingPageStyle == null) {
                    existingPageStyle = (Style) FormatFactory.create("style");
                    ElementFormatter.setFormat(pageElement, existingPageStyle);
                }
                themeManager.makeFormatInherit(existingPageStyle, style);
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
    public String getDefaultFlavor(String themePageName) {
        if (themePageResources != null) {
            ThemePage themePage = themePageResources.get(themePageName);
            if (themePage != null) {
                return themePage.getDefaultFlavor();
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
