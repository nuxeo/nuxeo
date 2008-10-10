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

package org.nuxeo.ecm.webengine.server.resteasy;

import java.lang.reflect.Field;

import javax.ws.rs.Path;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.registry.RootSegment;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.ResourceRegistry;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineResourceLoader implements ResourceRegistry {

    protected ResourceMethodRegistry registry;
    protected WebEngine engine;
    
    
    public WebEngineResourceLoader(WebEngine engine, ResourceMethodRegistry registry) {
        this.registry = registry;
        this.engine = engine;
        //TODO register registry earlier in booting process
        this.engine.setRegistry(this);
    }
    
    public void addResourceBinding(ResourceBinding binding) throws WebException {
        if (binding.className != null && binding.path != null) {
            Class<?> clazz = null;
            try {
                clazz = engine.loadClass(binding.className);
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
            if (binding.singleton) {
                Object obj = null;
                try {
                    obj = clazz.newInstance();
                } catch (Exception e) {
                    throw WebException.wrap(e);
                }
                if (clazz.getAnnotation(Path.class) != null) {
                    registry.addSingletonResource(obj);  
                } else {
                    registry.addResourceFactory(new SingletonResource(obj), binding.path, clazz);  
                }    
            } else {
                if (clazz.getAnnotation(Path.class) != null) {
                    registry.addPerRequestResource(clazz);  
                } else {
                    registry.addResourceFactory(new POJOResourceFactory(clazz), binding.path, clazz);  
                }      
            }
        } else {
            throw new WebException("Invalid resource binding: "+binding.path+" -> "+binding.className+". No resource path / class specified.");
        }      
    }
    
    public void removeResourceBinding(ResourceBinding binding)  throws WebException {
        try {
            Class<?> clazz = engine.loadClass(binding.className);
            if (clazz.getAnnotation(Path.class) != null) {
                removeRegistrations(binding.path, clazz);
            } else {
                registry.removeRegistrations(clazz);  
            }
        } catch (Exception e) {
            throw WebException.wrap(e); 
        }
    }

    protected void removeRegistrations(String base, Class<?> clazz) throws Exception {
        java.lang.reflect.Method m = registry.getClass().getDeclaredMethod("removeRegistrations", String.class, Class.class);
        m.invoke(registry, base, clazz);
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
                load();
            }
        } catch (Exception e) {
            throw WebException.wrap("Failed to reload resources", e);
        }
    }
    
    public void load() {       
        for (ResourceBinding binding : engine.getBindings()) {
            addResourceBinding(binding);
        }
    }
    
    
}
