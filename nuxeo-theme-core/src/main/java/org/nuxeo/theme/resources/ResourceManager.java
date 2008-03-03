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

    private final Map<URL, List<String>> cache = new HashMap<URL, List<String>>();

    private final TypeRegistry typeRegistry = Manager.getTypeRegistry();

    public void addResource(String name, URL themeUrl) {
        ResourceType resourceType = (ResourceType) typeRegistry.lookup(
                TypeFamily.RESOURCE, name);
        if (resourceType != null) {
            for (String dependency : resourceType.getDependencies()) {
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

    public List<String> getResourcesFor(URL themeUrl) {
        if (!cache.containsKey(themeUrl)) {
            cache.put(themeUrl, new ArrayList<String>());
        }
        return cache.get(themeUrl);
    }

    public void clear() {
        // TODO Auto-generated method stub
    }

}
