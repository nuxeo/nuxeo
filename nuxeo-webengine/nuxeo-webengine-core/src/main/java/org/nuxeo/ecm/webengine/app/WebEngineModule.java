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
package org.nuxeo.ecm.webengine.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationFactory;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultTypeLoader;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineModule extends Application implements ApplicationFactory {

    private static Log log = LogFactory.getLog(WebEngineModule.class);

    protected Bundle bundle;

    protected ModuleConfiguration cfg;

    void init(WebEngine engine, Bundle bundle, File moduleDir, Map<String,String> attrs) throws Exception {
        this.bundle = bundle;
        loadModuleConfigurationFile(engine, new File(moduleDir, "module.xml"));
        if (attrs != null) {
            String v = attrs.get("name");
            if (v != null) {
                cfg.name = v;
            }
            v = attrs.get("extends");
            if (v != null) {
                cfg.base = v;
            }
            v = attrs.get("headless");
            if (v != null) {
                cfg.isHeadless = Boolean.parseBoolean(v);
            }
        }
        if (cfg.name == null) {
            throw new IllegalStateException("No name given for web module in bundle "+bundle.getSymbolicName());
        }
        cfg.directory = moduleDir;
        initTypes(engine);
        initRoots(engine);
    }

    private void initTypes(WebEngine engine) throws Exception {
        cfg.types = getWebTypes();
        if (cfg.types == null) {
            // try the META-INF/web-types file
            loadMetaTypeFile(engine);
            if (cfg.types == null) {
                // try scanning the bundle
                // scan();
                if (cfg.types == null) {
                    throw new IllegalStateException("No web types defined in web module "+cfg.name+" from bundle "+bundle.getSymbolicName());
                }
            }
        }
    }

    private void loadMetaTypeFile(WebEngine engine) throws Exception {
        URL url = bundle.getEntry(DefaultTypeLoader.WEB_TYPES_FILE);
        if (url != null) {
            InputStream in = url.openStream();
            try {
                cfg.types = readWebTypes(engine.getWebLoader(), in);
            } finally {
                in.close();
            }
        }
    }

    private static Class<?>[] readWebTypes(WebLoader loader, InputStream in) throws Exception {
        HashSet<Class<?>> types = new HashSet<Class<?>>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                int p = line.indexOf('|');
                if (p > -1) {
                    line = line.substring(0, p);
                }
                Class<?> cl = loader.loadClass(line);
                types.add(cl);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return types.toArray(new Class<?>[types.size()]);
    }

    private void initRoots(WebEngine engine) throws Exception {
        ArrayList<Class<?>> roots = new ArrayList<Class<?>>();
        for (Class<?> cl : cfg.types) {
            if (cl.isAnnotationPresent(Path.class)) {
                roots.add(cl);
            } else if (cfg.rootType != null) {
                // compat mode - should be removed later
                WebObject wo = cl.getAnnotation(WebObject.class);
                if (wo != null && wo.type().equals(cfg.rootType)) {
                    log.warn("Invalid web module "+cfg.name+" from bundle "+bundle.getSymbolicName()+". The root-type "+cl+" in module.xml is deprecated. Consider using @Path annotation on you root web objects.");
                }
            }
        }
        if (roots.isEmpty()) {
            log.error("No root web objects found in web module "+cfg.name+" from bundle "+bundle.getSymbolicName());
            //throw new IllegalStateException("No root web objects found in web module "+cfg.name+" from bundle "+bundle.getSymbolicName());
        }
        cfg.roots = roots.toArray(new Class<?>[roots.size()]);
    }

    private ModuleConfiguration loadModuleConfigurationFile(WebEngine engine, File file) throws Exception {
        if (file != null && file.isFile()) {
            cfg = ModuleManager.readConfiguration(
                    engine, file);
        } else {
            cfg = new ModuleConfiguration();
        }
        cfg.engine = engine;
        cfg.file = file;
        return cfg;
    }

    public ModuleConfiguration getConfiguration() {
        return cfg;
    }

    public Module getModule() {
        return cfg.get();
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public Set<Class<?>> getClasses() {
        if (cfg.roots == null) {
            return new HashSet<Class<?>>();
        }
        HashSet<Class<?>> set = new HashSet<Class<?>>();
        for (Class<?> root : cfg.roots) {
            set.add(root);
        }
        return set;
    }

    public String getId() {
        return bundle.getSymbolicName();
    }

    public Class<?>[] getWebTypes() {
        return null; // types will be discovered
    }

    @Override
    public Application getApplication(Bundle bundle, Map<String, String> args) throws Exception {
        return WebEngineModuleFactory.getApplication(this, bundle, args);
    }

}
