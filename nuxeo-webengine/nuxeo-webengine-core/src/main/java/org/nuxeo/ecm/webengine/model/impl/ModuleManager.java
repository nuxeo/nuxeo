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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleManager {

    private static final Log log = LogFactory.getLog(ModuleManager.class);

    protected final Map<String, ModuleConfiguration> modules;

    protected final Map<String, ModuleConfiguration> paths;

    protected final Map<Class<?>, ModuleConfiguration> roots;

    protected WebEngine engine;

    public ModuleManager(WebEngine engine) {
        this.engine = engine;
        modules = new ConcurrentHashMap<String, ModuleConfiguration>();
        paths = new ConcurrentHashMap<String, ModuleConfiguration>();
        roots = new ConcurrentHashMap<Class<?>, ModuleConfiguration>();
    }

    /**
     * Gets a module given its name.
     *
     * @return the module or null if none
     */
    public ModuleConfiguration getModule(String key) {
        return modules.get(key);
    }

    public ModuleConfiguration getModuleByPath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return paths.get(path);
    }

    public ModuleConfiguration getRootModule() {
        return paths.get("/");
    }

    public ModuleConfiguration[] getModules() {
        return modules.values().toArray(new ModuleConfiguration[modules.size()]);
    }

    public ModuleConfiguration getModuleByConfigFile(File file) {
        ModuleConfiguration[] ar = getModules();
        for (ModuleConfiguration mc : ar) {
            if (file.equals(mc.file)) {
                return mc;
            }
        }
        return null;
    }

    public synchronized void registerModule(ModuleConfiguration descriptor) {
        log.info("Registering web module: " + descriptor.name);
        modules.put(descriptor.name, descriptor);
        String path = descriptor.path;
        if (path != null) {
            //TODO remove this
            // compat. method now modules should be declared through
            // WebApplication class
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            paths.put(path, descriptor);
        }
        if (descriptor.roots != null) {
            for (Class<?> cl : descriptor.roots) {
                roots.put(cl, descriptor);
            }
        }
    }

    // TODO the class path is not updated by this operation ...
    public synchronized File unregisterModule(String name) {
        ModuleConfiguration md = modules.remove(name);
        if (md == null) {
            return null;
        }
        Iterator<ModuleConfiguration> it = paths.values().iterator();
        while (it.hasNext()) { // remove all module occurrence in paths map
            ModuleConfiguration p = it.next();
            if (p.name.equals(md.name)) {
                it.remove();
            }
        }
        if (md.roots != null) {
            for (Class<?> cl : md.roots) {
                roots.remove(cl);
            }
        }
        return md.file;
    }

    public ModuleConfiguration getModuleByRootClass(Class<?> clazz) {
        return roots.get(clazz);
    }

    public synchronized void bind(String name, String path) {
        ModuleConfiguration md = modules.get(name);
        if (md != null) {
            paths.put(path, md);
        }
    }

    public void loadModules(File root) {
        for (String name : root.list()) {
            String path = name + "/module.xml";
            File file = new File(root, path);
            if (file.isFile()) {
                loadModule(file);
            }
        }
    }

    public void loadModule(ModuleConfiguration mc) {
        // this should be called after the class path is updated ...
        loadModuleRootResources(mc);
        mc.setEngine(engine);
        registerModule(mc);
    }

    public void loadModule(File file) {
        ModuleConfiguration md = loadConfiguration(file);
        // this should be called after the class path is updated ...
        loadModuleRootResources(md);
        md.setEngine(engine);
        registerModule(md);
    }

    public void loadModuleFromDir(File moduleRoot) {
        File file = new File(moduleRoot, "module.xml");
        if (file.isFile()) {
            loadModule(file);
        }
    }

    public void reloadModule(String name) {
        log.info("Reloading module: " + name);
        File cfg = unregisterModule(name);
        if (cfg != null) {
            loadModule(cfg);
        }
    }

    public void reloadModules() {
        log.info("Reloading modules");
        for (ModuleConfiguration mc : getModules()) {
            try {
                reloadModule(mc.name);
            } catch (Exception e) {
                log.error("Failed to redeploy module: " + mc.name);
            }
        }
    }

    protected ModuleConfiguration loadConfiguration(File file) {
        if (engine == null) {
            engine = Framework.getLocalService(WebEngine.class);
        }
        try {
            ModuleConfiguration mc = readConfiguration(engine, file);
            mc.file = file;
            if (mc.directory == null) {
                mc.directory = file.getParentFile().getCanonicalFile();
            }
            return mc;
        } catch (Exception e) {
            throw WebException.wrap("Faile to load module configuration: "
                    + file, e);
        }
    }

    public static ModuleConfiguration readConfiguration(final WebEngine engine,
            File file) throws Exception {
        XMap xmap = new XMap();
        xmap.register(ModuleConfiguration.class);
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        ModuleConfiguration mc = (ModuleConfiguration) xmap.load(
                createXMapContext(engine), in);
        return mc;
    }

    public void loadModuleRootResources(ModuleConfiguration mc) {
        if (mc.resources != null) {
            for (ResourceBinding rb : mc.resources) {
                try {
                    rb.resolve(engine);
                    engine.addResourceBinding(rb);
                } catch (Exception e) {
                    throw WebException.wrap(
                            "Faile to load module root resource: " + rb, e);
                }
            }
        }
    }

    protected static Context createXMapContext(final WebEngine engine) {
        return new Context() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> loadClass(String className)
                    throws ClassNotFoundException {
                return engine.getWebLoader().loadClass(className);
            }

            @Override
            public URL getResource(String name) {
                return engine.getWebLoader().getResource(name);
            }
        };
    }

}
