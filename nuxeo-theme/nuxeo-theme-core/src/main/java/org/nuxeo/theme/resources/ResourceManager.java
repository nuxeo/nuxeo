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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ResourceManager implements Registrable {

    private static final Log log = LogFactory.getLog(ResourceManager.class);

    private final Map<URI, List<String>> cache = new HashMap<URI, List<String>>();

    private final TypeRegistry typeRegistry = Manager.getTypeRegistry();

    public void addResource(String name, URL themeUrl) {
        log.debug("Theme resource: " + themeUrl + " -> " + name);
        ResourceType resourceType = (ResourceType) typeRegistry.lookup(
                TypeFamily.RESOURCE, name);
        if (resourceType != null) {
            for (String dependency : resourceType.getDependencies()) {
                log.debug("  Subresource dependency: " + name + " -> " + dependency);
                addResource(dependency, themeUrl);
            }

            List<String> scripts = getResourcesFor(themeUrl);
            if (!scripts.contains(name)) {
                scripts.add(name);
            }
        } else {
            log.warn("Resource not found: " + name);
        }
    }

    public List<String> getResourcesFor(String themeUrl) {
        try {
            return getResourcesFor(new URL(themeUrl));
        } catch (MalformedURLException e) {
            log.warn(e);
            return null;
        }
    }

    public List<String> getResourcesFor(URL themeUrl) {
        URI uri;
        try {
            uri = themeUrl.toURI();
        } catch (URISyntaxException e) {
            log.warn(e);
            return null;
        }
        if (!cache.containsKey(uri)) {
            cache.put(uri, new ArrayList<String>());
        }
        return cache.get(uri);
    }

    public void clear() {
        // TODO Auto-generated method stub
    }

}
