/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.OSGiBundleFile;
import org.nuxeo.osgi.OSGiBundleHost;
import org.nuxeo.osgi.OSGiLoader;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.RuntimeHarness;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;

public class DefaultRuntimeHarness implements RuntimeHarness {

    protected final Log log = LogFactory.getLog(DefaultRuntimeHarness.class);

    protected RuntimeService runtime;

    protected boolean restart = false;

    @Override
    public boolean isRestart() {
        return restart;
    }

    protected OSGiAdapter adapter;

    protected Bundle runtimeBundle;

    protected final List<WorkingDirectoryConfigurator> wdConfigs = new ArrayList<WorkingDirectoryConfigurator>();

    protected OSGiAdapter fetchAdapter() {
        ClassLoader loader = this.getClass().getClassLoader();
        return ((OSGiLoader) loader).getAdapter();
    }

    @Override
    public void addWorkingDirectoryConfigurator(
            WorkingDirectoryConfigurator config) {
        wdConfigs.add(config);
    }

    @Override
    public File getWorkingDir() {
        return adapter.getWorkingDir();
    }

    /**
     * Restarts the runtime and preserve homes directory.
     */
    @Override
    public void restart() throws Exception {
        restart = true;
        try {
            stop();
            start();
        } finally {
            restart = false;
        }
    }

    @Override
    public void start() throws Exception {
        System.setProperty("org.nuxeo.runtime.testing", "true");
        // super.setUp();
        wipeRuntime();
        initOsgiRuntime();
    }

    /**
     * Fire the event {@code FrameworkEvent.STARTED}.
     */
    @Override
    public void fireFrameworkStarted() throws Exception {
        adapter.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED,
                adapter.getSystemBundle(), null));
    }

    @Override
    public void stop() throws Exception {
        wipeRuntime();
        adapter = null;
    }

    @Override
    public boolean isStarted() {
        return runtime != null;
    }

    protected void initOsgiRuntime() throws Exception {
        adapter = fetchAdapter();
        for (WorkingDirectoryConfigurator cfg : wdConfigs) {
            cfg.configure(this, adapter.getWorkingDir());
        }
        runtimeBundle = adapter.getBundle("org.nuxeo.runtime");
        runtimeBundle.start();
        runtime = Framework.getRuntime();
    }


    /**
     * Makes sure there is no previous runtime hanging around.
     * <p>
     * This happens for instance if a previous test had errors in its
     * <code>setUp()</code>, because <code>tearDown()</code> has not been
     * called.
     */
    protected void wipeRuntime() throws Exception {
        if (runtimeBundle == null) {
            return;
        }
        runtimeBundle.stop();
        runtimeBundle = null;
        runtime = null;
    }

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }


    protected void deployContrib(RuntimeContext context, URL url) {
        assertEquals(runtime, Framework.getRuntime());
        log.info("Deploying contribution from " + url.toString());
        try {
            context.deploy(url);
        } catch (Exception cause) {
            throw new AssertionError("Failed to deploy contrib " + url.toString(), cause);
        }
    }


    @Override
    public void deployContrib(String name, String contrib) {
        Bundle bundle = adapter.getBundle(name);
        URL url = bundle.getEntry(contrib);
        if (url == null) {
            fail(String.format("Could not find entry %s in bundle '%s",
                    contrib, name));
        }
        RuntimeContext context = runtime.getContext(name);
        deployContrib(context, url);
    }

    /**
     * Deploy an XML contribution from outside a bundle.
     * <p>
     * This should be used by tests wiling to deploy test contribution as part
     * of a real bundle.
     * <p>
     * The bundle owner is important since the contribution may depend on
     * resources deployed in that bundle.
     * <p>
     * Note that the owner bundle MUST be an already deployed bundle.
     *
     * @param bundle the bundle that becomes the contribution owner
     * @param contrib the contribution to deploy as part of the given bundle
     */
    @Override
    public RuntimeContext deployTestContrib(String bundle, String contrib)
            throws Exception {
        Bundle b = adapter.getBundle(bundle);
        if (b != null) {
            OSGiRuntimeContext ctx = ((OSGiRuntimeService) runtime).getContext(bundle);
            ctx.deploy(contrib);
            return ctx;
        } else {
            throw new IllegalArgumentException("Bundle not deployed " + bundle);
        }
    }

    @Override
    public RuntimeContext deployTestContrib(String bundle, URL contrib)
            throws Exception {
        Bundle b = adapter.getBundle(bundle);
        if (b != null) {
            OSGiRuntimeContext ctx = ((OSGiRuntimeService) runtime).getContext(bundle);
            ctx.deploy(contrib);
            return ctx;
        } else {
            throw new IllegalArgumentException("Bundle not deployed " + bundle);
        }
    }


    /**
     * Undeploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     *
     * @param bundle the bundle
     * @param contrib the contribution
     */
    @Override
    public void undeployContrib(String bundle, String contrib) throws Exception {
        Bundle b = adapter.getBundle(bundle);
        URL url = b.getEntry(contrib);
        if (url == null) {
            fail(String.format("Could not find entry %s in bundle '%s'",
                    contrib, b));
        }
        runtime.getContext().undeploy(url);
    }

    // TODO: Never used. Remove?
    @Deprecated
    protected void undeployContrib(URL url, String contrib) {
        assertEquals(runtime, Framework.getRuntime());
        log.info("Undeploying contribution from " + url.toString());
        try {
            runtime.getContext().undeploy(url);
        } catch (Exception e) {
            log.error(e);
            fail("Failed to undeploy contrib " + url.toString());
        }
    }

    protected static boolean isVersionSuffix(String s) {
        if (s.length() == 0) {
            return true;
        }
        return s.matches("-(\\d+\\.?)+(-SNAPSHOT)?(\\.\\w+)?");
    }

    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in
     * <code>MANIFEST.MF</code> and then falls back to the bundle url (e.g.,
     * <code>nuxeo-platform-search-api</code>) for backwards compatibility.
     *
     * @param bundle the symbolic name
     */
    @Override
    public void deployBundle(String name) throws Exception {
        // install only if not yet installed
        Bundle bundle = adapter.getBundle(name);
        if (bundle == null) {
            throw new Exception(name + " is not installed, check class path");
        }
        if (bundle instanceof OSGiBundleHost) { // not a fragment
            bundle.start();
        }
    }

    protected String readSymbolicName(OSGiBundleFile bf) {
        Manifest manifest = bf.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attrs = manifest.getMainAttributes();
        String name = attrs.getValue("Bundle-SymbolicName");
        if (name == null) {
            return null;
        }
        String[] sp = name.split(";", 2);
        return sp[0];
    }

    @Override
    public void deployFolder(File folder, ClassLoader loader) throws Exception {
        adapter.install(folder.toURI());
    }

    @Override
    public Properties getProperties() {
        return runtime.getProperties();
    }

    @Override
    public RuntimeContext getContext() {
        return runtime.getContext();
    }

    @Override
    public OSGiAdapter getOSGiAdapter() {
        return adapter;
    }

    @Override
    public List<String> getClassLoaderFiles() throws URISyntaxException {
        List<String> files =new LinkedList<String>();
        for (Bundle bundle:adapter.getSystemBundle().getBundleContext().getBundles()) {
            int state = bundle.getState();
            if ((state&(Bundle.STARTING|Bundle.ACTIVE)) != 0) {
                files.add(bundle.getLocation());
            }
        }
        return files;
    }

}
