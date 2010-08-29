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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TemplateNotFoundException;
import org.nuxeo.ecm.webengine.model.TypeVisibility;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractResourceType implements ResourceType {

    protected final ModuleImpl owner;
    protected final String name;
    protected int visibility = TypeVisibility.DEFAULT;
    protected AbstractResourceType superType;
    protected volatile ClassProxy clazz;
    protected volatile Guard guard = Guard.DEFAULT;
    protected volatile Set<String> facets;
    protected volatile ConcurrentMap<String, ScriptFile> templateCache;


    protected AbstractResourceType(WebEngine engine, ModuleImpl module,
            AbstractResourceType superType, String name, ClassProxy clazz, int visibility) {
        templateCache = new ConcurrentHashMap<String, ScriptFile>();
        owner = module;
        this.superType = superType;
        this.name = name;
        this.clazz = clazz;
        this.visibility = visibility;
        AnnotationManager mgr = engine.getAnnotationManager();
        loadAnnotations(mgr);
    }

    public int getVisibility() {
        return visibility;
    }

    protected abstract void loadAnnotations(AnnotationManager annoMgr);

    public ResourceType getSuperType() {
        return superType;
    }

    public Module getOwnerModule() {
        return owner;
    }

    public Guard getGuard() {
        return guard;
    }

    public Set<String> getFacets() {
        return facets;
    }

    public boolean hasFacet(String facet) {
        return facets != null && facets.contains(facet);
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public Class<Resource> getResourceClass() {
        return (Class<Resource>)clazz.get();
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> T newInstance() {
        try {
            return (T)clazz.get().newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate web object: "+clazz, e);
        }
    }

    public boolean isEnabled(Resource ctx) {
        return guard.check(ctx);
    }

    public boolean isDerivedFrom(String type) {
        if (type.equals(name)) {
            return true;
        }
        if (superType != null) {
            return superType.isDerivedFrom(type);
        }
        return false;
    }

    public void flushCache() {
        templateCache = new ConcurrentHashMap<String, ScriptFile>();
    }

    protected void loadGuardFromAnnoation(Class<?> c) {
        org.nuxeo.ecm.webengine.model.Guard ag = c.getAnnotation(org.nuxeo.ecm.webengine.model.Guard.class);
        if (ag != null) {
            String g = ag.value();
            if (g != null && g.length() > 0) {
                try {
                    guard = PermissionService.parse(g);
                } catch (ParseException e) {
                    throw WebException.wrap(
                            "Failed to parse guard: "+g+" on WebObject "+c.getName(), e);
                }
            } else {
                Class<?> gc = ag.type();
                if (gc != null) {
                    try {
                        guard = (Guard) gc.newInstance();
                    } catch (Exception e) {
                        throw WebException.wrap(
                                "Failed to instantiate guard handler: "+gc.getName()
                                +" on WebObject "+c.getName(), e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return name + " extends " + superType + " [" + getResourceClass().getName() + "]";
    }

    public ScriptFile getView(Module module, String name) {
        ScriptFile file = findView(module, name);
        if (file == null) {
            throw new TemplateNotFoundException(this, name);
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
                .append(File.separatorChar).append(this.name)
                .append(File.separatorChar).append(name).toString());
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
            } catch (Exception e) {
                throw WebException.wrap("Failed to convert URL to URI: "+url, e);
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
            return new StringBuilder().append('/').append(path)
                    .append('/').append(fileName).toString();
        }
        return new StringBuilder().append('/')
                .append(fileName).toString();
    }

}
