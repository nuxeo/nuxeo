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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleRegistry extends AbstractContributionRegistry<String, ModuleDescriptor> {

    protected final Map<String, Module> modules = new ConcurrentHashMap<String, Module>();
    protected final Map<String, Module> moduleRoots = new ConcurrentHashMap<String, Module>();
    protected final WebEngine engine;


    public ModuleRegistry(WebEngine engine) {
        this.engine = engine;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public Module getModuleByRoot(String rootName) {
        return moduleRoots.get(rootName);
    }

    public void putModule(Module app) {
        modules.put(app.getName(), app);
    }

    public Module removeModule(String id) {
        return modules.remove(id);
    }

    public Module getModule(String id) {
        return modules.get(id);
    }

    public Module[] getModules() {
        return modules.values().toArray(new Module[modules.size()]);
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        modules.clear();
        moduleRoots.clear();
    }

    protected ModuleDescriptor clone(ModuleDescriptor descriptor) {
        return descriptor.clone();
    }

    @Override
    protected void applyFragment(ModuleDescriptor object, ModuleDescriptor fragment) {
        if (fragment.guardDescriptor != null) {
            object.guardDescriptor = fragment.guardDescriptor;
        }
    }

    @Override
    protected void installContribution(String key, ModuleDescriptor object) {
        try {
            Module app = new ModuleImpl(engine, object.directory, object);
            modules.put(key, app);
            if (object.directory != null) {
                moduleRoots.put(object.directory.getName(), app);
            }
        } catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    protected void updateContribution(String key, ModuleDescriptor object, ModuleDescriptor oldValue) {
        installContribution(key, object);
    }

    @Override
    protected void uninstallContribution(String key, ModuleDescriptor value) {
        modules.remove(key);
    }

    @Override
    protected boolean isMainFragment(ModuleDescriptor object) {
        return object.fragment == null || object.fragment.length() == 0;
    }

    public boolean registerDescriptor(File root, ModuleDescriptor desc) {
        // avoid loading twice the same module
        if (moduleRoots.containsKey(root.getName())) {
            return false;
        }
        desc.directory = root;
        addFragment(desc.name, desc, desc.base);
        return true;
    }

    public void unregisterDescriptor(ModuleDescriptor desc) {
        removeFragment(desc.name, desc);
    }

}
