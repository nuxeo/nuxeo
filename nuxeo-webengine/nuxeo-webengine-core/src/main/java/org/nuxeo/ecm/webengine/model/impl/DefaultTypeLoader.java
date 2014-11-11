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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.app.WebApplication;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * A type loader which is loading types from META-INF/web-types file. This
 * loader is also checking the web module nature. If the project has for example
 * a Groovy nature it will call at end the {@link GroovyTypeLoader}
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
                } catch (Exception e) {
                    log.error("Failed to load web types from file "
                            + WEB_TYPES_FILE, e);
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

    /**
     * New method to load types from the {@link WebApplication} declared types.
     */
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
    protected void loadTypesFile(File file) throws Exception {
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

    protected TypeDescriptor loadType(String className)
            throws ClassNotFoundException {
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
