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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.theme.styling.negotiation.Negotiator;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;
import org.nuxeo.theme.styling.service.descriptors.IconDescriptor;
import org.nuxeo.theme.styling.service.descriptors.LogoDescriptor;
import org.nuxeo.theme.styling.service.descriptors.NegotiationDescriptor;
import org.nuxeo.theme.styling.service.descriptors.NegotiatorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;
import org.nuxeo.theme.styling.service.descriptors.SassImport;
import org.nuxeo.theme.styling.service.descriptors.SimpleStyle;
import org.nuxeo.theme.styling.service.palettes.PaletteParseException;
import org.nuxeo.theme.styling.service.palettes.PaletteParser;
import org.nuxeo.theme.styling.service.registries.FlavorRegistry;
import org.nuxeo.theme.styling.service.registries.NegotiationRegistry;
import org.nuxeo.theme.styling.service.registries.PageRegistry;
import org.nuxeo.theme.styling.service.registries.StyleRegistry;

/**
 * Default implementation for the {@link ThemeStylingService}
 *
 * @since 5.5
 */
public class ThemeStylingServiceImpl extends DefaultComponent implements ThemeStylingService {

    private static final Log log = LogFactory.getLog(ThemeStylingServiceImpl.class);

    protected static final String WR_EX = "org.nuxeo.ecm.platform.WebResources";

    protected PageRegistry pageReg;

    protected FlavorRegistry flavorReg;

    protected StyleRegistry styleReg;

    protected NegotiationRegistry negReg;

    // Runtime Component API

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        pageReg = new PageRegistry();
        flavorReg = new FlavorRegistry();
        styleReg = new StyleRegistry();
        negReg = new NegotiationRegistry();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof FlavorDescriptor) {
            FlavorDescriptor flavor = (FlavorDescriptor) contribution;
            log.info(String.format("Register flavor '%s'", flavor.getName()));
            registerFlavor(flavor, contributor.getContext());
            log.info(String.format("Done registering flavor '%s'", flavor.getName()));
        } else if (contribution instanceof SimpleStyle) {
            SimpleStyle style = (SimpleStyle) contribution;
            log.info(String.format("Register style '%s'", style.getName()));
            String message = String.format("Style '%s' on component %s should now be contributed to extension "
                    + "point '%s': a compatibility registration was performed but it may not be "
                    + "accurate. Note that the 'flavor' processor should be used with this resource.", style.getName(),
                    contributor.getName(), WR_EX);
            DeprecationLogger.log(message, "7.4");
            Framework.getRuntime().getWarnings().add(message);
            ResourceDescriptor resource = getResourceFromStyle(style);
            registerResource(resource, contributor.getContext());
            log.info(String.format("Done registering style '%s'", style.getName()));
        } else if (contribution instanceof PageDescriptor) {
            PageDescriptor page = (PageDescriptor) contribution;
            log.info(String.format("Register page '%s'", page.getName()));
            if (page.hasResources()) {
                // automatically register a bundle for page resources
                WebResourceManager wrm = Framework.getService(WebResourceManager.class);
                wrm.registerResourceBundle(page.getComputedResourceBundle());
            }
            pageReg.addContribution(page);
            log.info(String.format("Done registering page '%s'", page.getName()));
        } else if (contribution instanceof ResourceDescriptor) {
            ResourceDescriptor resource = (ResourceDescriptor) contribution;
            log.info(String.format("Register resource '%s'", resource.getName()));
            String message = String.format("Resource '%s' on component %s should now be contributed to extension "
                    + "point '%s': a compatibility registration was performed but it may not be accurate.",
                    resource.getName(), contributor.getName(), WR_EX);
            DeprecationLogger.log(message, "7.4");
            Framework.getRuntime().getWarnings().add(message);
            // ensure path is absolute, consider that resource is in the war, and if not, user will have to declare it
            // directly to the WRM endpoint
            String path = resource.getPath();
            if (path != null && !path.startsWith("/")) {
                resource.setUri("/" + path);
            }
            registerResource(resource, contributor.getContext());
            log.info(String.format("Done registering resource '%s'", resource.getName()));
        } else if (contribution instanceof NegotiationDescriptor) {
            NegotiationDescriptor neg = (NegotiationDescriptor) contribution;
            log.info(String.format("Register negotiation for '%s'", neg.getTarget()));
            negReg.addContribution(neg);
            log.info(String.format("Done registering negotiation for '%s'", neg.getTarget()));
        } else {
            log.error(String.format(
                    "Unknown contribution to the theme " + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof FlavorDescriptor) {
            FlavorDescriptor flavor = (FlavorDescriptor) contribution;
            flavorReg.removeContribution(flavor);
        } else if (contribution instanceof Resource) {
            Resource resource = (Resource) contribution;
            unregisterResource(resource);
        } else if (contribution instanceof SimpleStyle) {
            SimpleStyle style = (SimpleStyle) contribution;
            unregisterResource(getResourceFromStyle(style));
        } else if (contribution instanceof PageDescriptor) {
            PageDescriptor page = (PageDescriptor) contribution;
            if (page.hasResources()) {
                WebResourceManager wrm = Framework.getService(WebResourceManager.class);
                wrm.unregisterResourceBundle(page.getComputedResourceBundle());
            }
            pageReg.removeContribution(page);
        } else if (contribution instanceof NegotiationDescriptor) {
            NegotiationDescriptor neg = (NegotiationDescriptor) contribution;
            negReg.removeContribution(neg);
        } else {
            log.error(String.format(
                    "Unknown contribution to the theme " + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    protected void registerFlavor(FlavorDescriptor flavor, RuntimeContext extensionContext) {
        // set flavor presets files content
        List<FlavorPresets> presets = flavor.getPresets();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String src = myPreset.getSrc();
                URL url = getUrlFromPath(src, extensionContext);
                if (url == null) {
                    log.error(String.format("Could not find resource at '%s'", src));
                } else {
                    String content;
                    try {
                        content = new String(FileUtils.readBytes(url));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    myPreset.setContent(content);
                }
            }
        }

        // set flavor sass variables
        List<SassImport> sassVars = flavor.getSassImports();
        if (sassVars != null) {
            for (SassImport var : sassVars) {
                String src = var.getSrc();
                URL url = getUrlFromPath(src, extensionContext);
                if (url == null) {
                    log.error(String.format("Could not find resource at '%s'", src));
                } else {
                    String content;
                    try {
                        content = new String(FileUtils.readBytes(url));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    var.setContent(content);
                }
            }
        }

        flavorReg.addContribution(flavor);
    }

    protected List<FlavorPresets> computePresets(FlavorDescriptor flavor, List<String> flavors) {
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
                    log.error("Cyclic dependency detected in flavor '" + flavor.getName() + "' hierarchy");
                    return presets;
                } else {
                    // retrieve the extended presets
                    flavors.add(flavor.getName());
                    FlavorDescriptor extendedFlavor = getFlavor(extendsFlavorName);
                    if (extendedFlavor != null) {
                        List<FlavorPresets> parentPresets = computePresets(extendedFlavor, flavors);
                        if (parentPresets != null) {
                            presets.addAll(0, parentPresets);
                        }
                    } else {
                        log.warn("Extended flavor '" + extendsFlavorName + "' not found");
                    }
                }
            }
        }
        return presets;
    }

    protected void registerResource(Resource resource, RuntimeContext extensionContext) {
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        wrm.registerResource(resource);
    }

    protected void unregisterResource(Resource resource) {
        // unregister directly to the WebResourceManager service
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        wrm.unregisterResource(resource);
    }

    protected ResourceDescriptor getResourceFromStyle(SimpleStyle style) {
        // turn style into a resource
        ResourceDescriptor resource = new ResourceDescriptor();
        resource.setPath(style.getSrc());
        String name = style.getName();
        if (name.endsWith(ResourceType.css.name())) {
            resource.setName(name);
        } else {
            resource.setName(name + "." + ResourceType.css.name());
        }
        resource.setProcessors(Arrays.asList(new String[] { "flavor" }));
        return resource;
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

    // service API

    @Override
    public String getDefaultFlavorName(String themePageName) {
        if (pageReg != null) {
            PageDescriptor themePage = pageReg.getPage(themePageName);
            if (themePage != null) {
                return themePage.getDefaultFlavor();
            }
        }
        return null;
    }

    @Override
    public FlavorDescriptor getFlavor(String flavorName) {
        if (flavorReg != null) {
            FlavorDescriptor flavor = flavorReg.getFlavor(flavorName);
            if (flavor != null) {
                FlavorDescriptor clone = flavor.clone();
                clone.setLogo(computeLogo(flavor, new ArrayList<String>()));
                clone.setPalettePreview(computePalettePreview(flavor, new ArrayList<String>()));
                clone.setFavicons(computeIcons(flavor, new ArrayList<String>()));
                return clone;
            }
        }
        return null;
    }

    @Override
    public LogoDescriptor getLogo(String flavorName) {
        FlavorDescriptor flavor = getFlavor(flavorName);
        if (flavor != null) {
            return flavor.getLogo();
        }
        return null;
    }

    protected LogoDescriptor computeLogo(FlavorDescriptor flavor, List<String> flavors) {
        if (flavor != null) {
            LogoDescriptor localLogo = flavor.getLogo();
            if (localLogo == null) {
                String extendsFlavorName = flavor.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavorName)) {
                    if (flavors.contains(extendsFlavorName)) {
                        // cyclic dependency => abort
                        log.error("Cyclic dependency detected in flavor '" + flavor.getName() + "' hierarchy");
                        return null;
                    } else {
                        // retrieved the extended logo
                        flavors.add(flavor.getName());
                        FlavorDescriptor extendedFlavor = getFlavor(extendsFlavorName);
                        if (extendedFlavor != null) {
                            localLogo = computeLogo(extendedFlavor, flavors);
                        } else {
                            log.warn("Extended flavor '" + extendsFlavorName + "' not found");
                        }
                    }
                }
            }
            return localLogo;
        }
        return null;
    }

    protected PalettePreview computePalettePreview(FlavorDescriptor flavor, List<String> flavors) {
        if (flavor != null) {
            PalettePreview localPalette = flavor.getPalettePreview();
            if (localPalette == null) {
                String extendsFlavorName = flavor.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavorName)) {
                    if (flavors.contains(extendsFlavorName)) {
                        // cyclic dependency => abort
                        log.error("Cyclic dependency detected in flavor '" + flavor.getName() + "' hierarchy");
                        return null;
                    } else {
                        // retrieved the extended colors
                        flavors.add(flavor.getName());
                        FlavorDescriptor extendedFlavor = getFlavor(extendsFlavorName);
                        if (extendedFlavor != null) {
                            localPalette = computePalettePreview(extendedFlavor, flavors);
                        } else {
                            log.warn("Extended flavor '" + extendsFlavorName + "' not found");
                        }
                    }
                }
            }
            return localPalette;
        }
        return null;
    }

    protected List<IconDescriptor> computeIcons(FlavorDescriptor flavor, List<String> flavors) {
        if (flavor != null) {
            List<IconDescriptor> localIcons = flavor.getFavicons();
            if (localIcons == null || localIcons.isEmpty()) {
                String extendsFlavorName = flavor.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavorName)) {
                    if (flavors.contains(extendsFlavorName)) {
                        // cyclic dependency => abort
                        log.error("Cyclic dependency detected in flavor '" + flavor.getName() + "' hierarchy");
                        return null;
                    } else {
                        // retrieved the extended icons
                        flavors.add(flavor.getName());
                        FlavorDescriptor extendedFlavor = getFlavor(extendsFlavorName);
                        if (extendedFlavor != null) {
                            localIcons = computeIcons(extendedFlavor, flavors);
                        } else {
                            log.warn("Extended flavor '" + extendsFlavorName + "' not found");
                        }
                    }
                }
            }
            return localIcons;
        }
        return null;
    }

    @Override
    public List<String> getFlavorNames(String themePageName) {
        if (pageReg != null) {
            PageDescriptor themePage = pageReg.getPage(themePageName);
            if (themePage != null) {
                List<String> flavors = new ArrayList<String>();
                List<String> localFlavors = themePage.getFlavors();
                if (localFlavors != null) {
                    flavors.addAll(localFlavors);
                }
                // add flavors from theme for all pages
                PageDescriptor forAllPage = pageReg.getConfigurationApplyingToAll();
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
    public List<FlavorDescriptor> getFlavors(String themePageName) {
        List<String> flavorNames = getFlavorNames(themePageName);
        if (flavorNames != null) {
            List<FlavorDescriptor> flavors = new ArrayList<FlavorDescriptor>();
            for (String flavorName : flavorNames) {
                FlavorDescriptor flavor = getFlavor(flavorName);
                if (flavor != null) {
                    flavors.add(flavor);
                }
            }
            return flavors;
        }
        return null;
    }

    protected Map<String, Map<String, String>> getPresetsByCat(FlavorDescriptor flavor) {
        String flavorName = flavor.getName();
        List<FlavorPresets> presets = computePresets(flavor, new ArrayList<String>());
        Map<String, Map<String, String>> presetsByCat = new HashMap<String, Map<String, String>>();
        if (presets != null) {
            for (FlavorPresets myPreset : presets) {
                String content = myPreset.getContent();
                if (content == null) {
                    log.error("Null content for preset with source '" + myPreset.getSrc() + "' in flavor '" + flavorName
                            + "'");
                } else {
                    String cat = myPreset.getCategory();
                    Map<String, String> allEntries;
                    if (presetsByCat.containsKey(cat)) {
                        allEntries = presetsByCat.get(cat);
                    } else {
                        allEntries = new HashMap<String, String>();
                    }
                    try {
                        Map<String, String> newEntries = PaletteParser.parse(content.getBytes(), myPreset.getSrc());
                        if (newEntries != null) {
                            allEntries.putAll(newEntries);
                        }
                        if (allEntries.isEmpty()) {
                            presetsByCat.remove(cat);
                        } else {
                            presetsByCat.put(cat, allEntries);
                        }
                    } catch (PaletteParseException e) {
                        log.error(String.format("Could not parse palette for "
                                + "preset with source '%s' in flavor '%s'", myPreset.getSrc(), flavorName), e);
                    }
                }
            }
        }
        return presetsByCat;
    }

    @Override
    public Map<String, String> getPresetVariables(String flavorName) {
        Map<String, String> res = new HashMap<String, String>();
        FlavorDescriptor flavor = getFlavor(flavorName);
        if (flavor == null) {
            return res;
        }
        Map<String, Map<String, String>> presetsByCat = getPresetsByCat(flavor);
        for (String cat : presetsByCat.keySet()) {
            Map<String, String> entries = presetsByCat.get(cat);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                res.put(entry.getKey() + " (" + ThemeStylingService.FLAVOR_MARKER + " " + cat + ")", entry.getValue());
            }
        }
        return res;
    }

    @Override
    public PageDescriptor getPage(String name) {
        PageDescriptor page = pageReg.getPage(name);
        if (page != null) {
            // merge with global resources
            PageDescriptor globalPage = pageReg.getPage("*");
            mergePage(page, globalPage);
        }
        return page;
    }

    @Override
    public List<PageDescriptor> getPages() {
        List<PageDescriptor> pages = new ArrayList<PageDescriptor>();
        List<String> names = pageReg.getPageNames();
        PageDescriptor globalPage = pageReg.getPage("*");
        for (String name : names) {
            if ("*".equals(name)) {
                continue;
            }
            PageDescriptor page = pageReg.getPage(name);
            if (page != null) {
                // merge with global resources
                mergePage(page, globalPage);
            }
            pages.add(page);
        }
        return pages;
    }

    protected void mergePage(PageDescriptor page, PageDescriptor globalPage) {
        if (page != null && globalPage != null) {
            // merge with global resources
            PageDescriptor clone = globalPage.clone();
            clone.setAppendFlavors(true);
            clone.setAppendResources(true);
            clone.setAppendStyles(true);
            page.merge(clone);
        }
    }

    @Override
    public String negotiate(String target, Object context) {
        String res = null;
        NegotiationDescriptor negd = negReg.getNegotiation(target);
        if (negd != null) {
            List<NegotiatorDescriptor> nds = negd.getNegotiators();
            for (NegotiatorDescriptor nd : nds) {
                Class<Negotiator> ndc = nd.getNegotiatorClass();
                try {
                    Negotiator neg = ndc.newInstance();
                    neg.setProperties(nd.getProperties());
                    res = neg.getResult(target, context);
                    if (res != null) {
                        break;
                    }
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return res;
    }

}
