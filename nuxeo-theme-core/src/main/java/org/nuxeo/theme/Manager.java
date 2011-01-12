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

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.UidManager;
import org.nuxeo.theme.vocabularies.VocabularyManager;

public final class Manager {

    private static final String PROTOCOL_HANDLER_PKG = "org.nuxeo.theme.protocol";

    private Manager() {
    }

    private static ThemeService getThemeService() {
        return (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
    }

    private static Map<String, Registrable> getRegistries() {
        // avoid error when clearing registries at shutdown
        ThemeService service = getThemeService();
        if (service != null) {
            return service.getRegistries();
        } else {
            return Collections.emptyMap();
        }
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
    }

    public static void resetProtocols() {
        String lastPkgs = System.getProperty("java.protocol.handler.pkgs");
        if (lastPkgs == null) {
            return;
        }
        String newPkgs = "";
        String sep = "";
        StringTokenizer tokenizer = new StringTokenizer(lastPkgs, "|");
        while (tokenizer.hasMoreElements()) {
            String pkg = tokenizer.nextToken();
            if (PROTOCOL_HANDLER_PKG.equals(pkg)) {
                continue;
            }
            if (pkg.isEmpty()) {
                continue;
            }
            newPkgs += sep + pkg;
            sep = "|";
        }
        System.setProperty("java.protocol.handler.pkgs", newPkgs);
        URLStreamHandlerFactoryInstaller.resetURLStreamHandlers();
    }
}
