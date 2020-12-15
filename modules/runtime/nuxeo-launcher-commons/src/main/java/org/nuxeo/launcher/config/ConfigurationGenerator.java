/*
 * (C) Copyright 2010-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Frantz Fischer <ffischer@nuxeo.com>
 */
package org.nuxeo.launcher.config;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.launcher.config.ServerConfigurator.PARAM_HTTP_TOMCAT_ADMIN_PORT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
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

    private static final Logger log = LogManager.getLogger(ConfigurationGenerator.class);

    /** @since 11.1 */
    public static final String NUXEO_ENVIRONMENT = "NUXEO_ENVIRONMENT";

    /** @since 11.1 */
    public static final String NUXEO_PROFILES = "NUXEO_PROFILES";

    /**
     * @since 6.0
     * @implNote also used for profiles
     */
    public static final String TEMPLATE_SEPARATOR = ",";

    /**
     * Accurate but not used internally. NXP-18023: Java 8 update 40+ required
     *
     * @since 5.7
     */
    public static final String[] COMPLIANT_JAVA_VERSIONS = new String[] { "1.8.0_40", "11" };

    /** @since 5.6 */
    protected static final String CONFIGURATION_PROPERTIES = "configuration.properties";

    public static final String NUXEO_CONF = "nuxeo.conf";

    public static final String TEMPLATES = "templates";

    public static final String NUXEO_DEFAULT_CONF = "nuxeo.defaults";

    /** @since 11.1 */
    public static final String NUXEO_ENVIRONMENT_CONF_FORMAT = "nuxeo.%s";

    /**
     * Absolute or relative PATH to the user chosen templates (comma separated list)
     */
    public static final String PARAM_TEMPLATES_NAME = "nuxeo.templates";

    public static final String PARAM_TEMPLATE_DBNAME = "nuxeo.dbtemplate";

    /** @since 9.3 */
    public static final String PARAM_TEMPLATE_DBSECONDARY_NAME = "nuxeo.dbnosqltemplate";

    public static final String PARAM_TEMPLATE_DBTYPE = "nuxeo.db.type";

    /** @since 9.3 */
    public static final String PARAM_TEMPLATE_DBSECONDARY_TYPE = "nuxeo.dbsecondary.type";

    public static final String OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.templates.parsing.extensions";

    public static final String PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.plaintext_parsing_extensions";

    public static final String PARAM_TEMPLATES_FREEMARKER_EXTENSIONS = "nuxeo.freemarker_parsing_extensions";

    /** Absolute or relative PATH to the included templates (comma separated list). */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    public static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String BOUNDARY_BEGIN = "### BEGIN - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final String BOUNDARY_END = "### END - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final List<String> DB_LIST = asList("default", "mongodb", "postgresql", "oracle", "mysql", "mariadb",
            "mssql", "db2");

    public static final List<String> DB_SECONDARY_LIST = singletonList("none");

    public static final List<String> DB_EXCLUDE_CHECK_LIST = asList("default", "none", "mongodb");

    /**
     * @deprecated since 11.1, Nuxeo Wizard has been removed.
     */
    @Deprecated(since = "11.1")
    public static final String PARAM_WIZARD_DONE = "nuxeo.wizard.done";

    /**
     * @deprecated since 11.1, Nuxeo Wizard has been removed.
     */
    @Deprecated(since = "11.1")
    public static final String PARAM_WIZARD_RESTART_PARAMS = "wizard.restart.params";

    public static final String PARAM_LOOPBACK_URL = "nuxeo.loopback.url";

    public static final int MIN_PORT = 1;

    public static final int MAX_PORT = 65535;

    public static final int ADDRESS_PING_TIMEOUT = 1000;

    public static final String PARAM_BIND_ADDRESS = "nuxeo.bind.address";

    public static final String PARAM_HTTP_PORT = "nuxeo.server.http.port";

    public static final String PARAM_CONTEXT_PATH = "org.nuxeo.ecm.contextPath";

    /**
     * @deprecated since 11.1, Nuxeo Wizard has been removed.
     */
    @Deprecated(since = "11.1")
    public static final String PARAM_MP_DIR = "nuxeo.distribution.marketplace.dir";

    /**
     * @deprecated since 11.1, Nuxeo Wizard has been removed.
     */
    @Deprecated(since = "11.1")
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
     * @deprecated since 11.1, seems unused
     */
    @Deprecated(since = "11.1")
    public static final String PARAM_MONGODB_NAME = "nuxeo.mongodb.dbname";

    /**
     * @since 8.1
     * @deprecated since 11.1, seems unused
     */
    @Deprecated(since = "11.1")
    public static final String PARAM_MONGODB_SERVER = "nuxeo.mongodb.server";

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

    /** @since 5.6 */
    public static final String PARAM_NUXEO_URL = "nuxeo.url";

    /**
     * Global dev property, duplicated from runtime framework
     *
     * @since 5.6
     */
    public static final String NUXEO_DEV_SYSTEM_PROP = "org.nuxeo.dev";

    /** @since 8.4 */
    public static final String JVMCHECK_PROP = "jvmcheck";

    /** @since 8.4 */
    public static final String JVMCHECK_FAIL = "fail";

    /** @since 8.4 */
    public static final String JVMCHECK_NOFAIL = "nofail";

    /**
     * Java options configured in {@code bin/nuxeo.conf} and {@code bin/nuxeoctl}.
     *
     * @since 9.3
     */
    public static final String JAVA_OPTS_PROP = "launcher.java.opts";

    public static final String VERSIONED_REGEX = "(-\\d+(\\.\\d+)*)?";

    public static final String BOOTSTRAP_JAR_REGEX = "bootstrap" + VERSIONED_REGEX + ".jar";

    public static final String JULI_JAR_REGEX = "tomcat-juli" + VERSIONED_REGEX + ".jar";

    /** @since 11.5 */
    protected static final Path DEFAULT_NUXEO_CONF_PATH = Path.of("bin", "nuxeo.conf");

    /** Environment used to load the configuration, generally {@link System#getenv()}. */
    private final Map<String, String> environment;

    /** @since 11.5 */
    private final ConfigurationHolder configHolder;

    /** @since 11.5 */
    private final ConfigurationLoader configLoader;

    private final ServerConfigurator serverConfigurator;

    private final BackingServiceConfigurator backingServicesConfigurator;

    private boolean forceGeneration;

    private boolean onceGeneration = false;

    // if PARAM_FORCE_GENERATION=once, set to false; else keep current value
    private boolean setOnceToFalse = true;

    // if PARAM_FORCE_GENERATION=false, set to once; else keep the current value
    private boolean setFalseToOnce = false;

    private final Level logLevel;

    private static boolean hideDeprecationWarnings = false;

    private Environment env;

    private Properties storedConfig;

    private String currentConfigurationDigest;

    protected static final Map<String, String> parametersMigration = Map.ofEntries(
            Map.entry(OLD_PARAM_TEMPLATES_PARSING_EXTENSIONS, PARAM_TEMPLATES_PARSING_EXTENSIONS), //
            Map.entry("nuxeo.db.user.separator.key", "nuxeo.db.user_separator_key"), //
            Map.entry("mail.pop3.host", "mail.store.host"), //
            Map.entry("mail.pop3.port", "mail.store.port"), //
            Map.entry("mail.smtp.host", "mail.transport.host"), //
            Map.entry("mail.smtp.port", "mail.transport.port"), //
            Map.entry("mail.smtp.username", "mail.transport.username"), //
            Map.entry("mail.transport.username", "mail.transport.user"), //
            Map.entry("mail.smtp.password", "mail.transport.password"), //
            Map.entry("mail.smtp.usetls", "mail.transport.usetls"), //
            Map.entry("mail.smtp.auth", "mail.transport.auth"), //
            Map.entry("nuxeo.server.tomcat-admin.port", PARAM_HTTP_TOMCAT_ADMIN_PORT));

    public ConfigurationGenerator() {
        this(true, false);
    }

    /**
     * @param quiet Suppress info level messages from the console output
     * @param debug Activate debug level logging
     * @since 5.6
     */
    public ConfigurationGenerator(boolean quiet, boolean debug) {
        logLevel = quiet ? Level.DEBUG : Level.INFO;
        environment = System.getenv();
        File serverHome = Environment.getDefault().getServerHome();
        Path nuxeoHome;
        if (serverHome != null) {
            nuxeoHome = serverHome.toPath();
        } else {
            nuxeoHome = Path.of(System.getProperty("user.dir"));
            if ("bin".equalsIgnoreCase(nuxeoHome.getFileName().toString())) {
                nuxeoHome = nuxeoHome.getParent();
            }
        }
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoConfPath == null) {
            configHolder = new ConfigurationHolder(nuxeoHome, nuxeoHome.resolve(DEFAULT_NUXEO_CONF_PATH));
        } else {
            configHolder = new ConfigurationHolder(nuxeoHome, Path.of(nuxeoConfPath));
        }
        System.setProperty(NUXEO_CONF, configHolder.getNuxeoConfPath().toString());

        configLoader = new ConfigurationLoader(System.getenv(), parametersMigration);

        serverConfigurator = new ServerConfigurator(this, configHolder);
        if (LoggerContext.getContext(false).getRootLogger().getAppenders().isEmpty()) {
            serverConfigurator.initLogs();
        }
        backingServicesConfigurator = new BackingServiceConfigurator(this);
        log.log(logLevel, "Nuxeo home:          {}", configHolder::getHomePath);
        log.log(logLevel, "Nuxeo configuration: {}", configHolder::getNuxeoConfPath);
        String nuxeoProfiles = environment.get(NUXEO_PROFILES);
        if (StringUtils.isNotBlank(nuxeoProfiles)) {
            log.log(logLevel, "Nuxeo profiles:      {}", nuxeoProfiles);
        }
    }

    /**
     * @since 5.7
     */
    protected Properties getStoredConfig() {
        if (storedConfig == null) {
            updateStoredConfig();
        }
        return storedConfig;
    }

    public void hideDeprecationWarnings(boolean hide) {
        hideDeprecationWarnings = hide;
    }

    /**
     * @since 11.5
     */
    protected boolean isGenerationOnce() {
        return "once".equals(configHolder.getProperty(PARAM_FORCE_GENERATION));
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
        return configHolder.userConfig;
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
                log.info("Configuration files generation (nuxeo.force.generation={})...",
                        () -> configHolder.getProperty(PARAM_FORCE_GENERATION));
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
        if (Files.notExists(configHolder.getNuxeoConfPath())) {
            log.info("Missing {}", configHolder::getNuxeoConfPath);
            return false;
        } else if (!configHolder.isLoaded() || forceReload) {
            try {
                loadConfiguration(true);
            } catch (ConfigurationException e) {
                log.warn("Error reading basic configuration.", e);
                return false;
            }
        }
        return configHolder.isLoaded();
    }

    private void loadConfiguration(boolean evalDynamicProperties) throws ConfigurationException {
        configHolder.clear();

        var templatesPath = configHolder.getTemplatesPath();
        if (Files.notExists(templatesPath.resolve(NUXEO_DEFAULT_CONF))) {
            throw new ConfigurationException("Missing nuxeo.defaults configuration in: " + templatesPath);
        }

        // Load default configuration
        configHolder.putDefaultAll(configLoader.loadNuxeoDefaults(templatesPath));
        // Load System properties
        configHolder.putDefaultAll(System.getProperties());
        // Load user configuration
        configHolder.putAll(configLoader.loadProperties(configHolder.getNuxeoConfPath()));
        forceGeneration = isGenerationOnce()
                || Boolean.parseBoolean(configHolder.getProperty(PARAM_FORCE_GENERATION, "false"));

        // Override default configuration with specific configuration(s) of
        // the chosen template(s) which can be outside of server filesystem
        includeTemplates();
        if (evalDynamicProperties) {
            Map<String, String> newParametersToSave = evalDynamicProperties();
            if (newParametersToSave != null && !newParametersToSave.isEmpty()) {
                saveConfiguration(newParametersToSave, false, false);
            }
        }
        if (configHolder.getPropertyAsBoolean(NUXEO_DEV_SYSTEM_PROP)) {
            log.warn("Nuxeo Dev mode is enabled");
        }
    }

    /**
     * @since 5.7
     */
    protected void includeTemplates() throws ConfigurationException {
        String templates = configHolder.getProperty(PARAM_TEMPLATES_NAME);
        if (isBlank(templates)) {
            log.warn("No template found in configuration! Fallback on 'default'.");
            templates = "default";
            configHolder.put(PARAM_TEMPLATES_NAME, templates);
        }
        String profiles = environment.get(NUXEO_PROFILES);
        if (StringUtils.isNotBlank(profiles)) {
            templates += TEMPLATE_SEPARATOR + profiles;
        }
        includeTemplates(templates, new HashSet<>());
        log.debug("Templates included: {}", configHolder::getIncludedTemplates);
    }

    /**
     * Generate properties which values are based on others
     *
     * @return Map with new parameters to save in {@code nuxeoConf}
     * @since 5.5
     */
    protected Map<String, String> evalDynamicProperties() throws ConfigurationException {
        Map<String, String> newParametersToSave = new HashMap<>();
        evalLoopbackURL();
        evalServerStatusKey(newParametersToSave);
        return newParametersToSave;
    }

    /**
     * Generate a server status key if not already set
     *
     * @see Environment#SERVER_STATUS_KEY
     * @since 5.5
     */
    private void evalServerStatusKey(Map<String, String> newParametersToSave) {
        if (configHolder.getOptProperty(Environment.SERVER_STATUS_KEY).isEmpty()) {
            newParametersToSave.put(Environment.SERVER_STATUS_KEY, UUID.randomUUID().toString().substring(0, 8));
        }
    }

    private void evalLoopbackURL() throws ConfigurationException {
        String loopbackURL = configHolder.getProperty(PARAM_LOOPBACK_URL);
        if (loopbackURL != null) {
            log.debug("Using configured loop back url: {}", loopbackURL);
            return;
        }
        InetAddress bindAddress = getBindAddress();
        String httpPort = configHolder.getProperty(PARAM_HTTP_PORT);
        String contextPath = configHolder.getProperty(PARAM_CONTEXT_PATH);
        // Is IPv6 or IPv4 ?
        if (bindAddress instanceof Inet6Address) {
            loopbackURL = "http://[" + bindAddress.getHostAddress() + "]:" + httpPort + contextPath;
        } else {
            loopbackURL = "http://" + bindAddress.getHostAddress() + ":" + httpPort + contextPath;
        }
        log.debug("Set as loop back URL: {}", loopbackURL);
        configHolder.putDefault(PARAM_LOOPBACK_URL, loopbackURL);
    }

    /**
     * @since 5.4.2
     * @param key Directory system key
     * @see Environment
     */
    public void setDirectoryWithProperty(String key) {
        String directory = configHolder.getProperty(key);
        if (directory == null) {
            configHolder.defaultConfig.setProperty(key, serverConfigurator.getDirectory(key).getPath());
        } else {
            serverConfigurator.setDirectory(key, directory);
        }
    }

    protected void generateFiles() throws ConfigurationException {
        try {
            serverConfigurator.parseAndCopy(configHolder.userConfig);
            serverConfigurator.dumpProperties(configHolder.userConfig);
            log.info("Configuration files generated.");
            // keep true or false, switch once to false
            if (isGenerationOnce()) {
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

    private List<Path> includeTemplates(String templatesList, Set<Path> includedTemplates)
            throws ConfigurationException {
        List<Path> orderedTemplates = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(templatesList, TEMPLATE_SEPARATOR);
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            Path chosenTemplate = Path.of(nextToken);
            if (!chosenTemplate.isAbsolute() || Files.notExists(chosenTemplate)) {
                chosenTemplate = configHolder.getTemplatesPath().resolve(nextToken);
            }
            if (includedTemplates.contains(chosenTemplate)) {
                log.debug("Already included {}", nextToken);
                continue;
            }
            if (Files.notExists(chosenTemplate)) {
                log.error(
                        "Template '{}' not found with relative or absolute path ({}). "
                                + "Check your {} parameter, and {} for included files.",
                        nextToken, chosenTemplate, PARAM_TEMPLATES_NAME, PARAM_INCLUDED_TEMPLATES);
                continue;
            }
            includedTemplates.add(chosenTemplate);
            if (Files.notExists(chosenTemplate.resolve(NUXEO_DEFAULT_CONF))) {
                log.warn("Ignore template (no default configuration): {}", nextToken);
                continue;
            }

            Properties templateProperties = configLoader.loadNuxeoDefaults(chosenTemplate);
            String subTemplatesList = templateProperties.getProperty(PARAM_INCLUDED_TEMPLATES);
            if (StringUtils.isNotBlank(subTemplatesList)) {
                orderedTemplates.addAll(includeTemplates(subTemplatesList, includedTemplates));
            }
            // Load configuration from chosen templates
            configHolder.putTemplateAll(chosenTemplate, templateProperties);
            orderedTemplates.add(chosenTemplate);
            log.log(logLevel, "Include template: {}", chosenTemplate);
        }
        return orderedTemplates;
    }

    public File getNuxeoHome() {
        return configHolder.getHomePath().toFile();
    }

    public File getNuxeoBinDir() {
        return configHolder.getHomePath().resolve("bin").toFile();
    }

    public List<File> getIncludedTemplates() {
        return configHolder.getIncludedTemplates().stream().map(Path::toFile).collect(Collectors.toUnmodifiableList());
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
     * {@link #PARAM_TEMPLATES_NAME} and {@link #PARAM_FORCE_GENERATION} cannot be unset, but their value can be
     * changed.
     * <p>
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
                configHolder.userConfig.remove(key);
            }
        }
        configHolder.userConfig.putAll(changedParameters);
        writeConfiguration();
        updateStoredConfig();
    }

    private void updateStoredConfig() {
        if (storedConfig == null) {
            storedConfig = new Properties(configHolder.defaultConfig);
        } else {
            storedConfig.clear();
        }
        storedConfig.putAll(configHolder.userConfig);
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
        for (Entry<String, String> entry : changedParameters.entrySet()) {
            String key = entry.getKey();
            String oldParam = configHolder.getProperty(key);
            String newParam = StringUtils.trim(entry.getValue());
            if (oldParam == null && StringUtils.isNotEmpty(newParam)
                    || oldParam != null && !oldParam.trim().equals(newParam)) {
                if (PARAM_TEMPLATES_NAME.equals(key)) {
                    newParam = StringUtils.defaultIfEmpty(newParam, "default");
                }
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
        for (Object o : new TreeSet<>(configHolder.keySet())) {
            String key = (String) o;
            // Ignore parameters already stored in newContent
            if (PARAM_FORCE_GENERATION.equals(key) || PARAM_TEMPLATES_NAME.equals(key)) {
                continue;
            }
            String oldValue = storedConfig.getProperty(key, "");
            String newValue = configHolder.userConfig.getRawProperty(key, "");
            if (!newValue.equals(oldValue)) {
                newContent.write("#" + key + "=" + oldValue + System.getProperty("line.separator"));
                newContent.write(key + "=" + newValue + System.getProperty("line.separator"));
            }
        }
        newContent.write(BOUNDARY_END + System.getProperty("line.separator"));

        // Write file only if content has changed
        if (!Hex.encodeHexString(newContentDigest.digest()).equals(currentConfigurationDigest)) {
            try (Writer writer = Files.newBufferedWriter(configHolder.getNuxeoConfPath())) {
                writer.append(newContent.getBuffer());
            } catch (IOException e) {
                throw new ConfigurationException("Error writing in: " + configHolder.getNuxeoConfPath(), e);
            }
        }
    }

    private StringBuilder readConfiguration() throws ConfigurationException {
        // Will change templatesParam value instead of appending it
        String templatesParam = configHolder.getProperty(PARAM_TEMPLATES_NAME);
        Integer generationIndex = null, templatesIndex = null;
        List<String> newLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(configHolder.getNuxeoConfPath())) {
            String line;
            MessageDigest digest = DigestUtils.getMd5Digest();
            boolean onConfiguratorContent = false;
            while ((line = reader.readLine()) != null) {
                digest.update(line.getBytes());
                if (!onConfiguratorContent) {
                    if (!line.startsWith(BOUNDARY_BEGIN)) {
                        if (line.startsWith(PARAM_FORCE_GENERATION)) {
                            if (setOnceToFalse && isGenerationOnce()) {
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
                                if (configHolder.getProperty(key) != null) {
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
                            if (!value.equals(configHolder.getRawProperty(key))) {
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
            throw new ConfigurationException("Error reading " + configHolder.getNuxeoConfPath(), e);
        }
        StringBuilder newContent = new StringBuilder();
        for (String newLine : newLines) {
            newContent.append(newLine.trim()).append(System.lineSeparator());
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
        for (var templatePath : configHolder.getIncludedTemplates()) {
            String template = templatePath.getFileName().toString();
            if (knownDbList.contains(template)) {
                dbTemplate = template;
                found = true;
            }
        }
        String dbType = configHolder.getProperty(paramTemplateDbType);
        if (!found && dbType != null) {
            log.warn(String.format("Didn't find a known database template in the list but "
                    + "some template contributed a value for %s.", paramTemplateDbType));
            dbTemplate = dbType;
        }
        if (dbTemplate != null && !dbTemplate.equals(dbType)) {
            if (dbType == null) {
                log.warn(String.format("Missing value for %s, using %s", paramTemplateDbType, dbTemplate));
                configHolder.put(paramTemplateDbType, dbTemplate);
            } else {
                log.debug(String.format("Different values between %s (%s) and %s (%s)", paramTemplateDbName, dbTemplate,
                        paramTemplateDbType, dbType));
            }
        }
        if (dbTemplate == null) {
            configHolder.defaultConfig.remove(paramTemplateDbName);
        } else {
            configHolder.defaultConfig.setProperty(paramTemplateDbName, dbTemplate);
        }
        return dbTemplate;
    }

    /**
     * @return nuxeo.conf file used
     */
    public File getNuxeoConf() {
        return configHolder.getNuxeoConfPath().toFile();
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
        return configHolder.getLogPath().toFile();
    }

    /**
     * @return pid directory
     * @since 5.4.2
     */
    public File getPidDir() {
        return configHolder.getLogPath().toFile();
    }

    /**
     * @return Data directory
     * @since 5.4.2
     */
    public File getDataDir() {
        return configHolder.getDataPath().toFile();
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
        getLogDir().mkdirs();
        getPidDir().mkdirs();
        getDataDir().mkdirs();
        getTmpDir().mkdirs();
        getPackagesDir().mkdirs();
        checkAddressesAndPorts();
        serverConfigurator.verifyInstallation();
        backingServicesConfigurator.verifyInstallation();
    }

    /**
     * @return Marketplace packages directory
     * @since 5.9.4
     */
    private File getPackagesDir() {
        return configHolder.getPackagesPath().toFile();
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
        checkPortAvailable(bindAddress, Integer.parseInt(configHolder.getProperty(PARAM_HTTP_PORT)));
    }

    /**
     * Checks the userConfig bind address is not 0.0.0.0 and replaces it with 127.0.0.1 if needed
     *
     * @return the userConfig bind address if not 0.0.0.0 else 127.0.0.1
     * @since 5.7
     */
    public InetAddress getBindAddress() throws ConfigurationException {
        return getBindAddress(configHolder.getProperty(PARAM_BIND_ADDRESS));
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
                log.debug("Bind address is \"ANY\", using local address instead: {}", bindAddress);
            }
            log.debug("Configured bind address: {}", bindAddress);
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
        } catch (IllegalArgumentException | IOException e) {
            throw new ConfigurationException("Unreachable bind address " + address, e);
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
        if (port == 0 || port == -1) {
            log.warn("Port is set to {} - assuming it is disabled - skipping availability check", port);
            return;
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        log.debug("Checking availability of port {} on address {}", port, address);
        try (ServerSocket socketTCP = new ServerSocket(port, 0, address)) {
            socketTCP.setReuseAddress(true);
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage() + ": " + address + ":" + port, e);
        }
    }

    /**
     * @return Temporary directory
     */
    public File getTmpDir() {
        return configHolder.getTmpPath().toFile();
    }

    /**
     * @return Log files produced by Log4J configuration without loading this configuration instead of current active
     *         one.
     * @since 5.4.2
     */
    public List<String> getLogFiles() {
        File log4jConfFile = serverConfigurator.getLogConfFile();
        System.setProperty(Environment.NUXEO_LOG_DIR, getLogDir().getPath());
        return Log4JHelper.getFileAppendersFileNames(log4jConfFile);
    }

    /**
     * Rebuild a templates string for use in nuxeo.conf
     *
     * @param dbTemplate database template to use instead of current one
     * @return new templates string using given dbTemplate
     * @since 5.4.2
     * @see #extractDatabaseTemplateName()
     */
    public String rebuildTemplatesStr(String dbTemplate) {
        List<String> templatesList = new ArrayList<>(
                asList(configHolder.getProperty(PARAM_TEMPLATES_NAME).split(TEMPLATE_SEPARATOR)));
        String currentDBTemplate = null;
        if (DB_LIST.contains(dbTemplate)) {
            currentDBTemplate = configHolder.getProperty(PARAM_TEMPLATE_DBNAME);
            if (currentDBTemplate == null) {
                currentDBTemplate = extractDatabaseTemplateName();
            }
        } else if (DB_SECONDARY_LIST.contains(dbTemplate)) {
            currentDBTemplate = configHolder.getProperty(PARAM_TEMPLATE_DBSECONDARY_NAME);
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
                return configHolder.getProperty(PARAM_TEMPLATES_NAME);
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
        return configLoader.replaceEnvironmentVariables(String.join(TEMPLATE_SEPARATOR, templatesList));
    }

    /**
     * @return Nuxeo config directory
     * @since 5.4.2
     */
    public File getConfigDir() {
        return configHolder.getConfigurationPath().toFile();
    }

    /**
     * @return Nuxeo runtime home
     */
    public File getRuntimeHome() {
        return configHolder.getRuntimeHomePath().toFile();
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
     * @deprecated since 11.1, Nuxeo Wizard has been removed.
     */
    @Deprecated(since = "11.1", forRemoval = true) // not used
    public File getDistributionMPDir() {
        String mpDir = configHolder.getProperty(PARAM_MP_DIR, DISTRIBUTION_MP_DIR);
        return new File(getNuxeoHome(), mpDir);
    }

    /**
     * @return Install/upgrade file
     * @since 5.4.1
     */
    public File getInstallFile() {
        return configHolder.getDataPath().resolve(INSTALL_AFTER_RESTART).toFile();
    }

    /**
     * Add template(s) to the {@link #PARAM_TEMPLATES_NAME} list if not already present
     *
     * @param templatesToAdd Comma separated templates to add
     * @since 5.5
     */
    public void addTemplate(String templatesToAdd) throws ConfigurationException {
        String newTemplatesStr = Stream.concat(
                Stream.of(configHolder.getProperty(PARAM_TEMPLATES_NAME).split(TEMPLATE_SEPARATOR)),
                Stream.of(templatesToAdd.split(TEMPLATE_SEPARATOR)))
                                       .distinct()
                                       .collect(Collectors.joining(TEMPLATE_SEPARATOR));
        saveFilteredConfiguration(Map.of(PARAM_TEMPLATES_NAME, newTemplatesStr));
        loadConfiguration(false);
    }

    /**
     * Return the list of templates declared by {@link #PARAM_TEMPLATES_NAME}.
     *
     * @since 9.2
     */
    public List<String> getTemplateList() {
        String currentTemplatesStr = configHolder.getProperty(PARAM_TEMPLATES_NAME);

        return Stream.of(configLoader.replaceEnvironmentVariables(currentTemplatesStr).split(TEMPLATE_SEPARATOR))
                     .collect(Collectors.toList());

    }

    /**
     * Remove template(s) from the {@link #PARAM_TEMPLATES_NAME} list
     *
     * @param templatesToRm Comma separated templates to remove
     * @since 5.5
     */
    public void rmTemplate(String templatesToRm) throws ConfigurationException {
        Set<String> templatesToRmSet = Set.of(templatesToRm.split(TEMPLATE_SEPARATOR));
        String newTemplatesStr = Stream.of(configHolder.getProperty(PARAM_TEMPLATES_NAME).split(TEMPLATE_SEPARATOR))
                                       .filter(not(templatesToRmSet::contains))
                                       .collect(Collectors.joining(TEMPLATE_SEPARATOR));
        saveFilteredConfiguration(Map.of(PARAM_TEMPLATES_NAME, newTemplatesStr));
        loadConfiguration(false);
    }

    /**
     * Set a property in nuxeo configuration
     *
     * @return The old value
     * @since 5.5
     */
    public String setProperty(String key, String value) throws ConfigurationException {
        Map<String, String> newParametersToSave = new HashMap<>();
        newParametersToSave.put(key, value);
        return setProperties(newParametersToSave).get(key);
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
            oldValues.put(key, configHolder.getProperty(key));
        }
        saveFilteredConfiguration(newParametersToSave);
        loadConfiguration(true);
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
        File templateDir = getTemplateDirectory(template);
        File templateConf;
        String nuxeoEnv = environment.get(NUXEO_ENVIRONMENT);
        if (isBlank(nuxeoEnv)) {
            templateConf = new File(templateDir, NUXEO_DEFAULT_CONF);
        } else {
            templateConf = new File(templateDir, String.format(NUXEO_ENVIRONMENT_CONF_FORMAT, nuxeoEnv));
        }
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
                        String value = line.substring(equalIdx + 1).trim();
                        oldValues.put(key, value);
                        newContent.append(key)
                                  .append("=")
                                  .append(newParametersToSave.get(key))
                                  .append(System.lineSeparator());
                    } else {
                        newContent.append(line).append(System.lineSeparator());
                    }
                }
                line = reader.readLine();
            }
        }
        for (String key : newParametersToSave.keySet()) {
            if (!oldValues.containsKey(key)) {
                newContent.append(key).append("=").append(newParametersToSave.get(key)).append(System.lineSeparator());
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(templateConf))) {
            writer.append(newContent.toString());
        }
        loadConfiguration(true);
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
            String dbHost, String dbPort) throws ConfigurationException, DatabaseDriverException, SQLException {
        Path databaseTemplateDir = configHolder.getTemplatesPath().resolve(databaseTemplate);
        String classname = configHolder.getProperty(PARAM_DB_DRIVER);
        String connectionUrl = configHolder.getProperty(PARAM_DB_JDBC_URL);
        // Load driver class from template or default lib directory
        Driver driver = lookupDriver(databaseTemplateDir.toFile(), classname);
        // Test db connection
        DriverManager.registerDriver(driver);
        Properties ttProps = new Properties(configHolder.userConfig);
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
    private Driver lookupDriver(File databaseTemplateDir, String classname) throws DatabaseDriverException {
        File[] files = ArrayUtils.addAll( //
                new File(databaseTemplateDir, "lib").listFiles(), //
                configHolder.getHomePath().resolve("lib").toFile().listFiles());
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
            return (Driver) Class.forName(classname, true, ucl).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
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
            var distribPath = configHolder.getTemplatesPath().resolve("common/config/distribution.properties");
            if (Files.exists(distribPath)) {
                try {
                    env.loadProperties(configLoader.loadProperties(distribPath));
                } catch (ConfigurationException e) {
                    log.error(e);
                }
            }
            env.loadProperties(configHolder.userConfig);
            env.setServerHome(getNuxeoHome());
            env.init();
            env.setData(configHolder.getProperty(Environment.NUXEO_DATA_DIR, "data"));
            env.setLog(configHolder.getProperty(Environment.NUXEO_LOG_DIR, "logs"));
            env.setTemp(configHolder.getProperty(Environment.NUXEO_TMP_DIR, "tmp"));
            env.setPath(Environment.NUXEO_MP_DIR, getPackagesDir(), env.getServerHome());
        }
        return env;
    }

    /**
     * @return The generated properties file with dumped configuration.
     * @since 5.6
     */
    public File getDumpedConfig() {
        return configHolder.getDumpedConfigurationPath().toFile();
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
        return configHolder.userConfig.getCrypto();
    }

    /**
     * @param template path to configuration template directory
     * @return A {@code nuxeo.defaults} file if it exists.
     * @throws ConfigurationException if the template file is not found.
     * @since 7.4
     * @deprecated since 11.1, there's several configuration files, use {@link #getTemplateDirectory(String)} instead
     */
    @Deprecated(since = "11.1", forRemoval = true) // not used
    public File getTemplateConf(String template) throws ConfigurationException {
        return new File(getTemplateDirectory(template), NUXEO_DEFAULT_CONF);
    }

    /**
     * @throws ConfigurationException if the template directory is not valid
     * @since 11.1
     */
    public File getTemplateDirectory(String template) throws ConfigurationException {
        // look for template declared with a path
        var templatePath = Path.of(template);
        if (!templatePath.isAbsolute()) {
            templatePath = configHolder.getTemplatesPath().resolve(template);
        }
        if (Files.notExists(templatePath.resolve(NUXEO_DEFAULT_CONF))) {
            throw new ConfigurationException("Template not found: " + template);
        }
        return templatePath.toFile();
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
     * Gets the Java options defined in Nuxeo configuration files, e.g. {@code bin/nuxeo.conf} and {@code bin/nuxeoctl}.
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
     * Returns the {@link ConfigurationHolder} held by the generator.
     * <p>
     * This configuration could be empty if the {@link #init()} method hasn't been called.
     *
     * @return the {@link ConfigurationHolder} held by the generator.
     * @see #init()
     * @since 11.5
     */
    public ConfigurationHolder getConfigurationHolder() {
        return configHolder;
    }
}
