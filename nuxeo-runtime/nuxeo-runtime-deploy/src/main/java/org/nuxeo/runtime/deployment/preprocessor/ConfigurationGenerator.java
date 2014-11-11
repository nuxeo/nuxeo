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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builder for server configuration and datasource files from templates
 * and properties.
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
     */
    protected static final String PARAM_TEMPLATE_NAME = "nuxeo.template";

    protected static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    private File nuxeoHome;

    // User configuration file
    private File nuxeoConf;

    // Common default configuration file
    private File nuxeoDefaultConf;

    // Default configuration file for the chosen template
    private File templateDefaultConf;

    private boolean isJBoss;

    private boolean isJetty;

    private boolean isTomcat;

    private ServerConfigurator serverConfigurator;

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
     */
    public void run() throws ConfigurationException {
        if (serverConfigurator == null) {
            log.warn("Unrecognized server. Considered as already configured.");
        } else if (!nuxeoConf.exists() || serverConfigurator.isConfigured()) {
            log.info("Server already configured");
        } else {
            log.info("No current configuration, generating files...");
            generateFiles();
            log.info("Configuration files generated.");
        }
    }

    protected void generateFiles() throws ConfigurationException {
        Properties config;
        try {
            config = getConfiguration();
            log.debug("Loaded configuration: " + config);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + nuxeoConf, e);
        }
        try {
            serverConfigurator.parseAndCopy(config);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure", e);
        }
    }

    /**
     * @return Properties of configured parameters with user and default values
     */
    protected Properties getConfiguration() throws FileNotFoundException,
            IOException {
        // Load default configuration
        Properties defaultConfig = new Properties();
        defaultConfig.load(new FileInputStream(getNuxeoDefaultConf()));
        // Load user configuration
        Properties userConfig = new Properties(defaultConfig);
        userConfig.load(new FileInputStream(nuxeoConf));
        // Override default configuration with specific configuration of the
        // chosen template which can be outside of server filesystem
        File chosenTemplate = new File(
                userConfig.getProperty(PARAM_TEMPLATE_NAME));
        if (!chosenTemplate.exists()) {
            chosenTemplate = new File(getNuxeoDefaultConf().getParentFile(),
                    userConfig.getProperty(PARAM_TEMPLATE_NAME));
        }
        templateDefaultConf = new File(chosenTemplate, NUXEO_DEFAULT_CONF);
        defaultConfig.load(new FileInputStream(getTemplateDefaultConf()));
        return userConfig;
    }

    public File getNuxeoHome() {
        return nuxeoHome;
    }

    public File getNuxeoDefaultConf() {
        return nuxeoDefaultConf;
    }

    public File getTemplateDefaultConf() {
        return templateDefaultConf;
    }

}
