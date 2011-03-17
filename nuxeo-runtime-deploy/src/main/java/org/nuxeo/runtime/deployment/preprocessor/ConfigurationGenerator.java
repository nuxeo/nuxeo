/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.deployment.preprocessor;

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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;

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

    protected static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String BOUNDARY_BEGIN = "### BEGIN - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final String BOUNDARY_END = "### END - DO NOT EDIT BETWEEN BEGIN AND END ###";

    public static final List<String> DB_LIST = Arrays.asList("default",
            "postgresql", "oracle", "mysql", "mssql");

    private final File nuxeoHome;

    // User configuration file
    private final File nuxeoConf;

    // Chosen templates
    private final List<File> includedTemplates = new ArrayList<File>();

    // Common default configuration file
    private File nuxeoDefaultConf;

    private boolean isJBoss;

    private boolean isJetty;

    private boolean isTomcat;

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

    public boolean isConfigurable() {
        return configurable;
    }

    public ConfigurationGenerator() {
        String nuxeoHomePath = System.getProperty(NUXEO_HOME);
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoHomePath != null && nuxeoConfPath != null) {
            nuxeoHome = new File(nuxeoHomePath);
            nuxeoConf = new File(nuxeoConfPath);
            nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES + File.separator
                    + NUXEO_DEFAULT_CONF);
        } else {
            nuxeoHome = new File(System.getProperty("user.dir")).getParentFile();
            nuxeoConf = new File(nuxeoHome, "bin" + File.separator
                    + "nuxeo.conf");
        }
        log.info("Nuxeo configuration: " + nuxeoConf.getPath());

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
    }

    public void setForceGeneration(boolean forceGeneration) {
        this.forceGeneration = forceGeneration;
    }

    public Properties getUserConfig() {
        return userConfig;
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

    private void setBasicConfiguration() throws ConfigurationException {
        try {
            // Load default configuration
            defaultConfig = new Properties();
            defaultConfig.load(new FileInputStream(nuxeoDefaultConf));
            userConfig = new Properties(defaultConfig);

            // Add useful system properties
            userConfig.putAll(System.getProperties());

            // Load user configuration
            userConfig.load(new FileInputStream(nuxeoConf));
            onceGeneration = "once".equals(userConfig.getProperty(PARAM_FORCE_GENERATION));
            forceGeneration = onceGeneration
                    || Boolean.parseBoolean(userConfig.getProperty(
                            PARAM_FORCE_GENERATION, "false"));

            // Add data and log system properties
            userConfig.put(Environment.NUXEO_DATA_DIR,
                    System.getProperty(Environment.NUXEO_DATA_DIR));
            userConfig.put(Environment.NUXEO_LOG_DIR,
                    System.getProperty(Environment.NUXEO_LOG_DIR));
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
            log.debug("Loaded configuration: " + userConfig);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
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
     *
     * @param changedParameters Map of modified parameters
     */
    public void saveConfiguration(Map<String, String> changedParameters)
            throws ConfigurationException {
        // Keep true or once; switch false to once
        setOnceToFalse = false;
        setFalseToOnce = true;
        writeConfiguration(loadConfiguration(), changedParameters);
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
}
