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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
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
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;
import org.nuxeo.theme.styling.service.descriptors.SimpleStyle;
import org.nuxeo.theme.styling.service.descriptors.ThemePage;
import org.nuxeo.theme.styling.service.registries.FlavorRegistry;
import org.nuxeo.theme.styling.service.registries.PageRegistry;
import org.nuxeo.theme.styling.service.registries.ResourceRegistry;
import org.nuxeo.theme.styling.service.registries.StyleRegistry;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

/**
 * Default implementation for the {@link ThemeStylingService}
 *
 * @since 5.5
 */
public class ThemeStylingServiceImpl extends DefaultComponent implements
        ThemeStylingService {

    private static final Log log = LogFactory.getLog(ThemeStylingServiceImpl.class);

    protected PageRegistry pageReg;

    protected FlavorRegistry flavorReg;

    protected StyleRegistry styleReg;

    protected ResourceRegistry resourceReg;

    // Runtime Component API

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        pageReg = new PageRegistry();
        flavorReg = new FlavorRegistry();
        styleReg = new StyleRegistry();
        resourceReg = new ResourceRegistry();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof Flavor) {
            Flavor flavor = (Flavor) contribution;
            log.info(String.format("Register flavor '%s'", flavor.getName()));
            registerFlavor(flavor, contributor.getContext());
            log.info(String.format("Done registering flavor '%s'",
                    flavor.getName()));
        } else if (contribution instanceof SimpleStyle) {
            SimpleStyle style = (SimpleStyle) contribution;
            log.info(String.format("Register style '%s'", style.getName()));
            registerStyle(style, contributor.getContext());
            log.info(String.format("Done registering style '%s'",
                    style.getName()));
        } else if (contribution instanceof ThemePage) {
            ThemePage themePage = (ThemePage) contribution;
            log.info(String.format("Register page '%s'", themePage.getName()));
            registerPage(themePage);
            log.info(String.format("Done registering page '%s'",
                    themePage.getName()));
        } else if (contribution instanceof ResourceType) {
            ResourceType resource = (ResourceType) contribution;
            log.info(String.format("Register resource '%s'", resource.getName()));
            registerResource(resource, contributor.getContext());
            log.info(String.format("Done registering resource '%s'",
                    resource.getName()));
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
        if (contribution instanceof Flavor) {
            Flavor flavor = (Flavor) contribution;
            flavorReg.removeContribution(flavor);
            Flavor newFlavor = flavorReg.getContribution(flavor.getName());
            if (newFlavor == null) {
                unregisterFlavorToThemeService(flavor);
            } else {
                if (!Framework.getRuntime().isShuttingDown()) {
                    // register again the new one
                    registerFlavor(newFlavor, contributor.getContext());
                }
            }
        } else if (contribution instanceof ResourceType) {
            ResourceType resource = (ResourceType) contribution;
            resourceReg.removeContribution(resource);
            ResourceType newResource = resourceReg.getContribution(resource.getName());
            if (newResource == null) {
                unregisterResourceToThemeService(resource);
            } else {
                if (!Framework.getRuntime().isShuttingDown()) {
                    // register again the new one
                    registerResource(newResource, contributor.getContext());
                }
            }
        } else if (contribution instanceof SimpleStyle) {
            SimpleStyle style = (SimpleStyle) contribution;
            styleReg.removeContribution(style);
            if (!Framework.getRuntime().isShuttingDown()) {
                // reload theme styles in case style content changed
                postRegisterAllThemePageResources();
            }
        } else if (contribution instanceof ThemePage) {
            ThemePage themePage = (ThemePage) contribution;
            pageReg.removeContribution(themePage);
            ThemePage newThemePage = pageReg.getContribution(themePage.getName());
            if (newThemePage == null) {
                unRegisterThemePageResources(themePage);
            } else {
                if (!Framework.getRuntime().isShuttingDown()) {
                    // reload conf
                    postRegisterThemePageResources(newThemePage);
                }
            }
        } else {
            log.error(String.format("Unknown contribution to the theme "
                    + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    protected void registerPage(ThemePage themePage) throws Exception {
        String themePageName = themePage.getName();
        ThemePage existingPage = pageReg.getContribution(themePageName);
        pageReg.addContribution(themePage);
        if (existingPage != null && existingPage.isLoaded()) {
            // reload
            ThemePage newPage = pageReg.getContribution(themePageName);
            postRegisterThemePageResources(newPage);
        }
    }

    protected void registerFlavor(Flavor flavor, RuntimeContext extensionContext)
            throws Exception {
        flavorReg.addContribution(flavor);
        String flavorName = flavor.getName();
        Flavor newFlavor = flavorReg.getContribution(flavorName);
        registerFlavorToThemeService(newFlavor, extensionContext);
        // register again all flavors extending it
        for (Flavor f : flavorReg.getFlavorsExtending(flavorName)) {
            log.info(String.format("Register again flavor '%s' "
                    + "as it extends flavor '%s'", f.getName(), flavorName));
            registerFlavorToThemeService(f, extensionContext);
        }
    }

    protected void registerFlavorToThemeService(Flavor flavor,
            RuntimeContext extensionContext) {
        String flavorName = flavor.getName();
        List<FlavorPresets> presets = new ArrayList<FlavorPresets>();
        String extendsFlavorName = flavor.getExtendsFlavor();
        if (!StringUtils.isBlank(extendsFlavorName)) {
            // check if it's registered already
            Flavor extendFlavor = flavorReg.getContribution(extendsFlavorName);
            if (extendFlavor == null) {
                log.warn(String.format("Extended flavor '%s' " + "not found",
                        extendsFlavorName));
            } else {
                List<FlavorPresets> extendedPresets = extendFlavor.getPresets();
                if (extendedPresets != null) {
                    presets.addAll(extendedPresets);
                }
            }
        }

        List<FlavorPresets> localPresets = flavor.getPresets();
        if (localPresets != null) {
            presets.addAll(localPresets);
        }
        Map<String, Map<String, String>> presetsByCat = new HashMap<String, Map<String, String>>();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String src = myPreset.getSrc();
                URL url = getUrlFromPath(src, extensionContext);
                if (url == null) {
                    log.error(String.format("Could not find resource at '%s'",
                            src));
                } else {
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

        // register all presets to the standard theme service registries
        unregisterFlavorToThemeService(flavor);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Register flavor '%s' to the theme service", flavorName));
        }

        TypeRegistry typeRegistry = Manager.getTypeRegistry();
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

    protected void unregisterFlavorToThemeService(Flavor flavor) {
        String flavorName = flavor.getName();
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Unregister flavor '%s' from the theme service", flavorName));
        }

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
    }

    protected void registerResource(ResourceType resource,
            RuntimeContext extensionContext) throws Exception {
        ResourceType oldResource = resourceReg.getContribution(resource.getName());
        if (oldResource != null) {
            // unregister it in case it was there
            unregisterResourceToThemeService(oldResource);
        }
        resourceReg.addContribution(resource);
        String resourceName = resource.getName();
        ResourceType newResource = resourceReg.getContribution(resourceName);
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        typeRegistry.register(newResource);
    }

    protected void unregisterResourceToThemeService(ResourceType resource) {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ThemeManager themeManager = Manager.getThemeManager();
        typeRegistry.unregister(resource);
        themeManager.unregisterResourceOrdering(resource);
    }

    protected void registerStyle(SimpleStyle style,
            RuntimeContext extensionContext) throws Exception {
        // load the style content
        String src = style.getPath();
        URL url = getUrlFromPath(src, extensionContext);
        if (url == null) {
            log.error(String.format("Could not find resource at '%s'", src));
        } else {
            String cssSource = new String(FileUtils.readBytes(url));
            style.setContent(cssSource);
        }
        styleReg.addContribution(style);
        // reload theme styles in case style content changed
        postRegisterAllThemePageResources();
    }

    protected String getPaletteName(String name, String category) {
        return name + " " + category;
    }

    protected URL getUrlFromPath(String path, RuntimeContext extensionContext) {
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            url = extensionContext.getLocalResource(path);
            if (url == null) {
                url = extensionContext.getResource(path);
            }
        }
        return url;
    }

    /**
     * Reload theme page resources conf according to new style
     */
    protected void postRegisterAllThemePageResources() {
        if (pageReg != null) {
            for (ThemePage res : pageReg.getThemePages()) {
                if (res.isLoaded()) {
                    try {
                        postRegisterThemePageResources(res);
                    } catch (ThemeException e) {
                        log.error(
                                String.format("Could not load theme page "
                                        + "resources for page '%s' ",
                                        res.getThemeName()), e);
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
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Register theme page '%s' to the theme service",
                        pageName));
            }

            List<String> styleNames = page.getStyles();
            if (styleNames != null) {

                Style style = themeManager.createStyle();
                style.setExternal(true);
                for (String styleName : styleNames) {
                    SimpleStyle simpleStyle = styleReg.getContribution(styleName);
                    if (simpleStyle == null) {
                        log.warn("Style unknown: " + styleName);
                    } else {
                        String cssSource = simpleStyle.getContent();
                        if (cssSource != null) {
                            cssSource = cssSource.replaceAll(FLAVOR_MARKER,
                                    ThemeManager.getCollectionCssMarker());
                            // merge all properties into new style
                            Utils.loadCss(style, cssSource, "*", true);
                        } else {
                            log.error("Null content for css style: "
                                    + styleName);
                        }
                    }
                }
                // link page and style
                style.setName(pageName + PAGE_STYLE_NAME_SUFFIX);
                style.setCollection(page.getPageName()
                        + PAGE_STYLE_CLASS_NAME_PREFIX);

                themeManager.setNamedObject(themeName, "style", style);
                Style existingPageStyle = (Style) ElementFormatter.getFormatFor(
                        pageElement, "style");
                if (existingPageStyle == null) {
                    existingPageStyle = (Style) FormatFactory.create("style");
                    ElementFormatter.setFormat(pageElement, existingPageStyle);
                }
                themeManager.makeFormatInherit(existingPageStyle, style);
            } else {
                // remove style linked to page
                themeManager.removeNamedObject(themeName, "style", pageName
                        + PAGE_STYLE_NAME_SUFFIX);
            }

            // mark page as loaded
            page.setLoaded(true);
            // reset cache
            themeManager.stylesModified(themeName);
            themeManager.themeModified(themeName);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Done registering theme page '%s' to the theme service",
                        pageName));
            }
        } else {
            log.error(String.format("Unknown theme page '%s'", page.getName()));
        }
    }

    protected void unRegisterThemePageResources(ThemePage page)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        String themeName = page.getThemeName();
        String pageName = page.getName();

        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            // not there anymore => ignore
            return;
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }
        PageElement pageElement = themeManager.getPageByPath(pageName);
        if (pageElement != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Unregister theme page '%s' from the theme service",
                        pageName));
            }

            // remove style linked to page
            themeManager.removeNamedObject(themeName, "style", pageName
                    + PAGE_STYLE_NAME_SUFFIX);
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
        if (pageReg != null) {
            ThemePage themePage = pageReg.getContribution(themePageName);
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
        if (pageReg != null) {
            for (ThemePage res : pageReg.getThemePages()) {
                if (themeName.equals(res.getThemeName())) {
                    try {
                        postRegisterThemePageResources(res);
                    } catch (ThemeException e) {
                        log.error("Could not load theme page "
                                + "resources for theme " + themeName, e);
                    }
                }
            }
        }
    }

    @Override
    public void themeGlobalResourcesRegistered(URL themeUrl) {
        // get all resources for given theme url and add them to the
        // ResourceManager instance
        String themePageName = ThemeManager.getPagePathByUrl(themeUrl);
        ThemePage themePage = pageReg.getContribution(themePageName);
        if (themePage != null) {
            List<String> resources = themePage.getResources();
            if (resources != null && !resources.isEmpty()) {
                ResourceManager resourceManager = Manager.getResourceManager();
                for (String r : resources) {
                    ResourceType resource = resourceReg.getContribution(r);
                    if (resource == null) {
                        log.warn(String.format(
                                "Missing resource '%s' referenced "
                                        + "in theme page '%s'", r,
                                themePageName));
                    } else {
                        resourceManager.addResource(r, themeUrl);
                    }
                }
            }
        }
    }

}
