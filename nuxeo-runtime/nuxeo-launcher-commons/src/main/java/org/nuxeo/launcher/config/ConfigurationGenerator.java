/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.launcher.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;
import org.nuxeo.log4j.Log4JHelper;

/**
 * Builder for server configuration and datasource files from templates and
 * properties.
 *
 * @author jcarsique
 */
public class ConfigurationGenerator {

    private static final Log log = LogFactory.getLog(ConfigurationGenerator.class);

    public static final String NUXEO_HOME = "nuxeo.home";

    public static final String NUXEO_CONF = "nuxeo.conf";

    protected static final String TEMPLATES = "templates";

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

    public static final String PARAM_TEMPLATES_NODB = "nuxeo.nodbtemplates";

    public static final String PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.templates.parsing.extensions";

    /**
     * Absolute or relative PATH to the included templates (comma separated
     * list)
     */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    public static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String BOUNDARY_BEGIN = "### BEGIN - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final String BOUNDARY_END = "### END - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final List<String> DB_LIST = Arrays.asList("default",
            "postgresql", "oracle", "mysql", "mssql");

    public static final String PARAM_WIZARD_DONE = "nuxeo.wizard.done";

    public static final String PARAM_WIZARD_RESTART_PARAMS = "wizard.restart.params";

    public static final String PARAM_FAKE_WINDOWS = "org.nuxeo.fake.vindoz";

    private final File nuxeoHome;

    // User configuration file
    private final File nuxeoConf;

    // Chosen templates
    private final List<File> includedTemplates = new ArrayList<File>();

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

    // if PARAM_FORCE_GENERATION=false, set to once; else keep the current value
    private boolean setFalseToOnce = false;

    private String wizardParam = null;

    public boolean isConfigurable() {
        return configurable;
    }

    public ConfigurationGenerator() {
        String nuxeoHomePath = System.getProperty(NUXEO_HOME);
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
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
        if (nuxeoConfPath != null) {
            nuxeoConf = new File(nuxeoConfPath);
        } else {
            nuxeoConf = new File(nuxeoHome, "bin" + File.separator
                    + "nuxeo.conf");
        }
        nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES + File.separator
                + NUXEO_DEFAULT_CONF);

        // detect server type based on System properties
        isJBoss = System.getProperty("jboss.home.dir") != null;
        isJetty = System.getProperty("jetty.home") != null;
        isTomcat = System.getProperty("tomcat.home") != null;
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
        if (isJBoss) {
            serverConfigurator = new JBossConfigurator(this);
        } else if (isTomcat) {
            serverConfigurator = new TomcatConfigurator(this);
        } else if (isJetty) {
            serverConfigurator = new JettyConfigurator(this);
        }
        if (Logger.getRootLogger().getAllAppenders() instanceof NullEnumeration) {
            serverConfigurator.initLogs();
        }
        log.info("Nuxeo home:          " + nuxeoHome.getPath());
        log.info("Nuxeo configuration: " + nuxeoConf.getPath());
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
                        + userConfig.getProperty(PARAM_FORCE_GENERATION,
                                "false") + ")...");
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
        if (serverConfigurator == null) {
            log.warn("Unrecognized server. Considered as already configured.");
            configurable = false;
        } else if (!nuxeoConf.exists()) {
            log.info("Missing " + nuxeoConf);
            configurable = false;
        } else if (userConfig == null) {
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
            includedTemplates.clear();
            templates = newTemplates;
            setBasicConfiguration();
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
        try {
            // Load default configuration
            defaultConfig = new Properties();
            defaultConfig.load(new FileInputStream(nuxeoDefaultConf));
            userConfig = new Properties(defaultConfig);

            // Add useful system properties
            userConfig.putAll(System.getProperties());

            // If Windows, replace backslashes in paths
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                replaceBackslashes();
            }
            // Load user configuration
            userConfig.load(new FileInputStream(nuxeoConf));
            onceGeneration = "once".equals(userConfig.getProperty(PARAM_FORCE_GENERATION));
            forceGeneration = onceGeneration
                    || Boolean.parseBoolean(userConfig.getProperty(
                            PARAM_FORCE_GENERATION, "false"));

            // Manage directories set from (or set to) system properties
            setDirectoryWithProperty(Environment.NUXEO_DATA_DIR);
            setDirectoryWithProperty(Environment.NUXEO_LOG_DIR);
            setDirectoryWithProperty(Environment.NUXEO_PID_DIR);
            setDirectoryWithProperty(Environment.NUXEO_TMP_DIR);
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
            if (templates == null) {
                templates = getUserTemplates();
            }
            extractDatabaseTemplateName();
            includeTemplates(templates);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }
    }

    /**
     * Read nuxeo.conf, replace backslashes in paths and write new
     * nuxeo.conf
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
            userConfig.setProperty(key,
                    serverConfigurator.getDirectory(key).getPath());
        } else {
            serverConfigurator.setDirectory(key, directory);
        }
    }

    private String getUserTemplates() {
        String userTemplatesList = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        if (userTemplatesList == null) {
            // backward compliance: manage parameter for a single
            // template
            userTemplatesList = userConfig.getProperty(PARAM_TEMPLATE_NAME);
        }
        return userTemplatesList;
    }

    protected void generateFiles() throws ConfigurationException {
        try {
            serverConfigurator.parseAndCopy(userConfig);
            serverConfigurator.dumpProperties(userConfig);
            log.info("Configuration files generated.");
            // keep true or false, switch once to false
            if (onceGeneration) {
                setOnceToFalse = true;
                writeConfiguration(loadConfiguration());
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure", e);
        }
    }

    private void writeConfiguration(StringBuffer configuration)
            throws ConfigurationException {
        writeConfiguration(configuration, null);
    }

    private void includeTemplates(String templatesList) throws IOException {
        StringTokenizer st = new StringTokenizer(templatesList, ",");
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
            } else if (chosenTemplate.exists()) {
                File chosenTemplateConf = new File(chosenTemplate,
                        NUXEO_DEFAULT_CONF);
                if (chosenTemplateConf.exists()) {
                    Properties subTemplateConf = new Properties();
                    subTemplateConf.load(new FileInputStream(chosenTemplateConf));
                    String subTemplatesList = subTemplateConf.getProperty(PARAM_INCLUDED_TEMPLATES);
                    if (subTemplatesList != null
                            && subTemplatesList.length() > 0) {
                        includeTemplates(subTemplatesList);
                    }
                    // Load configuration from chosen templates
                    defaultConfig.load(new FileInputStream(chosenTemplateConf));
                    log.info("Include template: " + chosenTemplate.getPath());
                } else {
                    log.debug("No default configuration for template "
                            + nextToken);
                }
                includedTemplates.add(chosenTemplate);
            } else {
                log.error(String.format(
                        "Template '%s' not found with relative or absolute path (%s). "
                                + "Check your %s parameter, and %s for included files.",
                        nextToken, chosenTemplate, PARAM_TEMPLATES_NAME,
                        PARAM_INCLUDED_TEMPLATES));
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
     * Save changed parameters in {@code nuxeo.conf}.
     * This method does not check values in map. Use
     * {@link #saveFilteredConfiguration(Map)} for parameters filtering.
     *
     * @param changedParameters Map of modified parameters
     * @see #saveFilteredConfiguration(Map)
     */
    public void saveConfiguration(Map<String, String> changedParameters)
            throws ConfigurationException {
        // Keep true or once; switch false to once
        setOnceToFalse = false;
        setFalseToOnce = true;
        // Will change wizardParam value instead of appending it
        wizardParam = changedParameters.remove(PARAM_WIZARD_DONE);
        writeConfiguration(loadConfiguration(), changedParameters);
    }

    /**
     * Save changed parameters in {@code nuxeo.conf}, filtering parameters with
     * {@link #getChangedParametersMap(Map, Map)}
     *
     * @param changedParametersMaps Maps of modified parameters
     * @since 5.4.2
     * @see #getChangedParameters(Map)
     */
    public void saveFilteredConfiguration(Map<String, String> changedParameters)
            throws ConfigurationException {
        saveConfiguration(getChangedParameters(changedParameters));
    }

    /**
     * Filters given parameters including them only if (there was no previous
     * value and new value is not empty/null) or (there was a previous value and
     * it differs from the new value)
     *
     * @param changedParameters parameters to be filtered
     * @return filtered map
     * @since 5.4.2
     */
    public Map<String, String> getChangedParameters(
            Map<String, String> changedParameters) {
        Map<String, String> filteredChangedParameters = new HashMap<String, String>();
        for (String key : changedParameters.keySet()) {
            String oldParam = userConfig.getProperty(key);
            String newParam = changedParameters.get(key).trim();
            if (oldParam == null && !newParam.isEmpty() || oldParam != null
                    && !oldParam.trim().equals(newParam)) {
                filteredChangedParameters.put(key,
                        changedParameters.get(key).trim());
            }
        }
        return filteredChangedParameters;
    }

    private void writeConfiguration(StringBuffer newContent,
            Map<String, String> changedParameters)
            throws ConfigurationException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(nuxeoConf, false);
            // Copy back file content
            writer.append(newContent.toString());
            if (changedParameters != null && !changedParameters.isEmpty()) {
                // Write changed parameters
                writer.write(BOUNDARY_BEGIN + " " + new Date().toString()
                        + System.getProperty("line.separator"));
                for (String key : changedParameters.keySet()) {
                    writer.write("#" + key + "=" + userConfig.getProperty(key)
                            + System.getProperty("line.separator"));
                    writer.write(key + "=" + changedParameters.get(key)
                            + System.getProperty("line.separator"));
                }
                writer.write(BOUNDARY_END
                        + System.getProperty("line.separator"));
            }
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

    private StringBuffer loadConfiguration() throws ConfigurationException {
        StringBuffer newContent = new StringBuffer();
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
                        } else if (line.startsWith(PARAM_WIZARD_DONE)) {
                            if (wizardParam != null) {
                                line = PARAM_WIZARD_DONE + "=" + wizardParam;
                            }
                        }
                        newContent.append(line
                                + System.getProperty("line.separator"));
                    } else {
                        onConfiguratorContent = true;
                    }
                } else {
                    if (!line.startsWith(BOUNDARY_END)) {
                        // ignore previously generated content
                        continue;
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
        return newContent;
    }

    /**
     * Extract a database template from a list of templates.
     * Return the last one if there are multiples
     *
     * @see #rebuildTemplatesStr(String)
     */
    public String extractDatabaseTemplateName() {
        String dbTemplate = "unknown";
        String nodbTemplates = "";
        StringTokenizer st = new StringTokenizer(templates, ",");
        while (st.hasMoreTokens()) {
            String template = st.nextToken();
            if (DB_LIST.contains(template)) {
                dbTemplate = template;
            } else {
                nodbTemplates += template;
            }
        }
        userConfig.put(PARAM_TEMPLATES_NODB, nodbTemplates);
        userConfig.put(PARAM_TEMPLATE_DBNAME, dbTemplate);
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
     * Create needed directories.
     * Check existence of old paths.
     * If old paths have been found and they cannot be upgraded automatically,
     * then upgrading message is logged and error thrown.
     *
     * @throws ConfigurationException If a deprecated directory has been
     *             detected.
     *
     * @since 5.4.2
     */
    public void verifyInstallation() throws ConfigurationException {
        if (!System.getProperty("java.version").startsWith("1.6")) {
            String message = "Nuxeo requires Java 1.6 (detected "
                    + System.getProperty("java.version") + ").";
            if ("nofail".equalsIgnoreCase(System.getProperty("jvmcheck", "fail"))) {
                log.error(message);
            } else {
                throw new ConfigurationException(message);
            }
        }
        ifNotExistsAndIsDirectoryThenCreate(getLogDir());
        ifNotExistsAndIsDirectoryThenCreate(getPidDir());
        ifNotExistsAndIsDirectoryThenCreate(getDataDir());
        ifNotExistsAndIsDirectoryThenCreate(getTmpDir());
        serverConfigurator.checkPaths();
        serverConfigurator.removeExistingLocks();
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
        System.setProperty(Environment.NUXEO_LOG_DIR, getLogDir().getPath());
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
        String nodbTemplates = userConfig.getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NODB);
        if (nodbTemplates == null) {
            extractDatabaseTemplateName();
            nodbTemplates = userConfig.getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NODB);
        }
        String newTemplates = nodbTemplates.isEmpty() ? dbTemplate : dbTemplate
                + "," + nodbTemplates;
        return newTemplates;
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
     * @return Install/upgrade file
     * @since 5.4.1
     */
    public File getInstallFile() {
        return new File(serverConfigurator.getDataDir(),
                "installAfterRestart.log");
    }

}
