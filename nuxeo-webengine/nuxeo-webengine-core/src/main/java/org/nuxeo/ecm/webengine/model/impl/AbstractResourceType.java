/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TypeVisibility;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotationManager;

import com.sun.jersey.server.spi.component.ResourceComponentConstructor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractResourceType implements ResourceType {

    protected final WebEngine engine;

    protected final Module owner;

    protected final ResourceComponentConstructor constructor;

    protected final String name;

    protected int visibility = TypeVisibility.DEFAULT;

    protected AbstractResourceType superType;

    protected volatile ClassProxy clazz;

    protected volatile Guard guard = Guard.DEFAULT;

    protected volatile Set<String> facets;

    protected volatile ConcurrentMap<String, ScriptFile> templateCache;

    protected AbstractResourceType(WebEngine engine, Module module, AbstractResourceType superType, String name,
            ClassProxy clazz, ResourceComponentConstructor constructor, int visibility) {
        this.engine = engine;
        owner = module;
        this.superType = superType;
        this.name = name;
        this.clazz = clazz;
        this.constructor = constructor;
        this.visibility = visibility;
        templateCache = new ConcurrentHashMap<>();
        AnnotationManager mgr = engine.getAnnotationManager();
        loadAnnotations(mgr);
    }

    public int getVisibility() {
        return visibility;
    }

    protected abstract void loadAnnotations(AnnotationManager annoMgr);

    @Override
    public ResourceType getSuperType() {
        return superType;
    }

    public Module getOwnerModule() {
        return owner;
    }

    @Override
    public Guard getGuard() {
        return guard;
    }

    @Override
    public Set<String> getFacets() {
        return facets;
    }

    @Override
    public boolean hasFacet(String facet) {
        return facets != null && facets.contains(facet);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Resource> getResourceClass() {
        return (Class<Resource>) clazz.get();
    }

    @Override
    public <T extends Resource> T newInstance(Class<T> typeof, WebContext context) {
        try {
            return typeof.cast(constructor.construct(context.getServerHttpContext()));
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Failed to instantiate web object: " + clazz, e);
        }
    }

    @Override
    public boolean isEnabled(Resource ctx) {
        return guard.check(ctx);
    }

    @Override
    public boolean isDerivedFrom(String type) {
        if (type.equals(name)) {
            return true;
        }
        if (superType != null) {
            return superType.isDerivedFrom(type);
        }
        return false;
    }

    @Override
    public void flushCache() {
        templateCache = new ConcurrentHashMap<>();
    }

    protected void loadGuardFromAnnoation(Class<?> c) {
        org.nuxeo.ecm.webengine.model.Guard ag = c.getAnnotation(org.nuxeo.ecm.webengine.model.Guard.class);
        if (ag != null) {
            String g = ag.value();
            if (g != null && g.length() > 0) {
                try {
                    guard = PermissionService.parse(g);
                } catch (ParseException e) {
                    throw new NuxeoException("Failed to parse guard: " + g + " on WebObject " + c.getName(), e);
                }
            } else {
                Class<?> gc = ag.type();
                if (gc != null) {
                    try {
                        guard = (Guard) gc.getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new NuxeoException(
                                "Failed to instantiate guard handler: " + gc.getName() + " on WebObject " + c.getName(),
                                e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return name + " extends " + superType + " [" + getResourceClass().getName() + "]";
    }

    @Override
    public ScriptFile getView(Module module, String name) {
        ScriptFile file = findView(module, name);
        if (file == null) {
            throw new WebResourceNotFoundException("Template " + name + " not found for object of type " + getName());
        }
        return file;
    }

    public ScriptFile findView(Module module, String name) {
        ScriptFile file = templateCache.get(name);
        if (file != null) {
            return file;
        }
        file = findSkinTemplate(module, name);
        if (file == null) {
            file = findTypeTemplate(module, name);
        }
        if (file == null) {
            AbstractResourceType t = (AbstractResourceType) getSuperType();
            if (t != null) {
                file = t.findView(module, name);
            }
        }
        if (file != null) {
            templateCache.put(name, file);
        }
        return file;
    }

    protected ScriptFile findSkinTemplate(Module module, String name) {
        return module.getFile(new StringBuilder().append("views")
                                                 .append(File.separatorChar)
                                                 .append(this.name)
                                                 .append(File.separatorChar)
                                                 .append(name)
                                                 .toString());
    }

    protected ScriptFile findTypeTemplate(Module module, String name) {
        String path = resolveResourcePath(clazz.getClassName(), name);
        URL url = clazz.get().getResource(path);
        if (url != null) {
            if (!"file".equals(url.getProtocol())) {
                // TODO ScriptFile is not supporting URLs .. must refactor ScriptFile
                return null;
            }
            try {
                return new ScriptFile(new File(url.toURI()));
            } catch (IOException | URISyntaxException e) {
                throw new NuxeoException("Failed to convert URL to URI: " + url, e);
            }
        }
        return null;
    }

    protected String resolveResourcePath(String className, String fileName) {
        // compute resource path for resource class name
        String path = className;
        int p = path.lastIndexOf('.');
        if (p > -1) {
            path = path.substring(0, p);
            path = path.replace('.', '/');
            return new StringBuilder().append('/').append(path).append('/').append(fileName).toString();
        }
        return new StringBuilder().append('/').append(fileName).toString();
    }

}
