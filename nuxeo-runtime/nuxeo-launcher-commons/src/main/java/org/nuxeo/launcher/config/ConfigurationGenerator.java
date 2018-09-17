/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.launcher.config;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringSubstitutor;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.launcher.commons.DatabaseDriverException;
import org.nuxeo.launcher.config.JVMVersion.UpTo;
import org.nuxeo.log4j.Log4JHelper;

import freemarker.core.ParseException;
import freemarker.template.TemplateException;

/**
 * Builder for server configuration and datasource files from templates and properties.
 *
 * @author jcarsique
 */
public class ConfigurationGenerator {

    /**
     * @since 6.0
     */
    public static final String TEMPLATE_SEPARATOR = ",";

    /**
     * Accurate but not used internally. NXP-18023: Java 8 update 40+ required
     *
     * @since 5.7
     */
    public static final String[] COMPLIANT_JAVA_VERSIONS = new String[] { "1.8.0_40", "11" };

    /**
     * @since 5.6
     */
    protected static final String CONFIGURATION_PROPERTIES = "configuration.properties";

    private static final Log log = LogFactory.getLog(ConfigurationGenerator.class);

    public static final String NUXEO_CONF = "nuxeo.conf";

    public static final String TEMPLATES = "templates";

    public static final String NUXEO_DEFAULT_CONF = "nuxeo.defaults";

    /**
     * Absolute or relative PATH to the user chosen templates (comma separated list)
     */
    public static final String PARAM_TEMPLATES_NAME = "nuxeo.templates";

    public static final String PARAM_TEMPLATE_DBNAME = "nuxeo.dbtemplate";

    /**
     * @since 9.3
     */
    public static final String PARAM_TEMPLATE_DBSECONDARY_NAME = "nuxeo.dbnosqltemplate";

    public static final String PARAM_TEMPLATE_DBTYPE = "nuxeo.db.type";

    /**
     * @since 9.3
     */
    public static final String PARAM_TEMPLATE_DBSECONDARY_TYPE = "nuxeo.dbsecondary.type";

    public static final String OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.templates.parsing.extensions";

    public static final String PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.plaintext_parsing_extensions";

    public static final String PARAM_TEMPLATES_FREEMARKER_EXTENSIONS = "nuxeo.freemarker_parsing_extensions";

    /**
     * Absolute or relative PATH to the included templates (comma separated list)
     */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    public static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String BOUNDARY_BEGIN = "### BEGIN - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final String BOUNDARY_END = "### END - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final List<String> DB_LIST = asList("default", "mongodb", "postgresql", "oracle", "mysql", "mariadb",
            "mssql", "db2");

    public static final List<String> DB_SECONDARY_LIST = asList("none", "marklogic");

    public static final List<String> DB_EXCLUDE_CHECK_LIST = asList("default", "none", "mongodb");

    public static final String PARAM_WIZARD_DONE = "nuxeo.wizard.done";

    public static final String PARAM_WIZARD_RESTART_PARAMS = "wizard.restart.params";

    public static final String PARAM_FAKE_WINDOWS = "org.nuxeo.fake.vindoz";

    public static final String PARAM_LOOPBACK_URL = "nuxeo.loopback.url";

    public static final int MIN_PORT = 1;

    public static final int MAX_PORT = 65535;

    public static final int ADDRESS_PING_TIMEOUT = 1000;

    public static final String PARAM_BIND_ADDRESS = "nuxeo.bind.address";

    public static final String PARAM_HTTP_PORT = "nuxeo.server.http.port";

    /**
     * @deprecated Since 7.4. Use {@link Environment#SERVER_STATUS_KEY} instead
     */
    @Deprecated
    public static final String PARAM_STATUS_KEY = Environment.SERVER_STATUS_KEY;

    public static final String PARAM_CONTEXT_PATH = "org.nuxeo.ecm.contextPath";

    public static final String PARAM_MP_DIR = "nuxeo.distribution.marketplace.dir";

    public static final String DISTRIBUTION_MP_DIR = "setupWizardDownloads";

    public static final String INSTALL_AFTER_RESTART = "installAfterRestart.log";

    public static final String PARAM_DB_DRIVER = "nuxeo.db.driver";

    public static final String PARAM_DB_JDBC_URL = "nuxeo.db.jdbc.url";

    public static final String PARAM_DB_HOST = "nuxeo.db.host";

    public static final String PARAM_DB_PORT = "nuxeo.db.port";

    public static final String PARAM_DB_NAME = "nuxeo.db.name";

    public static final String PARAM_DB_USER = "nuxeo.db.user";

    public static final String PARAM_DB_PWD = "nuxeo.db.password";

    /**
     * @since 8.1
     */
    public static final String PARAM_MONGODB_NAME = "nuxeo.mongodb.dbname";

    /**
     * @since 8.1
     */
    public static final String PARAM_MONGODB_SERVER = "nuxeo.mongodb.server";

    /**
     * Catch values like ${env:PARAM_KEY:defaultValue}
     *
     * @since 9.1
     */
    private static final Pattern ENV_VALUE_PATTERN = Pattern.compile(
            "\\$\\{env(?<boolean>\\?\\?)?:(?<envparam>\\w*)(:?(?<defaultvalue>.*?)?)?\\}");

    /**
     * Java options split by spaces followed by an even number of quotes (or zero).
     *
     * @since 9.3
     */
    protected static final Pattern JAVA_OPTS_PATTERN = Pattern.compile("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    /**
     * Keys which value must be displayed thoughtfully
     *
     * @since 8.1
     */
    public static final List<String> SECRET_KEYS = asList(PARAM_DB_PWD, "mailservice.password",
            "mail.transport.password", "nuxeo.http.proxy.password", "nuxeo.ldap.bindpassword",
            "nuxeo.user.emergency.password");

    /**
     * @deprecated Since 7.10. Use {@link Environment#PRODUCT_NAME}
     */
    @Deprecated
    public static final String PARAM_PRODUCT_NAME = Environment.PRODUCT_NAME;

    /**
     * @deprecated Since 7.10. Use {@link Environment#PRODUCT_VERSION}
     */
    @Deprecated
    public static final String PARAM_PRODUCT_VERSION = Environment.PRODUCT_VERSION;

    /**
     * @since 5.6
     */
    public static final String PARAM_NUXEO_URL = "nuxeo.url";

    /**
     * Global dev property, duplicated from runtime framework
     *
     * @since 5.6
     */
    public static final String NUXEO_DEV_SYSTEM_PROP = "org.nuxeo.dev";

    /**
     * Seam hot reload property, also controlled by {@link #NUXEO_DEV_SYSTEM_PROP}
     *
     * @since 5.6
     */
    public static final String SEAM_DEBUG_SYSTEM_PROP = "org.nuxeo.seam.debug";

    /** @since 8.4 */
    public static final String JVMCHECK_PROP = "jvmcheck";

    /** @since 8.4 */
    public static final String JVMCHECK_FAIL = "fail";

    /** @since 8.4 */
    public static final String JVMCHECK_NOFAIL = "nofail";

    /**
     * Java options configured in <tt>bin/nuxeo.conf</tt> and <tt>bin/nuxeoctl</tt>.
     *
     * @since 9.3
     */
    public static final String JAVA_OPTS_PROP = "launcher.java.opts";

    private final File nuxeoHome;

    // User configuration file
    private final File nuxeoConf;

    // Chosen templates
    private final List<File> includedTemplates = new ArrayList<>();

    // Common default configuration file
    private File nuxeoDefaultConf;

    public boolean isJetty;

    public boolean isTomcat;

    private ServerConfigurator serverConfigurator;

    private BackingServiceConfigurator backingServicesConfigurator;

    private boolean forceGeneration;

    private Properties defaultConfig;

    private CryptoProperties userConfig;

    private boolean configurable = false;

    private boolean onceGeneration = false;

    private String templates;

    // if PARAM_FORCE_GENERATION=once, set to false; else keep current value
    private boolean setOnceToFalse = true;

    // if PARAM_FORCE_GENERATION=false, set to once; else keep the current
    // value
    private boolean setFalseToOnce = false;

    public boolean isConfigurable() {
        return configurable;
    }

    public ConfigurationGenerator() {
        this(true, false);
    }

    private boolean quiet = false;

    private static boolean hideDeprecationWarnings = false;

    private Environment env;

    private Properties storedConfig;

    private String currentConfigurationDigest;

    /**
     * @since 5.7
     */
    protected Properties getStoredConfig() {
        if (storedConfig == null) {
            updateStoredConfig();
        }
        return storedConfig;
    }

    protected static final Map<String, String> parametersMigration = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        {
            put(OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS, PARAM_TEMPLATES_PARSING_EXTENSIONS);
            put("nuxeo.db.user.separator.key", "nuxeo.db.user_separator_key");
            put("mail.pop3.host", "mail.store.host");
            put("mail.pop3.port", "mail.store.port");
            put("mail.smtp.host", "mail.transport.host");
            put("mail.smtp.port", "mail.transport.port");
            put("mail.smtp.username", "mail.transport.username");
            put("mail.transport.username", "mail.transport.user");
            put("mail.smtp.password", "mail.transport.password");
            put("mail.smtp.usetls", "mail.transport.usetls");
            put("mail.smtp.auth", "mail.transport.auth");
        }
    };

    /**
     * @param quiet Suppress info level messages from the console output
     * @param debug Activate debug level logging
     * @since 5.6
     */
    public ConfigurationGenerator(boolean quiet, boolean debug) {
        this.quiet = quiet;
        File serverHome = Environment.getDefault().getServerHome();
        if (serverHome != null) {
            nuxeoHome = serverHome.getAbsoluteFile();
        } else {
            File userDir = new File(System.getProperty("user.dir"));
            if ("bin".equalsIgnoreCase(userDir.getName())) {
                nuxeoHome = userDir.getParentFile().getAbsoluteFile();
            } else {
                nuxeoHome = userDir.getAbsoluteFile();
            }
        }
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoConfPath != null) {
            nuxeoConf = new File(nuxeoConfPath).getAbsoluteFile();
        } else {
            nuxeoConf = new File(nuxeoHome, "bin" + File.separator + "nuxeo.conf").getAbsoluteFile();
        }
        System.setProperty(NUXEO_CONF, nuxeoConf.getPath());

        nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES + File.separator + NUXEO_DEFAULT_CONF);

        // detect server type based on System properties
        isJetty = System.getProperty(JettyConfigurator.JETTY_HOME) != null;
        isTomcat = System.getProperty(TomcatConfigurator.TOMCAT_HOME) != null;
        if (!isJetty && !isTomcat) {
            // fallback on jar detection
            isTomcat = new File(nuxeoHome, "bin/bootstrap.jar").exists();
            String[] files = nuxeoHome.list();
            for (String file : files) {
                if (file.startsWith("nuxeo-runtime-launcher")) {
                    isJetty = true;
                    break;
                }
            }
        }
        if (isTomcat) {
            serverConfigurator = new TomcatConfigurator(this);
        } else if (isJetty) {
            serverConfigurator = new JettyConfigurator(this);
        } else {
            serverConfigurator = new UnknownServerConfigurator(this);
        }
        if (Logger.getRootLogger().getAllAppenders() instanceof NullEnumeration) {
            serverConfigurator.initLogs();
        }
        backingServicesConfigurator = new BackingServiceConfigurator(this);
        String homeInfo = "Nuxeo home:          " + nuxeoHome.getPath();
        String confInfo = "Nuxeo configuration: " + nuxeoConf.getPath();
        if (quiet) {
            log.debug(homeInfo);
            log.debug(confInfo);
        } else {
            log.info(homeInfo);
            log.info(confInfo);
        }
    }

    public void hideDeprecationWarnings(boolean hide) {
        hideDeprecationWarnings = hide;
    }

    /**
     * @see #PARAM_FORCE_GENERATION
     */
    public void setForceGeneration(boolean forceGeneration) {
        this.forceGeneration = forceGeneration;
    }

    /**
     * @see #PARAM_FORCE_GENERATION
     * @return true if configuration will be generated from templates
     * @since 5.4.2
     */
    public boolean isForceGeneration() {
        return forceGeneration;
    }

    public CryptoProperties getUserConfig() {
        return userConfig;
    }

    /**
     * @since 5.4.2
     */
    public final ServerConfigurator getServerConfigurator() {
        return serverConfigurator;
    }

    /**
     * Runs the configuration files generation.
     */
    public void run() throws ConfigurationException {
        if (init()) {
            if (!serverConfigurator.isConfigured()) {
                log.info("No current configuration, generating files...");
                generateFiles();
            } else if (forceGeneration) {
                log.info("Configuration files generation (nuxeo.force.generation="
                        + userConfig.getProperty(PARAM_FORCE_GENERATION) + ")...");
                generateFiles();
            } else {
                log.info(
                        "Server already configured (set nuxeo.force.generation=true to force configuration files generation).");
            }
        }
    }

    /**
     * Initialize configurator, check requirements and load current configuration
     *
     * @return returns true if current install is configurable, else returns false
     */
    public boolean init() {
        return init(false);
    }

    /**
     * Initialize configurator, check requirements and load current configuration
     *
     * @since 5.6
     * @param forceReload If true, forces configuration reload.
     * @return returns true if current install is configurable, else returns false
     */
    public boolean init(boolean forceReload) {
        if (serverConfigurator instanceof UnknownServerConfigurator) {
            configurable = false;
            forceGeneration = false;
            log.warn("Server will be considered as not configurable.");
        }
        if (!nuxeoConf.exists()) {
            log.info("Missing " + nuxeoConf);
            configurable = false;
            userConfig = new CryptoProperties();
            defaultConfig = new Properties();
        } else if (userConfig == null || userConfig.size() == 0 || forceReload) {
            try {
                if (forceReload) {
                    // force 'templates' reload
                    templates = null;
                }
                setBasicConfiguration();
                configurable = true;
            } catch (ConfigurationException e) {
                log.warn("Error reading basic configuration.", e);
                configurable = false;
            }
        } else {
            configurable = true;
        }
        return configurable;
    }

    /**
     * @return Old templates
     */
    public String changeTemplates(String newTemplates) {
        String oldTemplates = templates;
        templates = newTemplates;
        try {
            setBasicConfiguration(false);
            configurable = true;
        } catch (ConfigurationException e) {
            log.warn("Error reading basic configuration.", e);
            configurable = false;
        }
        return oldTemplates;
    }

    /**
     * Change templates using given database template
     *
     * @param dbTemplate new database template
     * @since 5.4.2
     */
    public void changeDBTemplate(String dbTemplate) {
        changeTemplates(rebuildTemplatesStr(dbTemplate));
    }

    private void setBasicConfiguration() throws ConfigurationException {
        setBasicConfiguration(true);
    }

    private void setBasicConfiguration(boolean save) throws ConfigurationException {
        try {
            // Load default configuration
            defaultConfig = loadTrimmedProperties(nuxeoDefaultConf);
            // Add System properties
            defaultConfig.putAll(System.getProperties());
            userConfig = new CryptoProperties(defaultConfig);

            // If Windows, replace backslashes in paths in nuxeo.conf
            if (SystemUtils.IS_OS_WINDOWS) {
                replaceBackslashes();
            }
            // Load user configuration
            userConfig.putAll(loadTrimmedProperties(nuxeoConf));
            onceGeneration = "once".equals(userConfig.getProperty(PARAM_FORCE_GENERATION));
            forceGeneration = onceGeneration
                    || Boolean.parseBoolean(userConfig.getProperty(PARAM_FORCE_GENERATION, "false"));
            checkForDeprecatedParameters(userConfig);

            // Synchronize directories between serverConfigurator and
            // userConfig/defaultConfig
            setDirectoryWithProperty(org.nuxeo.common.Environment.NUXEO_DATA_DIR);
            setDirectoryWithProperty(org.nuxeo.common.Environment.NUXEO_LOG_DIR);
            setDirectoryWithProperty(org.nuxeo.common.Environment.NUXEO_PID_DIR);
            setDirectoryWithProperty(org.nuxeo.common.Environment.NUXEO_TMP_DIR);
            setDirectoryWithProperty(org.nuxeo.common.Environment.NUXEO_MP_DIR);
        } catch (NullPointerException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file: " + nuxeoDefaultConf + " or " + nuxeoConf, e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }

        // Override default configuration with specific configuration(s) of
        // the chosen template(s) which can be outside of server filesystem
        try {
            includeTemplates();
            checkForDeprecatedParameters(defaultConfig);
            extractDatabaseTemplateName();
            extractSecondaryDatabaseTemplateName();
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }

        Map<String, String> newParametersToSave = evalDynamicProperties();
        if (save && newParametersToSave != null && !newParametersToSave.isEmpty()) {
            saveConfiguration(newParametersToSave, false, false);
        }

        logDebugInformation();
    }

    /**
     * @since 5.7
     */
    protected void includeTemplates() throws IOException {
        includedTemplates.clear();
        List<File> orderedTemplates = includeTemplates(getUserTemplates());
        includedTemplates.clear();
        includedTemplates.addAll(orderedTemplates);
        log.debug(includedTemplates);
    }

    private void logDebugInformation() {
        String debugPropValue = userConfig.getProperty(NUXEO_DEV_SYSTEM_PROP);
        if (Boolean.parseBoolean(debugPropValue)) {
            log.debug("Nuxeo Dev mode enabled");
        } else {
            log.debug("Nuxeo Dev mode is not enabled");
        }

        // XXX: cannot init seam debug mode when global debug mode is set, as
        // it needs to be activated at startup, and requires the seam-debug jar
        // to be in the classpath anyway
        String seamDebugPropValue = userConfig.getProperty(SEAM_DEBUG_SYSTEM_PROP);
        if (Boolean.parseBoolean(seamDebugPropValue)) {
            log.debug("Nuxeo Seam HotReload is enabled");
        } else {
            log.debug("Nuxeo Seam HotReload is not enabled");
        }
    }

    /**
     * Generate properties which values are based on others
     *
     * @return Map with new parameters to save in {@code nuxeoConf}
     * @since 5.5
     */
    protected HashMap<String, String> evalDynamicProperties() throws ConfigurationException {
        HashMap<String, String> newParametersToSave = new HashMap<>();
        evalEnvironmentVariables(newParametersToSave);
        evalLoopbackURL();
        evalServerStatusKey(newParametersToSave);
        return newParametersToSave;
    }

    /**
     * Expand environment variable for properties values of the form ${env:MY_VAR}.
     *
     * @since 9.1
     */
    protected void evalEnvironmentVariables(Map<String, String> newParametersToSave) {
        for (Object keyObject : userConfig.keySet()) {
            String key = (String) keyObject;
            String value = userConfig.getProperty(key);

            if (StringUtils.isNotBlank(value)) {
                String newValue = replaceEnvironmentVariables(value);
                if (!value.equals(newValue)) {
                    newParametersToSave.put(key, newValue);
                }
            }
        }
    }

    private String replaceEnvironmentVariables(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        Matcher matcher = ENV_VALUE_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            boolean booleanValue = "??".equals(matcher.group("boolean"));
            String envVarName = matcher.group("envparam");
            String defaultValue = matcher.group("defaultvalue");

            String envValue = getEnvironmentVariableValue(envVarName);

            String result;
            if (booleanValue) {
                result = StringUtils.isBlank(envValue) ? "false" : "true";
            } else {
                result = StringUtils.isBlank(envValue) ? defaultValue : envValue;
            }
            matcher.appendReplacement(sb, result);
        }
        matcher.appendTail(sb);

        return sb.toString();

    }

    /**
     * Generate a server status key if not already set
     *
     * @see Environment#SERVER_STATUS_KEY
     * @since 5.5
     */
    private void evalServerStatusKey(Map<String, String> newParametersToSave) {
        if (userConfig.getProperty(Environment.SERVER_STATUS_KEY) == null) {
            newParametersToSave.put(Environment.SERVER_STATUS_KEY, UUID.randomUUID().toString().substring(0, 8));
        }
    }

    private void evalLoopbackURL() throws ConfigurationException {
        String loopbackURL = userConfig.getProperty(PARAM_LOOPBACK_URL);
        if (loopbackURL != null) {
            log.debug("Using configured loop back url: " + loopbackURL);
            return;
        }
        InetAddress bindAddress = getBindAddress();
        // Address and ports already checked by #checkAddressesAndPorts
        try {
            if (bindAddress.isAnyLocalAddress()) {
                boolean preferIPv6 = "false".equals(System.getProperty("java.net.preferIPv4Stack"))
                        && "true".equals(System.getProperty("java.net.preferIPv6Addresses"));
                bindAddress = preferIPv6 ? InetAddress.getByName("::1") : InetAddress.getByName("127.0.0.1");
                log.debug("Bind address is \"ANY\", using local address instead: " + bindAddress);
            }
        } catch (UnknownHostException e) {
            log.debug(e, e);
            log.error(e.getMessage());
        }

        String httpPort = userConfig.getProperty(PARAM_HTTP_PORT);
        String contextPath = userConfig.getProperty(PARAM_CONTEXT_PATH);
        // Is IPv6 or IPv4 ?
        if (bindAddress instanceof Inet6Address) {
            loopbackURL = "http://[" + bindAddress.getHostAddress() + "]:" + httpPort + contextPath;
        } else {
            loopbackURL = "http://" + bindAddress.getHostAddress() + ":" + httpPort + contextPath;
        }
        log.debug("Set as loop back URL: " + loopbackURL);
        defaultConfig.setProperty(PARAM_LOOPBACK_URL, loopbackURL);
    }

    /**
     * Read nuxeo.conf, replace backslashes in paths and write new nuxeo.conf
     *
     * @throws ConfigurationException if any error reading or writing nuxeo.conf
     * @since 5.4.1
     */
    protected void replaceBackslashes() throws ConfigurationException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(nuxeoConf))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches(".*:\\\\.*")) {
                    line = line.replaceAll("\\\\", "/");
                }
                sb.append(line).append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }
        try (FileWriter writer = new FileWriter(nuxeoConf, false)) {
            // Copy back file content
            writer.append(sb.toString());
        } catch (IOException e) {
            throw new ConfigurationException("Error writing in " + nuxeoConf, e);
        }
    }

    /**
     * @since 5.4.2
     * @param key Directory system key
     * @see Environment
     */
    public void setDirectoryWithProperty(String key) {
        String directory = userConfig.getProperty(key);
        if (directory == null) {
            defaultConfig.setProperty(key, serverConfigurator.getDirectory(key).getPath());
        } else {
            serverConfigurator.setDirectory(key, directory);
        }
    }

    public String getUserTemplates() {
        if (templates == null) {
            templates = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        }
        if (templates == null) {
            log.warn("No template found in configuration! Fallback on 'default'.");
            templates = "default";
        }
        templates = replaceEnvironmentVariables(templates);
        userConfig.setProperty(PARAM_TEMPLATES_NAME, templates);
        return templates;
    }

    protected void generateFiles() throws ConfigurationException {
        try {
            serverConfigurator.parseAndCopy(userConfig);
            serverConfigurator.dumpProperties(userConfig);
            log.info("Configuration files generated.");
            // keep true or false, switch once to false
            if (onceGeneration) {
                setOnceToFalse = true;
                writeConfiguration();
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file: " + e.getMessage(), e);
        } catch (TemplateException | ParseException e) {
            throw new ConfigurationException("Could not process FreeMarker template: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure: " + e.getMessage(), e);
        }
    }

    private List<File> includeTemplates(String templatesList) throws IOException {
        List<File> orderedTemplates = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(templatesList, TEMPLATE_SEPARATOR);
        while (st.hasMoreTokens()) {
            String nextToken = replaceEnvironmentVariables(st.nextToken());
            File chosenTemplate = new File(nextToken);
            // is it absolute and existing or relative path ?
            if (!chosenTemplate.exists() || !chosenTemplate.getPath().equals(chosenTemplate.getAbsolutePath())) {
                chosenTemplate = new File(nuxeoDefaultConf.getParentFile(), nextToken);
            }
            if (includedTemplates.contains(chosenTemplate)) {
                log.debug("Already included " + nextToken);
                continue;
            }
            if (!chosenTemplate.exists()) {
                log.error(String.format(
                        "Template '%s' not found with relative or absolute path (%s). "
                                + "Check your %s parameter, and %s for included files.",
                        nextToken, chosenTemplate, PARAM_TEMPLATES_NAME, PARAM_INCLUDED_TEMPLATES));
                continue;
            }
            File chosenTemplateConf = new File(chosenTemplate, NUXEO_DEFAULT_CONF);
            includedTemplates.add(chosenTemplate);
            if (!chosenTemplateConf.exists()) {
                log.warn("Ignore template (no default configuration): " + nextToken);
                continue;
            }

            Properties subTemplateConf = loadTrimmedProperties(chosenTemplateConf);
            String subTemplatesList = replaceEnvironmentVariables(
                    subTemplateConf.getProperty(PARAM_INCLUDED_TEMPLATES));
            if (subTemplatesList != null && subTemplatesList.length() > 0) {
                orderedTemplates.addAll(includeTemplates(subTemplatesList));
            }
            // Load configuration from chosen templates
            defaultConfig.putAll(subTemplateConf);
            orderedTemplates.add(chosenTemplate);
            String templateInfo = "Include template: " + chosenTemplate.getPath();
            if (quiet) {
                log.debug(templateInfo);
            } else {
                log.info(templateInfo);
            }
        }
        return orderedTemplates;
    }

    /**
     * Check for deprecated parameters
     *
     * @since 5.6
     */
    protected void checkForDeprecatedParameters(Properties properties) {
        serverConfigurator.addServerSpecificParameters(parametersMigration);
        @SuppressWarnings("rawtypes")
        Enumeration userEnum = properties.propertyNames();
        while (userEnum.hasMoreElements()) {
            String key = (String) userEnum.nextElement();
            if (parametersMigration.containsKey(key)) {
                String value = properties.getProperty(key);
                properties.setProperty(parametersMigration.get(key), value);
                // Don't remove the deprecated key yet - more
                // warnings but old things should keep working
                // properties.remove(key);
                if (!hideDeprecationWarnings) {
                    log.warn("Parameter " + key + " is deprecated - please use " + parametersMigration.get(key)
                            + " instead");
                }
            }
        }
    }

    public File getNuxeoHome() {
        return nuxeoHome;
    }

    public File getNuxeoDefaultConf() {
        return nuxeoDefaultConf;
    }

    public List<File> getIncludedTemplates() {
        return includedTemplates;
    }

    /**
     * Save changed parameters in {@code nuxeo.conf}. This method does not check values in map. Use
     * {@link #saveFilteredConfiguration(Map)} for parameters filtering.
     *
     * @param changedParameters Map of modified parameters
     * @see #saveFilteredConfiguration(Map)
     */
    public void saveConfiguration(Map<String, String> changedParameters) throws ConfigurationException {
        // Keep generation true or once; switch false to once
        saveConfiguration(changedParameters, false, true);
    }

    /**
     * Save changed parameters in {@code nuxeo.conf} calculating templates if changedParameters contains a value for
     * {@link #PARAM_TEMPLATE_DBNAME}. If a parameter value is empty ("" or null), then the property is unset.
     * {@link #PARAM_WIZARD_DONE}, {@link #PARAM_TEMPLATES_NAME} and {@link #PARAM_FORCE_GENERATION} cannot be unset,
     * but their value can be changed.<br/>
     * This method does not check values in map: use {@link #saveFilteredConfiguration(Map)} for parameters filtering.
     *
     * @param changedParameters Map of modified parameters
     * @param setGenerationOnceToFalse If generation was on (true or once), then set it to false or not?
     * @param setGenerationFalseToOnce If generation was off (false), then set it to once?
     * @see #saveFilteredConfiguration(Map)
     * @since 5.5
     */
    public void saveConfiguration(Map<String, String> changedParameters, boolean setGenerationOnceToFalse,
            boolean setGenerationFalseToOnce) throws ConfigurationException {
        setOnceToFalse = setGenerationOnceToFalse;
        setFalseToOnce = setGenerationFalseToOnce;
        updateStoredConfig();
        String newDbTemplate = changedParameters.remove(PARAM_TEMPLATE_DBNAME);
        if (newDbTemplate != null) {
            changedParameters.put(PARAM_TEMPLATES_NAME, rebuildTemplatesStr(newDbTemplate));
        }
        newDbTemplate = changedParameters.remove(PARAM_TEMPLATE_DBSECONDARY_NAME);
        if (newDbTemplate != null) {
            changedParameters.put(PARAM_TEMPLATES_NAME, rebuildTemplatesStr(newDbTemplate));
        }
        if (changedParameters.containsValue(null) || changedParameters.containsValue("")) {
            // There are properties to unset
            Set<String> propertiesToUnset = new HashSet<>();
            for (Entry<String, String> entry : changedParameters.entrySet()) {
                if (StringUtils.isEmpty(entry.getValue())) {
                    propertiesToUnset.add(entry.getKey());
                }
            }
            for (String key : propertiesToUnset) {
                changedParameters.remove(key);
                userConfig.remove(key);
            }
        }
        userConfig.putAll(changedParameters);
        writeConfiguration();
        updateStoredConfig();
    }

    private void updateStoredConfig() {
        if (storedConfig == null) {
            storedConfig = new Properties(defaultConfig);
        } else {
            storedConfig.clear();
        }
        storedConfig.putAll(userConfig);
    }

    /**
     * Save changed parameters in {@code nuxeo.conf}, filtering parameters with {@link #getChangedParameters(Map)}
     *
     * @param changedParameters Maps of modified parameters
     * @since 5.4.2
     * @see #saveConfiguration(Map)
     * @see #getChangedParameters(Map)
     */
    public void saveFilteredConfiguration(Map<String, String> changedParameters) throws ConfigurationException {
        Map<String, String> filteredParameters = getChangedParameters(changedParameters);
        saveConfiguration(filteredParameters);
    }

    /**
     * Filters given parameters including them only if (there was no previous value and new value is not empty/null) or
     * (there was a previous value and it differs from the new value)
     *
     * @param changedParameters parameters to be filtered
     * @return filtered map
     * @since 5.4.2
     */
    public Map<String, String> getChangedParameters(Map<String, String> changedParameters) {
        Map<String, String> filteredChangedParameters = new HashMap<>();
        for (String key : changedParameters.keySet()) {
            String oldParam = getStoredConfig().getProperty(key);
            String newParam = changedParameters.get(key);
            if (newParam != null) {
                newParam = newParam.trim();
            }
            if (oldParam == null && StringUtils.isNotEmpty(newParam)
                    || oldParam != null && !oldParam.trim().equals(newParam)) {
                filteredChangedParameters.put(key, newParam);
            }
        }
        return filteredChangedParameters;
    }

    private void writeConfiguration() throws ConfigurationException {
        final MessageDigest newContentDigest = DigestUtils.getMd5Digest();
        StringWriter newContent = new StringWriter() {
            @Override
            public void write(String str) {
                if (str != null) {
                    newContentDigest.update(str.getBytes());
                }
                super.write(str);
            }
        };
        // Copy back file content
        newContent.append(readConfiguration());
        // Write changed parameters
        newContent.write(BOUNDARY_BEGIN + System.getProperty("line.separator"));
        for (Object o : new TreeSet<>(userConfig.keySet())) {
            String key = (String) o;
            // Ignore parameters already stored in newContent
            if (PARAM_FORCE_GENERATION.equals(key) || PARAM_WIZARD_DONE.equals(key)
                    || PARAM_TEMPLATES_NAME.equals(key)) {
                continue;
            }
            String oldValue = storedConfig.getProperty(key, "");
            String newValue = userConfig.getRawProperty(key, "");
            if (!newValue.equals(oldValue)) {
                newContent.write("#" + key + "=" + oldValue + System.getProperty("line.separator"));
                newContent.write(key + "=" + newValue + System.getProperty("line.separator"));
            }
        }
        newContent.write(BOUNDARY_END + System.getProperty("line.separator"));

        // Write file only if content has changed
        if (!Hex.encodeHexString(newContentDigest.digest()).equals(currentConfigurationDigest)) {
            try (Writer writer = new FileWriter(nuxeoConf, false)) {
                writer.append(newContent.getBuffer());
            } catch (IOException e) {
                throw new ConfigurationException("Error writing in " + nuxeoConf, e);
            }
        }
    }

    private StringBuffer readConfiguration() throws ConfigurationException {
        // Will change wizardParam value instead of appending it
        String wizardParam = userConfig.getProperty(PARAM_WIZARD_DONE);

        // Will change templatesParam value instead of appending it
        String templatesParam = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        Integer generationIndex = null, wizardIndex = null, templatesIndex = null;
        List<String> newLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(nuxeoConf))) {
            String line;
            MessageDigest digest = DigestUtils.getMd5Digest();
            boolean onConfiguratorContent = false;
            while ((line = reader.readLine()) != null) {
                digest.update(line.getBytes());
                if (!onConfiguratorContent) {
                    if (!line.startsWith(BOUNDARY_BEGIN)) {
                        if (line.startsWith(PARAM_FORCE_GENERATION)) {
                            if (setOnceToFalse && onceGeneration) {
                                line = PARAM_FORCE_GENERATION + "=false";
                            }
                            if (setFalseToOnce && !forceGeneration) {
                                line = PARAM_FORCE_GENERATION + "=once";
                            }
                            if (generationIndex == null) {
                                newLines.add(line);
                                generationIndex = newLines.size() - 1;
                            } else {
                                newLines.set(generationIndex, line);
                            }
                        } else if (line.startsWith(PARAM_WIZARD_DONE)) {
                            if (wizardParam != null) {
                                line = PARAM_WIZARD_DONE + "=" + wizardParam;
                            }
                            if (wizardIndex == null) {
                                newLines.add(line);
                                wizardIndex = newLines.size() - 1;
                            } else {
                                newLines.set(wizardIndex, line);
                            }
                        } else if (line.startsWith(PARAM_TEMPLATES_NAME)) {
                            if (templatesParam != null) {
                                line = PARAM_TEMPLATES_NAME + "=" + templatesParam;
                            }
                            if (templatesIndex == null) {
                                newLines.add(line);
                                templatesIndex = newLines.size() - 1;
                            } else {
                                newLines.set(templatesIndex, line);
                            }
                        } else {
                            int equalIdx = line.indexOf("=");
                            if (equalIdx < 1 || line.trim().startsWith("#")) {
                                newLines.add(line);
                            } else {
                                String key = line.substring(0, equalIdx).trim();
                                if (userConfig.getProperty(key) != null) {
                                    newLines.add(line);
                                } else {
                                    newLines.add("#" + line);
                                }
                            }
                        }
                    } else {
                        // What must be written just before the BOUNDARY_BEGIN
                        if (templatesIndex == null && templatesParam != null) {
                            newLines.add(PARAM_TEMPLATES_NAME + "=" + templatesParam);
                            templatesIndex = newLines.size() - 1;
                        }
                        if (wizardIndex == null && wizardParam != null) {
                            newLines.add(PARAM_WIZARD_DONE + "=" + wizardParam);
                            wizardIndex = newLines.size() - 1;
                        }
                        onConfiguratorContent = true;
                    }
                } else {
                    if (!line.startsWith(BOUNDARY_END)) {
                        int equalIdx = line.indexOf("=");
                        if (line.startsWith("#" + PARAM_TEMPLATES_NAME) || line.startsWith(PARAM_TEMPLATES_NAME)) {
                            // Backward compliance, it must be ignored
                            continue;
                        }
                        if (equalIdx < 1) { // Ignore non-readable lines
                            continue;
                        }
                        if (line.trim().startsWith("#")) {
                            String key = line.substring(1, equalIdx).trim();
                            String value = line.substring(equalIdx + 1).trim();
                            getStoredConfig().setProperty(key, value);
                        } else {
                            String key = line.substring(0, equalIdx).trim();
                            String value = line.substring(equalIdx + 1).trim();
                            if (!value.equals(userConfig.getRawProperty(key))) {
                                getStoredConfig().setProperty(key, value);
                            }
                        }
                    } else {
                        onConfiguratorContent = false;
                    }
                }
            }
            reader.close();
            currentConfigurationDigest = Hex.encodeHexString(digest.digest());
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }
        StringBuffer newContent = new StringBuffer();
        for (String newLine : newLines) {
            newContent.append(newLine.trim()).append(System.getProperty("line.separator"));
        }
        return newContent;
    }

    /**
     * Extract a database template from the current list of templates. Return the last one if there are multiples.
     *
     * @see #rebuildTemplatesStr(String)
     */
    public String extractDatabaseTemplateName() {
        return extractDbTemplateName(DB_LIST, PARAM_TEMPLATE_DBTYPE, PARAM_TEMPLATE_DBNAME, "unknown");
    }

    /**
     * Extract a NoSQL database template from the current list of templates. Return the last one if there are multiples.
     *
     * @see #rebuildTemplatesStr(String)
     * @since 8.1
     */
    public String extractSecondaryDatabaseTemplateName() {
        return extractDbTemplateName(DB_SECONDARY_LIST, PARAM_TEMPLATE_DBSECONDARY_TYPE,
                PARAM_TEMPLATE_DBSECONDARY_NAME, null);
    }

    private String extractDbTemplateName(List<String> knownDbList, String paramTemplateDbType,
            String paramTemplateDbName, String defaultTemplate) {
        String dbTemplate = defaultTemplate;
        boolean found = false;
        for (File templateFile : includedTemplates) {
            String template = templateFile.getName();
            if (knownDbList.contains(template)) {
                dbTemplate = template;
                found = true;
            }
        }
        String dbType = userConfig.getProperty(paramTemplateDbType);
        if (!found && dbType != null) {
            log.warn(String.format("Didn't find a known database template in the list but "
                    + "some template contributed a value for %s.", paramTemplateDbType));
            dbTemplate = dbType;
        }
        if (dbTemplate != null && !dbTemplate.equals(dbType)) {
            if (dbType == null) {
                log.warn(String.format("Missing value for %s, using %s", paramTemplateDbType, dbTemplate));
                userConfig.setProperty(paramTemplateDbType, dbTemplate);
            } else {
                log.debug(String.format("Different values between %s (%s) and %s (%s)", paramTemplateDbName, dbTemplate,
                        paramTemplateDbType, dbType));
            }
        }
        if (dbTemplate == null) {
            defaultConfig.remove(paramTemplateDbName);
        } else {
            defaultConfig.setProperty(paramTemplateDbName, dbTemplate);
        }
        return dbTemplate;
    }

    /**
     * @return nuxeo.conf file used
     */
    public File getNuxeoConf() {
        return nuxeoConf;
    }

    /**
     * Delegate logs initialization to serverConfigurator instance
     *
     * @since 5.4.2
     */
    public void initLogs() {
        serverConfigurator.initLogs();
    }

    /**
     * @return log directory
     * @since 5.4.2
     */
    public File getLogDir() {
        return serverConfigurator.getLogDir();
    }

    /**
     * @return pid directory
     * @since 5.4.2
     */
    public File getPidDir() {
        return serverConfigurator.getPidDir();
    }

    /**
     * @return Data directory
     * @since 5.4.2
     */
    public File getDataDir() {
        return serverConfigurator.getDataDir();
    }

    /**
     * Create needed directories. Check existence of old paths. If old paths have been found and they cannot be upgraded
     * automatically, then upgrading message is logged and error thrown.
     *
     * @throws ConfigurationException If a deprecated directory has been detected.
     * @since 5.4.2
     * @see ServerConfigurator#verifyInstallation()
     */
    public void verifyInstallation() throws ConfigurationException {
        checkJavaVersion();
        ifNotExistsAndIsDirectoryThenCreate(getLogDir());
        ifNotExistsAndIsDirectoryThenCreate(getPidDir());
        ifNotExistsAndIsDirectoryThenCreate(getDataDir());
        ifNotExistsAndIsDirectoryThenCreate(getTmpDir());
        ifNotExistsAndIsDirectoryThenCreate(getPackagesDir());
        checkAddressesAndPorts();
        serverConfigurator.verifyInstallation();
        backingServicesConfigurator.verifyInstallation();

    }

    /**
     * @return Marketplace packages directory
     * @since 5.9.4
     */
    private File getPackagesDir() {
        return serverConfigurator.getPackagesDir();
    }

    /**
     * Check that the process is executed with a supported Java version. See
     * <a href="http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html">J2SE SDK/JRE Version String
     * Naming Convention</a>
     *
     * @since 5.6
     */
    public void checkJavaVersion() throws ConfigurationException {
        String version = System.getProperty("java.version");
        checkJavaVersion(version, COMPLIANT_JAVA_VERSIONS);
    }

    /**
     * Check the java version compared to compliant ones.
     *
     * @param version the java version
     * @param compliantVersions the compliant java versions
     * @since 9.1
     */
    protected static void checkJavaVersion(String version, String[] compliantVersions) throws ConfigurationException {
        // compliantVersions represents the java versions on which Nuxeo runs perfectly, so:
        // - if we run Nuxeo with a major java version present in compliantVersions and compatible with then this
        // method exits without error and without logging a warn message about loose compliance
        // - if we run Nuxeo with a major java version not present in compliantVersions but greater than once then
        // this method exits without error and logs a warn message about loose compliance
        // - if we run Nuxeo with a non valid java version then method exits with error
        // - if we run Nuxeo with a non valid java version and with jvmcheck=nofail property then method exits without
        // error and logs a warn message about loose compliance

        // try to retrieve the closest compliant java version
        String lastCompliantVersion = null;
        for (String compliantVersion : compliantVersions) {
            if (checkJavaVersion(version, compliantVersion, false, false)) {
                // current compliant version is valid, go to next one
                lastCompliantVersion = compliantVersion;
            } else if (lastCompliantVersion != null) {
                // current compliant version is not valid, but we found a valid one earlier, 1st case
                return;
            } else if (checkJavaVersion(version, compliantVersion, true, true)) {
                // current compliant version is not valid, try to check java version with jvmcheck=nofail, 4th case
                // here we will log about loose compliance for the lower compliant java version
                return;
            }
        }
        // we might have lastCompliantVersion, unless nothing is valid against the current java version
        if (lastCompliantVersion != null) {
            // 2nd case: log about loose compliance if current major java version is greater than the greatest
            // compliant java version
            checkJavaVersion(version, lastCompliantVersion, false, true);
            return;
        }

        // 3th case
        String message = String.format("Nuxeo requires Java %s (detected %s).", ArrayUtils.toString(compliantVersions),
                version);
        throw new ConfigurationException(message + " See '" + JVMCHECK_PROP + "' option to bypass version check.");
    }

    /**
     * Checks the java version compared to the required one.
     * <p>
     * Loose compliance is assumed if the major version is greater than the required major version or a jvmcheck=nofail
     * flag is set.
     *
     * @param version the java version
     * @param requiredVersion the required java version
     * @param allowNoFailFlag if {@code true} then check jvmcheck=nofail flag to always have loose compliance
     * @param warnIfLooseCompliance if {@code true} then log a WARN if the is loose compliance
     * @return true if the java version is compliant (maybe loosely) with the required version
     * @since 8.4
     */
    protected static boolean checkJavaVersion(String version, String requiredVersion, boolean allowNoFailFlag,
            boolean warnIfLooseCompliance) {
        allowNoFailFlag = allowNoFailFlag
                && JVMCHECK_NOFAIL.equalsIgnoreCase(System.getProperty(JVMCHECK_PROP, JVMCHECK_FAIL));
        try {
            JVMVersion required = JVMVersion.parse(requiredVersion);
            JVMVersion actual = JVMVersion.parse(version);
            boolean compliant = actual.compareTo(required) >= 0;
            if (compliant && actual.compareTo(required, UpTo.MAJOR) == 0) {
                return true;
            }
            if (!compliant && !allowNoFailFlag) {
                return false;
            }
            // greater major version or noFail is present in system property, considered loosely compliant but may warn
            if (warnIfLooseCompliance) {
                log.warn(String.format("Nuxeo requires Java %s+ (detected %s).", requiredVersion, version));
            }
            return true;
        } catch (java.text.ParseException cause) {
            if (allowNoFailFlag) {
                log.warn("Cannot check java version", cause);
                return true;
            }
            throw new IllegalArgumentException("Cannot check java version", cause);
        }
    }

    /**
     * Checks the java version compared to the required one.
     * <p>
     * If major version is same as required major version and minor is greater or equal, it is compliant.
     * <p>
     * If major version is greater than required major version, it is compliant.
     *
     * @param version the java version
     * @param requiredVersion the required java version
     * @return true if the java version is compliant with the required version
     * @since 8.4
     */
    public static boolean checkJavaVersion(String version, String requiredVersion) {
        return checkJavaVersion(version, requiredVersion, false, false);
    }

    /**
     * Will check the configured addresses are reachable and Nuxeo required ports are available on those addresses.
     * Server specific implementations should override this method in order to check for server specific ports.
     * {@link #PARAM_BIND_ADDRESS} must be set before.
     *
     * @since 5.5
     * @see ServerConfigurator#verifyInstallation()
     */
    public void checkAddressesAndPorts() throws ConfigurationException {
        InetAddress bindAddress = getBindAddress();
        // Sanity check
        if (bindAddress.isMulticastAddress()) {
            throw new ConfigurationException("Multicast address won't work: " + bindAddress);
        }
        checkAddressReachable(bindAddress);
        checkPortAvailable(bindAddress, Integer.parseInt(userConfig.getProperty(PARAM_HTTP_PORT)));
    }

    /**
     * Checks the userConfig bind address is not 0.0.0.0 and replaces it with 127.0.0.1 if needed
     *
     * @return the userConfig bind address if not 0.0.0.0 else 127.0.0.1
     * @since 5.7
     */
    public InetAddress getBindAddress() throws ConfigurationException {
        return getBindAddress(userConfig.getProperty(PARAM_BIND_ADDRESS));
    }

    /**
     * Checks hostName bind address is not 0.0.0.0 and replaces it with 127.0.0.1 if needed
     *
     * @param hostName the hostname of Nuxeo server (works also with the IP)
     * @return the bind address matching hostName parameter if not 0.0.0.0 else 127.0.0.1
     * @since 9.2
     */
    public static InetAddress getBindAddress(String hostName) throws ConfigurationException {
        InetAddress bindAddress;
        try {
            bindAddress = InetAddress.getByName(hostName);
            if (bindAddress.isAnyLocalAddress()) {
                boolean preferIPv6 = "false".equals(System.getProperty("java.net.preferIPv4Stack"))
                        && "true".equals(System.getProperty("java.net.preferIPv6Addresses"));
                bindAddress = preferIPv6 ? InetAddress.getByName("::1") : InetAddress.getByName("127.0.0.1");
                log.debug("Bind address is \"ANY\", using local address instead: " + bindAddress);
            }
            log.debug("Configured bind address: " + bindAddress);
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        }
        return bindAddress;
    }

    /**
     * @param address address to check for availability
     * @since 5.5
     */
    public static void checkAddressReachable(InetAddress address) throws ConfigurationException {
        try {
            log.debug("Checking availability of " + address);
            address.isReachable(ADDRESS_PING_TIMEOUT);
        } catch (IOException e) {
            throw new ConfigurationException("Unreachable bind address " + address);
        }
    }

    /**
     * Checks if port is available on given address.
     *
     * @param port port to check for availability
     * @throws ConfigurationException Throws an exception if address is unavailable.
     * @since 5.5
     */
    public static void checkPortAvailable(InetAddress address, int port) throws ConfigurationException {
        if ((port == 0) || (port == -1)) {
            log.warn("Port is set to " + Integer.toString(port)
                    + " - assuming it is disabled - skipping availability check");
            return;
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        ServerSocket socketTCP = null;
        // DatagramSocket socketUDP = null;
        try {
            log.debug("Checking availability of port " + port + " on address " + address);
            socketTCP = new ServerSocket(port, 0, address);
            socketTCP.setReuseAddress(true);
            // socketUDP = new DatagramSocket(port, address);
            // socketUDP.setReuseAddress(true);
            // return true;
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage() + ": " + address + ":" + port, e);
        } finally {
            // if (socketUDP != null) {
            // socketUDP.close();
            // }
            if (socketTCP != null) {
                try {
                    socketTCP.close();
                } catch (IOException e) {
                    // Do not throw
                }
            }
        }
    }

    /**
     * @return Temporary directory
     */
    public File getTmpDir() {
        return serverConfigurator.getTmpDir();
    }

    private void ifNotExistsAndIsDirectoryThenCreate(File directory) {
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
    }

    /**
     * @return Log files produced by Log4J configuration without loading this configuration instead of current active
     *         one.
     * @since 5.4.2
     */
    public ArrayList<String> getLogFiles() {
        File log4jConfFile = serverConfigurator.getLogConfFile();
        System.setProperty(org.nuxeo.common.Environment.NUXEO_LOG_DIR, getLogDir().getPath());
        return Log4JHelper.getFileAppendersFiles(log4jConfFile);
    }

    /**
     * Check if wizard must and can be ran
     *
     * @return true if configuration wizard is required before starting Nuxeo
     * @since 5.4.2
     */
    public boolean isWizardRequired() {
        return !"true".equalsIgnoreCase(getUserConfig().getProperty(PARAM_WIZARD_DONE, "true"))
                && serverConfigurator.isWizardAvailable();
    }

    /**
     * Rebuild a templates string for use in nuxeo.conf
     *
     * @param dbTemplate database template to use instead of current one
     * @return new templates string using given dbTemplate
     * @since 5.4.2
     * @see #extractDatabaseTemplateName()
     * @see #changeDBTemplate(String)
     * @see #changeTemplates(String)
     */
    public String rebuildTemplatesStr(String dbTemplate) {
        List<String> templatesList = new ArrayList<>(asList(templates.split(TEMPLATE_SEPARATOR)));
        String currentDBTemplate = null;
        if (DB_LIST.contains(dbTemplate)) {
            currentDBTemplate = userConfig.getProperty(PARAM_TEMPLATE_DBNAME);
            if (currentDBTemplate == null) {
                currentDBTemplate = extractDatabaseTemplateName();
            }
        } else if (DB_SECONDARY_LIST.contains(dbTemplate)) {
            currentDBTemplate = userConfig.getProperty(PARAM_TEMPLATE_DBSECONDARY_NAME);
            if (currentDBTemplate == null) {
                currentDBTemplate = extractSecondaryDatabaseTemplateName();
            }
            if ("none".equals(dbTemplate)) {
                dbTemplate = null;
            }
        }
        int dbIdx = templatesList.indexOf(currentDBTemplate);
        if (dbIdx < 0) {
            if (dbTemplate == null) {
                return templates;
            }
            // current db template is implicit => set the new one
            templatesList.add(dbTemplate);
        } else if (dbTemplate == null) {
            // current db template is explicit => remove it
            templatesList.remove(dbIdx);
        } else {
            // current db template is explicit => replace it
            templatesList.set(dbIdx, dbTemplate);
        }
        return replaceEnvironmentVariables(String.join(TEMPLATE_SEPARATOR, templatesList));
    }

    /**
     * @return Nuxeo config directory
     * @since 5.4.2
     */
    public File getConfigDir() {
        return serverConfigurator.getConfigDir();
    }

    /**
     * Ensure the server will start only wizard application, not Nuxeo
     *
     * @since 5.4.2
     */
    public void prepareWizardStart() {
        serverConfigurator.prepareWizardStart();
    }

    /**
     * Ensure the wizard won't be started and nuxeo is ready for use
     *
     * @since 5.4.2
     */
    public void cleanupPostWizard() {
        serverConfigurator.cleanupPostWizard();
    }

    /**
     * @return Nuxeo runtime home
     */
    public File getRuntimeHome() {
        return serverConfigurator.getRuntimeHome();
    }

    /**
     * @since 5.4.2
     * @return true if there's an install in progress
     */
    public boolean isInstallInProgress() {
        return getInstallFile().exists();
    }

    /**
     * @return File pointing to the directory containing the marketplace packages included in the distribution
     * @since 5.6
     */
    public File getDistributionMPDir() {
        String mpDir = userConfig.getProperty(PARAM_MP_DIR, DISTRIBUTION_MP_DIR);
        return new File(getNuxeoHome(), mpDir);
    }

    /**
     * @return Install/upgrade file
     * @since 5.4.1
     */
    public File getInstallFile() {
        return new File(serverConfigurator.getDataDir(), INSTALL_AFTER_RESTART);
    }

    /**
     * Add template(s) to the {@link #PARAM_TEMPLATES_NAME} list if not already present
     *
     * @param templatesToAdd Comma separated templates to add
     * @since 5.5
     */
    public void addTemplate(String templatesToAdd) throws ConfigurationException {
        List<String> templatesList = getTemplateList();
        List<String> templatesToAddList = asList(templatesToAdd.split(TEMPLATE_SEPARATOR));
        if (templatesList.addAll(templatesToAddList)) {
            String newTemplatesStr = String.join(TEMPLATE_SEPARATOR, templatesList);
            HashMap<String, String> parametersToSave = new HashMap<>();
            parametersToSave.put(PARAM_TEMPLATES_NAME, newTemplatesStr);
            saveFilteredConfiguration(parametersToSave);
            changeTemplates(newTemplatesStr);
        }
    }

    /**
     * Return the list of templates.
     *
     * @since 9.2
     */
    public List<String> getTemplateList() {
        String currentTemplatesStr = userConfig.getProperty(PARAM_TEMPLATES_NAME);

        return Stream.of(replaceEnvironmentVariables(currentTemplatesStr).split(TEMPLATE_SEPARATOR))
                     .collect(Collectors.toList());

    }

    /**
     * Remove template(s) from the {@link #PARAM_TEMPLATES_NAME} list
     *
     * @param templatesToRm Comma separated templates to remove
     * @since 5.5
     */
    public void rmTemplate(String templatesToRm) throws ConfigurationException {
        List<String> templatesList = getTemplateList();
        List<String> templatesToRmList = asList(templatesToRm.split(TEMPLATE_SEPARATOR));
        if (templatesList.removeAll(templatesToRmList)) {
            String newTemplatesStr = String.join(TEMPLATE_SEPARATOR, templatesList);
            Map<String, String> parametersToSave = new HashMap<>();
            parametersToSave.put(PARAM_TEMPLATES_NAME, newTemplatesStr);
            saveFilteredConfiguration(parametersToSave);
            changeTemplates(newTemplatesStr);
        }
    }

    /**
     * Set a property in nuxeo configuration
     *
     * @return The old value
     * @since 5.5
     */
    public String setProperty(String key, String value) throws ConfigurationException {
        String oldValue = getStoredConfig().getProperty(key);
        if (PARAM_TEMPLATES_NAME.equals(key)) {
            templates = StringUtils.isBlank(value) ? null : value;
        }
        HashMap<String, String> newParametersToSave = new HashMap<>();
        newParametersToSave.put(key, value);
        saveFilteredConfiguration(newParametersToSave);
        setBasicConfiguration();
        return oldValue;
    }

    /**
     * Set properties in nuxeo configuration
     *
     * @return The old values
     * @since 7.4
     */
    public Map<String, String> setProperties(Map<String, String> newParametersToSave) throws ConfigurationException {
        Map<String, String> oldValues = new HashMap<>();
        for (String key : newParametersToSave.keySet()) {
            oldValues.put(key, getStoredConfig().getProperty(key));
            if (PARAM_TEMPLATES_NAME.equals(key)) {
                String value = newParametersToSave.get(key);
                templates = StringUtils.isBlank(value) ? null : value;
            }
        }
        saveFilteredConfiguration(newParametersToSave);
        setBasicConfiguration();
        return oldValues;
    }

    /**
     * Set properties in the given template, if it exists
     *
     * @return The old values
     * @since 7.4
     */
    public Map<String, String> setProperties(String template, Map<String, String> newParametersToSave)
            throws ConfigurationException, IOException {
        File templateConf = getTemplateConf(template);
        Properties templateProperties = loadTrimmedProperties(templateConf);
        Map<String, String> oldValues = new HashMap<>();
        StringBuilder newContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(templateConf))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("## DO NOT EDIT THIS FILE")) {
                throw new ConfigurationException("The template states in its header that it must not be modified.");
            }
            while (line != null) {
                int equalIdx = line.indexOf("=");
                if (equalIdx < 1 || line.trim().startsWith("#")) {
                    newContent.append(line).append(System.getProperty("line.separator"));
                } else {
                    String key = line.substring(0, equalIdx).trim();
                    if (newParametersToSave.containsKey(key)) {
                        newContent.append(key).append("=").append(newParametersToSave.get(key)).append(
                                System.getProperty("line.separator"));
                    } else {
                        newContent.append(line).append(System.getProperty("line.separator"));
                    }
                }
                line = reader.readLine();
            }
        }
        for (String key : newParametersToSave.keySet()) {
            if (templateProperties.containsKey(key)) {
                oldValues.put(key, templateProperties.getProperty(key));
            } else {
                newContent.append(key).append("=").append(newParametersToSave.get(key)).append(
                        System.getProperty("line.separator"));
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(templateConf))) {
            writer.append(newContent.toString());
        }
        setBasicConfiguration();
        return oldValues;
    }

    /**
     * Check driver availability and database connection
     *
     * @param databaseTemplate Nuxeo database template
     * @param dbName nuxeo.db.name parameter in nuxeo.conf
     * @param dbUser nuxeo.db.user parameter in nuxeo.conf
     * @param dbPassword nuxeo.db.password parameter in nuxeo.conf
     * @param dbHost nuxeo.db.host parameter in nuxeo.conf
     * @param dbPort nuxeo.db.port parameter in nuxeo.conf
     * @since 5.6
     */
    public void checkDatabaseConnection(String databaseTemplate, String dbName, String dbUser, String dbPassword,
            String dbHost, String dbPort) throws IOException, DatabaseDriverException, SQLException {
        File databaseTemplateDir = new File(nuxeoHome, TEMPLATES + File.separator + databaseTemplate);
        Properties templateProperties = loadTrimmedProperties(new File(databaseTemplateDir, NUXEO_DEFAULT_CONF));
        String classname, connectionUrl;
        // check if value is set in nuxeo.conf
        if (userConfig.containsKey(PARAM_DB_DRIVER)) {
            classname = (String) userConfig.get(PARAM_DB_DRIVER);
        } else {
            classname = templateProperties.getProperty(PARAM_DB_DRIVER);
        }
        if (userConfig.containsKey(PARAM_DB_JDBC_URL)) {
            connectionUrl = (String) userConfig.get(PARAM_DB_JDBC_URL);
        } else {
            connectionUrl = templateProperties.getProperty(PARAM_DB_JDBC_URL);
        }
        // Load driver class from template or default lib directory
        Driver driver = lookupDriver(databaseTemplate, databaseTemplateDir, classname);
        // Test db connection
        DriverManager.registerDriver(driver);
        Properties ttProps = new Properties(userConfig);
        ttProps.put(PARAM_DB_HOST, dbHost);
        ttProps.put(PARAM_DB_PORT, dbPort);
        ttProps.put(PARAM_DB_NAME, dbName);
        ttProps.put(PARAM_DB_USER, dbUser);
        ttProps.put(PARAM_DB_PWD, dbPassword);
        TextTemplate tt = new TextTemplate(ttProps);
        String url = tt.processText(connectionUrl);
        Properties conProps = new Properties();
        conProps.put("user", dbUser);
        conProps.put("password", dbPassword);
        log.debug("Testing URL " + url + " with " + conProps);
        Connection con = driver.connect(url, conProps);
        con.close();
    }

    /**
     * Build an {@link URLClassLoader} for the given databaseTemplate looking in the templates directory and in the
     * server lib directory, then looks for a driver
     *
     * @param classname Driver class name, defined by {@link #PARAM_DB_DRIVER}
     * @return Driver driver if found, else an Exception must have been raised.
     * @throws DatabaseDriverException If there was an error when trying to instantiate the driver.
     * @since 5.6
     */
    private Driver lookupDriver(String databaseTemplate, File databaseTemplateDir, String classname)
            throws DatabaseDriverException {
        File[] files = (File[]) ArrayUtils.addAll( //
                new File(databaseTemplateDir, "lib").listFiles(), //
                serverConfigurator.getServerLibDir().listFiles());
        List<URL> urlsList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("jar")) {
                    try {
                        urlsList.add(new URL("jar:file:" + file.getPath() + "!/"));
                        log.debug("Added " + file.getPath());
                    } catch (MalformedURLException e) {
                        log.error(e);
                    }
                }
            }
        }
        URLClassLoader ucl = new URLClassLoader(urlsList.toArray(new URL[0]));
        try {
            return (Driver) Class.forName(classname, true, ucl).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new DatabaseDriverException(e);
        }
    }

    /**
     * @since 5.6
     * @return an {@link Environment} initialized with a few basics
     */
    public Environment getEnv() {
        /*
         * It could be useful to initialize DEFAULT env in {@link #setBasicConfiguration()}... For now, the generated
         * {@link Environment} is not static.
         */
        if (env == null) {
            env = new Environment(getRuntimeHome());
            File distribFile = new File(new File(nuxeoHome, TEMPLATES), "common/config/distribution.properties");
            if (distribFile.exists()) {
                try {
                    env.loadProperties(loadTrimmedProperties(distribFile));
                } catch (IOException e) {
                    log.error(e);
                }
            }
            env.loadProperties(userConfig);
            env.setServerHome(getNuxeoHome());
            env.init();
            env.setData(userConfig.getProperty(Environment.NUXEO_DATA_DIR, "data"));
            env.setLog(userConfig.getProperty(Environment.NUXEO_LOG_DIR, "logs"));
            env.setTemp(userConfig.getProperty(Environment.NUXEO_TMP_DIR, "tmp"));
            env.setPath(PARAM_MP_DIR, getDistributionMPDir(), env.getServerHome());
            env.setPath(Environment.NUXEO_MP_DIR, getPackagesDir(), env.getServerHome());
        }
        return env;
    }

    /**
     * @since 10.2
     * @param propsFile Properties file
     * @return String with the charset encoding for this file
     */
    public static Charset checkFileCharset(File propsFile) throws IOException {
        List<Charset> charsetsToBeTested = asList(US_ASCII, UTF_8, ISO_8859_1);
        for (Charset charsetTest : charsetsToBeTested) {
            CharsetDecoder decoder = charsetTest.newDecoder();
            decoder.reset();

            boolean identified = true; // assume the charset is this one, until it is not !
            try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(propsFile))) {
                byte[] buffer = new byte[512];
                while ((input.read(buffer) != -1) && (identified)) {
                    try {
                        decoder.decode(ByteBuffer.wrap(buffer));
                        identified = true;
                    } catch (CharacterCodingException e) {
                        identified = false;
                    }
                }
            }
            if (identified) {
                return charsetTest;
            }
        }
        return null;
    }

    /**
     * @since 5.6
     * @param propsFile Properties file
     * @return new Properties containing trimmed keys and values read in {@code propsFile}
     */
    public static Properties loadTrimmedProperties(File propsFile) throws IOException {
        Properties props = new Properties();
        Charset charset = checkFileCharset(propsFile);
        if (charset == null) {
            throw new IOException("Can't identify input file charset for " + propsFile.getName());
        }
        log.debug("Opening " + propsFile.getName() + " in " + charset.name());
        try (InputStreamReader propsIS = new InputStreamReader(new FileInputStream(propsFile), charset)) {
            loadTrimmedProperties(props, propsIS);
        }
        return props;
    }

    /**
     * @since 5.6
     * @param props Properties object to be filled
     * @param propsIS Properties InputStream
     */
    public static void loadTrimmedProperties(Properties props, InputStreamReader propsIS) throws IOException {
        if (props == null) {
            return;
        }
        Properties p = new Properties();
        p.load(propsIS);
        @SuppressWarnings("unchecked")
        Enumeration<String> pEnum = (Enumeration<String>) p.propertyNames();
        while (pEnum.hasMoreElements()) {
            String key = pEnum.nextElement();
            String value = p.getProperty(key);
            props.put(key.trim(), value.trim());
        }
    }

    /**
     * @return The generated properties file with dumped configuration.
     * @since 5.6
     */
    public File getDumpedConfig() {
        return new File(getConfigDir(), CONFIGURATION_PROPERTIES);
    }

    /**
     * Build a {@link Hashtable} which contains environment properties to instantiate a {@link InitialDirContext}
     *
     * @since 6.0
     */
    public Hashtable<Object, Object> getContextEnv(String ldapUrl, String bindDn, String bindPassword,
            boolean checkAuthentication) {
        Hashtable<Object, Object> contextEnv = new Hashtable<>();
        contextEnv.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        contextEnv.put("com.sun.jndi.ldap.connect.timeout", "10000");
        contextEnv.put(javax.naming.Context.PROVIDER_URL, ldapUrl);
        if (checkAuthentication) {
            contextEnv.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
            contextEnv.put(javax.naming.Context.SECURITY_PRINCIPAL, bindDn);
            contextEnv.put(javax.naming.Context.SECURITY_CREDENTIALS, bindPassword);
        }
        return contextEnv;
    }

    /**
     * Check if the LDAP parameters are correct to bind to a LDAP server. if authenticate argument is true, it will also
     * check if the authentication against the LDAP server succeeds
     *
     * @param authenticate Indicates if authentication against LDAP should be checked.
     * @since 6.0
     */
    public void checkLdapConnection(String ldapUrl, String ldapBindDn, String ldapBindPwd, boolean authenticate)
            throws NamingException {
        checkLdapConnection(getContextEnv(ldapUrl, ldapBindDn, ldapBindPwd, authenticate));
    }

    /**
     * @param contextEnv Environment properties to build a {@link InitialDirContext}
     * @since 6.0
     */
    public void checkLdapConnection(Hashtable<Object, Object> contextEnv) throws NamingException {
        DirContext dirContext = new InitialDirContext(contextEnv);
        dirContext.close();
    }

    /**
     * @return a {@link Crypto} instance initialized with the configuration parameters
     * @since 7.4
     * @see Crypto
     */
    public Crypto getCrypto() {
        return userConfig.getCrypto();
    }

    /**
     * @param template path to configuration template directory
     * @return A {@code nuxeo.defaults} file if it exists.
     * @throws ConfigurationException if the template file is not found.
     * @since 7.4
     */
    public File getTemplateConf(String template) throws ConfigurationException {
        File templateDir = new File(template);
        if (!templateDir.isAbsolute()) {
            templateDir = new File(System.getProperty("user.dir"), template);
            if (!templateDir.exists() || !new File(templateDir, NUXEO_DEFAULT_CONF).exists()) {
                templateDir = new File(nuxeoDefaultConf.getParentFile(), template);
            }
        }
        if (!templateDir.exists() || !new File(templateDir, NUXEO_DEFAULT_CONF).exists()) {
            throw new ConfigurationException("Template not found: " + template);
        }
        return new File(templateDir, NUXEO_DEFAULT_CONF);
    }

    /**
     * Gets the Java options with 'nuxeo.*' properties substituted. It enables usage of property like ${nuxeo.log.dir}
     * inside JAVA_OPTS.
     *
     * @return the Java options string.
     * @deprecated Since 9.3. Use {@link #getJavaOptsString()} instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    protected String getJavaOpts(String key, String value) {
        return getJavaOptsString();
    }

    /**
     * Gets the Java options defined in Nuxeo configuration files, e.g. <tt>bin/nuxeo.conf</tt> and
     * <tt>bin/nuxeoctl</tt>.
     *
     * @return the Java options.
     * @since 9.3
     */
    public List<String> getJavaOpts(Function<String, String> mapper) {
        return Arrays.stream(JAVA_OPTS_PATTERN.split(System.getProperty(JAVA_OPTS_PROP, "")))
                     .map(option -> StringSubstitutor.replace(option, getUserConfig()))
                     .map(mapper)
                     .collect(Collectors.toList());
    }

    /**
     * @return the Java options string.
     * @since 9.3
     * @see #getJavaOpts(Function)
     */
    protected String getJavaOptsString() {
        return String.join(" ", getJavaOpts(Function.identity()));
    }

    /**
     * @return the value of an environment variable. Overriden for testing.
     * @since 9.1
     */
    protected String getEnvironmentVariableValue(String key) {
        return System.getenv(key);
    }
}
