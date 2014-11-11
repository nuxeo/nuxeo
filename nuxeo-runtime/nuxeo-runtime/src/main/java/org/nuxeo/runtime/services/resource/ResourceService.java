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
package org.nuxeo.runtime.services.resource;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ReloadableComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceService extends ReloadableComponent {

    public final static String XP_RESOURCES = "resources";

    protected Map<String, URL> registry;

    public ResourceService() {
    }

    public URL getResource(String name) {
        return registry.get(name);
    }

    public void addResource(ResourceDescriptor resource) {
        addResource(resource.getName(), resource.getResource().toURL());
    }

    public void addResource(String name, URL url) {
        registry.put(name, url);
    }

    public URL removeResource(String name) {
        return registry.remove(name);
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry = new ConcurrentHashMap<String, URL>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_RESOURCES.equals(extensionPoint)) {
            addResource((ResourceDescriptor)contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_RESOURCES.equals(extensionPoint)) {
            ResourceDescriptor rd = (ResourceDescriptor)contribution;
            ResourceDescriptor last = findLastContributedResource(rd.getName());
            if (last != null) {
                addResource(last);
            } else {
                removeResource(rd.getName());
            }
        }
    }

    protected ResourceDescriptor findLastContributedResource(String name) {
        for (int i=extensions.size()-1; i>=0; i--) {
            Extension xt = extensions.get(i);
            Object[] contribs = xt.getContributions();
            for (int k=contribs.length-1; k>=0; k--) {
                ResourceDescriptor r = (ResourceDescriptor)contribs[k];
                if (name.equals(r.getName())) {
                    return r;
                }
            }
        }
        return null;
    }
}
