/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationBundleLoader {

    protected StandaloneBundleLoader bundleLoader;
    protected final StandaloneApplication app;
    protected boolean useCache = false;
    protected boolean extractNestedJARs = true;
    protected boolean scanForNestedJARs = true;

    public ApplicationBundleLoader(StandaloneApplication app) {
        this(app, false);
    }

    public ApplicationBundleLoader(StandaloneApplication app, boolean useCache) {
        this.app = app;
        bundleLoader = new StandaloneBundleLoader(app);
        this.useCache = useCache;
    }

    public void setScanForNestedJARs(boolean scanForNestedJARs) {
        this.scanForNestedJARs = scanForNestedJARs;
    }

    public boolean getScanForNestedJARs() {
        return scanForNestedJARs;
    }

    public void setExtractNestedJARs(boolean extractNestedJARs) {
        this.extractNestedJARs = extractNestedJARs;
    }

    public boolean getExtractNestedJARs() {
        return extractNestedJARs;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public boolean getUseCache() {
        return useCache;
    }

    public StandaloneBundleLoader getBundleLoader() {
        return bundleLoader;
    }

    public File getCacheFile() {
        return new File(app.getDataDir(), "bundles.cache");
    }

    public ClassLoader loadBundles(List<File> classPath) throws Exception {
        // create the standalone loader
        bundleLoader = new StandaloneBundleLoader(app, app.getSharedClassLoader());
        Thread.currentThread().setContextClassLoader(bundleLoader.getSharedClassLoader().getLoader());

        aboutToStartRuntime();
        boolean scan = true;
        File file = getCacheFile();
        if (useCache) {
            if (file.isFile()) { // use the cache
                scan = false;
                try {
                    fastLoad(file);
                } catch (BundleException e) {
                    scan = true;
                }
            }
        }
        if (scan) {
            List<BundleFile> bundles = new ArrayList<BundleFile>();
            List<BundleFile> jars = new ArrayList<BundleFile>();
            scanAndLoad(classPath, bundles, jars);
            writeCache(file, bundles, jars);
            app.installAll(bundles);
        }
        // that's all
        runtimeStarted();
        return bundleLoader.getSharedClassLoader().getLoader();
    }

    public void scanAndLoad(List<File> classPath, List<BundleFile> bundles, List<BundleFile> jars) {
        bundleLoader.setScanForNestedJARs(scanForNestedJARs);
        bundleLoader.setExtractNestedJARs(extractNestedJARs);

        for (File file : classPath) {
            if (file.isFile()) { // a JAR file
                String name = file.getName();
                if (!name.endsWith(".jar") || name.endsWith(".rar")
                        || name.endsWith(".zip") || name.endsWith(".sar")) {
                    continue;
                }
                try {
                    JarFile jar = new JarFile(file);
                    JarBundleFile bf = new JarBundleFile(jar);
                    if (bf.getSymbolicName() != null) {
                        bundles.add(bf);
                    } else {
                       jars.add(bf);
                    }
                } catch (IOException e) { // may be not a JAR
                    continue;
                }
            } else if (file.isDirectory()) { // a file directory
                try {
                    DirectoryBundleFile bf = new DirectoryBundleFile(file);
                    if (bf.getSymbolicName() != null) {
                        bundles.add(bf);
                    } else {
                        jars.add(bf);
                    }
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }

    public static void writeCache(File file, List<BundleFile> bundles, List<BundleFile> jars) throws BundleException {
        // write loaded bundles to the cache
        Throwable error = null;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (BundleFile bf : bundles) {
                writer.append(bf.getFile().getAbsolutePath());
                writer.newLine();
            }
            writer.append("#");
            writer.newLine();
            for (BundleFile bf : jars) {
                writer.append(bf.getFile().getAbsolutePath());
                writer.newLine();
            }
        } catch (Throwable e) {
            error = e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    error = e;
                }
            }
            if (error != null) {
                file.delete();
                throw new BundleException("failed to write cache file", error);
            }
        }
    }

    public void fastLoad(File file) throws BundleException {
        Throwable error = null;
        BufferedReader reader = null;
        List<BundleFile> bundles = new ArrayList<BundleFile>();
        try {
            reader = new BufferedReader(new FileReader(file));
            List<BundleFile> list = bundles;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#")) {
                    list = null;
                    continue;
                }
                BundleFile bf = null;
                File f = new File(line.trim());
                if (f.isDirectory()) {
                    bf = new DirectoryBundleFile(f);
                } else {
                    bf = new JarBundleFile(f);
                }
                bundleLoader.loadJAR(bf);
                if (list != null) {
                    list.add(bf);
                }
            }
        } catch (Throwable t) {
            error = t;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    error = e;
                }
            }
            if (error != null) {
                throw new BundleException("Failed to load runtime application from cache info", error);
            }
        }
        // install found bundles
        app.installAll(bundles);
    }

    protected void aboutToStartRuntime() {
        // do nothing
    }

    protected void runtimeStarted() {
        // do nothing
    }

}
