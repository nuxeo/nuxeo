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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.registry.RootSegment;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.ResourceRegistry;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceRegistryImpl implements ResourceRegistry {

    protected final Dispatcher dispatcher;
    protected final ResourceMethodRegistry registry;
    protected List<ResourceBinding> bindings;


    public ResourceRegistryImpl(Dispatcher dispatcher) {
        registry = (ResourceMethodRegistry)dispatcher.getRegistry();
        bindings = new ArrayList<ResourceBinding>();
        this.dispatcher = dispatcher;
    }

    public synchronized void addBinding(ResourceBinding binding) {
        registerBinding(binding);
        bindings.add(binding);
    }

    public void registerBinding(ResourceBinding binding) {
        if (binding.clazz == null) {
            throw new WebException(
                    "Invalid resource binding: "+binding.path
                    +" -> "+binding.clazz+". No resource class specified.");
        }

        if (binding.singleton) {
            Object obj = null;
            try {
                obj = binding.clazz.newInstance();
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
            if (binding.clazz.getAnnotation(Path.class) != null) {
                registry.addSingletonResource(obj);
            } else {
                registry.addResourceFactory(new SingletonResource(obj), binding.path, binding.clazz);
            }
        } else {
            if (binding.clazz.getAnnotation(Path.class) != null) {
                registry.addPerRequestResource(binding.clazz);
            } else {
                registry.addResourceFactory(new POJOResourceFactory(binding.clazz), binding.path, binding.clazz);
            }
        }
    }

    public synchronized void removeBinding(ResourceBinding binding) {
        unregisterBinding(binding);
        bindings.remove(binding);
    }

    public void unregisterBinding(ResourceBinding binding) {
        try {
            if (binding.clazz.getAnnotation(Path.class) == null) {
                removeRegistration(binding.path, binding.clazz);
            } else {
                registry.removeRegistrations(binding.clazz);
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public synchronized ResourceBinding[] getBindings() {
        return bindings.toArray(new ResourceBinding[bindings.size()]);
    }

    public synchronized void reload() {
        clearRegistrations();
        for (ResourceBinding binding : bindings) {
            registerBinding(binding);
        }
    }

    public synchronized void clear() {
        clearRegistrations();
        bindings = new ArrayList<ResourceBinding>();
    }

    public void clearRegistrations() {
        Field f;
        try {
            f = registry.getClass().getDeclaredField("rootSegment");
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            RootSegment rootSegment = new RootSegment();
            f.set(registry, rootSegment);
        } catch (Exception e) {
            throw WebException.wrap("Failed to reload resources", e);
        }
    }

    protected void removeRegistration(String base, Class<?> clazz) throws Exception {
        Method m = registry.getClass().getDeclaredMethod("removeRegistration", String.class, Class.class);
        m.setAccessible(true);
        m.invoke(registry, base, clazz);
    }

    public void addMessageBodyReader(MessageBodyReader<?> reader) {
        dispatcher.getProviderFactory().addMessageBodyReader(reader);
    }

    public void addMessageBodyWriter(MessageBodyWriter<?> writer) {
        dispatcher.getProviderFactory().addMessageBodyWriter(writer);
    }


}
