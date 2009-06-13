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

package org.nuxeo.theme;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.protocol.nxtheme.Handler;
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.UidManager;
import org.nuxeo.theme.vocabularies.VocabularyManager;

public final class Manager {

    private static final String PROTOCOL_HANDLER_PKG = "org.nuxeo.theme.protocol";

    static {
        initializeProtocols();
    }

    private Manager() {
    }

    private static ThemeService getThemeService() {
        return (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
    }

    private static Map<String, Registrable> getRegistries() {
        return getThemeService().getRegistries();
    }

    public static Registrable getRegistry(final String name) {
        return getRegistries().get(name);
    }

    public static RelationStorage getRelationStorage() {
        return (RelationStorage) getRegistry("relations");
    }

    public static UidManager getUidManager() {
        return (UidManager) getRegistry("uids");
    }

    public static ThemeManager getThemeManager() {
        return (ThemeManager) getRegistry("themes");
    }

    public static TypeRegistry getTypeRegistry() {
        return (TypeRegistry) getRegistry("types");
    }

    public static ResourceManager getResourceManager() {
        return (ResourceManager) getRegistry("resources");
    }

    public static PerspectiveManager getPerspectiveManager() {
        return (PerspectiveManager) getRegistry("perspectives");
    }

    public static VocabularyManager getVocabularyManager() {
        return (VocabularyManager) getRegistry("vocabularies");
    }

    @SuppressWarnings( { "ResultOfObjectAllocationIgnored" })
    public static void initializeProtocols() {
        Properties properties = System.getProperties();
        String handlers = System.getProperty("java.protocol.handler.pkgs");
        if (handlers == null) {
            properties.put("java.protocol.handler.pkgs", PROTOCOL_HANDLER_PKG);
        } else if (!handlers.matches(PROTOCOL_HANDLER_PKG)) {
            properties.put("java.protocol.handler.pkgs", PROTOCOL_HANDLER_PKG
                    + "|" + handlers);
        }
        System.setProperties(properties);

        /*
         * Register the 'nxtheme' URL protocol handler programmatically to get
         * around m2/surefire classloading bug.
         *
         * ref. http://jira.codehaus.org/browse/SUREFIRE-104
         *
         * TODO: remove with Maven surefire 2.4
         */

        boolean protocolInitialized = true;
        try {
            new URL("nxtheme://test");
        } catch (MalformedURLException e) {
            protocolInitialized = false;
        }

        if (!protocolInitialized) {
            try {
                URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                    public URLStreamHandler createURLStreamHandler(
                            String protocol) {
                        if ("nxtheme".equals(protocol)) {
                            return new Handler();
                        }
                        return null;
                    }
                });
            } catch (Throwable e) {
            }
        }
    }

}
