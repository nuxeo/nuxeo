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
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.presets.PaletteType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.styling.service.descriptors.Flavour;
import org.nuxeo.theme.styling.service.descriptors.FlavourPresets;
import org.nuxeo.theme.styling.service.descriptors.Style;
import org.nuxeo.theme.styling.service.descriptors.ThemePage;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

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
public class ThemeStylingService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(ThemeStylingService.class);

    // TODO: use ContributionFragmentRegistry instances for hot reload support
    protected Map<String, ThemePage> themePageResources = new HashMap<String, ThemePage>();

    protected Map<String, Flavour> themePageFlavours = new HashMap<String, Flavour>();

    protected Map<String, Style> themePageStyles = new HashMap<String, Style>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof ThemePage) {
            ThemePage item = (ThemePage) contribution;
            String themePage = item.getName();
            if (themePageResources.containsKey(themePage)) {
                if (!item.getAppendStyles() && !item.getAppendFlavours()) {
                    // override
                    themePageResources.put(themePage, item);
                } else {
                    // merge
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
                }
            } else {
                themePageResources.put(themePage, item);
            }
        } else if (contribution instanceof Flavour) {
            Flavour flavour = (Flavour) contribution;
            // TODO: merge of presets
            // flavour.getAppendPresets();
            themePageFlavours.put(flavour.getName(), flavour);
            registerPaletteToThemeServiceFor(contributor.getContext(), flavour);
        } else if (contribution instanceof Style) {
            Style style = (Style) contribution;
            themePageStyles.put(style.getName(), style);
            postRegisterThemePageResourcesForStyle(style);
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

    /**
     * Reload theme page resources conf according to new style
     *
     * @since 5.4.3
     * @param flavour
     */
    // TODO: see if useful
    protected void postRegisterThemePageResourcesForStyle(Style style) {
        if (themePageResources != null) {
            String styleName = style.getName();
            for (ThemePage res : themePageResources.values()) {
                List<String> styles = res.getStyles();
                if (styles != null && styles.contains(styleName)) {
                    postRegisterThemePageResources(res);
                }
            }
        }
    }

    /**
     * Reload theme page resources conf according to new flavour
     *
     * @since 5.4.3
     * @param flavour
     */
    // TODO: see if useful
    protected void postRegisterThemePageResourcesForFlavour(Flavour flavour) {
        if (themePageResources != null) {
            String flavourName = flavour.getName();
            for (ThemePage res : themePageResources.values()) {
                List<String> flavours = res.getFlavours();
                if (flavours != null && flavours.contains(flavourName)) {
                    postRegisterThemePageResources(res);
                }
            }
        }
    }

    protected void registerPaletteToThemeServiceFor(
            RuntimeContext extensionContext, Flavour flavour) {
        // register all presets to the standard registries
        // TODO: finish
        List<FlavourPresets> presets = flavour.getPresets();
        if (presets != null) {
            for (FlavourPresets myPreset : presets) {
                PaletteType palette = new PaletteType(flavour.getName(),
                        myPreset.getSrc(), myPreset.getCategory());
                TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
                String paletteName = palette.getName();
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

    protected void postRegisterThemePageResources(ThemePage page) {
        ThemeManager themeManager = Manager.getThemeManager();
        PageElement pageElement = themeManager.getPageByPath(page.getName());
        if (pageElement != null) {
            List<String> styles = page.getStyles();
            // link styles to the theme page
            if (styles != null) {
                for (String style : page.getStyles()) {
                    org.nuxeo.theme.formats.styles.Style pageStyle = (org.nuxeo.theme.formats.styles.Style) FormatFactory.create("style");
                    ElementFormatter.setFormat(pageElement, pageStyle);
                    themeManager.makeElementUseNamedStyle(pageElement, style,
                            page.getThemeName());
                }
            }
            List<String> flavours = page.getFlavours();
            // register flavours as collections
            if (flavours != null) {
                String themeName = page.getThemeName();
                for (String flavour : flavours) {
                    org.nuxeo.theme.formats.styles.Style style = (org.nuxeo.theme.formats.styles.Style) themeManager.getNamedObject(
                            themeName, "style", styleName);

                    if (style == null) {
                        style = themeManager.createStyle();
                        style.setName(styleName);
                        style.setRemote(true);
                        themeManager.setNamedObject(themeName, "style", style);
                    }

                    String resourceId = styleInfo.getResource();
                    String cssSource = ResourceManager.getBankResource(name,
                            flavour, "style", resourceId);
                    style.setCollection(collectionName);
                    Utils.loadCss(style, cssSource, "*");
                }
            }
        } else {
            log.error(String.format("Unknown theme page '%s'", page.getName()));
        }
    }
    // TODO:
    //
    // - add negociator handling the default flavour
    // - move local theme stuff here? (including negociator?)

}
