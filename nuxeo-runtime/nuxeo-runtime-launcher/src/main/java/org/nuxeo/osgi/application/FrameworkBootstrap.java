/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.osgi.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.management.JMException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FrameworkBootstrap implements LoaderConstants {

    protected static final String DEFAULT_BUNDLES_CP = "bundles/*:plugins/*";

    protected static final String DEFAULT_LIBS_CP = "lib/*:.:config";

    private static final Log log = LogFactory.getLog(FrameworkBootstrap.class);

    protected File home;

    protected MutableClassLoader loader;

    protected Map<String, Object> env;

    protected Class<?> frameworkLoaderClass;

    protected long startTime;

    protected boolean scanForNestedJars = true;

    protected boolean flushCache = false;

    public FrameworkBootstrap(ClassLoader cl, File home) throws IOException {
        this(new MutableClassLoaderDelegate(cl), home);
    }

    public FrameworkBootstrap(MutableClassLoader loader, File home) throws IOException {
        this.home = home.getCanonicalFile();
        this.loader = loader;
        initializeEnvironment();
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

    public void setFlushCache(boolean flushCache) {
        this.flushCache = flushCache;
    }

    public void setScanForNestedJars(boolean scanForNestedJars) {
        this.scanForNestedJars = scanForNestedJars;
    }

    public Map<String, Object> env() {
        return env;
    }

    public MutableClassLoader getLoader() {
        return loader;
    }

    public ClassLoader getClassLoader() {
        return loader.getClassLoader();
    }

    public File getHome() {
        return home;
    }

    public void initialize() throws ReflectiveOperationException, IOException {
        startTime = System.currentTimeMillis();
        List<File> bundleFiles = buildClassPath();
        frameworkLoaderClass = getClassLoader().loadClass("org.nuxeo.osgi.application.loader.FrameworkLoader");
        Method init = frameworkLoaderClass.getMethod("initialize", ClassLoader.class, File.class, List.class, Map.class);
        init.invoke(null, loader.getClassLoader(), home, bundleFiles, env);
    }

    public void start(MutableClassLoader cl) throws ReflectiveOperationException, IOException, JMException {

    }

    public void stop(MutableClassLoader cl) throws ReflectiveOperationException, JMException {

    }

    public String installBundle(File f) throws ReflectiveOperationException {
        if (frameworkLoaderClass == null) {
            throw new IllegalStateException("Framework Loader was not initialized. Call initialize() method first");
        }
        Method install = frameworkLoaderClass.getMethod("install", File.class);
        return (String) install.invoke(null, f);
    }

    public void uninstallBundle(String name) throws ReflectiveOperationException {
        if (frameworkLoaderClass == null) {
            throw new IllegalStateException("Framework Loader was not initialized. Call initialize() method first");
        }
        Method uninstall = frameworkLoaderClass.getMethod("uninstall", String.class);
        uninstall.invoke(null, name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void initializeEnvironment() throws IOException {
        System.setProperty(HOME_DIR, home.getAbsolutePath());
        env = new HashMap<String, Object>();
        // initialize with default values
        env.put(BUNDLES, DEFAULT_BUNDLES_CP);
        env.put(LIBS, DEFAULT_LIBS_CP);
        // load launcher.properties file if exists to overwrite default values
        File file = new File(home, "launcher.properties");
        if (!file.isFile()) {
            return;
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            env.putAll((Map) p);
            String v = (String) env.get(SCAN_FOR_NESTED_JARS);
            if (v != null) {
                scanForNestedJars = Boolean.parseBoolean(v);
            }
            v = (String) env.get(FLUSH_CACHE);
            if (v != null) {
                flushCache = Boolean.parseBoolean(v);
            }
        }
    }

    protected void printStartedMessage() {
        log.info("Framework started in " + ((System.currentTimeMillis() - startTime) / 1000) + " sec.");
    }

    protected File newFile(String path) throws IOException {
        if (path.startsWith("/")) {
            return new File(path).getCanonicalFile();
        } else {
            return new File(home, path).getCanonicalFile();
        }
    }

    /**
     * Fills the classloader with all jars found in the defined classpath.
     *
     * @return the list of bundle files.
     */
    protected List<File> buildClassPath() throws IOException {
        List<File> bundleFiles = new ArrayList<File>();
        String libsCp = (String) env.get(LIBS);
        if (libsCp != null) {
            buildLibsClassPath(libsCp);
        }
        String bundlesCp = (String) env.get(BUNDLES);
        if (bundlesCp != null) {
            buildBundlesClassPath(bundlesCp, bundleFiles);
        }
        extractNestedJars(bundleFiles, new File(home, "tmp/nested-jars"));
        return bundleFiles;
    }

    protected void buildLibsClassPath(String libsCp) throws IOException {
        String[] ar = libsCp.split(":");
        for (String entry : ar) {
            File entryFile;
            if (entry.endsWith("/*")) {
                entryFile = newFile(entry.substring(0, entry.length() - 2));
                File[] files = entryFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        loader.addURL(file.toURI().toURL());
                    }
                }
            } else {
                entryFile = newFile(entry);
                loader.addURL(entryFile.toURI().toURL());
            }
        }
    }

    protected void buildBundlesClassPath(String bundlesCp, List<File> bundleFiles) throws IOException {
        String[] ar = bundlesCp.split(":");
        for (String entry : ar) {
            File entryFile;
            if (entry.endsWith("/*")) {
                entryFile = newFile(entry.substring(0, entry.length() - 2));
                File[] files = entryFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String path = file.getPath();
                        if (path.endsWith(".jar") || path.endsWith(".zip") || path.endsWith(".war")
                                || path.endsWith("rar")) {
                            bundleFiles.add(file);
                            loader.addURL(file.toURI().toURL());
                        }
                    }
                }
            } else {
                entryFile = newFile(entry);
                bundleFiles.add(entryFile);
                loader.addURL(entryFile.toURI().toURL());
            }
        }
    }

    protected void extractNestedJars(List<File> bundleFiles, File dir) throws IOException {
        if (!scanForNestedJars) {
            return;
        }
        if (dir.isDirectory()) {
            if (flushCache) {
                deleteAll(dir);
            } else {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        loader.addURL(f.toURI().toURL());
                    }
                }
                return;
            }
        }
        dir.mkdirs();
        for (File f : bundleFiles) {
            if (f.isFile()) {
                extractNestedJars(f, dir);
            }
        }
    }

    protected void extractNestedJars(File file, File tmpDir) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            String fileName = file.getName();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();
                if (entry.getName().endsWith(".jar")) {
                    String name = path.replace('/', '_');
                    File dest = new File(tmpDir, fileName + '-' + name);
                    extractNestedJar(jarFile, entry, dest);
                    loader.addURL(dest.toURI().toURL());
                }
            }
        }
    }

    protected void extractNestedJar(JarFile file, ZipEntry entry, File dest) throws IOException {
        try (InputStream in = file.getInputStream(entry)) {
            copyToFile(in, dest);
        }
    }

    public static void deleteAll(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteAll(f);
                }
            }
        }
        file.delete();
    }

    public static void copyFile(File src, File file) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            copyToFile(in, file);
        }
    }

    public static void copyToFile(InputStream in, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = createBuffer(in.available());
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static final int BUFFER_SIZE = 1024 * 64; // 64K

    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K

    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K

    private static byte[] createBuffer(int preferredSize) {
        if (preferredSize < 1) {
            preferredSize = BUFFER_SIZE;
        }
        if (preferredSize > MAX_BUFFER_SIZE) {
            preferredSize = MAX_BUFFER_SIZE;
        } else if (preferredSize < MIN_BUFFER_SIZE) {
            preferredSize = MIN_BUFFER_SIZE;
        }
        return new byte[preferredSize];
    }

    public static File findFileStartingWidth(File dir, String prefix) {
        String[] names = dir.list();
        if (names != null) {
            for (String name : names) {
                if (name.startsWith(prefix)) {
                    return new File(dir, name);
                }
            }
        }
        return null;
    }

    public static void copyTree(File src, File dst) throws IOException {
        if (src.isFile()) {
            copyFile(src, dst);
        } else if (src.isDirectory()) {
            if (dst.exists()) {
                dst = new File(dst, src.getName());
                dst.mkdir();
            } else { // allows renaming dest dir
                dst.mkdirs();
            }
            File[] files = src.listFiles();
            for (File file : files) {
                copyTree(file, dst);
            }
        }
    }

}
