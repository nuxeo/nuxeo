package org.nuxeo.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.application.loader.FrameworkLoader;
import org.nuxeo.runtime.RuntimeServiceException;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class NuxeoRunner implements Runnable {

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        ClassLoader classLoader = thread.getContextClassLoader();
        try {
            File workingDir = File.createTempFile("nxrunner-", null, new File("target"));
            // OSGiAdapter osgi = new OSGiAdapter(workingDir);
            // BundleFile bf = new SystemBundleFile(workingDir);
            // StandaloneBundleLoader bundleLoader = new StandaloneBundleLoader(osgi, classLoader);
            //
            // classLoader = bundleLoader.getSharedClassLoader().getLoader();
            // Thread.currentThread().setContextClassLoader(classLoader);
            //
            // SystemBundle systemBundle = new SystemBundle(osgi, bf, classLoader);
            // osgi.setSystemBundle(systemBundle);
            //
            // bundleLoader.setScanForNestedJARs(false); // for now
            // bundleLoader.setExtractNestedJARs(false);
            //
            // BundleFile bundleFile = lookupBundle("org.nuxeo.runtime");
            // Bundle runtimeBundle = null;
            // runtimeBundle.start();
            //
            // OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
            Class<?> loaderClass = classLoader.loadClass(FrameworkLoader.class.getName());
            Method initialize = loaderClass.getDeclaredMethod("initialize", ClassLoader.class, File.class, List.class,
                    Map.class);
            List<File> bundles = new FastClasspathScanner().getUniqueClasspathElements();
            initialize.invoke(null, classLoader, workingDir, bundles, Collections.emptyMap());

            Method start = loaderClass.getDeclaredMethod("start");
            start.invoke(null);
        } catch (Exception e) {
            thread.getThreadGroup().uncaughtException(thread, e);
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
        BundleFile bundleFile;
        // BundleFile bundleFile = bundles.get(bundleName);
        // if (bundleFile != null) {
        // return bundleFile;
        // }
        for (URL url : introspectClasspath()) {
            URI uri = url.toURI();
            // if (readUris.contains(uri)) {
            // continue;
            // }
            File file = new File(uri);
            // readUris.add(uri);
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
                // log.debug("Bundle '{}' has URL {}", symbolicName, url);
                // bundles.put(symbolicName, bundleFile);
            }
            if (bundleName.equals(symbolicName)) {
                return bundleFile;
            }
        }
        throw new RuntimeServiceException(String.format("No bundle with symbolic name '%s';", bundleName));
    }

    // TODO remove it in order to optimize it
    protected URL[] introspectClasspath() {
        return new FastClasspathScanner().getUniqueClasspathElements().stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException cause) {
                throw new RuntimeServiceException("Could not get URL from " + file, cause);
            }
        }).toArray(URL[]::new);
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

}
