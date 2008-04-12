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
import org.nuxeo.theme.events.EventListener;
import org.nuxeo.theme.events.EventListenerType;
import org.nuxeo.theme.events.EventManager;
import org.nuxeo.theme.events.EventType;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.presets.PaletteType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeParser;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class ThemeService extends DefaultComponent {

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
    public void activate(ComponentContext context) {
        this.context = context.getRuntimeContext();
        registries = new HashMap<String, Registrable>();
        log.debug("Theme service activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        registries = null;
        log.debug("Theme service deactivated");
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("registries")) {
            registerRegistryExtension(extension);
        } else if (xp.equals("elements") || xp.equals("fragments")
                || xp.equals("models") || xp.equals("formats")
                || xp.equals("format-filters")
                || xp.equals("standalone-filters") || xp.equals("resources")
                || xp.equals("views") || xp.equals("negotiations")
                || xp.equals("shortcuts") || xp.equals("vocabularies")) {
            registerTypeExtension(extension);
        } else if (xp.equals("applications")) {
            registerApplicationExtension(extension);
        } else if (xp.equals("perspectives")) {
            registerPerspectiveExtension(extension);
        } else if (xp.equals("engines")) {
            registerEngineExtension(extension);
        } else if (xp.equals("event-listeners")) {
            registerEventListenerExtension(extension);
        } else if (xp.equals("themes")) {
            registerThemeExtension(extension);
        } else if (xp.equals("presets")) {
            registerPresetExtension(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("registries")) {
            unregisterRegistryExtension(extension);
        } else if (xp.equals("elements") || xp.equals("fragments")
                || xp.equals("models") || xp.equals("formats")
                || xp.equals("format-filters")
                || xp.equals("standalone-filters") || xp.equals("resources")
                || xp.equals("views") || xp.equals("engines")
                || xp.equals("negotiations") || xp.equals("perspectives")
                || xp.equals("applications") || xp.equals("shortcuts")
                || xp.equals("vocabularies")) {
            unregisterTypeExtension(extension);
        } else if (xp.equals("event-listeners")) {
            unregisterEventListenerExtension(extension);
        } else if (xp.equals("themes")) {
            unregisterThemeExtension(extension);
        } else if (xp.equals("presets")) {
            unregisterPresetExtension(extension);
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
                typeRegistry.register(application);

            } else {
                // Merge properties from already registered application
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

    private void registerThemeExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        RuntimeContext extensionContext = extension.getContext();

        for (Object contrib : contribs) {
            ThemeDescriptor themeDescriptor = (ThemeDescriptor) contrib;
            String src = themeDescriptor.getSrc();

            // register the theme descriptor even if the theme fails to load
            TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
            typeRegistry.register(themeDescriptor);

            URL url;
            try {
                url = new URL(src);
            } catch (MalformedURLException e) {
                url = extensionContext.getResource(src);
            }

            if (url != null) {
                String themeName = ThemeParser.registerTheme(url);
                if (themeName != null) {
                    // add some meta information to the theme descriptor
                    themeDescriptor.setLastLoaded(new Date());
                    themeDescriptor.setName(themeName);
                } else {
                    log.error("Could not parse theme: " + src);
                }
            } else {
                log.error("Could not load theme: " + src);
            }

        }
    }

    private void unregisterThemeExtension(Extension extension) {
        // TODO
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

    private void unregisterPresetExtension(Extension extension) {
        // TODO
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
            for (String key : entries.keySet()) {
                String value = ThemeManager.resolvePresets(entries.get(key));
                PresetType preset = new PresetType(key, value, paletteName,
                        category);
                typeRegistry.register(preset);
            }
        }
    }

    private void registerEventListenerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("types");
        EventManager eventManager = (EventManager) getRegistry("events");

        for (Object contrib : contribs) {
            EventListenerType eventListenerType = (EventListenerType) contrib;
            EventType eventType = new EventType(
                    eventListenerType.getEventName());
            typeRegistry.register(eventType);

            EventListener listener = null;
            try {
                listener = (EventListener) Class.forName(
                        eventListenerType.getHandlerClassName()).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (listener == null) {
                log.warn("Event handler not found: "
                        + eventListenerType.getHandlerClassName());
                continue;
            }
            listener.setEventType(eventType);
            eventManager.addListener(listener);
        }
    }

    private void unregisterEventListenerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        TypeRegistry typeRegistry = (TypeRegistry) getRegistry("events");
        if (typeRegistry != null) {
            for (Object contrib : contribs) {
                typeRegistry.unregister((Type) contrib);
            }
        }
    }

}
