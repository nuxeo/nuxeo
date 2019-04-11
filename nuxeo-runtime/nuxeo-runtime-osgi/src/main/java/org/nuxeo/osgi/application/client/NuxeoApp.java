/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.osgi.application.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.nuxeo.common.Environment;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.osgi.SystemBundleFile;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * Nuxeo Runtime launcher.
 * <p>
 * This launcher assumes all bundles are already on the classpath.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoApp {

    protected final ClassLoader loader;

    protected final Environment env;

    private OSGiAdapter osgi;

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? NuxeoApp.class.getClassLoader() : cl;
    }

    public NuxeoApp() {
        this(new File("."), getDefaultClassLoader());
    }

    public NuxeoApp(File home) {
        this(home, getDefaultClassLoader());
    }

    public NuxeoApp(File home, ClassLoader loader) {
        this.loader = loader;
        env = new Environment(home);
        Environment.setDefault(env);
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public Environment getEnvironment() {
        return env;
    }

    public void deployBundles(String bundlePath) throws BundleException, IOException {
        deployBundles(getBundleFiles(new File("."), bundlePath, ":"));
    }

    public void deployBundles(File baseDir, String bundlePath) throws BundleException, IOException {
        deployBundles(getBundleFiles(baseDir, bundlePath, ":"));
    }

    public synchronized void deployBundles(Collection<File> files) throws BundleException, IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Framework not started");
        }
        for (File file : files) {
            deployBundle(file);
        }
    }

    public synchronized void deployBundle(File file) throws BundleException, IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Framework not started");
        }
        if (!file.getPath().endsWith(".jar")) {
            return; // not a valid bundle
        }
        BundleFile bf = file.isDirectory() ? new DirectoryBundleFile(file) : new JarBundleFile(file);
        try {
            BundleImpl bundle = new BundleImpl(osgi, bf, loader);
            if (bundle.getSymbolicName() != null) {
                osgi.install(bundle);
            }
        } catch (NullPointerException t) {
            // do nothing: may happen with non OSGi manifests
            // System.out.println("Ignore: "+file);
        }
    }

    public synchronized void start() {
        if (osgi != null) {
            throw new IllegalStateException("Nuxeo Runtime already started");
        }
        osgi = new OSGiAdapter(env.getHome(), env.getData(), env.getProperties());
    }

    public synchronized boolean isStarted() {
        return osgi != null;
    }

    public synchronized OSGiAdapter getOsgi() {
        return osgi;
    }

    public synchronized void shutdown() throws IOException {
        if (osgi == null) {
            throw new IllegalStateException("Nuxeo Runtime not started");
        }
        osgi.shutdown();
        osgi = null;
    }

    public static Collection<File> getBundleFiles(File baseDir, String bundles, String delim) throws IOException {
        Collection<File> result = new LinkedHashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(bundles, delim == null ? " \t\n\r\f" : delim);
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            List<File> files = expandFiles(baseDir, tok);
            for (File file : files) {
                result.add(file.getCanonicalFile());
            }
        }
        return result;
    }

    public static File makeFile(File baseDir, String path) {
        if (path.startsWith("/")) {
            return new File(path);
        }
        return new File(baseDir, path);
    }

    public static List<File> expandFiles(File baseDir, String line) {
        int p = line.lastIndexOf("/");
        String fileName = null;
        if (p > -1) {
            fileName = line.substring(p + 1);
            baseDir = makeFile(baseDir, line.substring(0, p));
        } else {
            fileName = line;
        }
        if (fileName.length() == 0) {
            return Arrays.asList(baseDir.listFiles());
        }
        p = fileName.indexOf("*");
        if (p == -1) {
            return Collections.singletonList(makeFile(baseDir, fileName));
        } else if (p == 0) {
            String suffix = fileName.substring(p + 1);
            List<File> result = new ArrayList<>();
            String[] names = baseDir.list();
            if (names != null) {
                for (String name : names) {
                    if (name.endsWith(suffix)) {
                        result.add(makeFile(baseDir, name));
                    }
                }
            }
            return result;
        } else if (p == fileName.length() - 1) {
            String prefix = fileName.substring(0, p);
            List<File> result = new ArrayList<>();
            String[] names = baseDir.list();
            if (names != null) {
                for (String name : baseDir.list()) {
                    if (name.startsWith(prefix)) {
                        result.add(makeFile(baseDir, name));
                    }
                }
            }
            return result;
        } else {
            String prefix = fileName.substring(0, p);
            String suffix = fileName.substring(p + 1);
            List<File> result = new ArrayList<>();
            String[] names = baseDir.list();
            if (names != null) {
                for (String name : names) {
                    if (name.startsWith(prefix) && name.endsWith(suffix)) {
                        result.add(makeFile(baseDir, name));
                    }
                }
            }
            return result;
        }
    }

    public void fireFrameworkStarted() throws BundleException {
        if (osgi.getSystemBundle() == null) {
            SystemBundleFile file;
            try {
                file = new SystemBundleFile(osgi.getWorkingDir());
            } catch (IOException e) {
                throw new BundleException("Cannot create system bundle file", e);
            }
            osgi.setSystemBundle(new SystemBundle(osgi, file, loader));
        }
        osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, osgi.getSystemBundle(), null));
    }

}
