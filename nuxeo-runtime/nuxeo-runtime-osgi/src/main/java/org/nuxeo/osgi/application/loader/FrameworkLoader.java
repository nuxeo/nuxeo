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
package org.nuxeo.osgi.application.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.osgi.SystemBundleFile;
import org.osgi.framework.FrameworkEvent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FrameworkLoader {

    public static final String HOST_NAME = "org.nuxeo.app.host.name";
    public static final String HOST_VERSION = "org.nuxeo.app.host.version";
    public static final String HOME_DIR = "org.nuxeo.app.home";
    public static final String LOG_DIR = "org.nuxeo.app.log";
    public static final String DATA_DIR = "org.nuxeo.app.data";
    public static final String TMP_DIR = "org.nuxeo.app.tmp";
    public static final String WEB_DIR = "org.nuxeo.app.web";
    public static final String CONFIG_DIR = "org.nuxeo.app.config";
    public static final String LIBS = "org.nuxeo.app.libs"; // class path
    public static final String BUNDLES = "org.nuxeo.app.bundles"; // class path
    public static final String DEVMODE = "org.nuxeo.app.devmode";
    public static final String PREPROCESSING = "org.nuxeo.app.preprocessing";
    public static final String SCAN_FOR_NESTED_JARS = "org.nuxeo.app.scanForNestedJars";
    public static final String FLUSH_CACHE = "org.nuxeo.app.flushCache";
    public static final String ARGS = "org.nuxeo.app.args";

    
    private static boolean isInitialized;
    private static boolean isStarted;
    private static File home;    
    private static ClassLoader loader;
    private static List<File> bundleFiles;
    private static OSGiAdapter osgi;
    
    
    public static OSGiAdapter osgi() {
        return osgi;
    }
    
    public static ClassLoader getLoader() {
        return loader;
    }
    
    public static synchronized void initialize(ClassLoader cl, File home, List<File> bundleFiles, Map<String,Object> hostEnv) throws Exception {
        if (isInitialized) {
            return;
        }
        FrameworkLoader.home = home;
        FrameworkLoader.bundleFiles = bundleFiles == null ? new ArrayList<File>() : bundleFiles;
        loader = cl;
        doInitialize(hostEnv);
        osgi = new OSGiAdapter(home);
        isInitialized = true;
    }
    
    public static synchronized void start() throws Exception {
        if (isStarted) {
            return;
        }
        if (!isInitialized) {
            throw new IllegalStateException("Framework is not initialized. Call initialize method first");
        }
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            doStart();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        isStarted = true;   
    }

    public static synchronized void stop() throws Exception {
        if (!isStarted) {
            return;
        }
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            doStop();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        isStarted = false;
    }

    
    private static void doInitialize(Map<String, Object> hostEnv) {
        System.setProperty(HOME_DIR, home.getAbsolutePath()); // mkae sure this property was correctly initialized
        boolean doPreprocessing = true;
        String v = (String)hostEnv.get(PREPROCESSING);
        if (v != null) {
            doPreprocessing = Boolean.parseBoolean(v);
        }
        // build environment
        Environment env = createEnvironment(home, hostEnv);        
        Environment.setDefault(env);
        loadSystemProperties();
        // start bundle pre-processing if requested
        if (doPreprocessing) {
            try {
                preprocess();
            } catch (Exception e) {
                throw new RuntimeException("Failed to run preprocessing", e);
            }
        }
    }

    private static void doStart() throws Exception {
        printStartMessage();
        // install system bundle first
        BundleFile bf = new SystemBundleFile(home);
        SystemBundle systemBundle = new SystemBundle(osgi, bf, loader);
        osgi.install(systemBundle);
        for (File f : bundleFiles) {
            try {
                if (f.isDirectory()) {
                    bf = new DirectoryBundleFile(f);
                } else {
                    bf = new JarBundleFile(f);
                }
                BundleImpl bundle = new BundleImpl(osgi, bf, loader);
                osgi.install(bundle);
            } catch (Throwable t) { // silently ignore
                System.err.println("Failed to install bundle: "+f);
                // do nothing
            }
        }
        osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, systemBundle, null));
        //osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.AFTER_START, systemBundle, null));
    }

    private static void doStop() throws Exception {
        osgi.shutdown();        
    }
    
    public static void preprocess() throws Exception {
        File f = new File(home, "OSGI-INF/deployment-container.xml");
        if (!f.isFile()) { // make sure a preprocessing container is defined
            return;
        }
        Class<?> klass = loader.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
        Method main = klass.getMethod("main", String[].class);
        main.invoke(null, new Object[] {new String[] {home.getAbsolutePath()}});
    }

    protected static void loadSystemProperties() {
        System.setProperty("org.nuxeo.app.home", home.getAbsolutePath());
        File file = new File(home, "system.properties");
        if (!file.isFile()) {
            return;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            Properties p = new Properties();            
            p.load(in);
            for (Map.Entry<Object,Object> entry : p.entrySet()) {
                String v = (String)entry.getValue();
                v = StringUtils.expandVars(v, System.getProperties());
                System.setProperty((String)entry.getKey(), v);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system properties", e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    protected static Environment createEnvironment(File home, Map<String,Object> hostEnv) {
        Environment env = new Environment(home);
        String v = (String)hostEnv.get(HOST_NAME);
        env.setHostApplicationName(v == null ? Environment.NXSERVER_HOST : v);
        v = (String)hostEnv.get(HOST_VERSION);
        if (v != null) {
            env.setHostApplicationVersion((String)hostEnv.get(HOST_VERSION));
        }
        v = (String)hostEnv.get(DATA_DIR);
        if (v != null) {
            env.setData(new File(home, v));
        }
        v = (String)hostEnv.get(LOG_DIR);
        if (v != null) {
            env.setData(new File(home, v));
        }
        v = (String)hostEnv.get(TMP_DIR);
        if (v != null) {
            env.setData(new File(home, v));
        }
        v = (String)hostEnv.get(WEB_DIR);
        if (v != null) {
            env.setData(new File(home, v));
        }
        v = (String)hostEnv.get(CONFIG_DIR);
        if (v != null) {
            env.setData(new File(home, v));
        }
        v = (String)hostEnv.get(ARGS);
        if (v != null) {
            env.setCommandLineArguments(v.split("\\s+"));
        } else {
            env.setCommandLineArguments(new String[0]);
        }
        env.getData().mkdirs();
        env.getLog().mkdirs();
        env.getTemp().mkdirs();
        
        return env;
    }
    

    protected static void printStartMessage() {
        Environment env = Environment.getDefault();
        System.out.println("======================================================================");
        System.out.println("= Starting Nuxeo Framework");
        System.out.println("======================================================================");
        System.out.println("  * Home Directory = "+home);
        System.out.println("  * Data Directory = "+env.getData());
        System.out.println("  * Log Directory = "+env.getLog());
        System.out.println("  * Configuration Directory = "+env.getConfig());
        System.out.println("  * Temp Directory = "+env.getTemp());
//        System.out.println("  * System Bundle = "+systemBundle);
//        System.out.println("  * Command Line Args = "+Arrays.asList(env.getCommandLineArguments()));
        System.out.println("======================================================================");
    }
    
}
