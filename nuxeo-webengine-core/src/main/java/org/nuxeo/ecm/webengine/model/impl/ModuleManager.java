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
 */
package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.WebEngine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleManager {

    protected Map<String, ModuleDescriptor> modules;
    protected Map<String, ModuleDescriptor> paths;
    protected WebEngine engine;
    
    
    public ModuleManager(WebEngine engine) {
        this.engine = engine;
        modules = new ConcurrentHashMap<String, ModuleDescriptor>();
        paths = new ConcurrentHashMap<String, ModuleDescriptor>();
    }
    
    /**
     * Get a module given its name
     * @return the module or null if none
     */
    public ModuleDescriptor getModule(String key) {
        return modules.get(key);
    }
    
    public ModuleDescriptor getModuleByPath(String path) {
        if (!path.startsWith("/")) {
            path = "/"+path;
        }
        return paths.get(path);
    }
    
    public ModuleDescriptor[] getModules() {
        return modules.values().toArray(new ModuleDescriptor[modules.size()]);
    }
    
    
    public synchronized void registerModule(ModuleDescriptor descriptor) {
        modules.put(descriptor.name, descriptor);
        ModuleConfiguration cfg = descriptor.getConfiguration();
        String path = cfg.path;
        if (!path.startsWith("/")) {
            path = "/"+path;
        }
        paths.put(path, descriptor);    
    }
    
    public synchronized void unregisterModule(String name) {
        ModuleDescriptor md = modules.remove(name);
        Iterator<ModuleDescriptor> it = paths.values().iterator();
        while (it.hasNext()) { // remove all module occurrence in paths map
            ModuleDescriptor p = it.next();
            if (p.name.equals(md.name)) {
                it.remove();
            }
        }
    }
    
    public synchronized void bind(String name, String path) {
        ModuleDescriptor md = modules.get(name);
        if (md != null) {
            paths.put(path, md);
        }
    }


    public void loadModules(File root) throws IOException {
        for (String name : root.list()) {
            if (!name.equals("WEB-INF")) {
                loadModule(root, name);
            }
        }
    }

    public void loadModule(File root, String name) throws IOException {
        String path = name+"/module.xml";
        File file = new File(root, path);
        if (file.isFile()) {
            ModuleDescriptor md = new ModuleDescriptor(engine, name, file);
            registerModule(md);
        } 
    }
    
    public void reloadModule(File root, String name) throws IOException {
        unregisterModule(name);
        loadModule(root, name); 
    }

 
}
