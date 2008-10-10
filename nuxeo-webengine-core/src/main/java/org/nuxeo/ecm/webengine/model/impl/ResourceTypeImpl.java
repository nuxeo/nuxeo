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
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TemplateNotFoundException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceTypeImpl implements ResourceType {
    
    protected ModuleImpl module;
    protected String name;
    protected ResourceTypeImpl superType;
    protected volatile ClassProxy clazz;
    protected volatile Guard guard = Guard.DEFAULT;
    protected volatile Set<String> facets;    
    protected volatile ConcurrentMap<String, ScriptFile> templateCache;
    
    public ResourceTypeImpl(ModuleImpl module, ResourceTypeImpl superType, String name, ClassProxy clazz) {
        this.templateCache = new ConcurrentHashMap<String, ScriptFile>();        
        this.module = module;
        this.superType = superType;
        this.name = name;
        this.clazz = clazz;        
        AnnotationManager mgr = module.engine.getAnnotationManager();        
        loadAnnotations(mgr);        
    }
    
    public ResourceType getSuperType() {
        return superType;
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
    
    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }
    
    public Class<Resource> getResourceClass() {
        return (Class<Resource>)clazz.get();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Resource> T newInstance() throws WebException {
        try {           
            return (T)clazz.get().newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate web object: "+clazz, e);
        }
    }
    

    public boolean isEnabled(Resource ctx) {
        return guard.check(ctx);
    }
    
     
    protected void loadAnnotations(AnnotationManager annoMgr) {
        WebObject wo = clazz.get().getAnnotation(WebObject.class);
        if (wo == null) return;
        String g = wo.guard();
        if (g != null && g.length() > 0) {
            try {
                guard = PermissionService.parse(g);
            } catch (ParseException e) {
                throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+clazz.get().getName(), e);
            }
        }
        String[] facets = wo.facets();
        if (facets != null && facets.length > 0) {
            this.facets = new HashSet<String>(Arrays.asList(facets));
        }
    }
    
    
    public ScriptFile getTemplate(String name) throws WebException {
        if (name == null) {
            name = "view.ftl"; 
        }
        ScriptFile file = findTemplate(name);
        if (file == null) {
            throw new TemplateNotFoundException(this, name);
        }
        return file;
    }
    
    public ScriptFile findTemplate(String name) throws WebException {
        ScriptFile file = templateCache.get(name);
        if (file != null) {
            return file;
        }
        try {
            file = findSkinTemplate(name);
            if (file == null) {
                file = findTypeTemplate(name);
            }
        } catch (IOException e) {
            WebException.wrap("Failed to find template: "+name, e);
        }
        if (file != null) {
            templateCache.put(name, file);
        }
        return file;
    }
    
    protected ScriptFile findSkinTemplate(String name) throws IOException {
        return module.getFile(new StringBuilder().append("templates/")
                .append(this.name).append("/").append(name).toString());
    }
    
    protected ScriptFile findTypeTemplate(String name) throws IOException {
        String path = resolveResourcePath(clazz.getClassName(), name);
        File f = new File(module.getEngine().getRootDirectory(), path);
        if (f.isFile()) {
            return new ScriptFile(f);
        }
        ScriptFile file = null;
        ResourceTypeImpl t =(ResourceTypeImpl)getSuperType();
        while (t != null) {
            file = t.findTemplate(name);
            if (file != null) {
                break;
            }
            t = (ResourceTypeImpl)t.getSuperType();
        }     
        return file;
    }

    protected String resolveResourcePath(String className, String fileName) {
        // compute resource path for resource class name
        String path = className;
        int p = path.lastIndexOf('.');
        if (p > -1) {
            path = path.substring(0, p);
        }
        path = path.replace('.', '/');
        return new StringBuilder()
        .append("/")
        .append(path)
        .append('/')
        .append(fileName)
        .toString();        
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
        this.templateCache = new ConcurrentHashMap<String, ScriptFile>();
    }

}
