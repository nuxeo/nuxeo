/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
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

/**
 * Default implementation for the {@link ThemeStylingService}
 *
 * @since 5.5
 */
public class ThemeStylingServiceImpl extends DefaultComponent implements ThemeStylingService {

    private static final Log log = LogFactory.getLog(ThemeStylingServiceImpl.class);

    protected static final String WR_EX = "org.nuxeo.ecm.platform.WebResources";

    protected static final String PAGE_EP = "pages";

    protected FlavorRegistry flavorReg;

    protected NegotiationRegistry negReg;

    // Runtime Component API

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        flavorReg = new FlavorRegistry();
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
            ComponentName compName = contributor.getName();
            String message = String.format(
                    "Style '%s' on component %s should now be contributed to extension point '%s'. "
                            + "Note that the 'flavor' processor should be used with this resource.",
                    style.getName(), compName, WR_EX);
            DeprecationLogger.log(message, "7.4");
            addRuntimeMessage(Level.ERROR, message, Source.EXTENSION, compName.getName());
        } else if (contribution instanceof ResourceDescriptor) {
            ResourceDescriptor resource = (ResourceDescriptor) contribution;
            ComponentName compName = contributor.getName();
            String message = String.format(
                    "Resource '%s' on component %s should now be contributed to extension point '%s'.",
                    resource.getName(), compName, WR_EX);
            DeprecationLogger.log(message, "7.4");
            addRuntimeMessage(Level.ERROR, message, Source.EXTENSION, compName.getName());
        } else if (contribution instanceof NegotiationDescriptor) {
            NegotiationDescriptor neg = (NegotiationDescriptor) contribution;
            log.info(String.format("Register negotiation for '%s'", neg.getTarget()));
            negReg.addContribution(neg);
            log.info(String.format("Done registering negotiation for '%s'", neg.getTarget()));
        } else {
            log.error(String.format("Unknown contribution to the theme " + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof FlavorDescriptor) {
            FlavorDescriptor flavor = (FlavorDescriptor) contribution;
            flavorReg.removeContribution(flavor);
        } else if (contribution instanceof NegotiationDescriptor) {
            NegotiationDescriptor neg = (NegotiationDescriptor) contribution;
            negReg.removeContribution(neg);
        } else {
            log.error(String.format("Unknown contribution to the theme " + "styling service, extension point '%s': '%s",
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
                        content = new String(IOUtils.toByteArray(url));
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
                        content = new String(IOUtils.toByteArray(url));
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
        List<FlavorPresets> presets = new ArrayList<>();
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

    protected URL getUrlFromPath(String path, RuntimeContext extensionContext) {
        if (path == null) {
            return null;
        }
        URL url;
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

    protected PageRegistry getPageRegistry() {
        return getExtensionPointRegistry(PAGE_EP);
    }

    @Override
    public String getDefaultFlavorName(String themePageName) {
        return getPageRegistry().getContribution(themePageName)
                                .map(PageDescriptor.class::cast)
                                .map(PageDescriptor::getDefaultFlavor)
                                .orElse(null);
    }

    @Override
    public FlavorDescriptor getFlavor(String flavorName) {
        if (flavorReg != null) {
            FlavorDescriptor flavor = flavorReg.getFlavor(flavorName);
            if (flavor != null) {
                FlavorDescriptor clone = flavor.clone();
                clone.setLogo(computeLogo(flavor, new ArrayList<>()));
                clone.setPalettePreview(computePalettePreview(flavor, new ArrayList<>()));
                clone.setFavicons(computeIcons(flavor, new ArrayList<>()));
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
        PageRegistry pageReg = getPageRegistry();
        PageDescriptor themePage = pageReg.getPage(themePageName);
        String defaultFlavor = null;
        LinkedHashSet<String> flavors = new LinkedHashSet<>();
        if (themePage != null) {
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
            defaultFlavor = themePage.getDefaultFlavor();
        }
        List<String> res = new ArrayList<>(flavors);
        // add default flavor if it's not listed there
        if (defaultFlavor != null && !flavors.contains(defaultFlavor)) {
            res.add(0, defaultFlavor);
        }
        return res;
    }

    @Override
    public List<FlavorDescriptor> getFlavors(String themePageName) {
        List<String> flavorNames = getFlavorNames(themePageName);
        if (flavorNames != null) {
            List<FlavorDescriptor> flavors = new ArrayList<>();
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
        List<FlavorPresets> presets = computePresets(flavor, new ArrayList<>());
        Map<String, Map<String, String>> presetsByCat = new HashMap<>();
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
                        allEntries = new HashMap<>();
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
                        log.error(
                                String.format("Could not parse palette for " + "preset with source '%s' in flavor '%s'",
                                        myPreset.getSrc(), flavorName),
                                e);
                    }
                }
            }
        }
        return presetsByCat;
    }

    @Override
    public Map<String, String> getPresetVariables(String flavorName) {
        Map<String, String> res = new HashMap<>();
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
        return (PageDescriptor) getPageRegistry().getContribution(name).orElse(null);
    }

    @Override
    public List<PageDescriptor> getPages() {
        return getPageRegistry().getContributionValues();
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
                    Negotiator neg = ndc.getDeclaredConstructor().newInstance();
                    neg.setProperties(nd.getProperties());
                    res = neg.getResult(target, context);
                    if (res != null) {
                        break;
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return res;
    }

}
