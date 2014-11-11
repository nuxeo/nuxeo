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
package org.nuxeo.ecm.webengine.loader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.loader.store.FileResourceStore;
import org.nuxeo.ecm.webengine.scripting.GroovyScripting;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebLoader {

    private static final Log log = LogFactory.getLog(WebLoader.class);

    protected final WebEngine engine;
    protected final ReloadingClassLoader classLoader;
    protected final GroovyScripting gScripting; //TODO refactor groovy loading


    public WebLoader(WebEngine engine) {
        this.engine = engine;
        File root = engine.getRootDirectory();
        classLoader = new ReloadingClassLoader(getParentLoader());
        gScripting = new GroovyScripting(classLoader, engine.isDevMode());
        addClassPathElement(new File(root, "WEB-INF/classes"));
    }

    public WebEngine getEngine() {
        return engine;
    }

    /**
     * Adds a class or resource container to the reloading class loader.
     * The container is either a jar or a directory.
     */
    public void addClassPathElement(File container) {
        try {
            classLoader.addResourceStore(new FileResourceStore(container));
            gScripting.getGroovyClassLoader().addURL(container.toURI().toURL());
        } catch (MalformedURLException e) {
            log.error("Failed to convert file to url: "+container, e);
        } catch (Exception e) {
            log.error("Failed to create file store: "+container, e);
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
        return engine.isDevMode() ?
                new GroovyClassProxy(gScripting.getGroovyClassLoader(), className)
            : new StaticClassProxy(gScripting.loadClass(className));
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
