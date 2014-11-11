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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiAdapter {

    public static final String HOST_NAME = "org.nuxeo.app.host.name";

    public static final String HOST_VERSION = "org.nuxeo.app.host.version";

    public static final String HOST_SERVER = "org.nuxeo.app.host.server";

    public static final String HOME_DIR = "org.nuxeo.app.home";

    public static final String LOG_DIR = "org.nuxeo.app.log";

    public static final String DATA_DIR = "org.nuxeo.app.data";

    public static final String TMP_DIR = "org.nuxeo.app.tmp";

    public static final String NESTED_DIR = "org.nuxeo.app.nested";

    public static final String WEB_DIR = "org.nuxeo.app.web";

    public static final String CONFIG_DIR = "org.nuxeo.app.config";

    public static final String LIBS = "org.nuxeo.app.libs"; // class path

    public static final String BUNDLES = "org.nuxeo.app.bundles"; // class path

    public static final String DEVMODE = "org.nuxeo.app.devmode";

    public static final String PREPROCESSING = "org.nuxeo.app.preprocessing";

    public static final String SCAN_FOR_NESTED_JARS = "org.nuxeo.app.scanForNestedJars";

    public static final String BUNDLE_FACTORY = "org.nuxeo.app.bundleFactory";

    public static final String FLUSH_CACHE = "org.nuxeo.app.flushCache";

    public static final String ARGS = "org.nuxeo.app.args";

    public static final String BOOT_DELEGATION = "org.osgi.framework.bootdelegation";

    private static final Log log = LogFactory.getLog(OSGiAdapter.class);

    protected final File workingDir;

    protected final File dataDir;

    protected OSGiSystemBundle system;

    protected OSGiSystemContext osgi;

    protected final Properties properties = new Properties();

    public OSGiAdapter() throws IOException {
        this(System.getProperties());
    }

    public OSGiAdapter(Properties properties) throws IOException  {
        this.properties.putAll(properties);
        // setting up default properties
        properties.put(Constants.FRAMEWORK_VENDOR, "Nuxeo");
        properties.put(Constants.FRAMEWORK_VERSION, "1.0.0");
        String workingDirPath = getProperty(HOME_DIR,
                File.createTempFile("nxosgi", null).toString());
        workingDir = new File(workingDirPath);
        workingDir.mkdirs();
        String dataDirPath = getProperty(DATA_DIR,
                workingDirPath.concat("/data"));
        dataDir = new File(dataDirPath);
        dataDir.mkdirs();
    }

    public OSGiAdapter(File workingDir, File dataDir) {
        properties.put(Constants.FRAMEWORK_VENDOR, "Nuxeo");
        properties.put(Constants.FRAMEWORK_VERSION, "1.0.0");
        this.workingDir = workingDir;
        this.workingDir.mkdirs();
        properties.put(HOME_DIR, workingDir.toPath());
        this.dataDir = dataDir;
        this.dataDir.mkdirs();
        properties.put(DATA_DIR, dataDir);
    }

    public void start() throws BundleException {
        system.setResolved();
        system.start();
    }

    protected OSGiSystemBundle newSystemBundle() throws IOException,
            BundleException {
        String path = OSGiAdapter.class.getCanonicalName().replace('.', '/').concat(
                ".class");
        URL loc = OSGiAdapter.class.getClassLoader().getResource(path);
        String proto = loc.getProtocol();
        String jarPath;
        if ("jar".equals(proto)) {
            JarURLConnection connection = (JarURLConnection) loc.openConnection();
            jarPath = connection.getJarFileURL().getFile();
        } else if ("file".equals(proto)) {
            jarPath = loc.getFile();
            jarPath = jarPath.substring(0, jarPath.length() - path.length());
        } else {
            throw new UnsupportedOperationException("unknown protocol " + proto);
        }
        File file = new File(jarPath);
        OSGiSystemBundleFile bfile = new OSGiSystemBundleFile(file.toPath());
        return new OSGiSystemBundle(bfile, properties);
    }

    protected OSGiBundle newConfigBundle() throws BundleException, IOException {
        OSGiBundleFile bf = new OSGiSystemFragmentFile(workingDir.toPath());
        return new OSGiBundleFragment(bf);
    }

    public void initialize() {
        // setting up default properties
        try {
            system = newSystemBundle();
            system.init();
            osgi = system.osgi;
            osgi.adapter = this;
            OSGiBundle workingBundle = newConfigBundle();
            osgi.registry.register(workingBundle);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot create system bundle for " + workingDir, e);
        }
    }

    public void setHome(File home) {
        if (osgi != null) {
            throw new IllegalStateException("OSGi framework already started");
        }
        properties.put("nuxeo.runtime.home", home.getPath());
        File file = new File(home, "system.properties");
        if (!file.isFile()) {
            return;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            Properties p = new Properties();
            p.load(in);
            setProperties(p);
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

    public String getHome() {
        return properties.getProperty("nuxeo.runtime.home");
    }

    public void shutdown() throws BundleException, IOException {
        system.uninstall();
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public File getDataDir() {
        return dataDir;
    }

    public Bundle getBundle(String symbolicName) {
        OSGiBundleRegistration reg = osgi.registry.bundlesByName.get(symbolicName);
        if (reg == null) {
            return null;
        }
        return reg.bundle;
    }

    public Bundle[] getInstalledBundles() {
        return osgi.getBundles();
    }

    public Bundle install(URI location) throws BundleException {
        return osgi.installBundle(location.toASCIIString());
    }

    public void uninstall(Bundle bundle) throws BundleException {
        bundle.uninstall();
    }

    public void fireFrameworkEvent(FrameworkEvent event) {
        osgi.fireFrameworkEvent(event);
    }

    public void fireBundleEvent(BundleEvent event) {

    }

    public OSGiSystemBundle getSystemBundle() {
        return system;
    }

    public OSGiSystemContext getSytemContext() {
        return (OSGiSystemContext) system.context;
    }

    public OSGiLoader getSystemLoader() {
        return osgi.loader;
    }

    public void setProperties(Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public Bundle install(OSGiBundleFile bf) throws BundleException {
        return osgi.installBundle(bf);
    }

}
