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

package org.nuxeo.theme.resources;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ResourceManager implements Registrable {

    private static final Log log = LogFactory.getLog(ResourceManager.class);

    private final HashMap<URL, List<String>> globalCache = new HashMap<URL, List<String>>();

    private final ThreadLocal<List<String>> localCache = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    private final TypeRegistry typeRegistry = Manager.getTypeRegistry();

    public void addResource(String name, URL themeUrl) {
        addResource(name, themeUrl, false);
    }

    public void addResource(String name, URL themeUrl, boolean local) {
        if (local) {
            log.debug("Added local resource: " + name);
        } else {
            log.debug("Added theme resource: " + name);
        }
        ResourceType resourceType = (ResourceType) typeRegistry.lookup(
                TypeFamily.RESOURCE, name);
        if (resourceType != null) {
            for (String dependency : resourceType.getDependencies()) {
                log.debug("  Subresource dependency: " + name + " -> "
                        + dependency);
                addResource(dependency, themeUrl, local);
            }
            List<String> scripts;
            if (local) {
                scripts = getLocalResources();
            } else {
                scripts = getGlobalResourcesFor(themeUrl);
            }
            if (!scripts.contains(name)) {
                scripts.add(name);
            }
        } else {
            log.warn("Resource not found: " + name);
        }
    }

    public void flush() {
        getLocalResources().clear();
    }

    private List<String> getLocalResources() {
        return localCache.get();
    }

    public List<String> getResourcesFor(String themeUrl) {
        List<String> resources = new ArrayList<String>();
        resources.addAll(getGlobalResourcesFor(themeUrl));
        for (String localResource : getLocalResources()) {
            if (!resources.contains(localResource)) {
                resources.add(localResource);
            }
        }
        return resources;
    }

    public List<String> getGlobalResourcesFor(String themeUrl) {
        try {
            return getGlobalResourcesFor(new URL(themeUrl));
        } catch (MalformedURLException e) {
            log.warn(e);
            return null;
        }
    }

    public synchronized List<String> getGlobalResourcesFor(URL themeUrl) {
        if (!globalCache.containsKey(themeUrl)) {
            globalCache.put(themeUrl, new ArrayList<String>());
        }
        return globalCache.get(themeUrl);
    }

    public void clearGlobalCache(String themeName) {
        List<URL> toRemove = new ArrayList<URL>();
        for (URL themeUrl : globalCache.keySet()) {
            String name = ThemeManager.getThemeNameByUrl(themeUrl);
            if (themeName.equals(name)) {
                toRemove.add(themeUrl);

            }
        }
        for (URL themeUrl : toRemove) {
            globalCache.remove(themeUrl);
        }
    }

    public static byte[] getBinaryBankResource(String resourceBankName,
            String collectionName, String typeName, String resourceName)
            throws ThemeException {
        byte[] data = null;
        ResourceBank resourceBank = ThemeManager.getResourceBank(resourceBankName);
        data = resourceBank.getResourceContent(collectionName, typeName,
                resourceName);
        if (data == null) {
            throw new ThemeException(
                    "Resource bank content could not be read: " + resourceName);
        }
        return data;
    }

    public static String getBankResource(String resourceBankName,
            String collectionName, String typeName, String resourceName)
            throws ThemeException {
        byte[] data = getBinaryBankResource(resourceBankName, collectionName,
                typeName, resourceName);
        if (data == null) {
            throw new ThemeException("Could not get bank resource: "
                    + resourceName);
        }
        return new String(data);
    }

    @Override
    public void clear() {
        globalCache.clear();
    }

}
