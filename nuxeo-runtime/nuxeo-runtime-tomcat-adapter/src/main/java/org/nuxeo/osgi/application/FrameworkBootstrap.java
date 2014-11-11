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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FrameworkBootstrap implements LoaderConstants {

    protected static final String DEFAULT_BUNDLES_CP = "bundles/*";
    protected static final String DEFAULT_LIBS_CP = "lib/*:.:config";
    
    protected File home;
    protected MutableClassLoader loader;
    protected Map<String,Object> env;
    protected Class<?> frameworkLoaderClass;

    protected long startTime;
    protected boolean scanForNestedJars = true;
    protected boolean flushCache = false;
    
    

    public FrameworkBootstrap(ClassLoader cl, File home) throws IOException {
        this (new MutableClassLoaderDelegate(cl), home);
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
    
    public void initialize() throws Exception {
        startTime = System.currentTimeMillis();
        List<File> bundleFiles = buildClassPath();
        frameworkLoaderClass = getClassLoader().loadClass("org.nuxeo.osgi.application.loader.FrameworkLoader");
        Method init = frameworkLoaderClass.getMethod("initialize", ClassLoader.class, File.class, List.class, Map.class);
        init.invoke(null, loader.getClassLoader(), home, bundleFiles, env);
    }
    
    public void start() throws Exception {
        if (frameworkLoaderClass == null) {
            throw new IllegalStateException("Framework Loader was not initialized. Call initialize() method first");
        }
        Method start = frameworkLoaderClass.getMethod("start");
        start.invoke(null);
        printStartedMessage();
    }

    public void stop() throws Exception {
        if (frameworkLoaderClass == null) {
            throw new IllegalStateException("Framework Loader was not initialized. Call initialize() method first");
        }
        Method stop = frameworkLoaderClass.getMethod("stop");
        stop.invoke(null);        
    }    


    @SuppressWarnings("unchecked")
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
        FileInputStream in = new FileInputStream(file);
        try {
            p.load(in);
            env.putAll((Map)p);
            String v = (String)env.get(SCAN_FOR_NESTED_JARS);
            if (v != null) {
                scanForNestedJars = Boolean.parseBoolean(v);
            }
            v = (String)env.get(FLUSH_CACHE);
            if (v != null) {
                flushCache = Boolean.parseBoolean(v);
            }            
        } finally {
            in.close();
        }
    }
    
    protected void printStartedMessage() {
        System.out.println("Framework started in "+((System.currentTimeMillis()-startTime)/1000)+" sec."); 
    }
    
    protected File newFile(String path) throws IOException {
        if (path.startsWith("/")) {
            return new File(path).getCanonicalFile();
        } else {
            return new File(home, path).getCanonicalFile();
        }
    }

    /**
     * Fill the classloader with all jars found in the defined classpath.
     * Return the list of bundle files. 
     * @param cl
     * @return
     */
    protected List<File> buildClassPath() throws IOException {
        List<File> bundleFiles = new ArrayList<File>();
        String libsCp = (String)env.get(LIBS);
        if (libsCp != null) {
            buildLibsClassPath(libsCp);
        }
        String bundlesCp = (String)env.get(BUNDLES);
        if (libsCp != null) {
            buildBundlesClassPath(bundlesCp, bundleFiles);
        }
        extractNestedJars(bundleFiles, new File(home, "tmp/nested-jars"));
        return bundleFiles;
    }

    protected void buildLibsClassPath(String libsCp) throws IOException {
        String[] ar = libsCp.split(":");
        for (int i=0; i<ar.length; i++) {
            String entry = ar[i];
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
        for (int i=0; i<ar.length; i++) {
            String entry = ar[i];
            File entryFile;
            if (entry.endsWith("/*")) {
                entryFile = newFile(entry.substring(0, entry.length() - 2));
                File[] files = entryFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        bundleFiles.add(file);
                        loader.addURL(file.toURI().toURL());
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
        JarFile jarFile = new JarFile(file);
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

    protected void extractNestedJar(JarFile file, ZipEntry entry, File dest) throws IOException {
        InputStream in = null;
        try {
            in = file.getInputStream(entry);
            copyToFile(in, dest);
        } finally {
            if (in != null) {
                in.close();
            }
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
        FileInputStream in = new FileInputStream(src); 
        try {
            copyToFile(in, file);
        } finally {
            in.close();
        }
    }
    
    public static void copyToFile(InputStream in, File file) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buffer = createBuffer(in.available());
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (out != null) {
                out.close();
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
