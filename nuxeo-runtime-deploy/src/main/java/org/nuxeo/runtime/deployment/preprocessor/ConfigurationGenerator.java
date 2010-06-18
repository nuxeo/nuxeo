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

    private File nuxeoHome;

    // User configuration file
    private File nuxeoConf;

    // Common default configuration file
    private File nuxeoDefaultConf;

    // Chosen templates
    private List<File> includedTemplates = new ArrayList<File>();

    private boolean isJBoss;

    private boolean isJetty;

    private boolean isTomcat;

    private ServerConfigurator serverConfigurator;

    private boolean forceGeneration;

    public void setForceGeneration(boolean forceGeneration) {
        this.forceGeneration = forceGeneration;
    }

    private Properties defaultConfig;

    private Properties userConfig = null;

    public Properties getUserConfig() {
        return userConfig;
    }

    public ConfigurationGenerator() {
        String nuxeoHomePath = System.getProperty(NUXEO_HOME);
        String nuxeoConfPath = System.getProperty(NUXEO_CONF);
        if (nuxeoHomePath != null && nuxeoConfPath != null) {
            nuxeoHome = new File(nuxeoHomePath);
            nuxeoConf = new File(nuxeoConfPath);
            nuxeoDefaultConf = new File(getNuxeoHome(), TEMPLATES
                    + File.separator + NUXEO_DEFAULT_CONF);
        } else {
            nuxeoHome = new File(System.getProperty("user.dir")).getParentFile();
            nuxeoConf = new File(nuxeoHome, "bin" + File.separator
                    + "nuxeo.conf");
        }

        // detect server type based on System properties
        isJBoss = System.getProperty("jboss.home.dir") != null;
        isJetty = System.getProperty("jetty.home") != null;
        isTomcat = System.getProperty("tomcat.home") != null;
        if (!isJBoss && !isJetty && !isTomcat) {
            // fallback on jar detection
            isJBoss = new File(getNuxeoHome(), "bin/run.jar").exists();
            isTomcat = new File(getNuxeoHome(), "bin/bootstrap.jar").exists();
            String[] files = getNuxeoHome().list();
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

    /**
     * Run the configuration files generation
     *
     * @throws ConfigurationException
     */
    public void run() throws ConfigurationException {
        if (userConfig == null) {
            try {
                setBasicConfiguration();
            } catch (ConfigurationException e) {
                log.warn("Error reading basic configuration. Server is considered as already configured.");
                log.debug(e);
                return;
            }
        }
        if (serverConfigurator == null) {
            log.warn("Unrecognized server. Considered as already configured.");
        } else if (!nuxeoConf.exists()) {
            log.info("Missing " + nuxeoConf);
        } else if (forceGeneration) {
            log.info("Force files generation...");
            generateFiles();
            log.info("Configuration files generated.");
        } else if (serverConfigurator.isConfigured()) {
            log.info("Server already configured");
        } else {
            log.info("No current configuration, generating files...");
            generateFiles();
            log.info("Configuration files generated.");
        }
    }

    /**
     * @throws ConfigurationException
     *
     */
    private void setBasicConfiguration() throws ConfigurationException {
        try {
            // Load default configuration
            defaultConfig = new Properties();
            defaultConfig.load(new FileInputStream(getNuxeoDefaultConf()));
            // Load user configuration
            userConfig = new Properties(defaultConfig);
            userConfig.load(new FileInputStream(nuxeoConf));
            forceGeneration = Boolean.parseBoolean(userConfig.getProperty(
                    PARAM_FORCE_GENERATION, "false"));
        } catch (NullPointerException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
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
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure", e);
        }
    }

    protected void browseTemplates() throws FileNotFoundException, IOException,
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

    /**
     * @param templatesList
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void includeTemplates(String templatesList)
            throws FileNotFoundException, IOException {
        StringTokenizer st = new StringTokenizer(templatesList, ",");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            File chosenTemplate = new File(nextToken);
            if (!chosenTemplate.exists()) {
                chosenTemplate = new File(
                        getNuxeoDefaultConf().getParentFile(), nextToken);
            }
            if (includedTemplates.contains(chosenTemplate)) {
                log.debug("Aleady included " + nextToken);
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
                    log.debug("Include " + nextToken);
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

}
