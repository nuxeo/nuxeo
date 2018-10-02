/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.osgi.SystemBundleFile;
import org.nuxeo.osgi.application.StandaloneBundleLoader;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.StreamRef;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * Default RuntimeHarness implementation.
 *
 * @since 10.2
 */
public class RuntimeHarnessImpl implements RuntimeHarness {

    protected static final Logger log = LogManager.getLogger();

    protected static URL[] introspectClasspath() {
        return new FastClasspathScanner().getUniqueClasspathElements().stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException cause) {
                throw new RuntimeServiceException("Could not get URL from " + file, cause);
            }
        }).toArray(URL[]::new);
    }

    protected StandaloneBundleLoader bundleLoader;

    protected Map<String, BundleFile> bundles;

    protected boolean frameworkStarted;

    protected OSGiAdapter osgi;

    protected Set<URI> readUris;

    protected OSGiRuntimeService runtime;

    protected Bundle runtimeBundle;

    protected TargetResourceLocator targetResourceLocator;

    protected URL[] urls; // classpath urls, used for bundles lookup

    protected List<WorkingDirectoryConfigurator> wdConfigs;

    protected File workingDir;

    protected RuntimeHarnessImpl() {
        wdConfigs = new ArrayList<>();
    }

    public RuntimeHarnessImpl(Class<?> clazz) {
        this();
        targetResourceLocator = new TargetResourceLocator(clazz);
    }

    @Override
    public void addWorkingDirectoryConfigurator(WorkingDirectoryConfigurator config) {
        wdConfigs.add(config);
    }

    @Override
    public void deployBundle(String name) throws Exception {
        // install only if not yet installed
        Bundle bundle = bundleLoader.getOSGi().getRegistry().getBundle(name);
        if (bundle == null) {
            BundleFile bundleFile = lookupBundle(name);
            bundleLoader.loadBundle(bundleFile);
            bundleLoader.installBundle(bundleFile);
            bundle = bundleLoader.getOSGi().getRegistry().getBundle(name);
        } else {
            log.info("A bundle with name {} has been found. Deploy is ignored.", name);
        }
        if (runtime.getContext(bundle) == null) {
            runtime.createContext(bundle);
        }
    }

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
        } else {
            context.deploy(contrib);
        }
    }

    @Override
    @Deprecated
    public void deployFolder(File folder, ClassLoader loader) throws Exception {
        DirectoryBundleFile bf = new DirectoryBundleFile(folder);
        BundleImpl bundle = new BundleImpl(osgi, bf, loader);
        osgi.install(bundle);
    }

    @Override
    public RuntimeContext deployPartial(String name, Set<TargetExtensions> targetExtensions) throws Exception {
        // Do not install bundle; we only need the Object to list his components
        Bundle bundle = new BundleImpl(osgi, lookupBundle(name), getClass().getClassLoader());
        RuntimeContext ctx = new OSGiRuntimeContext(runtime, bundle);
        listBundleComponents(bundle).map(URLStreamRef::new).forEach(component -> {
            try {
                deployPartialComponent(ctx, targetExtensions, component);
            } catch (IOException e) {
                log.error("PartialBundle: {} failed to load: {}", name, component, e);
            }
        });
        return ctx;
    }

    @Override
    @Deprecated
    public RuntimeContext deployTestContrib(String bundle, String contrib) throws Exception {
        URL url = targetResourceLocator.getTargetTestResource(contrib);
        return deployTestContrib(bundle, url);
    }

    @Override
    @Deprecated
    public RuntimeContext deployTestContrib(String bundle, URL contrib) throws Exception {
        Bundle b = bundleLoader.getOSGi().getRegistry().getBundle(bundle);
        if (b == null) {
            b = osgi.getSystemBundle();
        }
        OSGiRuntimeContext ctx = new OSGiRuntimeContext(runtime, b);
        ctx.deploy(contrib);
        return ctx;
    }

    @Override
    public void fireFrameworkStarted() {
        if (frameworkStarted) {
            throw new IllegalStateException("fireFrameworkStarted must not be called more than once");
        }
        frameworkStarted = true;
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

    @Override
    @Deprecated
    public List<String> getClassLoaderFiles() throws URISyntaxException {
        List<String> files = new ArrayList<>(urls.length);
        for (URL url : urls) {
            files.add(url.toURI().getPath());
        }
        return files;
    }

    @Override
    public RuntimeContext getContext() {
        return runtime.getContext();
    }

    @Override
    public OSGiAdapter getOSGiAdapter() {
        return osgi;
    }

    @Override
    @Deprecated
    public Properties getProperties() {
        return runtime.getProperties();
    }

    @Override
    public File getWorkingDir() {
        return workingDir;
    }

    @Override
    public boolean isRestart() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return runtime != null;
    }

    @Override
    public void restart() throws Exception {
        // do nothing
    }

    @Override
    public void start() throws Exception {
        System.setProperty("org.nuxeo.runtime.testing", "true");
        wipeEmptyTestSystemProperties();
        wipeRuntime();
        initUrls();
        if (urls == null) {
            throw new UnsupportedOperationException("no bundles available");
        }
        initOsgiRuntime();
    }

    @Override
    public void stop() throws Exception {
        wipeRuntime();
        if (workingDir != null) {
            if (workingDir.exists() && !FileUtils.deleteQuietly(workingDir)) {
                log.warn("Cannot delete {}", workingDir);
            }
            workingDir = null;
        }
        readUris = null;
        bundles = null;
    }

    @Override
    public void undeployContrib(String name, String contrib) {
        RuntimeContext context = runtime.getContext(name);
        if (context == null) {
            context = runtime.getContext();
        }
        context.undeploy(contrib);
    }

    /**
     * Read a component from his StreamRef and create a new component (suffixed with `-partial`, and the base component
     * name aliased) with only matching contributions of the extensionPoints parameter.
     *
     * @param ctx RuntimeContext in which the new component will be deployed
     * @param extensionPoints Set of white listed TargetExtensions
     * @param component Reference to the original component
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void deployPartialComponent(RuntimeContext ctx, Set<TargetExtensions> extensionPoints,
            StreamRef component) throws IOException {
        RegistrationInfo ri = ((DefaultRuntimeContext) ctx).createRegistrationInfo(component);
        String name = ri.getName().getName() + "-partial";

        // Flatten Target Extension Points
        Set<String> targets = extensionPoints.stream()
                                             .map(TargetExtensions::getTargetExtensions)
                                             .flatMap(Set::stream)
                                             .collect(Collectors.toSet());

        String ext = Arrays.stream(ri.getExtensions())
                           .filter(e -> targets.contains(TargetExtensions.newTargetExtension(
                                   e.getTargetComponent().getName(), e.getExtensionPoint())))
                           .map(Extension::toXML)
                           .collect(joining());

        ctx.deploy(new InlineRef(name, String.format("<component name=\"%s\">%s</component>", name, ext)));
    }

    /**
     * Inits the osgi runtime.
     *
     * @throws Exception the exception
     */
    protected void initOsgiRuntime() throws Exception {
        try {
            Environment.setDefault(null);
            if (System.getProperties().remove("nuxeo.home") != null) {
                log.warn("Removed System property nuxeo.home.");
            }
            workingDir = File.createTempFile("nxruntime-" + Thread.currentThread().getName() + "-", null,
                    new File("target"));
            Files.delete(workingDir.toPath());
        } catch (IOException e) {
            log.error("Could not init working directory", e);
            throw e;
        }
        osgi = new OSGiAdapter(workingDir);
        BundleFile bf = new SystemBundleFile(workingDir);
        bundleLoader = new StandaloneBundleLoader(osgi, RuntimeHarnessImpl.class.getClassLoader());
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

        runtime = (OSGiRuntimeService) Framework.getRuntime();

    }

    /**
     * Inits the urls.
     */
    protected void initUrls() {
        urls = introspectClasspath();
        log.debug("URLs on the classpath:\n{}", () -> Stream.of(urls).map(URL::toString).collect(joining("\n")));
        readUris = new HashSet<>();
        bundles = new HashMap<>();
    }

    /**
     * Listing component's urls of a bundle. Inspired from org.nuxeo.runtime.osgi.OSGiRuntimeService#loadComponents but
     * without deploying anything.
     *
     * @param bundle Bundle to be read
     * @return the stream
     */
    protected Stream<URL> listBundleComponents(Bundle bundle) {
        String list = OSGiRuntimeService.getComponentsList(bundle);
        String name = bundle.getSymbolicName();
        log.debug("PartialBundle: {} components: {}", name, list);
        if (list == null) {
            return Stream.empty();
        } else {
            return Arrays.stream(list.split("[, \t\n\r\f]")).map(bundle::getEntry).filter(Objects::nonNull);
        }
    }

    /**
     * Lookup bundle.
     *
     * @param bundleName the bundle name
     * @return the bundle file
     * @throws Exception the exception
     */
    protected BundleFile lookupBundle(String bundleName) throws Exception {
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
                log.debug("Bundle '{}' has URL {}", symbolicName, url);
                bundles.put(symbolicName, bundleFile);
            }
            if (bundleName.equals(symbolicName)) {
                return bundleFile;
            }
        }
        throw new RuntimeServiceException(String.format("No bundle with symbolic name '%s';", bundleName));
    }

    /**
     * Read symbolic name.
     *
     * @param bf the bf
     * @return the string
     */
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

    /**
     * Makes sure there is no previous runtime hanging around.
     * <p>
     * This happens for instance if a previous test had errors in its <code>setUp()</code>, because
     * <code>tearDown()</code> has not been called.
     */
    protected void wipeRuntime() {
        // Make sure there is no active runtime (this might happen if an
        // exception is raised during a previous setUp -> tearDown is not called afterwards).
        runtime = null;
        frameworkStarted = false;
        if (Framework.getRuntime() != null) {
            try {
                Framework.shutdown();
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                throw new RuntimeServiceException("Interrupted during shutdown", cause);
            }
        }
    }

    /**
     * Removes Nuxeo test system properties that are empty.
     * <p>
     * This is needed when using maven surefire > 2.17 because since SUREFIRE-649 surefire propagates empty system
     * properties.
     */
    protected void wipeEmptyTestSystemProperties() {
        List<String> emptyProps = System.getProperties()
                                        .entrySet()
                                        .stream()
                                        .filter(this::isAnEmptyTestProperty)
                                        .map(entry -> entry.getKey().toString())
                                        .collect(Collectors.toList());
        emptyProps.forEach(System::clearProperty);
        if (log.isDebugEnabled()) {
            emptyProps.forEach(property -> log.debug("Removed empty test system property: {}", property));
        }
    }

    protected boolean isAnEmptyTestProperty(Map.Entry<Object, Object> entry) {
        if (!entry.getKey().toString().startsWith("nuxeo.test.")) {
            return false;
        }
        return entry.getValue() == null || entry.getValue().toString().isEmpty();
    }

}
