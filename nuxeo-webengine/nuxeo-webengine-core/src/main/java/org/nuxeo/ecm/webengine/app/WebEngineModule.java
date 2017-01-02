/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webengine.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationFactory;
import org.nuxeo.ecm.webengine.jaxrs.scan.Scanner;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultTypeLoader;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.osgi.framework.Bundle;

import com.sun.jersey.server.impl.inject.ServerInjectableProviderContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineModule extends Application implements ApplicationFactory {

    private final static Log log = LogFactory.getLog(WebEngineModule.class);

    public final static String WEBOBJECT_ANNO = "Lorg/nuxeo/ecm/webengine/model/WebObject;";

    public final static String WEBADAPTER_ANNO = "Lorg/nuxeo/ecm/webengine/model/WebAdapter;";

    protected ServerInjectableProviderContext context;

    protected Bundle bundle;

    protected ModuleConfiguration cfg;

    void init(WebEngine engine, Bundle bundle, File configuration, Map<String, String> attrs)
            throws ReflectiveOperationException, IOException {
        this.bundle = bundle;
        loadModuleConfigurationFile(engine, configuration);
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
            throw new IllegalStateException("No name given for web module in bundle " + bundle.getSymbolicName());
        }
        initTypes(bundle, attrs.get("package"), engine);
    }

    private void initTypes(Bundle bundle, String packageBase, WebEngine engine) throws ReflectiveOperationException,
            IOException {
        cfg.types = getWebTypes();
        if (cfg.types == null) {
            // try the META-INF/web-types file
            loadMetaTypeFile(engine);
            if (cfg.types == null) {
                // try scanning the bundle
                scan(bundle, packageBase);
                if (cfg.types == null) {
                    throw new IllegalStateException("No web types defined in web module " + cfg.name + " from bundle "
                            + bundle.getSymbolicName());
                }
            } else {
                initRoots(engine);
            }
        } else {
            initRoots(engine);
        }
    }

    private void scan(Bundle bundle, String packageBase) throws ReflectiveOperationException, IOException {
        if (packageBase == null) {
            packageBase = "/";
        }
        Scanner scanner = new Scanner(bundle, packageBase, Scanner.PATH_ANNO, Scanner.PROVIDER_ANNO, WEBOBJECT_ANNO,
                WEBADAPTER_ANNO);
        scanner.scan();
        Collection<Class<?>> paths = scanner.getCollector(Scanner.PATH_ANNO);
        Collection<Class<?>> providers = scanner.getCollector(Scanner.PROVIDER_ANNO);
        cfg.roots = new Class<?>[paths.size() + providers.size()];
        int i = 0;
        for (Class<?> cl : paths) {
            cfg.roots[i++] = cl;
        }
        for (Class<?> cl : providers) {
            cfg.roots[i++] = cl;
        }
        Collection<Class<?>> objs = scanner.getCollector(WEBOBJECT_ANNO);
        Collection<Class<?>> adapters = scanner.getCollector(WEBADAPTER_ANNO);
        cfg.types = new Class<?>[objs.size() + adapters.size()];
        i = 0;
        for (Class<?> cl : objs) {
            cfg.types[i++] = cl;
        }
        for (Class<?> cl : adapters) {
            cfg.types[i++] = cl;
        }
    }

    private void loadMetaTypeFile(WebEngine engine) throws ReflectiveOperationException, IOException {
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

    private static Class<?>[] readWebTypes(WebLoader loader, InputStream in) throws ReflectiveOperationException,
            IOException {
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

    private void initRoots(WebEngine engine) {
        ArrayList<Class<?>> roots = new ArrayList<Class<?>>();
        for (Class<?> cl : cfg.types) {
            if (cl.isAnnotationPresent(Path.class)) {
                roots.add(cl);
            } else if (cfg.rootType != null) {
                // compat mode - should be removed later
                WebObject wo = cl.getAnnotation(WebObject.class);
                if (wo != null && wo.type().equals(cfg.rootType)) {
                    log.warn("Invalid web module " + cfg.name + " from bundle " + bundle.getSymbolicName()
                            + ". The root-type " + cl
                            + " in module.xml is deprecated. Consider using @Path annotation on you root web objects.");
                }
            }
        }
        if (roots.isEmpty()) {
            log.error("No root web objects found in web module " + cfg.name + " from bundle "
                    + bundle.getSymbolicName());
            // throw new
            // IllegalStateException("No root web objects found in web module "+cfg.name+" from bundle "+bundle.getSymbolicName());
        }
        cfg.roots = roots.toArray(new Class<?>[roots.size()]);
    }

    private ModuleConfiguration loadModuleConfigurationFile(WebEngine engine, File file) throws IOException {
        if (file != null && file.isFile()) {
            cfg = ModuleManager.readConfiguration(engine, file);
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

    public Module getModule(WebContext context) {
        return cfg.get(context);
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
    public Application getApplication(Bundle bundle, Map<String, String> args) throws ReflectiveOperationException,
            IOException {
        return WebEngineModuleFactory.getApplication(this, bundle, args);
    }

}
