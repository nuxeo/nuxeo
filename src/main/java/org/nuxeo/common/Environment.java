/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Environment {

    private static Log logger = LogFactory.getLog(Environment.class);

    /**
     * Constants that identifies possible hosts for the framework.
     */
    public static final String JBOSS_HOST = "JBoss";

    // Jetty or GF3 embedded
    public static final String NXSERVER_HOST = "NXServer";

    public static final String TOMCAT_HOST = "Tomcat";

    public static final String NUXEO_HOME_DIR = "nuxeo.home.dir";

    /**
     * @since 5.6
     */
    public static final String NUXEO_HOME = "nuxeo.home";

    /**
     * @since 5.4.2
     */
    public static final String NUXEO_RUNTIME_HOME = "nuxeo.runtime.home";

    public static final String NUXEO_DATA_DIR = "nuxeo.data.dir";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    public static final String NUXEO_PID_DIR = "nuxeo.pid.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

    public static final String NUXEO_CONFIG_DIR = "nuxeo.config.dir";

    public static final String NUXEO_WEB_DIR = "nuxeo.web.dir";

    /**
     * @since 5.6
     */
    public static final String NUXEO_CONTEXT_PATH = "org.nuxeo.ecm.contextPath";

    /**
     * The home directory.
     *
     * @deprecated never defined; use {@link #NUXEO_HOME_DIR}
     */
    @Deprecated
    public static final String HOME_DIR = "org.nuxeo.app.home";

    /**
     * The web root.
     *
     * @deprecated never defined; use {@link #NUXEO_WEB_DIR}
     */
    @Deprecated
    public static final String WEB_DIR = "org.nuxeo.app.web";

    /**
     * The config directory.
     *
     * @deprecated never defined; use {@link #NUXEO_CONFIG_DIR}
     */
    @Deprecated
    public static final String CONFIG_DIR = "org.nuxeo.app.config";

    /**
     * The data directory.
     *
     * @deprecated never defined; use {@link #NUXEO_DATA_DIR}
     */
    @Deprecated
    public static final String DATA_DIR = "org.nuxeo.app.data";

    /**
     * The log directory.
     *
     * @deprecated never defined; use {@link #NUXEO_LOG_DIR}
     */
    @Deprecated
    public static final String LOG_DIR = "org.nuxeo.app.log";

    /**
     * The application layout (optional):
     * directory containing nuxeo runtime osgi bundles.
     */
    public static final String BUNDLES_DIR = "nuxeo.osgi.app.bundles";

    public static final String BUNDLES = "nuxeo.osgi.bundles";

    private static volatile Environment DEFAULT;

    protected final File home;

    protected File data;

    protected File log;

    protected File config;

    protected File web;

    protected File temp;

    protected final Properties properties;

    protected String[] args;

    protected boolean isAppServer;

    protected String hostAppName;

    protected String hostAppVersion;

    protected Iterable<URL> configProvider;

    // Handy parameter to distinguish from (Runtime)home
    private File serverHome = null;

    // Handy parameter to distinguish from (Server)home
    private File runtimeHome;

    public static final String DISTRIBUTION_NAME = "org.nuxeo.distribution.name";

    public static final String DISTRIBUTION_VERSION = "org.nuxeo.distribution.version";

    public Environment(File home) {
        this(home, null);
    }

    public Environment(File home, Properties properties) {
        this.home = home;
        this.properties = new Properties();
        if (properties != null) {
            loadProperties(properties);
        }
        this.properties.put(HOME_DIR, this.home.getAbsolutePath());
    }

    public static synchronized void setDefault(Environment env) {
        DEFAULT = env;
    }

    public static Environment getDefault() {
        if (DEFAULT == null) {
            tryInitEnvironment();
        }
        return DEFAULT;
    }

    private static synchronized void tryInitEnvironment() {
        String homeDir = System.getProperty(NUXEO_HOME);
        if (homeDir != null) {
            File home = new File(homeDir);
            if (home.isDirectory()) {
                DEFAULT = new Environment(home);
            }
        }
    }

    public File getHome() {
        return home;
    }

    public boolean isApplicationServer() {
        return isAppServer;
    }

    public void setIsApplicationServer(boolean isAppServer) {
        this.isAppServer = isAppServer;
    }

    public String getHostApplicationName() {
        return hostAppName;
    }

    public String getHostApplicationVersion() {
        return hostAppVersion;
    }

    public void setHostApplicationName(String name) {
        hostAppName = name;
    }

    public void setHostApplicationVersion(String version) {
        hostAppVersion = version;
    }

    public File getTemp() {
        if (temp == null) {
            temp = new File(home, "tmp");
        }
        return temp;
    }

    public void setTemp(File temp) {
        this.temp = temp;
        properties.put(NUXEO_TMP_DIR, temp.getAbsolutePath());
    }

    public File getConfig() {
        if (config == null) {
            config = new File(home, "config");
        }
        return config;
    }

    public void setConfig(File config) {
        this.config = config;
        properties.put(NUXEO_CONFIG_DIR, config.getAbsolutePath());
    }

    public File getLog() {
        if (log == null) {
            log = new File(home, "log");
        }
        return log;
    }

    public void setLog(File log) {
        this.log = log;
        properties.put(NUXEO_LOG_DIR, log.getAbsolutePath());
    }

    public File getData() {
        if (data == null) {
            data = new File(home, "data");
        }
        return data;
    }

    public void setData(File data) {
        this.data = data;
        properties.put(NUXEO_DATA_DIR, data.getAbsolutePath());
    }

    public File getWeb() {
        if (web == null) {
            web = new File(home, "web");
        }
        return web;
    }

    public void setWeb(File web) {
        this.web = web;
        properties.put(NUXEO_WEB_DIR, web.getAbsolutePath());
    }

    /**
     * @since 5.4.2
     */
    public File getRuntimeHome() {
        if (runtimeHome == null) {
            initRuntimeHome();
        }
        return runtimeHome;
    }

    /**
     * @since 5.4.2
     */
    public void setRuntimeHome(File runtimeHome) {
        this.runtimeHome = runtimeHome;
        properties.put(NUXEO_RUNTIME_HOME, runtimeHome.getAbsolutePath());
    }

    public String[] getCommandLineArguments() {
        return args;
    }

    public void setCommandLineArguments(String[] args) {
        this.args = args;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String val = properties.getProperty(key);
        return val == null ? defaultValue : val;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public Properties getProperties() {
        return properties;
    }

    public void loadProperties(Properties props) {
        properties.putAll(props);
    }

    public boolean isJBoss() {
        return JBOSS_HOST.equals(hostAppName);
    }

    public boolean isJetty() {
        return NXSERVER_HOST.equals(hostAppName);
    }

    public boolean isTomcat() {
        return TOMCAT_HOST.equals(hostAppName);
    }

    /**
     * Initialization with System properties to avoid issues due to home set
     * with runtime home instead of server home.
     *
     * @since 5.4.1
     */
    public void init() {
        String dataDir = System.getProperty(NUXEO_DATA_DIR);
        String configDir = System.getProperty(NUXEO_CONFIG_DIR);
        String logDir = System.getProperty(NUXEO_LOG_DIR);
        String tmpDir = System.getProperty(NUXEO_TMP_DIR);

        initServerHome();
        initRuntimeHome();
        if (StringUtils.isNotEmpty(dataDir)) {
            setData(new File(dataDir));
        }
        if (StringUtils.isNotEmpty(configDir)) {
            setConfig(new File(configDir));
        }
        if (StringUtils.isNotEmpty(logDir)) {
            setLog(new File(logDir));
        }
        if (StringUtils.isNotEmpty(tmpDir)) {
            setTemp(new File(tmpDir));
        }
    }

    private void initRuntimeHome() {
        String runtimeDir = System.getProperty(NUXEO_RUNTIME_HOME);
        if (runtimeDir != null && !runtimeDir.isEmpty()) {
            runtimeHome = new File(runtimeDir);
        } else {
            runtimeHome = home;
        }
    }

    /**
     * This method always returns the server home (or null if
     * {@link #NUXEO_HOME_DIR} is not set), whereas {@link #getHome()} may
     * return runtime home.
     *
     * @since 5.4.2
     * @return Server home
     */
    public File getServerHome() {
        if (serverHome == null) {
            initServerHome();
        }
        return serverHome;
    }

    /**
     * @since 5.4.2
     */
    public void setServerHome(File serverHome) {
        this.serverHome = serverHome;
        properties.put(NUXEO_HOME_DIR, serverHome.getAbsolutePath());
    }

    private void initServerHome() {
        String homeDir = System.getProperty(NUXEO_HOME,
                System.getProperty(NUXEO_HOME_DIR));
        if (homeDir != null && !homeDir.isEmpty()) {
            serverHome = new File(homeDir);
        } else {
            logger.warn(String.format(
                    "Could not get %s neither %s system properties, will use %s",
                    NUXEO_HOME, NUXEO_HOME_DIR, home));
            serverHome = home;
        }
        logger.debug(this);
    }

    public void setConfigurationProvider(Iterable<URL> configProvider) {
        this.configProvider = configProvider;
    }

    public Iterable<URL> getConfigurationProvider() {
        return this.configProvider;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
