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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    protected static final String PARAM_TEMPLATE_NAME = "nuxeo.template";

    /**
     * Absolute or relative PATH to the user chosen templates (comma separated
     * list)
     */
    protected static final String PARAM_TEMPLATES_NAME = "nuxeo.templates";

    /**
     * Absolute or relative PATH to the included templates (comma separated
     * list)
     */
    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    protected static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

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

    public ConfigurationGenerator() {
        String nuxeoHomePath = System.getProperty(NUXEO_HOME);
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoHomePath != null && nuxeoConfPath != null) {
            nuxeoHome = new File(nuxeoHomePath);
            nuxeoConf = new File(nuxeoConfPath);
            nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES
                    + File.separator + NUXEO_DEFAULT_CONF);
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
        if (userConfig == null) {
            try {
                setBasicConfiguration();
            } catch (ConfigurationException e) {
                log.warn(
                        "Error reading basic configuration. Server is considered as already configured.",
                        e);
                return;
            }
        }
        if (serverConfigurator == null) {
            log.warn("Unrecognized server. Considered as already configured.");
        } else if (!nuxeoConf.exists()) {
            log.info("Missing " + nuxeoConf);
        } else if (!serverConfigurator.isConfigured()) {
            log.info("No current configuration, generating files...");
            generateFiles();
        } else if (forceGeneration) {
            log.info("Configuration files generation (nuxeo.force.generation=true)...");
            generateFiles();
        } else {
            log.info("Server already configured (set nuxeo.force.generation=true to force configuration files generation).");
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
            forceGeneration = Boolean.parseBoolean(userConfig.getProperty(
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
    }

    protected void generateFiles() throws ConfigurationException {
        try {
            browseTemplates();
            log.debug("Loaded configuration: " + userConfig);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }
        try {
            serverConfigurator.parseAndCopy(userConfig);
            log.info("Configuration files generated.");
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure", e);
        }
    }

    protected void browseTemplates() throws IOException,
            ConfigurationException {
        if (userConfig == null) {
            setBasicConfiguration();
        }
        // Override default configuration with specific configuration(s) of the
        // chosen template(s) which can be outside of server filesystem
        String userTemplatesList = userConfig.getProperty(PARAM_TEMPLATES_NAME);
        if (userTemplatesList == null) {
            // backward compliance: manage parameter for a single template
            userTemplatesList = userConfig.getProperty(PARAM_TEMPLATE_NAME);
        }
        includeTemplates(userTemplatesList);
    }

    private void includeTemplates(String templatesList) throws IOException {
        StringTokenizer st = new StringTokenizer(templatesList, ",");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            File chosenTemplate = new File(nextToken);
            if (!chosenTemplate.exists()) {
                chosenTemplate = new File(nuxeoDefaultConf.getParentFile(), nextToken);
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
                log.warn("No template found neither with absolute ("
                        + nextToken + ") or relative (" + chosenTemplate
                        + ") path! Please check your includes.");
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

}
