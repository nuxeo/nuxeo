/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.services;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.CachingDef;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.RegistryType;
import org.nuxeo.theme.ViewDef;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.presets.PaletteType;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeParser;
import org.nuxeo.theme.themes.ThemeSet;
import org.nuxeo.theme.themes.ThemeSetEntry;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.ViewType;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

public class ThemeService extends DefaultComponent implements FrameworkListener {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.theme.services.ThemeService");

    private static final Log log = LogFactory.getLog(ThemeService.class);

    private Map<String, Registrable> registries;

    private RuntimeContext context;

    public Map<String, Registrable> getRegistries() {
        return registries;
    }

    public Registrable getRegistry(String name) {
        return registries.get(name);
    }

    public synchronized void addRegistry(String name, Registrable registry) {
        registries.put(name, registry);
    }

    public synchronized void removeRegistry(String name) {
        registries.remove(name);
    }

    @Override
    public void activate(ComponentContext ctx) {
        this.context = ctx.getRuntimeContext();
        registries = new HashMap<String, Registrable>();
        ctx.getRuntimeContext().getBundle().getBundleContext().addFrameworkListener(
                this);
        log.debug("Theme service activated");
    }

    @Override
    public void deactivate(ComponentContext ctx) {
        registries = null;
        ctx.getRuntimeContext().getBundle().getBundleContext().removeFrameworkListener(
                this);
        log.debug("Theme service deactivated");
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            registerCustomThemes();
        }
        ThemeManager.updateThemeDescriptors();
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("registries")) {
            registerRegistryExtension(extension);
        } else if (xp.equals("elements") || xp.equals("fragments")
                || xp.equals("formats") || xp.equals("format-filters")
                || xp.equals("standalone-filters") || xp.equals("resources")
                || xp.equals("negotiations") || xp.equals("shortcuts")
                || xp.equals("vocabularies") || xp.equals("previews")) {
            registerTypeExtension(extension);
        } else if (xp.equals("applications")) {
            registerApplicationExtension(extension);
        } else if (xp.equals("perspectives")) {
            registerPerspectiveExtension(extension);
        } else if (xp.equals("engines")) {
            registerEngineExtension(extension);
        } else if (xp.equals("template-engines")) {
            registerTemplateEngineExtension(extension);
        } else if (xp.equals("themes")) {
            registerThemeExtension(extension);
        } else if (xp.equals("themesets")) {
            registerThemeSetExtension(extension);
        } else if (xp.equals("presets")) {
            registerPresetExtension(extension);
        } else if (xp.equals("views")) {
            registerViewExtension(extension);
        } else if (xp.equals("models")) {
            registerModelExtension(extension);
        } else {
            log.warn(String.format("Unknown extension point: %s", xp));
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("registries")) {
            unregisterRegistryExtension(extension);
        } else if (xp.equals("elements") || xp.equals("fragments")
                || xp.equals("formats") || xp.equals("format-filters")
                || xp.equals("standalone-filters") || xp.equals("resources")
                || xp.equals("engines") || xp.equals("template-engines")
                || xp.equals("negotiations") || xp.equals("perspectives")
                || xp.equals("applications") || xp.equals("shortcuts")
                || xp.equals("vocabularies") || (xp.equals("presets"))
                || xp.equals("views") || xp.equals("themes")
                || xp.equals("themesets") || xp.equals("previews")) {
            unregisterTypeExtension(extension);
        } else if (xp.equals("views")) {
            unregisterViewExtension(extension);
        } else if (xp.equals("models")) {
            unregisterModelExtension(extension);
        } else {
            log.warn(String.format("Unknown extension point: %s", xp));
        }
    }

    private void registerRegistryExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            RegistryType registryType = (RegistryType) contrib;
            Registrable registry = null;
            try {
                registry = (Registrable) context.loadClass(
                        registryType.getClassName()).newInstance();
            } catch (Exception e) {
                log.warn("Could not create registry: " + registryType.getName()
                        + "(" + registryType.getClassName() + ")");
            }
            if (registry != null) {
                addRegistry(registryType.getName(), registry);
            }
        }
    }

    private void unregisterRegistryExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            RegistryType registryType = (RegistryType) contrib;
            removeRegistry(registryType.getName());
        }
    }

    private void registerTypeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            typeRegistry.register((Type) contrib);
        }
    }

    private void unregisterTypeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        if (typeRegistry != null) {
            for (Object contrib : contribs) {
                typeRegistry.unregister((Type) contrib);
            }
        }
    }

    private void registerApplicationExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            ApplicationType application = (ApplicationType) contrib;

            ApplicationType oldApplication = (ApplicationType) typeRegistry.lookup(
                    TypeFamily.APPLICATION, application.getTypeName());

            if (oldApplication == null) {
                String templateEngine = application.getTemplateEngine();
                if (templateEngine == null) {
                    final String defaultTemplateEngine = ThemeManager.getDefaultTemplateEngineName();
                    log.warn(String.format(
                            "Please set the 'template-engine' attribute on <application root=\"%s\" template-engine=\"...\"> (default is '%s')",
                            application.getRoot(), defaultTemplateEngine));
                    application.setTemplateEngine(defaultTemplateEngine);
                }
                typeRegistry.register(application);

            } else {
                // Merge properties from already registered application
                final String templateEngine = application.getTemplateEngine();
                if (templateEngine != null) {
                    oldApplication.setTemplateEngine(templateEngine);
                }

                NegotiationDef negotiation = application.getNegotiation();
                if (negotiation != null) {
                    NegotiationDef oldNegotiation = oldApplication.getNegotiation();
                    if (oldNegotiation == null) {
                        oldNegotiation = new NegotiationDef();
                        oldApplication.setNegotiation(oldNegotiation);
                    }
                    if (negotiation.getStrategy() != null) {
                        oldNegotiation.setStrategy(negotiation.getStrategy());
                    }
                    if (negotiation.getDefaultTheme() != null) {
                        oldNegotiation.setDefaultTheme(negotiation.getDefaultTheme());
                    }
                    if (negotiation.getDefaultPerspective() != null) {
                        oldNegotiation.setDefaultPerspective(negotiation.getDefaultPerspective());
                    }
                    if (negotiation.getDefaultEngine() != null) {
                        oldNegotiation.setDefaultEngine(negotiation.getDefaultEngine());
                    }
                }

                CachingDef resourceCaching = application.getResourceCaching();
                if (resourceCaching != null) {
                    CachingDef oldResourceCaching = oldApplication.getResourceCaching();
                    if (oldResourceCaching == null) {
                        oldResourceCaching = new CachingDef();
                        oldApplication.setResourceCaching(oldResourceCaching);
                    }
                    if (resourceCaching.getLifetime() != null) {
                        oldResourceCaching.setLifetime(resourceCaching.getLifetime());
                    }
                }

                CachingDef styleCaching = application.getStyleCaching();
                if (styleCaching != null) {
                    CachingDef oldStyleCaching = oldApplication.getStyleCaching();
                    if (oldStyleCaching == null) {
                        oldStyleCaching = new CachingDef();
                        oldApplication.setStyleCaching(oldStyleCaching);
                    }
                    if (styleCaching.getLifetime() != null) {
                        oldStyleCaching.setLifetime(styleCaching.getLifetime());
                    }
                }

                Map<String, ViewDef> viewDefs = application.getViewDefs();
                if (!viewDefs.isEmpty()) {
                    Map<String, ViewDef> oldViewDefs = oldApplication.getViewDefs();
                    for (Map.Entry<String, ViewDef> entry : viewDefs.entrySet()) {
                        oldViewDefs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    private void registerPerspectiveExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            PerspectiveType perspective = (PerspectiveType) contrib;
            if (!perspective.getName().matches("[a-z0-9_\\-]+")) {
                log.error("Perspective names may only contain lowercase alphanumeric characters, underscores and hyphens ");
                continue;
            }
            typeRegistry.register(perspective);
        }
    }

    private void registerEngineExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            EngineType engine = (EngineType) contrib;
            if (!engine.getName().matches("[a-z0-9_\\-]+")) {
                log.error("Rendering engine names may only contain lowercase alphanumeric characters, underscores and hyphens ");
                continue;
            }
            typeRegistry.register(engine);
        }
    }

    private void registerTemplateEngineExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            TemplateEngineType engine = (TemplateEngineType) contrib;
            if (!engine.getName().matches("[a-z0-9_\\-]+")) {
                log.error("Template engine names may only contain lowercase alphanumeric characters, underscores and hyphens ");
                continue;
            }
            typeRegistry.register(engine);
        }
    }

    private void registerThemeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");

        for (Object contrib : contribs) {
            ThemeDescriptor themeDescriptor = (ThemeDescriptor) contrib;
            themeDescriptor.setConfigured(true);

            String src = themeDescriptor.getSrc();

            // register the theme descriptor even if the theme fails to load
            typeRegistry.register(themeDescriptor);

            if (src == null) {
                themeDescriptor.setLoadingFailed(true);
                log.error("Could not load theme, source not set. ");
                continue;
            }

            String themeName;
            try {
                themeName = ThemeParser.registerTheme(themeDescriptor);
            } catch (ThemeIOException e) {
                themeDescriptor.setLoadingFailed(true);
                log.error("Could not register theme: " + src + " "
                        + e.getMessage());
                continue;
            }

            // add some meta information to the theme descriptor
            themeDescriptor.setLoadingFailed(false);
            themeDescriptor.setLastLoaded(new Date());
            themeDescriptor.setName(themeName);
        }
    }

    private void registerThemeSetExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");

        for (Object contrib : contribs) {
            ThemeSet themeSet = (ThemeSet) contrib;
            String name = themeSet.getName();

            ThemeSet oldThemeSet = (ThemeSet) typeRegistry.lookup(
                    TypeFamily.THEMESET, name);

            if (oldThemeSet == null) {
                typeRegistry.register(themeSet);
            } else {
                for (ThemeSetEntry theme : themeSet.getThemes()) {
                    String themeName = theme.getName();
                    ThemeSetEntry oldTheme = oldThemeSet.getTheme(themeName);
                    if (oldTheme == null) {
                        oldTheme = new ThemeSetEntry(themeName);
                        oldThemeSet.setTheme(oldTheme);
                    }
                    for (String feature : theme.getFeatures()) {
                        oldThemeSet.addFeatureToTheme(themeName, feature);
                    }
                }

            }
        }
    }

    private void registerCustomThemes() {
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (File file : ThemeManager.getCustomThemeFiles()) {
            ThemeDescriptor themeDescriptor = new ThemeDescriptor();
            themeDescriptor.setConfigured(false);

            String src = null;
            try {
                src = String.format("file://%s", file.getCanonicalPath());
            } catch (Exception e) {
                themeDescriptor.setLoadingFailed(true);
                log.error("Could not read theme file: " + src);
                continue;
            }

            themeDescriptor.setSrc(src);

            String themeName;
            try {
                themeName = ThemeParser.registerTheme(themeDescriptor);
            } catch (ThemeIOException e) {
                themeDescriptor.setLoadingFailed(true);
                log.error("Could not register theme: " + src + " "
                        + e.getMessage());
                continue;
            }

            themeDescriptor.setLoadingFailed(false);
            themeDescriptor.setLastLoaded(new Date());
            themeDescriptor.setName(themeName);
            typeRegistry.register(themeDescriptor);
        }
        log.debug("Registered local themes");
    }

    private void registerPresetExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        RuntimeContext extensionContext = extension.getContext();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            if (contrib instanceof PaletteType) {
                registerPalette((PaletteType) contrib, extensionContext);
            } else {
                typeRegistry.register((Type) contrib);
            }
        }
    }

    private void registerPalette(PaletteType palette,
            RuntimeContext extensionContext) {
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        String paletteName = palette.getName();
        String src = palette.getSrc();
        String category = palette.getCategory();
        URL url = null;
        try {
            url = new URL(src);
        } catch (MalformedURLException e) {
            url = extensionContext.getResource(src);
        }

        if (url != null) {
            typeRegistry.register(palette);
            Map<String, String> entries = PaletteParser.parse(url);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String value = PresetManager.resolvePresets(null,
                        entry.getValue());
                PresetType preset = new PresetType(entry.getKey(), value,
                        paletteName, category);
                typeRegistry.register(preset);
            }
        }
    }

    private void registerViewExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        for (Object contrib : contribs) {
            ViewType viewType = (ViewType) contrib;
            String templateEngineAttr = viewType.getTemplateEngine();
            final String viewName = viewType.getViewName();
            if (templateEngineAttr == null) {
                final String defaultTemplateEngineName = ThemeManager.getDefaultTemplateEngineName();
                templateEngineAttr = defaultTemplateEngineName;
                log.warn(String.format(
                        "Please set the 'template-engine' attribute on <view name=\"%s\" template-engine=\"...\"> (using default '%s')",
                        viewName, defaultTemplateEngineName));
            } else {
                for (String templateEngineName : templateEngineAttr.split(",")) {
                    viewType.setTemplateEngine(templateEngineName);
                    typeRegistry.register(viewType);
                }
            }
        }
    }

    private void unregisterViewExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        if (typeRegistry != null) {
            for (Object contrib : contribs) {
                typeRegistry.unregister((ViewType) contrib);
            }
        }
    }

    private void registerModelExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        ThemeManager themeManager = (ThemeManager) getRegistry("themes");
        for (Object contrib : contribs) {
            ModelType modelType = (ModelType) contrib;
            final String modelTypeName = modelType.getTypeName();
            final ModelType oldModelType = (ModelType) typeRegistry.lookup(
                    TypeFamily.MODEL, modelTypeName);
            if (oldModelType != null) {
                if (oldModelType.getClassName().equals(modelType.getClassName())) {
                    log.debug("Model type '" + modelTypeName + "' ("
                            + oldModelType.getClassName()
                            + ") has already been registered.");
                } else {
                    log.warn("Failed to reregister model type '"
                            + modelTypeName + "' ("
                            + oldModelType.getClassName() + "). The new class "
                            + modelType.getClassName() + " will be ignored.");
                }
                continue;
            }
            typeRegistry.register(modelType);
            themeManager.registerModelByClassname(modelType);
        }
    }

    private void unregisterModelExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        ThemeManager themeManager = (ThemeManager) getRegistry("themes");
        if (typeRegistry != null) {
            for (Object contrib : contribs) {
                ModelType modelType = (ModelType) contrib;
                themeManager.unregisterModelByClassname(modelType);
                typeRegistry.unregister(modelType);
            }
        }
    }

}
