/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.UUID;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;

import org.nuxeo.common.Environment;
import org.nuxeo.launcher.commons.DatabaseDriverException;
import org.nuxeo.launcher.commons.text.TextTemplate;
import org.nuxeo.log4j.Log4JHelper;

import freemarker.template.TemplateException;

/**
 * Builder for server configuration and datasource files from templates and
 * properties.
 *
 * @author jcarsique
 */
public class ConfigurationGenerator {

    /**
     * @since 5.9.6
     */
    public static final String TEMPLATE_SEPARATOR = ",";

    /**
     * @since 5.7
     */
    public static final String[] COMPLIANT_JAVA_VERSIONS = new String[] { "1.7" };

    /**
     * @since 5.6
     */
    protected static final String CONFIGURATION_PROPERTIES = "configuration.properties";

    private static final Log log = LogFactory.getLog(ConfigurationGenerator.class);

    /**
     * @deprecated Since 5.6, use {@link Environment#NUXEO_HOME} instead
     */
    @Deprecated
    public static final String NUXEO_HOME = Environment.NUXEO_HOME;

    public static final String NUXEO_CONF = "nuxeo.conf";

    public static final String TEMPLATES = "templates";

    protected static final String NUXEO_DEFAULT_CONF = "nuxeo.defaults";

    /**
     * Absolute or relative PATH to the user chosen template
     *
     * @deprecated use {@link #PARAM_TEMPLATES_NAME} instead
     */
    @Deprecated
    public static final String PARAM_TEMPLATE_NAME = "nuxeo.template";

    /**
     * Absolute or relative PATH to the user chosen templates (comma separated
     * list)
     */
    public static final String PARAM_TEMPLATES_NAME = "nuxeo.templates";

    public static final String PARAM_TEMPLATE_DBNAME = "nuxeo.dbtemplate";

    public static final String PARAM_TEMPLATE_DBTYPE = "nuxeo.db.type";

    /**
     * @deprecated since 5.7
     */
    @Deprecated
    public static final String PARAM_TEMPLATES_NODB = "nuxeo.nodbtemplates";

    public static final String OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.templates.parsing.extensions";

    public static final String PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.plaintext_parsing_extensions";

    public static final String PARAM_TEMPLATES_FREEMARKER_EXTENSIONS = "nuxeo.freemarker_parsing_extensions";

    /**
     * Absolute or relative PATH to the included templates (comma separated
     * list)
     */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    public static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String BOUNDARY_BEGIN = "### BEGIN - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final String BOUNDARY_END = "### END - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final List<String> DB_LIST = Arrays.asList("default",
            "postgresql", "oracle", "mysql", "mssql", "db2");

    public static final String PARAM_WIZARD_DONE = "nuxeo.wizard.done";

    public static final String PARAM_WIZARD_RESTART_PARAMS = "wizard.restart.params";

    public static final String PARAM_FAKE_WINDOWS = "org.nuxeo.fake.vindoz";

    public static final String PARAM_LOOPBACK_URL = "nuxeo.loopback.url";

    public static final int MIN_PORT = 1;

    public static final int MAX_PORT = 65535;

    public static final int ADDRESS_PING_TIMEOUT = 1000;

    public static final String PARAM_BIND_ADDRESS = "nuxeo.bind.address";

    public static final String PARAM_HTTP_PORT = "nuxeo.server.http.port";

    public static final String PARAM_STATUS_KEY = "server.status.key";

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

    public static final String PARAM_PRODUCT_NAME = "org.nuxeo.ecm.product.name";

    public static final String PARAM_PRODUCT_VERSION = "org.nuxeo.ecm.product.version";

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
     * Seam hot reload property, also controlled by
     * {@link #NUXEO_DEV_SYSTEM_PROP}
     *
     * @since 5.6
     */
    public static final String SEAM_DEBUG_SYSTEM_PROP = "org.nuxeo.seam.debug";

    /**
     * Old way of detecting if seam debug should be enabled, by looking for the
     * presence of this file. Setting property {@link #SEAM_DEBUG_SYSTEM_PROP}
     * in nuxeo.conf is enough now.
     *
     * @deprecated since 5.6
     * @since 5.6
     */
    @Deprecated
    public static final String SEAM_HOT_RELOAD_GLOBAL_CONFIG_FILE = "seam-debug.properties";

    private final File nuxeoHome;

    // User configuration file
    private final File nuxeoConf;

    // Chosen templates
    private final List<File> includedTemplates = new ArrayList<>();

    // Common default configuration file
    private File nuxeoDefaultConf;

    public boolean isJBoss;

    public boolean isJetty;

    public boolean isTomcat;

    private ServerConfigurator serverConfigurator;

    private boolean forceGeneration;

    private Properties defaultConfig;

    private Properties userConfig;

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

    @SuppressWarnings("unused")
    private boolean debug = false;

    private static boolean hideDeprecationWarnings = false;

    private Environment env;

    private Properties storedConfig;

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
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            put(OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS,
                    PARAM_TEMPLATES_PARSING_EXTENSIONS);
            put("nuxeo.db.user.separator.key", "nuxeo.db.user_separator_key");
            put("mail.pop3.host", "mail.store.host");
            put("mail.pop3.port", "mail.store.port");
            put("mail.smtp.host", "mail.transport.host");
            put("mail.smtp.port", "mail.transport.port");
            put("mail.smtp.username", "mail.transport.username");
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
        this.debug = debug;
        String nuxeoHomePath = Environment.getDefault().getServerHome().getAbsolutePath();
        if (nuxeoHomePath != null) {
            nuxeoHome = new File(nuxeoHomePath);
        } else {
            File userDir = new File(System.getProperty("user.dir"));
            if ("bin".equalsIgnoreCase(userDir.getName())) {
                nuxeoHome = userDir.getParentFile();
            } else {
                nuxeoHome = userDir;
            }
        }
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoConfPath != null) {
            nuxeoConf = new File(nuxeoConfPath).getAbsoluteFile();
        } else {
            nuxeoConf = new File(nuxeoHome, "bin" + File.separator
                    + "nuxeo.conf").getAbsoluteFile();
        }
        System.setProperty(NUXEO_CONF, nuxeoConf.getPath());

        nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES + File.separator
                + NUXEO_DEFAULT_CONF);

        // detect server type based on System properties
        isJetty = System.getProperty(JettyConfigurator.JETTY_HOME) != null;
        isTomcat = System.getProperty(TomcatConfigurator.TOMCAT_HOME) != null;
        if (!isJBoss && !isJetty && !isTomcat) {
            // fallback on jar detection
            isJBoss = new File(nuxeoHome, "bin/run.jar").exists();
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
     * @param forceGeneration
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

    public Properties getUserConfig() {
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
                        + userConfig.getProperty(PARAM_FORCE_GENERATION)
                        + ")...");
                generateFiles();
            } else {
                log.info("Server already configured (set nuxeo.force.generation=true to force configuration files generation).");
            }
        }
    }

    /**
     * Initialize configurator, check requirements and load current
     * configuration
     *
     * @return returns true if current install is configurable, else returns
     *         false
     */
    public boolean init() {
        return init(false);
    }

    /**
     * Initialize configurator, check requirements and load current
     * configuration
     *
     * @since 5.6
     * @param forceReload If true, forces configuration reload.
     * @return returns true if current install is configurable, else returns
     *         false
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
            userConfig = new Properties();
        } else if (userConfig == null || userConfig.size() == 0 || forceReload) {
            try {
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

    public void changeTemplates(String newTemplates) {
        try {
            templates = newTemplates;
            setBasicConfiguration(false);
            configurable = true;
        } catch (ConfigurationException e) {
            log.warn("Error reading basic configuration.", e);
            configurable = false;
        }
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

    private void setBasicConfiguration(boolean save)
            throws ConfigurationException {
        try {
            // Load default configuration
            defaultConfig = loadTrimmedProperties(nuxeoDefaultConf);
            // Add System properties
            defaultConfig.putAll(System.getProperties());
            userConfig = new Properties(defaultConfig);

            // If Windows, replace backslashes in paths in nuxeo.conf
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                replaceBackslashes();
            }
            // Load user configuration
            userConfig.putAll(loadTrimmedProperties(nuxeoConf));
            onceGeneration = "once".equals(userConfig.getProperty(PARAM_FORCE_GENERATION));
            forceGeneration = onceGeneration
                    || Boolean.parseBoolean(userConfig.getProperty(
                            PARAM_FORCE_GENERATION, "false"));
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
            throw new ConfigurationException("Missing file: "
                    + nuxeoDefaultConf + " or " + nuxeoConf, e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }

        // Override default configuration with specific configuration(s) of
        // the chosen template(s) which can be outside of server filesystem
        try {
            includeTemplates();
            checkForDeprecatedParameters(defaultConfig);
            extractDatabaseTemplateName();
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }

        Map<String, String> newParametersToSave = evalDynamicProperties();
        if (save && newParametersToSave != null
                && !newParametersToSave.isEmpty()) {
            saveConfiguration(newParametersToSave, false, false);
        }

        logDebugInformation();

        // Could be useful to initialize DEFAULT env...
        // initEnv();
    }

    /**
     * @since 5.7
     * @throws IOException
     */
    protected void includeTemplates() throws IOException {
        includedTemplates.clear();
        List<File> orderedTemplates = includeTemplates(getUserTemplates());
        includedTemplates.clear();
        includedTemplates.addAll(orderedTemplates);
        log.debug(includedTemplates);
    }

    /**
     * Old way of detecting if seam debug is set, by checking for the presence
     * of a file.
     * <p>
     * On 5.6, using the config generator to get the info from the nuxeo.conf
     * file makes it possible to get the property value this early, so adding an
     * empty file at {@link #SEAM_HOT_RELOAD_GLOBAL_CONFIG} is no longer needed.
     *
     * @deprecated since 5.6
     */
    @Deprecated
    protected boolean hasSeamDebugFile() {
        File f = new File(getServerConfigurator().getConfigDir(),
                SEAM_HOT_RELOAD_GLOBAL_CONFIG_FILE);
        if (!f.exists()) {
            return false;
        }
        return true;
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
        if (Boolean.parseBoolean(seamDebugPropValue) || hasSeamDebugFile()) {
            log.debug("Nuxeo Seam HotReload is enabled");
            // add it to the system props for compat, in case this mode was
            // detected because of presence of the file in the config dir, and
            // because it's checked there on code that relies on it
            System.setProperty(SEAM_DEBUG_SYSTEM_PROP, "true");
        } else {
            log.debug("Nuxeo Seam HotReload is not enabled");
        }
    }

    /**
     * Generate properties which values are based on others
     *
     * @return Map with new parameters to save in {@code nuxeoConf}
     * @throws ConfigurationException
     * @since 5.5
     */
    protected HashMap<String, String> evalDynamicProperties()
            throws ConfigurationException {
        HashMap<String, String> newParametersToSave = new HashMap<>();
        evalLoopbackURL();
        evalServerStatusKey(newParametersToSave);
        return newParametersToSave;
    }

    /**
     * Generate a server status key if not already set
     *
     * @param newParametersToSave
     * @throws ConfigurationException
     * @see #PARAM_STATUS_KEY
     * @since 5.5
     */
    private void evalServerStatusKey(Map<String, String> newParametersToSave)
            throws ConfigurationException {
        if (userConfig.getProperty(PARAM_STATUS_KEY) == null) {
            newParametersToSave.put(PARAM_STATUS_KEY,
                    UUID.randomUUID().toString().substring(0, 8));
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
                bindAddress = preferIPv6 ? InetAddress.getByName("::1")
                        : InetAddress.getByName("127.0.0.1");
                log.debug("Bind address is \"ANY\", using local address instead: "
                        + bindAddress);
            }
        } catch (UnknownHostException e) {
            log.debug(e, e);
            log.error(e.getMessage());
        }

        String httpPort = userConfig.getProperty(PARAM_HTTP_PORT);
        String contextPath = userConfig.getProperty(PARAM_CONTEXT_PATH);
        // Is IPv6 or IPv4 ?
        if (bindAddress instanceof Inet6Address) {
            loopbackURL = "http://[" + bindAddress.getHostAddress() + "]:"
                    + httpPort + contextPath;
        } else {
            loopbackURL = "http://" + bindAddress.getHostAddress() + ":"
                    + httpPort + contextPath;
        }
        log.debug("Set as loop back URL: " + loopbackURL);
        defaultConfig.setProperty(PARAM_LOOPBACK_URL, loopbackURL);
    }

    /**
     * Read nuxeo.conf, replace backslashes in paths and write new nuxeo.conf
     *
     * @throws ConfigurationException if any error reading or writing
     *             nuxeo.conf
     * @since 5.4.1
     */
    protected void replaceBackslashes() throws ConfigurationException {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(nuxeoConf));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches(".*:\\\\.*")) {
                    line = line.replaceAll("\\\\", "/");
                }
                sb.append(line + System.getProperty("line.separator"));
            }
            reader.close();
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(nuxeoConf, false);
            // Copy back file content
            writer.append(sb.toString());
        } catch (IOException e) {
            throw new ConfigurationException("Error writing in " + nuxeoConf, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
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
            defaultConfig.setProperty(key,
                    serverConfigurator.getDirectory(key).getPath());
        } else {
            serverConfigurator.setDirectory(key, directory);
        }
    }

    public String getUserTemplates() {
        if (templates == null) {
            templates = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        }
        if (templates == null) {
            // backward compliance: manage parameter for a single template
            templates = userConfig.getProperty(PARAM_TEMPLATE_NAME);
        }
        if (templates == null) {
            log.warn("No template found in configuration! Fallback on 'default'.");
            templates = "default";
        }
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
            throw new ConfigurationException("Missing file: " + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure: "
                    + e.getMessage(), e);
        } catch (TemplateException e) {
            throw new ConfigurationException(
                    "Could not process FreeMarker template: " + e.getMessage(),
                    e);
        }
    }

    private List<File> includeTemplates(String templatesList)
            throws IOException {
        List<File> orderedTemplates = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(templatesList,
                TEMPLATE_SEPARATOR);
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            File chosenTemplate = new File(nextToken);
            // is it absolute and existing or relative path ?
            if (!chosenTemplate.exists()
                    || !chosenTemplate.getPath().equals(
                            chosenTemplate.getAbsolutePath())) {
                chosenTemplate = new File(nuxeoDefaultConf.getParentFile(),
                        nextToken);
            }
            if (includedTemplates.contains(chosenTemplate)) {
                log.debug("Already included " + nextToken);
                continue;
            }
            if (!chosenTemplate.exists()) {
                log.error(String.format(
                        "Template '%s' not found with relative or absolute path (%s). "
                                + "Check your %s parameter, and %s for included files.",
                        nextToken, chosenTemplate, PARAM_TEMPLATES_NAME,
                        PARAM_INCLUDED_TEMPLATES));
                continue;
            }
            File chosenTemplateConf = new File(chosenTemplate,
                    NUXEO_DEFAULT_CONF);
            includedTemplates.add(chosenTemplate);
            if (!chosenTemplateConf.exists()) {
                log.warn("Ignore template (no default configuration): "
                        + nextToken);
                continue;
            }

            Properties subTemplateConf = loadTrimmedProperties(chosenTemplateConf);
            String subTemplatesList = subTemplateConf.getProperty(PARAM_INCLUDED_TEMPLATES);
            if (subTemplatesList != null && subTemplatesList.length() > 0) {
                orderedTemplates.addAll(includeTemplates(subTemplatesList));
            }
            // Load configuration from chosen templates
            defaultConfig.putAll(subTemplateConf);
            orderedTemplates.add(chosenTemplate);
            String templateInfo = "Include template: "
                    + chosenTemplate.getPath();
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
     * @param properties
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
                    log.warn("Parameter " + key
                            + " is deprecated - please use "
                            + parametersMigration.get(key) + " instead");
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

    public static void main(String[] args) throws ConfigurationException {
        new ConfigurationGenerator().run();
    }

    /**
     * Save changed parameters in {@code nuxeo.conf}. This method does not
     * check values in map. Use {@link #saveFilteredConfiguration(Map)} for
     * parameters filtering.
     *
     * @param changedParameters Map of modified parameters
     * @see #saveFilteredConfiguration(Map)
     */
    public void saveConfiguration(Map<String, String> changedParameters)
            throws ConfigurationException {
        // Keep generation true or once; switch false to once
        saveConfiguration(changedParameters, false, true);
    }

    /**
     * Save changed parameters in {@code nuxeo.conf} calculating templates if
     * changedParameters contains a value for {@link #PARAM_TEMPLATE_DBNAME}. If
     * a parameter value is empty ("" or null), then the property is unset. This
     * method does not check values in map: use
     * {@link #saveFilteredConfiguration(Map)} for parameters filtering.
     *
     * @param changedParameters Map of modified parameters
     * @param setGenerationOnceToFalse If generation was on (true or once),
     *            then set it to false or not?
     * @param setGenerationFalseToOnce If generation was off (false), then set
     *            it to once?
     * @see #saveFilteredConfiguration(Map)
     * @since 5.5
     */
    public void saveConfiguration(Map<String, String> changedParameters,
            boolean setGenerationOnceToFalse, boolean setGenerationFalseToOnce)
            throws ConfigurationException {
        setOnceToFalse = setGenerationOnceToFalse;
        setFalseToOnce = setGenerationFalseToOnce;
        updateStoredConfig();
        String newDbTemplate = changedParameters.remove(PARAM_TEMPLATE_DBNAME);
        if (newDbTemplate != null) {
            changedParameters.put(PARAM_TEMPLATES_NAME,
                    rebuildTemplatesStr(newDbTemplate));
        }
        if (changedParameters.containsValue(null)
                || changedParameters.containsValue("")) {
            // There are properties to unset
            Set<String> propertiesToUnset = new HashSet<>();
            for (String key : changedParameters.keySet()) {
                if (StringUtils.isEmpty(changedParameters.get(key))) {
                    propertiesToUnset.add(key);
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
     * Save changed parameters in {@code nuxeo.conf}, filtering parameters with
     * {@link #getChangedParametersMap(Map, Map)}
     *
     * @param changedParameters Maps of modified parameters
     * @since 5.4.2
     * @see #saveConfiguration(Map, boolean)
     * @see #getChangedParametersMap(Map, Map)
     */
    public void saveFilteredConfiguration(Map<String, String> changedParameters)
            throws ConfigurationException {
        Map<String, String> filteredParameters = getChangedParameters(changedParameters);
        saveConfiguration(filteredParameters);
    }

    /**
     * Filters given parameters including them only if (there was no previous
     * value and new value is not empty/null) or (there was a previous value
     * and it differs from the new value)
     *
     * @param changedParameters parameters to be filtered
     * @return filtered map
     * @since 5.4.2
     */
    public Map<String, String> getChangedParameters(
            Map<String, String> changedParameters) {
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
        StringBuffer newContent = readConfiguration();
        FileWriter writer = null;
        try {
            writer = new FileWriter(nuxeoConf, false);
            // Copy back file content
            writer.append(newContent.toString());
            // Write changed parameters
            writer.write(BOUNDARY_BEGIN + " " + new Date().toString()
                    + System.getProperty("line.separator"));
            for (Object o : new TreeSet<>(userConfig.keySet())) {
                String key = (String) o;
                // Ignore parameters already stored in newContent
                if (PARAM_FORCE_GENERATION.equals(key)
                        || PARAM_WIZARD_DONE.equals(key)
                        || PARAM_TEMPLATES_NAME.equals(key)) {
                    continue;
                }
                String oldValue = storedConfig.getProperty(key, "");
                String newValue = userConfig.getProperty(key, "");
                if (!newValue.equals(oldValue)) {
                    writer.write("#" + key + "=" + oldValue
                            + System.getProperty("line.separator"));
                    writer.write(key + "=" + newValue
                            + System.getProperty("line.separator"));
                }
            }
            writer.write(BOUNDARY_END + System.getProperty("line.separator"));
        } catch (IOException e) {
            throw new ConfigurationException("Error writing in " + nuxeoConf, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
        }
    }

    private StringBuffer readConfiguration() throws ConfigurationException {
        String wizardParam = null, templatesParam = null;
        Integer generationIndex = null, wizardIndex = null, templatesIndex = null;
        // Will change wizardParam value instead of appending it
        wizardParam = userConfig.getProperty(PARAM_WIZARD_DONE);
        // Will change templatesParam value instead of appending it
        templatesParam = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        ArrayList<String> newLines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(nuxeoConf));
            String line;
            boolean onConfiguratorContent = false;
            while ((line = reader.readLine()) != null) {
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
                                line = PARAM_TEMPLATES_NAME + "="
                                        + templatesParam;
                            }
                            if (templatesIndex == null) {
                                newLines.add(line);
                                templatesIndex = newLines.size() - 1;
                            } else {
                                newLines.set(templatesIndex, line);
                            }
                        } else {
                            newLines.add(line);
                        }
                    } else {
                        // What must be written just before the BOUNDARY_BEGIN
                        if (templatesIndex == null && templatesParam != null) {
                            newLines.add(PARAM_TEMPLATES_NAME + "="
                                    + templatesParam);
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
                        if (line.startsWith("#" + PARAM_TEMPLATES_NAME)
                                || line.startsWith(PARAM_TEMPLATES_NAME)) {
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
                            if (!value.equals(userConfig.getProperty(key))) {
                                getStoredConfig().setProperty(key, value);
                            }
                        }
                    } else {
                        onConfiguratorContent = false;
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
        }
        StringBuffer newContent = new StringBuffer();
        for (int i = 0; i < newLines.size(); i++) {
            newContent.append(newLines.get(i).trim()
                    + System.getProperty("line.separator"));
        }
        return newContent;
    }

    /**
     * Extract a database template from the current list of templates. Return
     * the last one if there are multiples.
     *
     * @see #rebuildTemplatesStr(String)
     */
    public String extractDatabaseTemplateName() {
        String dbTemplate = "unknown";
        boolean found = false;
        for (File templateFile : includedTemplates) {
            String template = templateFile.getName();
            if (DB_LIST.contains(template)) {
                dbTemplate = template;
                found = true;
            }
        }
        String dbType = userConfig.getProperty(PARAM_TEMPLATE_DBTYPE);
        if (!found && dbType != null) {
            log.warn(String.format(
                    "Didn't find a known database template in the list but "
                            + "some template contributed a value for %s.",
                    PARAM_TEMPLATE_DBTYPE));
            dbTemplate = dbType;
        }
        if (!dbTemplate.equals(dbType)) {
            if (dbType == null) {
                log.warn(String.format("Missing value for %s, using %s",
                        PARAM_TEMPLATE_DBTYPE, dbTemplate));
                userConfig.setProperty(PARAM_TEMPLATE_DBTYPE, dbTemplate);
            } else {
                log.error(String.format(
                        "Inconsistent values between %s (%s) and %s (%s)",
                        PARAM_TEMPLATE_DBNAME, dbTemplate,
                        PARAM_TEMPLATE_DBTYPE, dbType));
            }
        }
        defaultConfig.setProperty(PARAM_TEMPLATE_DBNAME, dbTemplate);
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
     * Create needed directories. Check existence of old paths. If old paths
     * have been found and they cannot be upgraded automatically, then
     * upgrading message is logged and error thrown.
     *
     * @throws ConfigurationException If a deprecated directory has been
     *             detected.
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
        if (!"default".equals(userConfig.getProperty(PARAM_TEMPLATE_DBTYPE))) {
            try {
                checkDatabaseConnection(
                        userConfig.getProperty(PARAM_TEMPLATE_DBNAME),
                        userConfig.getProperty(PARAM_DB_NAME),
                        userConfig.getProperty(PARAM_DB_USER),
                        userConfig.getProperty(PARAM_DB_PWD),
                        userConfig.getProperty(PARAM_DB_HOST),
                        userConfig.getProperty(PARAM_DB_PORT));
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            } catch (DatabaseDriverException e) {
                log.debug(e, e);
                log.error(e.getMessage());
                throw new ConfigurationException(
                        "Could not find database driver: " + e.getMessage());
            } catch (SQLException e) {
                log.debug(e, e);
                log.error(e.getMessage());
                throw new ConfigurationException(
                        "Failed to connect on database: " + e.getMessage());
            }
        }
    }

    /**
     * @return Marketplace packages directory
     *
     * @since 5.9.4
     */
    private File getPackagesDir() {
        return serverConfigurator.getPackagesDir();
    }

    /**
     * Check that the process is executed with a supported Java version
     *
     * @throws ConfigurationException
     * @since 5.6
     */
    public void checkJavaVersion() throws ConfigurationException {
        String version = System.getProperty("java.version");
        boolean isCompliant = false;
        boolean isGreater = false;
        for (String compliantJava : COMPLIANT_JAVA_VERSIONS) {
            if (version.startsWith(compliantJava)) {
                isCompliant = true;
                break;
            } else if (version.compareTo(compliantJava) > 0) {
                isGreater = true;
            }
        }
        if (!isCompliant) {
            String message = String.format(
                    "Nuxeo requires Java %s (detected %s).",
                    ArrayUtils.toString(COMPLIANT_JAVA_VERSIONS), version);
            if (isGreater
                    || "nofail".equalsIgnoreCase(System.getProperty("jvmcheck",
                            "fail"))) {
                log.warn(message);
            } else {
                throw new ConfigurationException(message);
            }
        }
    }

    /**
     * Will check the configured addresses are reachable and Nuxeo required
     * ports are available on those addresses. Server specific implementations
     * should override this method in order to check for server specific ports.
     * {@link #bindAddress} must be set before.
     *
     * @throws ConfigurationException
     * @since 5.5
     * @see ServerConfigurator#checkNetwork()
     */
    public void checkAddressesAndPorts() throws ConfigurationException {
        InetAddress bindAddress = getBindAddress();
        // Sanity check
        if (bindAddress.isMulticastAddress()) {
            throw new ConfigurationException("Multicast address won't work: "
                    + bindAddress);
        }
        checkAddressReachable(bindAddress);
        checkPortAvailable(bindAddress,
                Integer.parseInt(userConfig.getProperty(PARAM_HTTP_PORT)));
    }

    public InetAddress getBindAddress() throws ConfigurationException {
        InetAddress bindAddress;
        try {
            bindAddress = InetAddress.getByName(userConfig.getProperty(PARAM_BIND_ADDRESS));
            log.debug("Configured bind address: " + bindAddress);
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        }
        return bindAddress;
    }

    /**
     * @param address address to check for availability
     * @throws ConfigurationException
     * @since 5.5
     */
    public static void checkAddressReachable(InetAddress address)
            throws ConfigurationException {
        try {
            log.debug("Checking availability of " + address);
            address.isReachable(ADDRESS_PING_TIMEOUT);
        } catch (IOException e) {
            throw new ConfigurationException("Unreachable bind address "
                    + address);
        }
    }

    /**
     * Checks if port is available on given address.
     *
     * @param port port to check for availability
     * @throws ConfigurationException Throws an exception if address is
     *             unavailable.
     * @since 5.5
     */
    public static void checkPortAvailable(InetAddress address, int port)
            throws ConfigurationException {
        if ((port == 0) || (port == -1)) {
            log.warn("Port is set to "
                    + Integer.toString(port)
                    + " - assuming it is disabled - skipping availability check");
            return;
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        ServerSocket socketTCP = null;
        // DatagramSocket socketUDP = null;
        try {
            log.debug("Checking availability of port " + port + " on address "
                    + address);
            socketTCP = new ServerSocket(port, 0, address);
            socketTCP.setReuseAddress(true);
            // socketUDP = new DatagramSocket(port, address);
            // socketUDP.setReuseAddress(true);
            // return true;
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage() + ": " + address
                    + ":" + port, e);
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
     * @return Log files produced by Log4J configuration without loading this
     *         configuration instead of current active one.
     * @since 5.4.2
     */
    public ArrayList<String> getLogFiles() {
        File log4jConfFile = serverConfigurator.getLogConfFile();
        System.setProperty(org.nuxeo.common.Environment.NUXEO_LOG_DIR,
                getLogDir().getPath());
        return Log4JHelper.getFileAppendersFiles(log4jConfFile);
    }

    /**
     * Check if wizard must and can be ran
     *
     * @return true if configuration wizard is required before starting Nuxeo
     * @since 5.4.2
     */
    public boolean isWizardRequired() {
        return !"true".equalsIgnoreCase(getUserConfig().getProperty(
                PARAM_WIZARD_DONE, "true"))
                && serverConfigurator.isWizardAvailable();
    }

    /**
     * Rebuild a templates string for use in nuxeo.conf
     *
     * @param dbTemplate database template to use instead of current one
     * @return new templates string using given dbTemplate
     * @since 5.4.2
     * @see #extractDatabaseTemplateName()
     * @see {@link #changeDBTemplate(String)}
     * @see {@link #changeTemplates(String)}
     */
    public String rebuildTemplatesStr(String dbTemplate) {
        String currentDBTemplate = userConfig.getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME);
        if (currentDBTemplate == null) {
            currentDBTemplate = extractDatabaseTemplateName();
        }
        List<String> templatesList = new ArrayList<>();
        templatesList.addAll(Arrays.asList(templates.split(TEMPLATE_SEPARATOR)));
        int dbIdx = templatesList.indexOf(currentDBTemplate);
        if (dbIdx < 0) {
            // current db template is implicit => override it
            templatesList.add(dbTemplate);
        } else {
            // current db template is explicit => replace it
            templatesList.set(dbIdx, dbTemplate);
        }
        return StringUtils.join(templatesList, TEMPLATE_SEPARATOR);
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
     * @return File pointing to the directory containing the marketplace
     *         packages included in the distribution
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
     * Add template(s) to the {@link #PARAM_TEMPLATES_NAME} list if not already
     * present
     *
     * @param templatesToAdd Comma separated templates to add
     * @throws ConfigurationException
     * @since 5.5
     */
    public void addTemplate(String templatesToAdd)
            throws ConfigurationException {
        String currentTemplatesStr = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        List<String> templatesList = new ArrayList<>();
        templatesList.addAll(Arrays.asList(currentTemplatesStr.split(TEMPLATE_SEPARATOR)));
        List<String> templatesToAddList = Arrays.asList(templatesToAdd.split(TEMPLATE_SEPARATOR));
        if (templatesList.addAll(templatesToAddList)) {
            String newTemplatesStr = StringUtils.join(templatesList,
                    TEMPLATE_SEPARATOR);
            HashMap<String, String> parametersToSave = new HashMap<>();
            parametersToSave.put(PARAM_TEMPLATES_NAME, newTemplatesStr);
            saveFilteredConfiguration(parametersToSave);
            changeTemplates(newTemplatesStr);
        }
    }

    /**
     * Remove template(s) from the {@link #PARAM_TEMPLATES_NAME} list
     *
     * @param templates Comma separated templates to remove
     * @throws ConfigurationException
     * @since 5.5
     */
    public void rmTemplate(String templatesToRm) throws ConfigurationException {
        String currentTemplatesStr = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        List<String> templatesList = new ArrayList<>();
        templatesList.addAll(Arrays.asList(currentTemplatesStr.split(TEMPLATE_SEPARATOR)));
        List<String> templatesToRmList = Arrays.asList(templatesToRm.split(TEMPLATE_SEPARATOR));
        if (templatesList.removeAll(templatesToRmList)) {
            String newTemplatesStr = StringUtils.join(templatesList,
                    TEMPLATE_SEPARATOR);
            HashMap<String, String> parametersToSave = new HashMap<>();
            parametersToSave.put(PARAM_TEMPLATES_NAME, newTemplatesStr);
            saveFilteredConfiguration(parametersToSave);
            changeTemplates(newTemplatesStr);
        }
    }

    /**
     * Set a property in nuxeo configuration
     *
     * @param key
     * @param value
     * @throws ConfigurationException
     * @return The old value
     * @since 5.5
     */
    public String setProperty(String key, String value)
            throws ConfigurationException {
        String oldValue = getStoredConfig().getProperty(key);
        HashMap<String, String> newParametersToSave = new HashMap<>();
        newParametersToSave.put(key, value);
        saveFilteredConfiguration(newParametersToSave);
        setBasicConfiguration();
        return oldValue;
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
     * @throws DatabaseDriverException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws SQLException
     * @since 5.6
     */
    public void checkDatabaseConnection(String databaseTemplate, String dbName,
            String dbUser, String dbPassword, String dbHost, String dbPort)
            throws FileNotFoundException, IOException, DatabaseDriverException,
            SQLException {
        File databaseTemplateDir = new File(nuxeoHome, TEMPLATES
                + File.separator + databaseTemplate);
        Properties templateProperties = loadTrimmedProperties(new File(
                databaseTemplateDir, NUXEO_DEFAULT_CONF));
        String classname, connectionUrl;
        if (userConfig.getProperty(PARAM_TEMPLATE_DBNAME).equals(
                databaseTemplateDir)) {
            // userConfig already includes databaseTemplate
            classname = userConfig.getProperty(PARAM_DB_DRIVER);
            connectionUrl = userConfig.getProperty(PARAM_DB_JDBC_URL);
        } else { // testing a databaseTemplate not included in userConfig
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
        }
        // Load driver class from template or default lib directory
        Driver driver = lookupDriver(databaseTemplate, databaseTemplateDir,
                classname);
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
     * Build an {@link URLClassLoader} for the given databaseTemplate looking
     * in the templates directory and in the server lib directory, then looks
     * for a driver
     *
     * @param databaseTemplate
     * @param databaseTemplateDir
     * @param classname Driver class name, defined by {@link #PARAM_DB_DRIVER}
     * @return Driver driver if found, else an Exception must have been raised.
     * @throws IOException
     * @throws FileNotFoundException
     * @throws DatabaseDriverException If there was an error when trying to
     *             instantiate the driver.
     * @since 5.6
     */
    private Driver lookupDriver(String databaseTemplate,
            File databaseTemplateDir, String classname)
            throws FileNotFoundException, IOException, DatabaseDriverException {
        File[] files = (File[]) ArrayUtils.addAll( //
                new File(databaseTemplateDir, "lib").listFiles(), //
                serverConfigurator.getServerLibDir().listFiles());
        List<URL> urlsList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("jar")) {
                    try {
                        urlsList.add(new URL("jar:file:" + file.getPath()
                                + "!/"));
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
        } catch (InstantiationException e) {
            throw new DatabaseDriverException(e);
        } catch (IllegalAccessException e) {
            throw new DatabaseDriverException(e);
        } catch (ClassNotFoundException e) {
            throw new DatabaseDriverException(e);
        }
    }

    /**
     * @since 5.6
     * @return an {@link Environment} initialized with a few basics
     */
    public Environment getEnv() {
        /*
         * It could be useful to initialize DEFAULT env in {@link
         * #setBasicConfiguration()}... For now, the generated {@link
         * Environment} is not static.
         */
        if (env == null) {
            env = new Environment(getRuntimeHome());
            env.init();
            env.setServerHome(getNuxeoHome());
            env.setData(new File(
                    userConfig.getProperty(Environment.NUXEO_DATA_DIR)));
            env.setLog(new File(
                    userConfig.getProperty(Environment.NUXEO_LOG_DIR)));
            env.setTemp(new File(
                    userConfig.getProperty(Environment.NUXEO_TMP_DIR)));
            File distribFile = new File(new File(nuxeoHome, TEMPLATES),
                    "common/config/distribution.properties");
            if (distribFile.exists()) {
                try {
                    env.loadProperties(loadTrimmedProperties(distribFile));
                } catch (FileNotFoundException e) {
                    log.error(e);
                } catch (IOException e) {
                    log.error(e);
                }
            }
            env.loadProperties(userConfig);
            env.setProperty(PARAM_MP_DIR,
                    getDistributionMPDir().getAbsolutePath());
            env.setProperty(Environment.NUXEO_MP_DIR,
                    getPackagesDir().getAbsolutePath());
        }
        return env;
    }

    /**
     * @since 5.6
     * @param props Properties object to be filled
     * @param propsFile Properties file
     * @return new Properties containing trimmed keys and values read in
     *         {@code propsFile}
     * @throws IOException
     */
    public static Properties loadTrimmedProperties(File propsFile)
            throws IOException {
        Properties props = new Properties();
        FileInputStream propsIS = new FileInputStream(propsFile);
        try {
            loadTrimmedProperties(props, propsIS);
        } finally {
            propsIS.close();
        }
        return props;
    }

    /**
     * @since 5.6
     * @param props Properties object to be filled
     * @param propsIS Properties InputStream
     * @throws IOException
     */
    public static void loadTrimmedProperties(Properties props,
            InputStream propsIS) throws IOException {
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
     * Build a {@link Hashtable} which contains environment properties to
     * instantiate a {@link InitialDirContext}
     *
     * @since 5.9.6
     */
    public Hashtable<Object, Object> getContextEnv(String ldapUrl,
            String bindDn, String bindPassword, boolean checkAuthentication) {
        Hashtable<Object, Object> contextEnv = new Hashtable<>();
        contextEnv.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        contextEnv.put("com.sun.jndi.ldap.connect.timeout", "10000");
        contextEnv.put(javax.naming.Context.PROVIDER_URL, ldapUrl);
        if (checkAuthentication) {
            contextEnv.put(javax.naming.Context.SECURITY_AUTHENTICATION,
                    "simple");
            contextEnv.put(javax.naming.Context.SECURITY_PRINCIPAL, bindDn);
            contextEnv.put(javax.naming.Context.SECURITY_CREDENTIALS,
                    bindPassword);
        }
        return contextEnv;
    }

    /**
     * Check if the LDAP parameters are correct to bind to a LDAP server. if
     * authenticate argument is true, it will also check if the authentication
     * against the LDAP server succeeds
     *
     * @param ldapUrl
     * @param ldapBindDn
     * @param ldapBindPwd
     * @param authenticate Indicates if authentication against LDAP should be
     *            checked.
     * @since 5.9.6
     */
    public void checkLdapConnection(String ldapUrl, String ldapBindDn,
            String ldapBindPwd, boolean authenticate) throws NamingException {
        checkLdapConnection(getContextEnv(ldapUrl, ldapBindDn, ldapBindPwd,
                authenticate));
    }

    /**
     * @oaram env Environment properties to build a {@link InitialDirContext}
     * @since 5.9.6
     */
    public void checkLdapConnection(Hashtable<Object, Object> contextEnv)
            throws NamingException {
        DirContext dirContext = new InitialDirContext(contextEnv);
        dirContext.close();
    }
}
