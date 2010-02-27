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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.BundledApplication;
import org.nuxeo.ecm.webengine.app.ModuleHandler;
import org.nuxeo.ecm.webengine.app.annotations.ResourceExtension;

/**
 * An internal registry implementing the internal work of registering applications and extensions.
 * The registry is not thread safe and neither secure to concurrent modifications - synchronization is ensured by the application manager.
 * Modifying the registry is happening only at deploy and redeploy time.  
 * At runtime only lookups are performed. 
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleRegistry {

    protected WebEngine engine;
    
    /**
     * A map between bundled application ID and the provided WebEngine module
     */
    protected Map<String, ModuleHandler> modules;
    
    /**
     * Mapping between root resource type and its module
     */
    protected Map<String, ModuleHandler> modulesByRoot;
    
    /**
     * Resources contributed from an applications to other applications.
     * A map of key : value where key is the target parent resource class 
     * and value is a list of contributed child resources.
     */
    protected Map<Class<?>, ResourceContributions> contributions;
    
    //TODO impl resource factories
    //protected Map<String, ResourceFactory> factories;


    public ModuleRegistry(WebEngine engine) {
        this.engine = engine;
        modules = new HashMap<String, ModuleHandler>();
        modulesByRoot = new HashMap<String, ModuleHandler>();
        contributions = new HashMap<Class<?>, ResourceContributions>();
    }
    
    public ModuleHandler[] getModuleHandlers() {
        return modulesByRoot.values().toArray(new ModuleHandler[modulesByRoot.size()]);
    }
    
    public ModuleHandler getModuleHandlerFor(Class<?> rootResource) {
        return modulesByRoot.get(rootResource.getName());
    }

    public ModuleHandler getModuleHandler(String appId) {
        return modules.get(appId);
    } 
    
    public void addApplication(BundledApplication app) {
        if (app.isWebEngineModule()) {
            ModuleHandler mh = new ModuleHandler(engine, app);
            modules.put(app.getId(), mh);
            Class<?>[] rc = mh.getRootClasses();
            for (Class<?> c : rc) {
                modulesByRoot.put(c.getName(), mh);
            }
            addContributions(app, mh);
        }
    }
    
    public BundledApplication removeApplication(BundledApplication app) {
        ModuleHandler mh = modules.remove(app.getId());
        if (mh != null) {
            Class<?>[] rc = mh.getRootClasses();
            for (Class<?> c : rc) {
                modulesByRoot.remove(c.getName());
            }            
            removeContributions(app, mh);
        }
        return app;
    }

    public Object getContribution(Object target, String key) throws Exception {
        ResourceContributions rcs = contributions.get(target.getClass().getName());
        if (rcs != null) {
            Class<?> type = rcs.getContribution(key);
            Object obj = type.newInstance();
            //TODO ((ContributedResource)obj).initialize(target);
            return obj;
        }
        return null;
    }

    protected void addContributions(BundledApplication app, ModuleHandler mh) {
        Class<?>[] xts = mh.getContributions();
        if (xts != null && xts.length > 0) {
            for (Class<?> xt : xts) {
                ResourceExtension rxt = xt.getAnnotation(ResourceExtension.class);
                if (rxt == null) { // should never happen
                    throw new Error("Trying to export a resource extension which is not annotated with "+ResourceExtension.class+" in bundle "+app.getId()+", resource extension: "+xt);
                }
                Class<?> target = rxt.target();
                ResourceContributions rcs = contributions.get(target);
                if (rcs == null) {
                    rcs = new ResourceContributions(target);
                    contributions.put(target, rcs);
                }
                rcs.addContribution(rxt.key(), xt);
            }
        }
    }

    protected void removeContributions(BundledApplication app, ModuleHandler mh) {
        Class<?>[] xts = mh.getContributions();
        if (xts != null && xts.length > 0) {
            for (Class<?> xt : xts) {
                ResourceExtension rxt = xt.getAnnotation(ResourceExtension.class);
                if (rxt == null) { // should never happen
                    throw new Error("Trying to remove a resource extension which is not annotated with "+ResourceExtension.class+" in bundle "+app.getId()+", resource extension: "+xt);
                }
                String key = rxt.target().getName();
                ResourceContributions rcs = contributions.get(key);
                rcs.removeContribution(rxt.key());
                // remove rcs if empty?
            }
        }
    }
    
}
