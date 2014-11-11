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
import org.nuxeo.theme.styling.service.descriptors.Logo;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;
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
            Flavor newFlavor = flavorReg.getFlavor(flavor.getName());
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
            ResourceType newResource = resourceReg.getResource(resource.getName());
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
            ThemePage newThemePage = pageReg.getThemePage(themePage.getName());
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
        ThemePage existingPage = pageReg.getThemePage(themePageName);
        pageReg.addContribution(themePage);
        if (existingPage != null && existingPage.isLoaded()) {
            if ("*".equals(themePageName)) {
                // reload all
                postRegisterAllThemePageResources();
            } else {
                // reload this page
                ThemePage newPage = pageReg.getThemePage(themePageName);
                postRegisterThemePageResources(newPage);
            }
        }
    }

    protected void registerFlavor(Flavor flavor, RuntimeContext extensionContext)
            throws Exception {
        // set flavor presets files content
        List<FlavorPresets> presets = flavor.getPresets();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String src = myPreset.getSrc();
                URL url = getUrlFromPath(src, extensionContext);
                if (url == null) {
                    log.error(String.format("Could not find resource at '%s'",
                            src));
                } else {
                    String content = new String(FileUtils.readBytes(url));
                    myPreset.setContent(content);
                }
            }
        }
        flavorReg.addContribution(flavor);
        String flavorName = flavor.getName();
        Flavor newFlavor = flavorReg.getFlavor(flavorName);
        registerFlavorToThemeService(newFlavor, extensionContext);
        // register again all flavors extending it
        for (Flavor f : flavorReg.getFlavorsExtending(flavorName)) {
            log.info(String.format("Register again flavor '%s' "
                    + "as it extends flavor '%s'", f.getName(), flavorName));
            registerFlavorToThemeService(f, extensionContext);
        }
    }

    protected List<FlavorPresets> computePresets(Flavor flavor,
            List<String> flavors) {
        List<FlavorPresets> presets = new ArrayList<FlavorPresets>();
        if (flavor != null) {
            List<FlavorPresets> localPresets = flavor.getPresets();
            if (localPresets != null) {
                presets.addAll(localPresets);
            }
            String extendsFlavorName = flavor.getExtendsFlavor();
            if (!StringUtils.isBlank(extendsFlavorName)) {
                if (flavors.contains(extendsFlavorName)) {
                    // cyclic dependency => abort
                    log.error(String.format(
                            "Cyclic dependency detected in flavor '%s' hierarchy",
                            flavor.getName()));
                    return presets;
                } else {
                    // retrieve the extended presets
                    flavors.add(flavor.getName());
                    Flavor extendedFlavor = getFlavor(extendsFlavorName);
                    if (extendedFlavor != null) {
                        List<FlavorPresets> parentPresets = computePresets(
                                extendedFlavor, flavors);
                        if (parentPresets != null) {
                            presets.addAll(0, parentPresets);
                        }
                    } else {
                        log.warn(String.format("Extended flavor '%s' "
                                + "not found", extendsFlavorName));
                    }
                }
            }
        }
        return presets;
    }

    protected void registerFlavorToThemeService(Flavor flavor,
            RuntimeContext extensionContext) {
        String flavorName = flavor.getName();
        List<FlavorPresets> presets = computePresets(flavor,
                new ArrayList<String>());
        Map<String, Map<String, String>> presetsByCat = new HashMap<String, Map<String, String>>();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String content = myPreset.getContent();
                if (content == null) {
                    log.error(String.format("Null content for preset with "
                            + "source '%s' in flavor '%s'", myPreset.getSrc(),
                            flavorName));
                } else {
                    String cat = myPreset.getCategory();
                    Map<String, String> allEntries;
                    if (presetsByCat.containsKey(cat)) {
                        allEntries = presetsByCat.get(cat);
                    } else {
                        allEntries = new HashMap<String, String>();
                    }
                    try {
                        Map<String, String> newEntries = PaletteParser.parse(
                                content.getBytes(), myPreset.getSrc());
                        if (newEntries != null) {
                            allEntries.putAll(newEntries);
                        }
                        if (allEntries.isEmpty()) {
                            presetsByCat.remove(cat);
                        } else {
                            presetsByCat.put(cat, allEntries);
                        }
                    } catch (Exception e) {
                        log.error(String.format("Could not parse palette for "
                                + "preset with source '%s' in flavor '%s'",
                                myPreset.getSrc(), flavorName), e);
                    }
                }
            }
        }

        // unregister potential existing presets
        unregisterFlavorToThemeService(flavor);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Register flavor '%s' to the theme service", flavorName));
        }

        // register all presets to the standard theme service registries
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
        ResourceType oldResource = resourceReg.getResource(resource.getName());
        if (oldResource != null) {
            // unregister it in case it was there
            unregisterResourceToThemeService(oldResource);
        }
        resourceReg.addContribution(resource);
        String resourceName = resource.getName();
        ResourceType newResource = resourceReg.getResource(resourceName);
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        typeRegistry.register(newResource);
        // Need to update resource ordering here as we just registered a new
        // resource
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.updateResourceOrdering();
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
        String src = style.getSrc();
        if (src == null) {
            log.error(String.format("Null source for style '%s'",
                    style.getName()));
            return;
        }
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
        if (path == null) {
            return null;
        }
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
                        log.error(String.format("Could not load theme page "
                                + "resources for page '%s' ", res.getName()), e);
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
        String pageName = page.getName();
        if (!"*".equals(pageName)) {
            // include page conf for all themes
            ThemePage forAllPage = pageReg.getConfigurationApplyingToAllThemes();
            postRegisterThemePageResources(pageName, page, forAllPage);
        }
    }

    protected void postRegisterThemePageResources(String themePageName,
            ThemePage page, ThemePage pageApplyingToAll) throws ThemeException {
        String themeName = ThemePage.getThemeName(themePageName);
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            log.error(String.format(
                    "Could not resolve theme descriptor for name '%s'",
                    themeName));
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }
        PageElement pageElement = themeManager.getPageByPath(themePageName);
        if (pageElement != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Register theme page '%s' to the theme service",
                        themePageName));
            }

            List<String> allStyleNames = new ArrayList<String>();
            List<String> styleNames = page.getStyles();
            if (styleNames != null) {
                allStyleNames.addAll(styleNames);
            }
            if (pageApplyingToAll != null) {
                styleNames = pageApplyingToAll.getStyles();
                if (styleNames != null) {
                    allStyleNames.addAll(styleNames);
                }
            }
            if (!allStyleNames.isEmpty()) {
                Style style = themeManager.createStyle();
                style.setExternal(true);
                for (String styleName : allStyleNames) {
                    SimpleStyle simpleStyle = styleReg.getStyle(styleName);
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
                style.setName(themePageName + PAGE_STYLE_NAME_SUFFIX);
                style.setCollection(themePageName
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
                themeManager.removeNamedObject(themeName, "style",
                        themePageName + PAGE_STYLE_NAME_SUFFIX);
            }

            // mark page(s) as loaded
            page.setLoaded(true);
            if (pageApplyingToAll != null) {
                pageApplyingToAll.setLoaded(true);
            }
            // reset cache
            themeManager.stylesModified(themeName);
            themeManager.themeModified(themeName);
            themeManager.resetCachedResources();

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Done registering theme page '%s' to the theme service",
                        themePageName));
            }
        } else {
            log.error(String.format("Unknown theme page '%s'", page.getName()));
        }
    }

    protected void unRegisterThemePageResources(ThemePage page)
            throws ThemeException {
        String pageName = page.getName();
        if (!"*".equals(pageName)) {
            unRegisterThemePageResources(pageName, page);
        }
    }

    protected void unRegisterThemePageResources(String themePageName,
            ThemePage page) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        String themeName = ThemePage.getThemeName(themePageName);
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            // not there anymore => ignore
            return;
        }
        if (themeDescriptor != null && !themeDescriptor.isLoaded()) {
            ThemeManager.loadTheme(themeDescriptor);
        }
        PageElement pageElement = themeManager.getPageByPath(themePageName);
        if (pageElement != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Unregister theme page '%s' from the theme service",
                        themePageName));
            }

            // remove style linked to page
            themeManager.removeNamedObject(themeName, "style", themePageName
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
    public String getDefaultFlavorName(String themePageName) {
        if (pageReg != null) {
            ThemePage themePage = pageReg.getThemePage(themePageName);
            if (themePage != null) {
                return themePage.getDefaultFlavor();
            }
        }
        return null;
    }

    @Override
    public Flavor getFlavor(String flavorName) {
        if (flavorReg != null) {
            Flavor flavor = flavorReg.getFlavor(flavorName);
            Flavor clone = null;
            if (flavor != null) {
                if (flavor.getLogo() == null) {
                    // resolve and attach the computed logo from extended
                    // flavor
                    clone = flavor.clone();
                    clone.setLogo(computeLogo(flavor, new ArrayList<String>()));
                }
                if (flavor.getPalettePreview() == null) {
                    if (clone == null) {
                        clone = flavor.clone();
                    }
                    clone.setPalettePreview(computePalettePreview(flavor,
                            new ArrayList<String>()));
                }
            }
            if (clone != null) {
                return clone;
            }
            return flavor;
        }
        return null;
    }

    @Override
    public Logo getLogo(String flavorName) {
        Flavor flavor = getFlavor(flavorName);
        if (flavor != null) {
            return flavor.getLogo();
        }
        return null;
    }

    protected Logo computeLogo(Flavor flavor, List<String> flavors) {
        if (flavor != null) {
            Logo localLogo = flavor.getLogo();
            if (localLogo == null) {
                String extendsFlavorName = flavor.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavorName)) {
                    if (flavors.contains(extendsFlavorName)) {
                        // cyclic dependency => abort
                        log.error(String.format(
                                "Cyclic dependency detected in flavor '%s' hierarchy",
                                flavor.getName()));
                        return null;
                    } else {
                        // retrieved the extended logo
                        flavors.add(flavor.getName());
                        Flavor extendedFlavor = getFlavor(extendsFlavorName);
                        if (extendedFlavor != null) {
                            localLogo = computeLogo(extendedFlavor, flavors);
                        } else {
                            log.warn(String.format("Extended flavor '%s' "
                                    + "not found", extendsFlavorName));
                        }
                    }
                }
            }
            return localLogo;
        }
        return null;
    }

    protected PalettePreview computePalettePreview(Flavor flavor,
            List<String> flavors) {
        if (flavor != null) {
            PalettePreview localPalette = flavor.getPalettePreview();
            if (localPalette == null) {
                String extendsFlavorName = flavor.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavorName)) {
                    if (flavors.contains(extendsFlavorName)) {
                        // cyclic dependency => abort
                        log.error(String.format(
                                "Cyclic dependency detected in flavor '%s' hierarchy",
                                flavor.getName()));
                        return null;
                    } else {
                        // retrieved the extended colors
                        flavors.add(flavor.getName());
                        Flavor extendedFlavor = getFlavor(extendsFlavorName);
                        if (extendedFlavor != null) {
                            localPalette = computePalettePreview(
                                    extendedFlavor, flavors);
                        } else {
                            log.warn(String.format("Extended flavor '%s' "
                                    + "not found", extendsFlavorName));
                        }
                    }
                }
            }
            return localPalette;
        }
        return null;
    }

    @Override
    public List<String> getFlavorNames(String themePageName) {
        if (pageReg != null) {
            ThemePage themePage = pageReg.getThemePage(themePageName);
            if (themePage != null) {
                List<String> flavors = new ArrayList<String>();
                List<String> localFlavors = themePage.getFlavors();
                if (localFlavors != null) {
                    flavors.addAll(localFlavors);
                }
                // add flavors from theme for all pages
                ThemePage forAllPage = pageReg.getConfigurationApplyingToAllThemes();
                if (forAllPage != null) {
                    localFlavors = forAllPage.getFlavors();
                    if (localFlavors != null) {
                        flavors.addAll(localFlavors);
                    }
                }
                // add default flavor if it's not listed there
                String defaultFlavor = themePage.getDefaultFlavor();
                if (defaultFlavor != null) {
                    if (!flavors.contains(defaultFlavor)) {
                        flavors.add(0, defaultFlavor);
                    }
                }
                return flavors;
            }
        }
        return null;
    }

    @Override
    public List<Flavor> getFlavors(String themePageName) {
        List<String> flavorNames = getFlavorNames(themePageName);
        if (flavorNames != null) {
            List<Flavor> flavors = new ArrayList<Flavor>();
            for (String flavorName : flavorNames) {
                Flavor flavor = getFlavor(flavorName);
                if (flavor != null) {
                    flavors.add(flavor);
                }
            }
            return flavors;
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
                String name = ThemePage.getThemeName(res.getName());
                if (themeName.equals(name)) {
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
        ThemePage themePage = pageReg.getThemePage(themePageName);
        if (themePage != null) {
            List<String> resources = new ArrayList<String>();
            List<String> localResources = themePage.getResources();
            if (localResources != null) {
                resources.addAll(localResources);
            }
            ThemePage forAllPage = pageReg.getConfigurationApplyingToAllThemes();
            if (forAllPage != null) {
                localResources = forAllPage.getResources();
                if (localResources != null) {
                    resources.addAll(localResources);
                }
            }
            if (resources != null && !resources.isEmpty()) {
                ResourceManager resourceManager = Manager.getResourceManager();
                for (String r : resources) {
                    ResourceType resource = resourceReg.getResource(r);
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
            // Not sure if needed but just in case
            ThemeManager themeManager = Manager.getThemeManager();
            themeManager.updateResourceOrdering();
        }
    }

}
