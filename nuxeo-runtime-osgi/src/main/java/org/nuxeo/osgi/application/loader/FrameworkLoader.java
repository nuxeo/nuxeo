/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.osgi.application.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FrameworkLoader {

    public static final String HOST_NAME = "org.nuxeo.app.host.name";

    public static final String HOST_VERSION = "org.nuxeo.app.host.version";

    /**
     * @deprecated prefer use of {@link Environment#NUXEO_TMP_DIR}
     */
    @Deprecated
    public static final String TMP_DIR = "org.nuxeo.app.tmp";

    public static final String LIBS = "org.nuxeo.app.libs"; // class path

    public static final String BUNDLES = "org.nuxeo.app.bundles"; // class path

    public static final String DEVMODE = "org.nuxeo.app.devmode";

    public static final String PREPROCESSING = "org.nuxeo.app.preprocessing";

    public static final String SCAN_FOR_NESTED_JARS = "org.nuxeo.app.scanForNestedJars";

    public static final String FLUSH_CACHE = "org.nuxeo.app.flushCache";

    public static final String ARGS = "org.nuxeo.app.args";

    private static final Log log = LogFactory.getLog(FrameworkLoader.class);

    private static FrameworkLoader loader;

    protected final File[] bundleFiles;

    protected final File[] libraryFiles;

    protected final OSGiAdapter adapter;

    public FrameworkLoader(OSGiAdapter adapter, File[] libraries, File[] bundles) {
        libraryFiles = newSortedFiles(libraries);
        bundleFiles = newSortedFiles(bundles);
        this.adapter = adapter;
    }

    protected static File[] newSortedFiles(File[] files) {
        File[] sortedFiles = new File[files.length];
        System.arraycopy(files, 0, sortedFiles, 0, files.length);
        Arrays.sort(sortedFiles);
        return sortedFiles;
    }

    public static OSGiAdapter osgi() {
        return loader.adapter;
    }

    public static synchronized void initialize(File home,
            File[] libraries, File[] bundles, Properties env)
            throws IOException, BundleException {
        if (loader != null) {
            return;
        }
        OSGiAdapter osgi = new OSGiAdapter(env);
        osgi.setHome(home);
        loader = new FrameworkLoader(osgi, newSortedFiles(libraries),
                newSortedFiles(bundles));
    }

    public static synchronized void start() throws BundleException, IOException {
        if (loader == null) {
            throw new IllegalStateException(
                    "Framework is not initialized. Call initialize method first");
        }
        loader.doStart();
    }

    public static synchronized void stop() throws BundleException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            loader.doStop();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    protected void printDeploymentOrderInfo() {
        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder();
            for (File file : bundleFiles) {
                if (file != null) {
                    buf.append("\n\t" + file.getPath());
                }
            }
            log.debug("Deployment order: " + buf.toString());
        }
    }

    protected static Attributes.Name SYMBOLIC_NAME = new Attributes.Name(
            Constants.BUNDLE_SYMBOLICNAME);

    protected static boolean isBundle(File f) {
        Manifest mf;
        try {
            if (f.isFile()) { // jar file
                JarFile jf = new JarFile(f);
                try {
                    mf = jf.getManifest();
                } finally {
                    jf.close();
                }
                if (mf == null) {
                    return false;
                }
            } else if (f.isDirectory()) { // directory
                f = new File(f, "META-INF/MANIFEST.MF");
                if (!f.isFile()) {
                    return false;
                }
                mf = new Manifest();
                FileInputStream input = new FileInputStream(f);
                try {
                    mf.read(input);
                } finally {
                    input.close();
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return mf.getMainAttributes().containsKey(SYMBOLIC_NAME);
    }

    protected void doStart() throws BundleException, IOException {
        for (File f : libraryFiles) {
            installLibrary(f);
        }
        printDeploymentOrderInfo();
        for (File f : bundleFiles) {
            if (!isBundle(f)) {
                continue;
            }
            try {
                install(f);
            } catch (IOException e) {
                log.error("Failed to install bundle: " + f, e);
                // continue
            } catch (BundleException e) {
                log.error("Failed to install bundle: " + f, e);
                // continue
            } catch (RuntimeException e) {
                log.error("Failed to install bundle: " + f, e);
                // continue
            }
        }
        adapter.start();
        adapter.getBundle("org.nuxeo.runtime").start(); // auto start runtime
    }

    protected void doStop() throws BundleException {
        try {
            adapter.shutdown();
        } catch (IOException e) {
            throw new BundleException("Cannot shutdown OSGi", e);
        }
    }

    public static void uninstall(String symbolicName) throws BundleException {
        Bundle bundle = loader.adapter.getBundle(symbolicName);
        if (bundle != null) {
            bundle.uninstall();
        }
    }

    public static ClassLoader getOSGiLoader() {
        return loader.adapter.getSystemLoader();
    }

    public static String install(File f) throws IOException, BundleException {
        Bundle bundle = loader.adapter.install(f.toURI());
        return bundle.getSymbolicName();
    }

    public static void installLibrary(File f) throws IOException, BundleException {
        loader.adapter.install(f.toURI());
    }

    protected static void loadSystemProperties(File home) {
        loader.adapter.setHome(home);
    }

    /**
     * @since 5.5
     * @return Environment summary
     */
    protected StringBuilder getStartMessage() {
        String newline = System.getProperty("line.separator");
        String hr = newline
                + "======================================================================"
                + newline;
        StringBuilder msg = new StringBuilder(hr);
        msg.append("= Starting Nuxeo Framework" + newline);
        // System.out.println("  * System Bundle = "+systemBundle);
        // System.out.println("  * Command Line Args = "+Arrays.asList(env.getCommandLineArguments()));
        msg.append(hr);
        return msg;
    }

}
