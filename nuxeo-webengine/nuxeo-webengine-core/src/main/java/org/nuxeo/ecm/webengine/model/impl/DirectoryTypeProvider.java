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

import groovy.lang.GroovyClassLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.GroovyClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirectoryTypeProvider extends TypeConfigurationProvider {

    public static final Log log = LogFactory.getLog(DirectoryTypeProvider.class);

    public static final String CRLF = System.getProperty("line.separator");

    protected final GroovyClassLoader loader;
    protected final File root;
    protected final WebEngine engine;
    protected boolean isLoaded = false;

    public DirectoryTypeProvider(WebEngine engine) {
        this.engine = engine;
        loader = engine.getScripting().getGroovyScripting().getGroovyClassLoader();
        root = engine.getRootDirectory();
    }

    @Override
    public synchronized void flushCache() {
        log.info("Flush directory type provider cache");
        File cache = new File(root, "resources.cache");
        cache.delete();
        isLoaded = false;
    }

    public synchronized void load() {
        if (isLoaded) {
            return;
        }
        try {
            File cache = new File(root, "resources.cache");
            if (cache.isFile()) {
                for (String line : FileUtils.readLines(cache)) {
                    loadClassFile(line);
                }
            } else {
                Writer w = new BufferedWriter(new FileWriter(cache));
                try {
                    scan(root, null, w);
                    w.close();
                    isLoaded = true;
                } catch (Throwable t) {
                    w.close();
                    cache.delete();
                    throw WebException.wrap(t);
                }
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    protected void scan(File root, String path, Writer cache) {
        for (File file : root.listFiles()) {
            String name = file.getName();
            if (file.isDirectory() && !"skin".equals(name)
                    && !"WEB-INF".equals(name) && !"resources".equals(name) && !"samples".equals(name)) {
                scan(file, path == null ? name :
                    new StringBuilder().append(path).append('.').append(name).toString(),
                    cache);
            } else if (name.endsWith(".groovy") && Character.isUpperCase(name.charAt(0))) {
                String className = null;
                if (path == null) {
                    className = name.substring(0, name.length()-7);
                } else {
                    StringBuilder buf = new StringBuilder().append(path).append('.').append(name);
                    buf.setLength(buf.length()-7);
                    className = buf.toString();
                }
                try {
                    loadClassFileAndRecord(cache, className);
                } catch (Exception e) {
                    throw WebException.wrap(e);
                }
            }
        }
    }

    protected void loadClassFileAndRecord(Writer cache, String className) throws ClassNotFoundException, IOException  {
        if (loadClassFile(className)) {
            cache.write(className);
            cache.write(CRLF);
        }
    }

    protected boolean loadClassFile(String className) throws ClassNotFoundException {
        ClassProxy clazz = null;
        if (engine.isDebug()) {
            clazz = new GroovyClassProxy(loader, className);
        } else {
            clazz = new StaticClassProxy(loader.loadClass(className));
        }
        WebObject type = clazz.get().getAnnotation(WebObject.class);
        if (type != null) {
            registerType(TypeDescriptor.fromAnnotation(clazz, type));
            return true;
        } else {
            WebAdapter ws = clazz.get().getAnnotation(WebAdapter.class);
            if (ws != null) {
                registerType(AdapterDescriptor.fromAnnotation(clazz, ws));
                return true;
            }
        }
        return false;
    }

}
