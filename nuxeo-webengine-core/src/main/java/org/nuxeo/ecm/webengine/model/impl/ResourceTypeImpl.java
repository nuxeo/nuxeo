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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TemplateNotFoundException;
import org.nuxeo.ecm.webengine.model.ViewDescriptor;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.WebView;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotatedClass;
import org.nuxeo.runtime.annotations.AnnotatedMethod;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceTypeImpl implements ResourceType {
    
    protected ModuleImpl module;
    protected String name;
    protected ResourceTypeImpl superType;
    // the views and class may be changed when a type is updated
    protected volatile Map<String,ViewDescriptor> views;
    protected volatile Class<Resource> clazz;
    protected volatile Guard guard = Guard.DEFAULT;
    protected volatile Set<String> facets;    
    protected volatile ConcurrentMap<String, ScriptFile> templateCache;
    
    public ResourceTypeImpl(ModuleImpl module, ResourceTypeImpl superType, String name, Class<Resource> clazz) {
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
    
    public Class<Resource> getObjectType() {
        return this.clazz; 
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
        return clazz;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Resource> T newInstance() throws WebException {
        try {            
            return (T)clazz.newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate web object: "+clazz, e);
        }
    }
    
    public ViewDescriptor getView(String name) {
        return views.get(name);
    }

    public List<ViewDescriptor> getViews() {
        return new ArrayList<ViewDescriptor>(views.values());  
    }
    
    public List<ViewDescriptor> getViews(String category) {
        ArrayList<ViewDescriptor> result = new ArrayList<ViewDescriptor>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.hasCategory(category)) {
                result.add(vd);
            }            
        }
        return result;  
    }
    
    public List<ViewDescriptor> getEnabledViews(Resource obj) {
        ArrayList<ViewDescriptor> result = new ArrayList<ViewDescriptor>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.isEnabled(obj)) {
                result.add(vd);
            }            
        }
        return result;
    }
    
    public List<ViewDescriptor> getEnabledViews(Resource obj, String category) {
        ArrayList<ViewDescriptor> result = new ArrayList<ViewDescriptor>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.hasCategory(category) && vd.isEnabled(obj)) {
                result.add(vd);
            }            
        }
        return result;  
    }

    public List<String> getViewNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (ViewDescriptor vd : views.values()) {
            result.add(vd.getName());
        }
        return result;    
    }
    
    public List<String> getViewNames(String category) {
        ArrayList<String> result = new ArrayList<String>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.hasCategory(category)) {
                result.add(vd.getName());
            }            
        }
        return result;  
    }
    
    public List<String> getEnabledViewNames(Resource obj) {
        ArrayList<String> result = new ArrayList<String>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.isEnabled(obj)) {
                result.add(vd.getName());
            }            
        }
        return result;
    }
    
    public List<String> getEnabledViewNames(Resource obj, String category) {
        ArrayList<String> result = new ArrayList<String>();
        for (ViewDescriptor vd : views.values()) {
            if (vd.hasCategory(category) && vd.isEnabled(obj)) {
                result.add(vd.getName());
            }            
        }
        return result;  
    }

    public boolean isEnabled(Resource ctx) {
        return guard.check(ctx);
    }
    

    protected void loadViews(AnnotationManager annoMgr) {
        views = new HashMap<String, ViewDescriptor>();
        AnnotatedClass<?> ac = annoMgr.load(clazz);
        AnnotatedMethod[] methods = ac.getAnnotatedMethods(WebView.class);
        for (AnnotatedMethod m : methods) {
            WebView anno = m.getAnnotation(WebView.class);
            try {
                ViewDescriptor vd = new ViewDescriptor(anno);
                // register the view
                views.put(vd.getName(), vd);                
            } catch (ParseException e) {
                WebException.wrap("Failed to parse view guard "+anno.guard(), e);
            }
        }
    }
     
    protected void loadAnnotations(AnnotationManager annoMgr) {
        loadViews(annoMgr);
        WebObject wo = clazz.getAnnotation(WebObject.class);
        if (wo == null) return;
        String g = wo.guard();
        if (g != null && g.length() > 0) {
            try {
                guard = PermissionService.parse(g);
            } catch (ParseException e) {
                throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+clazz.getName(), e);
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
        String path = resolveResourcePath(clazz, name);
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

    protected String resolveResourcePath(Class<?> resClass, String fileName) {
        // compute resource path for resource class name
        String path = resClass.getName();
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
