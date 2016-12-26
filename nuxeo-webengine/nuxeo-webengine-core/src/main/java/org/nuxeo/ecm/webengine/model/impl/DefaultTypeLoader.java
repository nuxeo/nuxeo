/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * A type loader which is loading types from META-INF/web-types file. This loader is also checking the web module
 * nature. If the project has for example a Groovy nature it will call at end the {@link GroovyTypeLoader}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultTypeLoader {

    public static final Log log = LogFactory.getLog(DefaultTypeLoader.class);

    public static final String WEB_TYPES_FILE = "META-INF/web-types";

    protected GroovyTypeLoader gLoader;

    protected final ModuleImpl module;

    protected final WebLoader loader;

    protected final TypeRegistry typeReg;

    protected final File root;

    public DefaultTypeLoader(ModuleImpl module, TypeRegistry typeReg, File root) {
        this.typeReg = typeReg;
        this.root = root;
        this.module = module;
        loader = module.getEngine().getWebLoader();
        if (module.hasNature("groovy")) {
            gLoader = new GroovyTypeLoader(module.getEngine(), typeReg, root);
        }
    }

    public ModuleImpl getModule() {
        return module;
    }

    public void load() {
        if (module.configuration.types != null) {
            loadTypes(module.configuration.types);
        } else {
            File file = new File(module.getRoot(), WEB_TYPES_FILE);
            if (file.isFile()) {
                try {
                    loadTypesFile(file);
                } catch (IOException | ClassNotFoundException e) {
                    log.error("Failed to load web types from file " + WEB_TYPES_FILE, e);
                }
            }
            if (gLoader != null) {
                gLoader.load();
            }
        }
    }

    public void flushCache() {
        if (gLoader != null) {
            gLoader.flushCache();
        }
    }

    protected void loadTypes(Class<?>[] types) {
        for (Class<?> type : types) {
            TypeDescriptor td = loadType(type);
            if (td != null) {
                typeReg.registerTypeDescriptor(td);
            }
        }
    }

    /**
     * Old method to load types from a web-types file generated at build time
     */
    protected void loadTypesFile(File file) throws IOException, ClassNotFoundException {
        List<String> lines = FileUtils.readLines(file);
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            int p = line.indexOf('|');
            if (p > -1) {
                line = line.substring(0, p);
            }
            TypeDescriptor td = loadType(line);
            if (td != null) {
                typeReg.registerTypeDescriptor(td);
            }
        }
    }

    protected TypeDescriptor loadType(String className) throws ClassNotFoundException {
        return loadType(loader.getClassProxy(className));
    }

    protected TypeDescriptor loadType(Class<?> clazz) {
        return loadType(new StaticClassProxy(clazz));
    }

    protected TypeDescriptor loadType(ClassProxy clazz) {
        WebObject type = clazz.get().getAnnotation(WebObject.class);
        if (type != null) {
            return TypeDescriptor.fromAnnotation(clazz, type);
        }
        WebAdapter ws = clazz.get().getAnnotation(WebAdapter.class);
        if (ws != null) {
            return AdapterDescriptor.fromAnnotation(clazz, ws);
        }
        return null;
    }

}
