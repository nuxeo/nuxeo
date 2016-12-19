/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.services.resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// FIXME: make it handle hot reload correctly
public class ResourceService extends DefaultComponent {

    public final static String XP_RESOURCES = "resources";

    protected Map<String, URL> registry;

    protected List<Extension> extensions = new ArrayList<>();

    public ResourceService() {
    }

    @Override
    public void registerExtension(Extension extension) {
        super.registerExtension(extension);
        extensions.add(extension);
    }

    @Override
    public void unregisterExtension(Extension extension) {
        extensions.remove(extension);
        super.unregisterExtension(extension);
    }

    public void reload(ComponentContext context) {
        deactivate(context);
        activate(context);
        for (Extension xt : extensions) {
            super.registerExtension(xt);
        }
    }

    public List<Extension> getExtensions() {
        return extensions;
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
    public void activate(ComponentContext context) {
        registry = new ConcurrentHashMap<String, URL>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_RESOURCES.equals(extensionPoint)) {
            addResource((ResourceDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_RESOURCES.equals(extensionPoint)) {
            ResourceDescriptor rd = (ResourceDescriptor) contribution;
            ResourceDescriptor last = findLastContributedResource(rd.getName());
            if (last != null) {
                addResource(last);
            } else {
                removeResource(rd.getName());
            }
        }
    }

    protected ResourceDescriptor findLastContributedResource(String name) {
        for (int i = extensions.size() - 1; i >= 0; i--) {
            Extension xt = extensions.get(i);
            Object[] contribs = xt.getContributions();
            for (int k = contribs.length - 1; k >= 0; k--) {
                ResourceDescriptor r = (ResourceDescriptor) contribs[k];
                if (name.equals(r.getName())) {
                    return r;
                }
            }
        }
        return null;
    }
}
