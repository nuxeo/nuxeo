/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.server.resteasy.registry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Path;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.registry.RootSegment;
import org.nuxeo.ecm.webengine.ResourceRegistry;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineRegistry implements ResourceRegistry {

    protected ResourceMethodRegistry registry;
    protected Map<String, ResourceRegistration> resources;
    
    public WebEngineRegistry(ResourceMethodRegistry registry) {
        this.registry = registry;
        this.resources =  new ConcurrentHashMap<String,ResourceRegistration>(); 
    }
    
    public void register(ResourceRegistration reg) {
        resources.put(reg.getResourcePath(), reg);
        reg.register(registry);        
    }
    
    public void registerPerRequestResource(Object resource) {
        register(new PerRequestRegistration(resource));
    }

    public void registerPerRequestResource(String path, Object resource) {
        register(new PerRequestRegistration(resource, path));
    }

    public void registerSingletonResource(Object resource) {
        register(new SingletonRegistration(resource));
    }

    public void registerSingletonResource(String path, Object resource) {
        register(new SingletonRegistration(resource, path));
    }

    public void unregisterResource(String path) {
        ResourceRegistration reg = resources.remove(path);
        if (reg != null) {
            reg.unregister(registry);
        }        
    }

    public void unregisterResource(Class<?> clazz) {
        unregisterResource(clazz.getAnnotation(Path.class).value());
    }

    public void reload() {
        // TODO: submit new feature request to be able to reload resources
        Field f;
        try {
            f = registry.getClass().getDeclaredField("rootSegment");        
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            RootSegment rootSegment = new RootSegment();
            synchronized (this) {
                f.set(registry, rootSegment);
                for (ResourceRegistration reg : resources.values().toArray(new ResourceRegistration[resources.size()])) {
                    reg.register(registry);
                }
            }
        } catch (Exception e) {
            throw WebException.wrap("Failed to reload resources", e);
        }
    }

    
}
