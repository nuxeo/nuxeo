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

import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.launcher.config.ConfigurationConstants.ENV_NUXEO_ENVIRONMENT;
import static org.nuxeo.launcher.config.ConfigurationConstants.ENV_NUXEO_PROFILES;
import static org.nuxeo.launcher.config.ConfigurationConstants.FILE_NUXEO_CONF;
import static org.nuxeo.launcher.config.ConfigurationConstants.FILE_NUXEO_DEFAULTS;
import static org.nuxeo.launcher.config.ConfigurationConstants.FILE_TEMPLATE_DISTRIBUTION_PROPS;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_CONTEXT_PATH;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_FORCE_GENERATION;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_HTTP_PORT;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_HTTP_TOMCAT_ADMIN_PORT;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_LOOPBACK_URL;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_NUXEO_CONF;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_NUXEO_DEV;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_TEMPLATES_NAME;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_TEMPLATES_PARSING_EXTENSIONS;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.log4j.Log4JHelper;

/**
 * Builder for server configuration and datasource files from templates and properties.
 *
 * @author jcarsique
 */
public class ConfigurationGenerator {

    private static final Logger log = LogManager.getLogger(ConfigurationGenerator.class);

    /**
     * @since 6.0
     * @implNote also used for profiles
     */
    public static final String TEMPLATE_SEPARATOR = ",";

    /** @since 11.1 */
    public static final String NUXEO_ENVIRONMENT_CONF_FORMAT = "nuxeo.%s";

    /** Absolute or relative PATH to the included templates (comma separated list). */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    /**
     * Java options split by spaces followed by an even number of quotes (or zero).
     *
     * @since 9.3
     */
    protected static final Pattern JAVA_OPTS_PATTERN = Pattern.compile("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

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
    protected static final Path DEFAULT_NUXEO_CONF_PATH = Path.of("bin", FILE_NUXEO_CONF);

    /** Environment used to load the configuration, generally {@link System#getenv()}. */
    private final Map<String, String> environment;

    /**
     * System properties used to load the configuration, generally {@link System#getProperties()}.
     *
     * @since 11.5
     */
    protected final Properties systemProperties;

    /** @since 11.5 */
    private final ConfigurationHolder configHolder;

    /** @since 11.5 */
    private final ConfigurationLoader configLoader;

    /** @since 11.5 */
    private final ConfigurationMarshaller configMarshaller;

    /** @since 11.5 */
    private final ConfigurationChecker configChecker;

    private final Level logLevel;

    private Environment env;

    /**
     * @deprecated since 11.5, use {@link ConfigurationGenerator#build()} instead.
     */
    @Deprecated(since = "11.5")
    public ConfigurationGenerator() {
        this(builder());
    }

    /**
     * @param quiet Suppress info level messages from the console output
     * @param debug Activate debug level logging
     * @since 5.6
     * @deprecated since 11.5, use {@link ConfigurationGenerator#builder()} instead.
     */
    @Deprecated(since = "11.5")
    public ConfigurationGenerator(boolean quiet, boolean debug) {
        this(builder().quiet(quiet));
    }

    /**
     * @since 11.5
     */
    protected ConfigurationGenerator(Builder builder) {
        logLevel = builder.quiet ? Level.DEBUG : Level.INFO;
        environment = builder.environment;
        systemProperties = builder.systemProperties;
        configHolder = new ConfigurationHolder(builder.nuxeoHome, builder.nuxeoConf);
        configLoader = new ConfigurationLoader(builder.environment, builder.parametersMigration,
                builder.hideDeprecationWarnings);
        configMarshaller = new ConfigurationMarshaller(systemProperties);
        configChecker = new ConfigurationChecker(systemProperties);

        systemProperties.setProperty(PARAM_NUXEO_CONF, configHolder.getNuxeoConfPath().toString());

        initLogsIfNeeded(configHolder);

        log.log(logLevel, "Nuxeo home:          {}", configHolder::getHomePath);
        log.log(logLevel, "Nuxeo configuration: {}", configHolder::getNuxeoConfPath);
        String nuxeoProfiles = environment.get(ENV_NUXEO_PROFILES);
        if (StringUtils.isNotBlank(nuxeoProfiles)) {
            log.log(logLevel, "Nuxeo profiles:      {}", nuxeoProfiles);
        }
    }

    /**
     * @since 11.5
     */
    public static ConfigurationGenerator build() {
        return new Builder().build();
    }

    /**
     * @since 11.5
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes log configuration if it doesn't exist.
     * <p>
     * This is called in {@link ConfigurationGenerator#ConfigurationGenerator(Builder)}, so the given
     * {@link ConfigurationHolder configHolder} is not yet {@link ConfigurationHolder#isLoaded() loaded}.
     *
     * @since 11.5
     */
    protected static void initLogsIfNeeded(ConfigurationHolder configHolder) {
        if (LoggerContext.getContext(false).getRootLogger().getAppenders().isEmpty()) {
            // log config is relative to home path, which is populated at initialization
            Path logConfigPath = configHolder.getLogConfigPath();
            if (Files.notExists(logConfigPath)) {
                System.out.println("No logs configuration, will setup a basic one.");
                Configurator.initialize(new DefaultConfiguration());
            } else {
                System.out.println("Try to configure logs with configuration: " + logConfigPath);
                Configurator.initialize(Log4JHelper.newConfiguration(logConfigPath.toFile()));
            }
            log.info("Logs successfully configured.");
        }
    }

    public CryptoProperties getUserConfig() {
        return configHolder.userConfig;
    }

    /**
     * Runs the configuration files generation.
     */
    public void run() throws ConfigurationException {
        if (init()) {
            if (!configChecker.isConfigured(configHolder)) {
                log.info("No current configuration, generating files...");
                generateFiles();
            } else if (configHolder.isForceGeneration()) {
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
        if (Files.notExists(templatesPath.resolve(FILE_NUXEO_DEFAULTS))) {
            throw new ConfigurationException("Missing nuxeo.defaults configuration in: " + templatesPath);
        }

        // Load default configuration
        configHolder.putDefaultAll(configLoader.loadNuxeoDefaults(templatesPath));
        // Load System properties
        configHolder.putDefaultAll(systemProperties);
        // Load user configuration
        configHolder.putAll(configLoader.loadProperties(configHolder.getNuxeoConfPath()));
        // Override default configuration with specific configuration(s) of
        // the chosen template(s) which can be outside of server filesystem
        includeTemplates();
        if (evalDynamicProperties) {
            Map<String, String> newParametersToSave = evalDynamicProperties();
            if (newParametersToSave != null && !newParametersToSave.isEmpty()) {
                saveConfiguration(newParametersToSave, false, false);
            }
        }
        if (configHolder.getPropertyAsBoolean(PARAM_NUXEO_DEV)) {
            log.warn("Nuxeo Dev mode is enabled");
        }
    }

    /**
     * @since 5.7
     */
    protected void includeTemplates() throws ConfigurationException {
        // include nuxeo.templates
        String templates = configHolder.getProperty(PARAM_TEMPLATES_NAME);
        if (isBlank(templates)) {
            log.warn("No template found in configuration! Fallback on 'default'.");
            templates = "default";
            configHolder.put(PARAM_TEMPLATES_NAME, templates);
        }
        // include nuxeo.append.templates.*
        String templatesWildcard = configHolder.stringPropertyNames()
                                               .stream()
                                               .filter(k -> k.startsWith("nuxeo.append.templates."))
                                               .sorted()
                                               .map(configHolder::getProperty)
                                               .collect(Collectors.joining(TEMPLATE_SEPARATOR));
        if (isNotBlank(templatesWildcard)) {
            templates += TEMPLATE_SEPARATOR + templatesWildcard;
        }
        // include NUXEO_PROFILES
        String profiles = environment.get(ENV_NUXEO_PROFILES);
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
        InetAddress bindAddress = configChecker.getBindAddress(configHolder);
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

    protected void generateFiles() throws ConfigurationException {
        configMarshaller.dumpConfiguration(configHolder);
        log.info("Configuration files generated.");
        // keep true or false, switch once to false
        if (configHolder.isForceGenerationOnce()) {
            configHolder.put(PARAM_FORCE_GENERATION, "false");
            configMarshaller.persistNuxeoConf(configHolder);
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
            if (Files.notExists(chosenTemplate.resolve(FILE_NUXEO_DEFAULTS))) {
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
     * Save changed parameters in {@code nuxeo.conf}. If a parameter value is empty ("" or null), then the property is
     * unset. {@link ConfigurationConstants#PARAM_TEMPLATES_NAME} and
     * {@link ConfigurationConstants#PARAM_FORCE_GENERATION} cannot be unset, but their value can be changed.
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
        if (setGenerationOnceToFalse && configHolder.isForceGenerationOnce()) {
            configHolder.put(PARAM_FORCE_GENERATION, "false");
        } else if (setGenerationFalseToOnce && !configHolder.isForceGeneration()) {
            configHolder.put(PARAM_FORCE_GENERATION, "once");
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
        configMarshaller.persistNuxeoConf(configHolder);
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
            if (PARAM_FORCE_GENERATION.equals(key)) {
                // force generation should not be modifiable like this
                continue;
            }
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

    /**
     * Create needed directories. Check existence of old paths. If old paths have been found and they cannot be upgraded
     * automatically, then upgrading message is logged and error thrown.
     *
     * @throws ConfigurationException If a deprecated directory has been detected.
     * @since 5.4.2
     */
    public void verifyInstallation() throws ConfigurationException {
        try {
            createDirectoriesIfNotExist(configHolder.getLogPath());
            createDirectoriesIfNotExist(configHolder.getPidDirPath());
            createDirectoriesIfNotExist(configHolder.getDataPath());
            createDirectoriesIfNotExist(configHolder.getTmpPath());
            createDirectoriesIfNotExist(configHolder.getPackagesPath());
        } catch (IOException e) {
            throw new ConfigurationException("Unable to create server directories", e);
        }
        configChecker.verify(configHolder);
    }

    /**
     * @since 11.5
     */
    protected void createDirectoriesIfNotExist(Path path) throws IOException {
        // createDirectories throws an error if there's a symlink in the path, so check the existence
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Add template(s) to the {@link ConfigurationConstants#PARAM_TEMPLATES_NAME} list if not already present
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
     * Remove template(s) from the {@link ConfigurationConstants#PARAM_TEMPLATES_NAME} list
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
            throws ConfigurationException {
        Path templatePath = Path.of(template);
        if (!templatePath.isAbsolute()) {
            templatePath = configHolder.getTemplatesPath().resolve(template);
        }
        Path templateConf;
        String nuxeoEnv = environment.get(ENV_NUXEO_ENVIRONMENT);
        if (isBlank(nuxeoEnv)) {
            templateConf = templatePath.resolve(FILE_NUXEO_DEFAULTS);
        } else {
            templateConf = templatePath.resolve(String.format(NUXEO_ENVIRONMENT_CONF_FORMAT, nuxeoEnv));
        }
        Map<String, String> oldValues = configMarshaller.persistNuxeoDefaults(templateConf, newParametersToSave);
        loadConfiguration(true);
        return oldValues;
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
            env = new Environment(configHolder.getRuntimeHomePath().toFile());
            var distribPath = configHolder.getTemplatesPath().resolve(FILE_TEMPLATE_DISTRIBUTION_PROPS);
            if (Files.exists(distribPath)) {
                try {
                    env.loadProperties(configLoader.loadProperties(distribPath));
                } catch (ConfigurationException e) {
                    log.error(e);
                }
            }
            env.loadProperties(configHolder.userConfig);
            env.setServerHome(configHolder.getHomePath().toFile());
            env.init();
            env.setData(configHolder.getDataPath().toFile());
            env.setLog(configHolder.getLogPath().toFile());
            env.setTemp(configHolder.getTmpPath().toFile());
            env.setPath(Environment.NUXEO_MP_DIR, configHolder.getPackagesPath().toFile(), env.getServerHome());
        }
        return env;
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
     * Gets the Java options defined in Nuxeo configuration files, e.g. {@code bin/nuxeo.conf} and {@code bin/nuxeoctl}.
     *
     * @return the Java options.
     * @since 9.3
     */
    public List<String> getJavaOpts(Function<String, String> mapper) {
        return Arrays.stream(JAVA_OPTS_PATTERN.split(systemProperties.getProperty(JAVA_OPTS_PROP, "")))
                     .map(option -> StringSubstitutor.replace(option, getUserConfig()))
                     .map(mapper)
                     .collect(Collectors.toList());
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

    /**
     * @since 11.5
     */
    public static class Builder {

        protected Path nuxeoHome;

        protected Path nuxeoConf;

        protected Map<String, String> environment = System.getenv();

        protected Properties systemProperties = System.getProperties();

        protected boolean quiet = true;

        protected Map<String, String> parametersMigration = Map.ofEntries(
                Map.entry("nuxeo.templates.parsing.extensions", PARAM_TEMPLATES_PARSING_EXTENSIONS), //
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

        protected boolean hideDeprecationWarnings;

        protected boolean init;

        protected Builder() {
            // nothing
        }

        protected Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        protected Builder systemProperties(Properties systemProperties) {
            this.systemProperties = systemProperties;
            return this;
        }

        protected Builder putSystemProperty(String key, String value) {
            this.systemProperties.put(key, value);
            return this;
        }

        public Builder quiet(boolean quiet) {
            this.quiet = quiet;
            return this;
        }

        public Builder hideDeprecationWarnings(boolean hideDeprecationWarnings) {
            this.hideDeprecationWarnings = hideDeprecationWarnings;
            return this;
        }

        public Builder init(boolean init) {
            this.init = init;
            return this;
        }

        public ConfigurationGenerator build() {
            if (nuxeoHome == null) {
                // resolve nuxeoHome from System properties
                Environment nuxeoEnvironment = Environment.getDefault(systemProperties);
                if (nuxeoEnvironment != null && nuxeoEnvironment.getServerHome() != null) {
                    nuxeoHome = nuxeoEnvironment.getServerHome().toPath();
                } else {
                    nuxeoHome = Path.of(systemProperties.getProperty("user.dir"));
                    if ("bin".equalsIgnoreCase(nuxeoHome.getFileName().toString())) {
                        nuxeoHome = nuxeoHome.getParent();
                    }
                }
            }
            if (nuxeoConf == null) {
                // resolve nuxeoConf from System properties
                String nuxeoConfProperty = systemProperties.getProperty(PARAM_NUXEO_CONF);
                if (nuxeoConfProperty == null) {
                    nuxeoConf = nuxeoHome.resolve(DEFAULT_NUXEO_CONF_PATH);
                } else {
                    nuxeoConf = Path.of(nuxeoConfProperty);
                }
            }

            var generator = new ConfigurationGenerator(this);
            if (init) {
                generator.init();
            }
            return generator;
        }
    }
}
