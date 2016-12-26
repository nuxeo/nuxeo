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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * Load web types extracted from Groovy source files. Types are cached in META-INF/groovy-web-types. When types are
 * reloaded this file will be removed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GroovyTypeLoader {

    public static final Log log = LogFactory.getLog(GroovyTypeLoader.class);

    public static final String CRLF = System.getProperty("line.separator");

    public static final String WEB_TYPES_FILE = "META-INF/groovy-web-types";

    protected final WebLoader loader;

    protected final TypeRegistry typeReg;

    protected final File root;

    public GroovyTypeLoader(WebEngine engine, TypeRegistry typeReg, File root) {
        this.typeReg = typeReg;
        this.root = root;
        loader = engine.getWebLoader();
    }

    public synchronized void flushCache() {
        log.info("Flush directory type provider cache");
        File cache = new File(root, WEB_TYPES_FILE);
        cache.delete();
    }

    public synchronized void load() {
        try {
            File cache = new File(root, WEB_TYPES_FILE);
            if (cache.isFile()) {
                for (String line : FileUtils.readLines(cache)) {
                    if (line.equals("")) {
                        continue;
                    }
                    TypeDescriptor td = loadType(line);
                    if (td != null) {
                        typeReg.registerTypeDescriptor(td);
                    }
                }
            } else {
                cache.getParentFile().mkdirs();
                boolean completedAbruptly = true;
                try (Writer w = new BufferedWriter(new FileWriter(cache))) {
                    scan(root, null, w);
                    completedAbruptly = false;
                } finally {
                    if (completedAbruptly) {
                        cache.delete();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw WebException.wrap(e);
        }
    }

    protected void scan(File root, String path, Writer cache) {
        for (File file : root.listFiles()) {
            String name = file.getName();
            if (file.isDirectory() && !"skin".equals(name) && !"samples".equals(name)) {
                scan(file, path == null ? name : path + '.' + name, cache);
            } else if (name.endsWith(".groovy") && Character.isUpperCase(name.charAt(0))) {
                String className;
                if (path == null) {
                    className = name.substring(0, name.length() - 7);
                } else {
                    StringBuilder buf = new StringBuilder().append(path).append('.').append(name);
                    buf.setLength(buf.length() - 7);
                    className = buf.toString();
                }
                try {
                    TypeDescriptor td = loadTypeAndRecord(cache, className);
                    if (td != null) {
                        typeReg.registerTypeDescriptor(td);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw WebException.wrap(e);
                }
            }
        }
    }

    /**
     * Loads a type and cache it.
     */
    protected TypeDescriptor loadTypeAndRecord(Writer cache, String className) throws ClassNotFoundException,
            IOException {
        TypeDescriptor td = loadType(className);
        if (td != null) {
            cache.write(className);
            cache.write(CRLF);
        }
        return td;
    }

    /**
     * Gets a type descriptor given an absolute className.
     * <p>
     * If this class doesn't define a type or type adapter, return null.
     */
    protected TypeDescriptor loadType(String className) throws ClassNotFoundException {
        ClassProxy clazz = loader.getGroovyClassProxy(className);
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
