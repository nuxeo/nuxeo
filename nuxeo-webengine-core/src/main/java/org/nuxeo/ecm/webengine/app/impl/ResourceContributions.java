/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.app.annotations.ResourceExtension;
import org.nuxeo.ecm.webengine.app.extensions.ExtensibleResource;
import org.nuxeo.ecm.webengine.app.extensions.ResourceContribution;

/**
 * Describe the external contributions on a parent resource
 *
 * Class names are used to store references to involved resources instead of class objects since
 * WebEngine is using a specific class loader (not the one used by the framework) to load resources - and doing so we avoid class cast exceptions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceContributions {

    /**
     * The target resource class name
     */
    protected Class<?> target;

    /**
     * A map of key : values where key is the segment name that should match a contribution resource and value is the contribution resource class
     */
    protected Map<String, ResourceContribution> contribs;

    /**
     * contributions by category cache
     */
    protected ConcurrentMap<String, List<ResourceContribution>> contribsByCategories;


    public ResourceContributions(Class<?> target) {
        this.target = target;
        contribs = new HashMap<String, ResourceContribution>();
        contribsByCategories = new ConcurrentHashMap<String, List<ResourceContribution>>();
    }

    public Class<?> getTarget() {
        return target;
    }

    public ResourceContribution getContribution(String key) {
        return contribs.get(key);
    }

    public ResourceContribution[] getContributions() {
        return contribs.values().toArray(new ResourceContribution[contribs.size()]);
    }

    public List<ResourceContribution> getContributions(ExtensibleResource target, String category) {
        List<ResourceContribution> contribs = getContributions(target.getClass(), category);
        if (!contribs.isEmpty()) {
            List<ResourceContribution> result = new ArrayList<ResourceContribution>();
            for (ResourceContribution c : contribs) {
                if (c.accept(target)) {
                    result.add(c);
                }
            }
            return result;
        }
        return contribs;
    }

    public List<ResourceContribution> getContributions(Class<? extends ExtensibleResource> target, String category) {
        List<ResourceContribution> result = contribsByCategories.get(category);
        if (result == null) {
            result = new ArrayList<ResourceContribution>();
            for (ResourceContribution c : contribs.values()) {
                if (c.hasCategory(category)) {
                    result.add(c);
                }
            }
            contribsByCategories.put(category, result);
        }
        return result;
    }

    public void addContribution(ResourceContribution c) throws Exception {
        contribs.put(c.getKey(), c);
    }

    public void removeContribution(String key) {
        contribs.remove(key);
    }

    public void addContribution(Class<? extends ResourceContribution> contrib) throws Exception {
        ResourceContribution c = contrib.newInstance();
        addContribution(c);
    }

    public void removeContribution(Class<?> contrib) {
        ResourceExtension xt = contrib.getAnnotation(ResourceExtension.class);
        if (xt != null) {
            removeContribution(xt.key());
        }
    }

}
