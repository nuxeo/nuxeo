/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    public static final String NUXEO_HOME_DIR = "nuxeo.home.dir";

    public static final String NUXEO_DATA_DIR = "nuxeo.data.dir";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

    public static final String NUXEO_CONFIG_DIR = "nuxeo.config.dir";

    public static final String NUXEO_WEB_DIR = "nuxeo.web.dir";

    private static final Log log = LogFactory.getLog(FrameworkLoader.class);

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

    public static synchronized void initialize(ClassLoader cl, File home,
            List<File> bundleFiles, Map<String, Object> hostEnv) {
        if (isInitialized) {
            return;
        }
        FrameworkLoader.home = home;
        FrameworkLoader.bundleFiles = bundleFiles == null ? new ArrayList<File>()
                : bundleFiles;
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
            throw new IllegalStateException(
                    "Framework is not initialized. Call initialize method first");
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
        System.setProperty(HOME_DIR, home.getAbsolutePath()); // make sure this
        // property was
        // correctly
        // initialized
        boolean doPreprocessing = true;
        String v = (String) hostEnv.get(PREPROCESSING);
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
                log.warn("Failed to install bundle: " + f, t);
                // do nothing
            }
        }
        osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED,
                systemBundle, null));
        // osgi.fireFrameworkEvent(new
        // FrameworkEvent(FrameworkEvent.AFTER_START, systemBundle, null));
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
        main.invoke(null,
                new Object[] { new String[] { home.getAbsolutePath() } });
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
            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                String v = (String) entry.getValue();
                v = StringUtils.expandVars(v, System.getProperties());
                System.setProperty((String) entry.getKey(), v);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system properties", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    protected static String getEnvProperty(String key,
            Map<String, Object> hostEnv, Properties sysprops,
            boolean addToSystemProperties) {
        String v = (String) hostEnv.get(key);
        if (v == null) {
            v = System.getProperty(key);
        }
        if (v != null) {
            v = StringUtils.expandVars(v, sysprops);
            if (addToSystemProperties) {
                sysprops.setProperty(key, v);
            }
        }
        return v;
    }

    protected static Environment createEnvironment(File home,
            Map<String, Object> hostEnv) {
        Properties sysprops = System.getProperties();
        sysprops.setProperty(NUXEO_HOME_DIR, home.getAbsolutePath());

        Environment env = new Environment(home);
        String v = (String) hostEnv.get(HOST_NAME);
        env.setHostApplicationName(v == null ? Environment.NXSERVER_HOST : v);
        v = (String) hostEnv.get(HOST_VERSION);
        if (v != null) {
            env.setHostApplicationVersion((String) hostEnv.get(HOST_VERSION));
        }

        v = getEnvProperty(NUXEO_DATA_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setData(new File(v));
        } else {
            sysprops.setProperty(NUXEO_DATA_DIR,
                    env.getData().getAbsolutePath());
        }

        v = getEnvProperty(NUXEO_LOG_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setLog(new File(v));
        } else {
            sysprops.setProperty(NUXEO_LOG_DIR, env.getLog().getAbsolutePath());
        }

        v = getEnvProperty(NUXEO_TMP_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setTemp(new File(v));
        } else {
            sysprops.setProperty(NUXEO_TMP_DIR, env.getTemp().getAbsolutePath());
        }

        v = getEnvProperty(NUXEO_CONFIG_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setConfig(new File(v));
        } else {
            sysprops.setProperty(NUXEO_CONFIG_DIR,
                    env.getConfig().getAbsolutePath());
        }

        v = getEnvProperty(NUXEO_WEB_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setWeb(new File(v));
        } else {
            sysprops.setProperty(NUXEO_WEB_DIR, env.getWeb().getAbsolutePath());
        }

        v = (String) hostEnv.get(ARGS);
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
        String newline = System.getProperty("line.separator");
        Environment env = Environment.getDefault();
        String hr = newline
                + "======================================================================"
                + newline;
        StringBuilder msg = new StringBuilder(hr);
        msg.append("= Starting Nuxeo Framework" + newline);
        msg.append(hr);
        msg.append("  * Home Directory = " + home + newline);
        msg.append("  * Data Directory = " + env.getData() + newline);
        msg.append("  * Log Directory = " + env.getLog() + newline);
        msg.append("  * Configuration Directory = " + env.getConfig() + newline);
        msg.append("  * Temp Directory = " + env.getTemp() + newline);
        // System.out.println("  * System Bundle = "+systemBundle);
        // System.out.println("  * Command Line Args = "+Arrays.asList(env.getCommandLineArguments()));
        msg.append(hr);
        log.info(msg);
    }

}
