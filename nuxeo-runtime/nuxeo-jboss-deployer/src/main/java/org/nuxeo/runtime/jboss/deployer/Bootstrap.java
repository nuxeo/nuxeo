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
package org.nuxeo.runtime.jboss.deployer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.nuxeo.runtime.jboss.deployer.Constants.*;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Bootstrap {

    protected static final Log log = LogFactory.getLog(Boolean.class);

    protected final File homeDir;

    protected final ClassLoader cl;

    protected final List<File> bundles;

    protected final Map<String, Object> env;

    protected Class<?> frameworkLoaderClass;

    protected long startTime;

    public Bootstrap(File homeDir, List<File> bundles, ClassLoader cl)
            throws Exception {
        this.homeDir = homeDir;
        this.bundles = bundles;
        this.cl = cl;
        this.env = new HashMap<String, Object>();
        this.frameworkLoaderClass = cl.loadClass("org.nuxeo.osgi.application.loader.FrameworkLoader");
        setHostName("JBoss");
        setHostVersion("5.1.0");
        setDoPreprocessing(false);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public void setHostName(String value) {
        env.put(HOST_NAME, value);
    }

    public void setHostVersion(String value) {
        env.put(HOST_VERSION, value);
    }

    public void setDoPreprocessing(boolean doPreprocessing) {
        env.put(PREPROCESSING, Boolean.toString(doPreprocessing));
    }

    public void setDevMode(String devMode) {
        env.put(DEVMODE, devMode);
    }

    public void setProperty(String key, Object value) {
        env.put(key, value);
    }

    public Map<String, Object> getProperties() {
        return env;
    }

    public void startNuxeo() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader oldcl = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        try {
            doStart();
        } finally {
            thread.setContextClassLoader(oldcl);
        }
    }

    public void stopNuxeo() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader oldcl = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        try {
            invokeStop();
        } finally {
            thread.setContextClassLoader(oldcl);
        }
    }

    protected void doStart() throws Exception {
        startTime = System.currentTimeMillis();
        invokeInitialize(bundles);
        invokeStart();
        printStartedMessage();
    }

    protected void printStartedMessage() {
        log.info("Nuxeo Framework started in "
                + ((System.currentTimeMillis() - startTime) / 1000) + " sec.");
    }

    protected void invokeInitialize(List<File> bundleFiles) throws Exception {
        Method init = frameworkLoaderClass.getMethod("initialize",
                ClassLoader.class, File.class, List.class, Map.class);
        init.invoke(null, cl, homeDir, bundleFiles, env);
    }

    protected void invokeStart() throws Exception {
        Method start = frameworkLoaderClass.getMethod("start");
        start.invoke(null);
    }

    protected void invokeStop() throws Exception {
        Method stop = frameworkLoaderClass.getMethod("stop");
        stop.invoke(null);
    }

    protected List<File> getBundles() {
        return bundles;
    }

}
