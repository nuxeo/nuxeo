/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.loader.store.FileResourceStore;
import org.nuxeo.ecm.webengine.scripting.GroovyScripting;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebLoader {

    private static final Log log = LogFactory.getLog(WebLoader.class);

    protected final WebEngine engine;

    protected final ReloadingClassLoader classLoader;

    protected final GroovyScripting gScripting; // TODO refactor groovy loading

    public WebLoader(WebEngine engine) {
        this.engine = engine;
        File root = engine.getRootDirectory();
        classLoader = new ReloadingClassLoader(getParentLoader());
        gScripting = new GroovyScripting(classLoader);
        addClassPathElement(new File(root, "WEB-INF/classes"));
    }

    public WebEngine getEngine() {
        return engine;
    }

    /**
     * Adds a class or resource container to the reloading class loader. The container is either a jar or a directory.
     */
    public void addClassPathElement(File container) {
        try {
            classLoader.addResourceStore(new FileResourceStore(container));
            gScripting.getGroovyClassLoader().addURL(container.toURI().toURL());
        } catch (IOException e) {
            log.error("Failed to create file store: " + container, e);
        }
    }

    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    public ReloadingClassLoader getClassLoader() {
        return classLoader;
    }

    public GroovyScripting getGroovyScripting() {
        return gScripting;
    }

    public void flushCache() {
        log.info("Flushing loader cache");
        classLoader.reload();
        gScripting.clearCache();
    }

    public ClassProxy getGroovyClassProxy(String className) throws ClassNotFoundException {
        return new StaticClassProxy(gScripting.loadClass(className));
    }

    public ClassProxy getClassProxy(String className) throws ClassNotFoundException {
        return new StaticClassProxy(classLoader.loadClass(className));
    }

    public ClassProxy getClassProxy(Bundle bundle, String className) throws ClassNotFoundException {
        return new StaticClassProxy(bundle.loadClass(className));
    }

    public static ClassLoader getParentLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? GroovyScripting.class.getClassLoader() : cl;
    }

}
