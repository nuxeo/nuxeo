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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.osgi.application.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;

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

    public static synchronized void initialize(ClassLoader cl, File home, List<File> bundleFiles,
            Map<String, Object> hostEnv) {
        if (isInitialized) {
            return;
        }
        FrameworkLoader.home = home;
        FrameworkLoader.bundleFiles = bundleFiles == null ? new ArrayList<>() : bundleFiles;
        Collections.sort(FrameworkLoader.bundleFiles);

        loader = cl;
        doInitialize(hostEnv);
        osgi = new OSGiAdapter(home);
        isInitialized = true;
    }

    public static synchronized void start() throws BundleException {
        if (isStarted) {
            return;
        }
        if (!isInitialized) {
            throw new IllegalStateException("Framework is not initialized. Call initialize method first");
        }

        try {
            doStart();
        } finally {
            isStarted = true;
        }
    }

    public static synchronized void stop() throws BundleException {
        if (!isStarted) {
            return;
        }
        try {
            doStop();
        } finally {
            isStarted = false;
        }
    }

    private static void doInitialize(Map<String, Object> hostEnv) {
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
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to run preprocessing", e);
            }
        }
    }

    protected static void printDeploymentOrderInfo(List<File> files) {
        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder();
            for (File file : files) {
                if (file != null) {
                    buf.append("\n\t" + file.getPath());
                }
            }
            log.debug("Deployment order: " + buf.toString());
        }
    }

    protected static Attributes.Name SYMBOLIC_NAME = new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME);

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

    private static void doStart() throws BundleException {
        printStartMessage();
        // install system bundle first
        BundleFile bf;
        try {
            bf = new SystemBundleFile(home);
        } catch (IOException e) {
            throw new BundleException("Cannot create system bundle for " + home, e);
        }
        SystemBundle systemBundle = new SystemBundle(osgi, bf, loader);
        osgi.setSystemBundle(systemBundle);
        printDeploymentOrderInfo(bundleFiles);
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
        osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, systemBundle, null));
        // osgi.fireFrameworkEvent(new
        // FrameworkEvent(FrameworkEvent.AFTER_START, systemBundle, null));
    }

    private static void doStop() throws BundleException {
        try {
            osgi.shutdown();
        } catch (IOException e) {
            throw new BundleException("Cannot shutdown OSGi", e);
        }
    }

    public static void uninstall(String symbolicName) throws BundleException {
        BundleImpl bundle = osgi.getBundle(symbolicName);
        if (bundle != null) {
            bundle.uninstall();
        }
    }

    public static String install(File f) throws IOException, BundleException {
        BundleFile bf = null;
        if (f.isDirectory()) {
            bf = new DirectoryBundleFile(f);
        } else {
            bf = new JarBundleFile(f);
        }
        BundleImpl bundle = new BundleImpl(osgi, bf, loader);
        if (bundle.getState() == 0) {
            // not a bundle (no Bundle-SymbolicName)
            return null;
        }
        osgi.install(bundle);
        return bundle.getSymbolicName();
    }

    public static void preprocess() {
        File f = new File(home, "OSGI-INF/deployment-container.xml");
        if (!f.isFile()) { // make sure a preprocessing container is defined
            return;
        }
        try {
            Class<?> klass = loader.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
            Method main = klass.getMethod("main", String[].class);
            main.invoke(null, new Object[] { new String[] { home.getAbsolutePath() } });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void loadSystemProperties() {
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
        } catch (IOException e) {
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

    protected static String getEnvProperty(String key, Map<String, Object> hostEnv, Properties sysprops,
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

    protected static Environment createEnvironment(File home, Map<String, Object> hostEnv) {
        Properties sysprops = System.getProperties();
        sysprops.setProperty(Environment.NUXEO_RUNTIME_HOME, home.getAbsolutePath());

        Environment env = Environment.getDefault();
        if (env == null) {
            env = new Environment(home);
        }
        if (!home.equals(env.getRuntimeHome())) {
            env.setRuntimeHome(home);
        }

        String v = (String) hostEnv.get(HOST_NAME);
        env.setHostApplicationName(v == null ? Environment.NXSERVER_HOST : v);
        v = (String) hostEnv.get(HOST_VERSION);
        if (v != null) {
            env.setHostApplicationVersion((String) hostEnv.get(HOST_VERSION));
        }

        v = getEnvProperty(Environment.NUXEO_DATA_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setData(new File(v));
        } else {
            sysprops.setProperty(Environment.NUXEO_DATA_DIR, env.getData().getAbsolutePath());
        }

        v = getEnvProperty(Environment.NUXEO_LOG_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setLog(new File(v));
        } else {
            sysprops.setProperty(Environment.NUXEO_LOG_DIR, env.getLog().getAbsolutePath());
        }

        v = getEnvProperty(Environment.NUXEO_TMP_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setTemp(new File(v));
        } else {
            sysprops.setProperty(Environment.NUXEO_TMP_DIR, env.getTemp().getAbsolutePath());
        }

        v = getEnvProperty(Environment.NUXEO_CONFIG_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setConfig(new File(v));
        } else {
            sysprops.setProperty(Environment.NUXEO_CONFIG_DIR, env.getConfig().getAbsolutePath());
        }

        v = getEnvProperty(Environment.NUXEO_WEB_DIR, hostEnv, sysprops, true);
        if (v != null) {
            env.setWeb(new File(v));
        } else {
            sysprops.setProperty(Environment.NUXEO_WEB_DIR, env.getWeb().getAbsolutePath());
        }

        v = (String) hostEnv.get(ARGS);
        if (v != null) {
            env.setCommandLineArguments(v.split("\\s+"));
        } else {
            env.setCommandLineArguments(new String[0]);
        }
        return env;
    }

    protected static void printStartMessage() {
        StringBuilder msg = getStartMessage();
        log.info(msg);
    }

    /**
     * @since 5.5
     * @return Environment summary
     */
    protected static StringBuilder getStartMessage() {
        String newline = System.getProperty("line.separator");
        Environment env = Environment.getDefault();
        String hr = "======================================================================";
        StringBuilder msg = new StringBuilder(newline);
        msg.append(hr + newline);
        msg.append("= Starting Nuxeo Framework" + newline);
        msg.append(hr + newline);
        msg.append("  * Server home = " + env.getServerHome() + newline);
        msg.append("  * Runtime home = " + env.getRuntimeHome() + newline);
        msg.append("  * Data Directory = " + env.getData() + newline);
        msg.append("  * Log Directory = " + env.getLog() + newline);
        msg.append("  * Configuration Directory = " + env.getConfig() + newline);
        msg.append("  * Temp Directory = " + env.getTemp() + newline);
        // System.out.println("  * System Bundle = "+systemBundle);
        // System.out.println("  * Command Line Args = "+Arrays.asList(env.getCommandLineArguments()));
        msg.append(hr);
        return msg;
    }
}
