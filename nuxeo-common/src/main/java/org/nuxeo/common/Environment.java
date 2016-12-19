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

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_DATA_DIR = "data";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_LOG_DIR = "log";

    public static final String NUXEO_PID_DIR = "nuxeo.pid.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_TMP_DIR = "tmp";

    public static final String NUXEO_CONFIG_DIR = "nuxeo.config.dir";

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_CONFIG_DIR = "config";

    public static final String NUXEO_WEB_DIR = "nuxeo.web.dir";

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_WEB_DIR = "web";

    /**
     * @since 5.9.4
     */
    public static final String NUXEO_MP_DIR = "nuxeo.mp.dir";

    /**
     * @since 5.9.4
     */
    public static final String DEFAULT_MP_DIR = "packages";

    /**
     * @since 5.6
     */
    public static final String NUXEO_CONTEXT_PATH = "org.nuxeo.ecm.contextPath";

    /**
     * The application layout (optional): directory containing nuxeo runtime osgi bundles.
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
    private File runtimeHome = null;

    public static final String SERVER_STATUS_KEY = "server.status.key";

    public static final String DISTRIBUTION_NAME = "org.nuxeo.distribution.name";

    public static final String DISTRIBUTION_VERSION = "org.nuxeo.distribution.version";

    /**
     * @since 7.10
     */
    public static final String DISTRIBUTION_SERVER = "org.nuxeo.distribution.server";

    /**
     * @since 7.10
     */
    public static final String DISTRIBUTION_DATE = "org.nuxeo.distribution.date";

    /**
     * @since 7.10
     */
    public static final String DISTRIBUTION_PACKAGE = "org.nuxeo.distribution.package";

    /**
     * @since 7.10
     */
    public static final String PRODUCT_NAME = "org.nuxeo.ecm.product.name";

    /**
     * @since 7.10
     */
    public static final String PRODUCT_VERSION = "org.nuxeo.ecm.product.version";

    // proxy
    /**
     * @since 6.0
     */
    public static final String NUXEO_HTTP_PROXY_HOST = "nuxeo.http.proxy.host";

    /**
     * @since 6.0
     */
    public static final String NUXEO_HTTP_PROXY_PORT = "nuxeo.http.proxy.port";

    /**
     * @since 6.0
     */
    public static final String NUXEO_HTTP_PROXY_LOGIN = "nuxeo.http.proxy.login";

    /**
     * @since 6.0
     */
    public static final String NUXEO_HTTP_PROXY_PASSWORD = "nuxeo.http.proxy.password";

    /**
     * @since 7.4
     */
    public static final String CRYPT_ALGO = "server.crypt.algorithm";

    /**
     * @since 7.4
     */
    public static final String CRYPT_KEY = "server.crypt.secretkey";

    /**
     * @since 7.4
     */
    public static final String CRYPT_KEYALIAS = "server.crypt.keyalias";

    /**
     * @since 7.4
     */
    public static final String CRYPT_KEYSTORE_PATH = "server.crypt.keystore.path";

    /**
     * @since 7.4
     */
    public static final String CRYPT_KEYSTORE_PASS = "server.crypt.keystore.pass";

    /**
     * @since 7.4
     */
    public static final String JAVA_DEFAULT_KEYSTORE = "javax.net.ssl.keyStore";

    /**
     * @since 7.4
     */
    public static final String JAVA_DEFAULT_KEYSTORE_PASS = "javax.net.ssl.keyStorePassword";

    /**
     * Call to that constructor should be followed by a call to {@link #init()}. Depending on the available System
     * properties, you may want to also call {@link #loadProperties(Properties)} or {@link #setServerHome(File)} methods
     * before {@link #init()}; here is the recommended order:
     *
     * <pre>
     * Environment env = new Environment(home);
     * Environment.setDefault(env);
     * env.loadProperties(properties);
     * env.setServerHome(home);
     * env.init();
     * </pre>
     *
     * @param home Root path used for most defaults. It is recommended to make it match the server home rather than the
     *            runtime home.
     * @see #init()
     */
    public Environment(File home) {
        this(home, null);
    }

    /**
     * Call to that constructor should be followed by a call to {@link #init()}. Depending on the available System
     * properties, you may want to also call {@link #setServerHome(File)} method before {@link #init()}; here is the
     * recommended order:
     *
     * <pre>
     * Environment env = new Environment(home, properties);
     * Environment.setDefault(env);
     * env.setServerHome(home);
     * env.init();
     * </pre>
     *
     * @param home Root path used for most defaults. It is recommended to make it match the server home rather than the
     *            runtime home.
     * @param properties Source properties for initialization. It is used as an {@code Hashtable}: ie only the custom
     *            values are read, the properties default values are ignored if any.
     * @see #init()
     */
    public Environment(File home, Properties properties) {
        this.home = home.getAbsoluteFile();
        this.properties = new Properties();
        if (properties != null) {
            loadProperties(properties);
        }
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
                DEFAULT.init();
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
            setTemp(properties.getProperty(NUXEO_TMP_DIR, DEFAULT_TMP_DIR));
        }
        return temp;
    }

    /**
     * Resolve the path against {@link Environment#serverHome} if not absolute.
     *
     * @since 8.1
     */
    public void setTemp(String temp) {
        setTemp(getServerHome().toPath().resolve(temp).toFile());
    }

    public void setTemp(File temp) {
        this.temp = temp.getAbsoluteFile();
        setProperty(NUXEO_TMP_DIR, temp.getAbsolutePath());
        temp.mkdirs();
    }

    public File getConfig() {
        if (config == null) {
            setConfig(properties.getProperty(NUXEO_CONFIG_DIR, DEFAULT_CONFIG_DIR));
        }
        return config;
    }

    /**
     * Resolve the path against {@link Environment#runtimeHome} if not absolute.
     *
     * @since 8.1
     */
    public void setConfig(String config) {
        File configFile = getRuntimeHome().toPath().resolve(config).toFile();
        setConfig(configFile);
    }

    public void setConfig(File config) {
        this.config = config.getAbsoluteFile();
        setProperty(NUXEO_CONFIG_DIR, config.getAbsolutePath());
        config.mkdirs();
    }

    public File getLog() {
        if (log == null) {
            setLog(properties.getProperty(NUXEO_LOG_DIR, DEFAULT_LOG_DIR));
        }
        return log;
    }

    /**
     * Resolve the path against {@link Environment#serverHome} if not absolute.
     *
     * @since 8.1
     */
    public void setLog(String log) {
        setLog(getServerHome().toPath().resolve(log).toFile());
    }

    public void setLog(File log) {
        this.log = log.getAbsoluteFile();
        setProperty(NUXEO_LOG_DIR, log.getAbsolutePath());
        log.mkdirs();
    }

    public File getData() {
        if (data == null) {
            setData(properties.getProperty(NUXEO_DATA_DIR, DEFAULT_DATA_DIR));
        }
        return data;
    }

    /**
     * Resolve the path against {@link Environment#runtimeHome} if not absolute.
     *
     * @since 8.1
     */
    public void setData(String data) {
        setData(getRuntimeHome().toPath().resolve(data).toFile());
    }

    public void setData(File data) {
        this.data = data.getAbsoluteFile();
        setProperty(NUXEO_DATA_DIR, data.getAbsolutePath());
        data.mkdirs();
    }

    public File getWeb() {
        if (web == null) {
            setWeb(properties.getProperty(NUXEO_WEB_DIR, DEFAULT_WEB_DIR));
        }
        return web;
    }

    /**
     * Resolve the path against {@link Environment#runtimeHome} if not absolute.
     *
     * @since 8.1
     */
    public void setWeb(String web) {
        setWeb(getRuntimeHome().toPath().resolve(web).toFile());
    }

    public void setWeb(File web) {
        this.web = web;
        setProperty(NUXEO_WEB_DIR, web.getAbsolutePath());
    }

    /**
     * @since 5.4.2
     */
    public File getRuntimeHome() {
        initRuntimeHome();
        return runtimeHome;
    }

    /**
     * @since 5.4.2
     */
    public void setRuntimeHome(File runtimeHome) {
        this.runtimeHome = runtimeHome.getAbsoluteFile();
        setProperty(NUXEO_RUNTIME_HOME, runtimeHome.getAbsolutePath());
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

    /**
     * If setting a path property, consider using {@link #setPath(String, String)}
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
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
     * Initialization with System properties to avoid issues due to home set with runtime home instead of server home.
     * If {@link #NUXEO_HOME} System property is not set, or if you want to set a custom server home, then you should
     * call {@link #setServerHome(File)} before.
     *
     * @since 5.4.1
     */
    public void init() {
        initServerHome();
        initRuntimeHome();

        String dataDir = System.getProperty(NUXEO_DATA_DIR);
        if (StringUtils.isNotEmpty(dataDir)) {
            setData(new File(dataDir));
        }

        String configDir = System.getProperty(NUXEO_CONFIG_DIR);
        if (StringUtils.isNotEmpty(configDir)) {
            setConfig(new File(configDir));
        }

        String logDir = System.getProperty(NUXEO_LOG_DIR);
        if (StringUtils.isNotEmpty(logDir)) {
            setLog(new File(logDir));
        }

        String tmpDir = System.getProperty(NUXEO_TMP_DIR);
        if (StringUtils.isNotEmpty(tmpDir)) {
            setTemp(new File(tmpDir));
        }

        String mpDir = System.getProperty(NUXEO_MP_DIR);
        setPath(NUXEO_MP_DIR, StringUtils.isNotEmpty(mpDir) ? mpDir : DEFAULT_MP_DIR, getServerHome());
    }

    private void initRuntimeHome() {
        if (runtimeHome != null) {
            return;
        }
        String runtimeDir = System.getProperty(NUXEO_RUNTIME_HOME);
        if (runtimeDir != null && !runtimeDir.isEmpty()) {
            setRuntimeHome(new File(runtimeDir));
        } else {
            setRuntimeHome(home);
        }
    }

    /**
     * This method always returns the server home (or {@link #getHome()} if {@link #NUXEO_HOME_DIR} is not set).
     *
     * @since 5.4.2
     * @return Server home
     */
    public File getServerHome() {
        initServerHome();
        return serverHome;
    }

    /**
     * @since 5.4.2
     */
    public void setServerHome(File serverHome) {
        this.serverHome = serverHome.getAbsoluteFile();
        setProperty(NUXEO_HOME_DIR, serverHome.getAbsolutePath());
    }

    private void initServerHome() {
        if (serverHome != null) {
            return;
        }
        String homeDir = System.getProperty(NUXEO_HOME, System.getProperty(NUXEO_HOME_DIR));
        if (homeDir != null && !homeDir.isEmpty()) {
            setServerHome(new File(homeDir));
        } else {
            logger.warn(String.format("Could not set the server home from %s or %s system properties, will use %s",
                    NUXEO_HOME, NUXEO_HOME_DIR, home));
            setServerHome(home);
        }
        logger.debug(this);
    }

    public void setConfigurationProvider(Iterable<URL> configProvider) {
        this.configProvider = configProvider;
    }

    public Iterable<URL> getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Add a file path as a property
     *
     * @param key Property key
     * @param value Property value: an absolute or relative file
     * @param baseDir The directory against which the file will be resolved if not absolute
     * @since 8.1
     * @see #setProperty(String, String)
     * @see #setPath(String, String, File)
     * @see #setPath(String, File)
     */
    public void setPath(String key, File value, File baseDir) {
        setProperty(key, baseDir.toPath().resolve(value.toPath()).toFile().getAbsolutePath());
    }

    /**
     * Add a file path as a property
     *
     * @param key Property key
     * @param value Property value: an absolute or relative file path
     * @param baseDir The directory against which the file will be resolved if not absolute
     * @since 8.1
     * @see #setProperty(String, String)
     * @see #setPath(String, File, File)
     * @see #setPath(String, File)
     */
    public void setPath(String key, String value, File baseDir) {
        setProperty(key, baseDir.toPath().resolve(value).toFile().getAbsolutePath());
    }

    /**
     * Add a file path as a property
     *
     * @param key Property key
     * @param value Property value: an absolute or relative file; if relative, it will be resolved against {@link #home}
     * @since 8.1
     * @see #setProperty(String, String)
     * @see #setPath(String, File, File)
     */
    public void setPath(String key, File value) {
        setPath(key, value, home);
    }

    /**
     * Add a file path as a property
     *
     * @param key Property key
     * @param value Property value: an absolute or relative file path; if relative, it will be resolved against
     *            {@link #home}
     * @since 8.1
     * @see #setProperty(String, String)
     * @see #setPath(String, String, File)
     */
    public void setPath(String key, String value) {
        setPath(key, value, home);
    }

    /**
     * @return the file which path is associated with the given key. The file is guaranteed to be absolute if it has
     *         been set with {@link #setPath(String, File)}
     * @since 8.1
     */
    public File getPath(String key) {
        return getPath(key, null);
    }

    /**
     * @param key the property key
     * @param defaultValue the default path, absolute or relative to server home
     * @return the file which path is associated with the given key. The file is guaranteed to be absolute if it has
     *         been set with {@link #setPath(String, File)}
     * @since 8.1
     */
    public File getPath(String key, String defaultValue) {
        String path = properties.getProperty(key);
        if (path != null) {
            return new File(path);
        } else if (defaultValue != null) {
            return getServerHome().toPath().resolve(defaultValue).toFile();
        }
        return null;
    }
}
