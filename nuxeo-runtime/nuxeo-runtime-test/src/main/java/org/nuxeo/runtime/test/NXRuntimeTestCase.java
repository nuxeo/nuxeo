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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.runtime.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.osgi.SystemBundleFile;
import org.nuxeo.osgi.application.StandaloneBundleLoader;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.MDCFeature;
import org.nuxeo.runtime.test.runner.RandomBug;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * Abstract base class for test cases that require a test runtime service.
 * <p>
 * The runtime service itself is conveniently available as the <code>runtime</code> instance variable in derived
 * classes.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// Make sure this class is kept in sync with with RuntimeHarness
@RunWith(FeaturesRunner.class)
@Features({ MDCFeature.class, ConditionalIgnoreRule.Feature.class, RandomBug.Feature.class })
@Ignore
public class NXRuntimeTestCase implements RuntimeHarness {

    protected Mockery jmcontext = new JUnit4Mockery();

    static {
        // jul to jcl redirection may pose problems (infinite loops) in some
        // environment
        // where slf4j to jul, and jcl over slf4j is deployed
        System.setProperty(AbstractRuntimeService.REDIRECT_JUL, "false");
    }

    private static final Log log = LogFactory.getLog(NXRuntimeTestCase.class);

    protected OSGiRuntimeService runtime;

    protected URL[] urls; // classpath urls, used for bundles lookup

    protected File workingDir;

    protected StandaloneBundleLoader bundleLoader;

    private Set<URI> readUris;

    protected Map<String, BundleFile> bundles;

    protected boolean restart = false;

    @Override
    public boolean isRestart() {
        return restart;
    }

    protected OSGiAdapter osgi;

    protected Bundle runtimeBundle;

    protected final List<WorkingDirectoryConfigurator> wdConfigs = new ArrayList<>();

    protected final TargetResourceLocator targetResourceLocator;

    public NXRuntimeTestCase() {
        targetResourceLocator = new TargetResourceLocator(this.getClass());
    }

    public NXRuntimeTestCase(String name) {
        this();
    }

    public NXRuntimeTestCase(Class<?> clazz) {
        targetResourceLocator = new TargetResourceLocator(clazz);
    }

    @Override
    public void addWorkingDirectoryConfigurator(WorkingDirectoryConfigurator config) {
        wdConfigs.add(config);
    }

    @Override
    public File getWorkingDir() {
        return workingDir;
    }

    /**
     * Restarts the runtime and preserve homes directory.
     */
    @Override
    public void restart() throws Exception {
        restart = true;
        try {
            tearDown();
            setUp();
        } finally {
            restart = false;
        }
    }

    @Override
    public void start() throws Exception {
        setUp();
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty("org.nuxeo.runtime.testing", "true");
        // super.setUp();
        wipeRuntime();
        initUrls();
        if (urls == null) {
            throw new UnsupportedOperationException("no bundles available");
        }
        initOsgiRuntime();
    }

    /**
     * Fire the event {@code FrameworkEvent.STARTED}.
     */
    @Override
    public void fireFrameworkStarted() throws Exception {
        boolean txStarted = !TransactionHelper.isTransactionActiveOrMarkedRollback()
                && TransactionHelper.startTransaction();
        boolean txFinished = false;
        try {
            osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, runtimeBundle, null));
            txFinished = true;
        } finally {
            if (!txFinished) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            if (txStarted) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        wipeRuntime();
        if (workingDir != null) {
            if (!restart) {
                if (workingDir.exists() && !FileUtils.deleteQuietly(workingDir)) {
                    log.warn("Cannot delete " + workingDir);
                }
                workingDir = null;
            }
        }
        readUris = null;
        bundles = null;
    }

    @Override
    public void stop() throws Exception {
        tearDown();
    }

    @Override
    public boolean isStarted() {
        return runtime != null;
    }

    protected void initOsgiRuntime() throws Exception {
        try {
            if (!restart) {
                Environment.setDefault(null);
                if (System.getProperties().remove("nuxeo.home") != null) {
                    log.warn("Removed System property nuxeo.home.");
                }
                workingDir = File.createTempFile("nxruntime-" + Thread.currentThread().getName() + "-", null,
                        new File("target"));
                workingDir.delete();
            }
        } catch (IOException e) {
            log.error("Could not init working directory", e);
            throw e;
        }
        osgi = new OSGiAdapter(workingDir);
        BundleFile bf = new SystemBundleFile(workingDir);
        bundleLoader = new StandaloneBundleLoader(osgi, NXRuntimeTestCase.class.getClassLoader());
        SystemBundle systemBundle = new SystemBundle(osgi, bf, bundleLoader.getSharedClassLoader().getLoader());
        osgi.setSystemBundle(systemBundle);
        Thread.currentThread().setContextClassLoader(bundleLoader.getSharedClassLoader().getLoader());

        for (WorkingDirectoryConfigurator cfg : wdConfigs) {
            cfg.configure(this, workingDir);
        }

        bundleLoader.setScanForNestedJARs(false); // for now
        bundleLoader.setExtractNestedJARs(false);

        BundleFile bundleFile = lookupBundle("org.nuxeo.runtime");
        runtimeBundle = new RootRuntimeBundle(osgi, bundleFile, bundleLoader.getClass().getClassLoader(), true);
        runtimeBundle.start();

        runtime = handleNewRuntime((OSGiRuntimeService) Framework.getRuntime());

        assertNotNull(runtime);
    }

    protected OSGiRuntimeService handleNewRuntime(OSGiRuntimeService aRuntime) {
        return aRuntime;
    }

    public static URL[] introspectClasspath(ClassLoader loader) {
        return new FastClasspathScanner().getUniqueClasspathElements().stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException cause) {
                throw new Error("Could not get URL from " + file, cause);
            }
        }).toArray(URL[]::new);
    }

    protected void initUrls() throws Exception {
        ClassLoader classLoader = NXRuntimeTestCase.class.getClassLoader();
        urls = introspectClasspath(classLoader);
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("URLs on the classpath: ");
            for (URL url : urls) {
                sb.append(url.toString());
                sb.append('\n');
            }
            log.debug(sb.toString());
        }
        readUris = new HashSet<>();
        bundles = new HashMap<>();
    }

    /**
     * Makes sure there is no previous runtime hanging around.
     * <p>
     * This happens for instance if a previous test had errors in its <code>setUp()</code>, because
     * <code>tearDown()</code> has not been called.
     */
    protected void wipeRuntime() throws Exception {
        // Make sure there is no active runtime (this might happen if an
        // exception is raised during a previous setUp -> tearDown is not called
        // afterwards).
        runtime = null;
        if (Framework.getRuntime() != null) {
            Framework.shutdown();
        }
    }

    public static URL getResource(String name) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String callerName = Thread.currentThread().getStackTrace()[2].getClassName();
        final String relativePath = callerName.replace('.', '/').concat(".class");
        final String fullPath = loader.getResource(relativePath).getPath();
        final String basePath = fullPath.substring(0, fullPath.indexOf(relativePath));
        Enumeration<URL> resources;
        try {
            resources = loader.getResources(name);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getPath().startsWith(basePath)) {
                    return resource;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return loader.getResource(name);
    }

    protected void deployContrib(URL url) {
        assertEquals(runtime, Framework.getRuntime());
        log.info("Deploying contribution from " + url.toString());
        try {
            runtime.getContext().deploy(url);
        } catch (Exception e) {
            fail("Failed to deploy contrib " + url.toString());
        }
    }

    /**
     * Deploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     * <p>
     * For compatibility reasons the name of the bundle may be a jar name, but this use is discouraged and deprecated.
     *
     * @param name the name of the bundle to peek the contrib in
     * @param contrib the path to contrib in the bundle.
     */
    @Override
    public void deployContrib(String name, String contrib) throws Exception {
        RuntimeContext context = runtime.getContext(name);
        if (context == null) {
            context = runtime.getContext();
            BundleFile file = lookupBundle(name);
            URL location = file.getEntry(contrib);
            if (location == null) {
                throw new AssertionError("Cannot locate " + contrib + " in " + name);
            }
            context.deploy(location);
            return;
        }
        context.deploy(contrib);
    }

    /**
     * Deploy an XML contribution from outside a bundle.
     * <p>
     * This should be used by tests wiling to deploy test contribution as part of a real bundle.
     * <p>
     * The bundle owner is important since the contribution may depend on resources deployed in that bundle.
     * <p>
     * Note that the owner bundle MUST be an already deployed bundle.
     *
     * @param bundle the bundle that becomes the contribution owner
     * @param contrib the contribution to deploy as part of the given bundle
     */
    @Override
    public RuntimeContext deployTestContrib(String bundle, String contrib) throws Exception {
        URL url = targetResourceLocator.getTargetTestResource(contrib);
        return deployTestContrib(bundle, url);
    }

    @Override
    public RuntimeContext deployTestContrib(String bundle, URL contrib) throws Exception {
        Bundle b = bundleLoader.getOSGi().getRegistry().getBundle(bundle);
        if (b == null) {
            b = osgi.getSystemBundle();
        }
        OSGiRuntimeContext ctx = new OSGiRuntimeContext(runtime, b);
        ctx.deploy(contrib);
        return ctx;
    }

    /**
     * Undeploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     *
     * @param name the bundle
     * @param contrib the contribution
     */
    @Override
    public void undeployContrib(String name, String contrib) throws Exception {
        RuntimeContext context = runtime.getContext(name);
        if (context == null) {
            context = runtime.getContext();
        }
        context.undeploy(contrib);
    }

    protected static boolean isVersionSuffix(String s) {
        if (s.length() == 0) {
            return true;
        }
        return s.matches("-(\\d+\\.?)+(-SNAPSHOT)?(\\.\\w+)?");
    }

    /**
     * Resolves an URL for bundle deployment code.
     * <p>
     * TODO: Implementation could be finer...
     *
     * @return the resolved url
     */
    protected URL lookupBundleUrl(String bundle) {
        for (URL url : urls) {
            String[] pathElts = url.getPath().split("/");
            for (int i = 0; i < pathElts.length; i++) {
                if (pathElts[i].startsWith(bundle) && isVersionSuffix(pathElts[i].substring(bundle.length()))) {
                    // we want the main version of the bundle
                    boolean isTestVersion = false;
                    for (int j = i + 1; j < pathElts.length; j++) {
                        // ok for Eclipse (/test) and Maven (/test-classes)
                        if (pathElts[j].startsWith("test")) {
                            isTestVersion = true;
                            break;
                        }
                    }
                    if (!isTestVersion) {
                        log.info("Resolved " + bundle + " as " + url.toString());
                        return url;
                    }
                }
            }
        }
        throw new RuntimeException("Could not resolve bundle " + bundle);
    }

    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in <code>MANIFEST.MF</code> and then falls back to the bundle
     * url (e.g., <code>nuxeo-platform-search-api</code>) for backwards compatibility.
     *
     * @param name the symbolic name
     */
    @Override
    public void deployBundle(String name) throws Exception {
        // install only if not yet installed
        BundleImpl bundle = bundleLoader.getOSGi().getRegistry().getBundle(name);
        if (bundle == null) {
            BundleFile bundleFile = lookupBundle(name);
            bundleLoader.loadBundle(bundleFile);
            bundleLoader.installBundle(bundleFile);
            bundle = bundleLoader.getOSGi().getRegistry().getBundle(name);
        }
        if (runtime.getContext(bundle) == null) {
            runtime.createContext(bundle);
        }
    }

    protected String readSymbolicName(BundleFile bf) {
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

    public BundleFile lookupBundle(String bundleName) throws Exception {
        BundleFile bundleFile = bundles.get(bundleName);
        if (bundleFile != null) {
            return bundleFile;
        }
        for (URL url : urls) {
            URI uri = url.toURI();
            if (readUris.contains(uri)) {
                continue;
            }
            File file = new File(uri);
            readUris.add(uri);
            try {
                if (file.isDirectory()) {
                    bundleFile = new DirectoryBundleFile(file);
                } else {
                    bundleFile = new JarBundleFile(file);
                }
            } catch (IOException e) {
                // no manifest => not a bundle
                continue;
            }
            String symbolicName = readSymbolicName(bundleFile);
            if (symbolicName != null) {
                log.info(String.format("Bundle '%s' has URL %s", symbolicName, url));
                bundles.put(symbolicName, bundleFile);
            }
            if (bundleName.equals(symbolicName)) {
                return bundleFile;
            }
        }
        throw new RuntimeServiceException(String.format("No bundle with symbolic name '%s';", bundleName));
    }

    @Override
    public void deployFolder(File folder, ClassLoader loader) throws Exception {
        DirectoryBundleFile bf = new DirectoryBundleFile(folder);
        BundleImpl bundle = new BundleImpl(osgi, bf, loader);
        osgi.install(bundle);
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
        return osgi;
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.runtime.test.runner.RuntimeHarness#getClassLoaderFiles()
     */
    @Override
    public List<String> getClassLoaderFiles() throws URISyntaxException {
        List<String> files = new ArrayList<>(urls.length);
        for (URL url : urls) {
            files.add(url.toURI().getPath());
        }
        return files;
    }

}
