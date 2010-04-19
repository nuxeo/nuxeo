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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;

/**
 * @author jcarsique
 * 
 *         Builder for server configuration and datasource files from templates
 *         and properties.
 */
public class ServerConfigurator {

    private static final Log log = LogFactory.getLog(ServerConfigurator.class);

    public static final String NUXEO_HOME = "nuxeo.home";

    public static final String NUXEO_CONF = "nuxeo.conf";

    private static final String TEMPLATES = "templates";

    private static final String NUXEO_DEFAULT_CONF = "nuxeo.defaults";

    private static final String PARAM_TEMPLATE_NAME = "nuxeo.template";

    private static final String PARAM_INCLUDED_TEMPLATES = "nuxeo.template.includes";

    public static final String JBOSS_CONFIG = "server/default/deploy/nuxeo.ear/config";

    private File nuxeoHome;

    // User configuration file
    private File nuxeoConf;

    // Common default configuration file
    private File nuxeoDefaultConf;

    private String chosenTemplate;

    // Default configuration file for the chosen template
    private File templateDefaultConf;

    public ServerConfigurator() {
        nuxeoHome = new File(System.getProperty(NUXEO_HOME));
        nuxeoConf = new File(System.getProperty(NUXEO_CONF));
        nuxeoDefaultConf = new File(nuxeoHome, TEMPLATES + File.separator
                + NUXEO_DEFAULT_CONF);
    }

    /**
     * Run the configuration files generation
     * 
     * @throws ConfigurationException
     */
    public void run() throws ConfigurationException {
        if (isConfigured()) {
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
            parseAndCopy(config);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Missing file", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration failure", e);
        }
    }

    /**
     * @param config Properties containing keys and values for parsing
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void parseAndCopy(Properties config) throws FileNotFoundException,
            IOException {
        TextTemplate templateParser = new TextTemplate(config);

        // copy files to nuxeo.ear
        File outputDirectory = new File(nuxeoHome,
                new File(JBOSS_CONFIG).getParent());

        // List template directories to copy from (order is: included templates
        // then chosen template)
        List<File> inputDirectories = new ArrayList<File>();
        // FilenameFilter to exclude "nuxeo.defaults" files from copy
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !"nuxeo.defaults".equals(name);
            }
        };
        // add included templates directories
        StringTokenizer st = new StringTokenizer(
                config.getProperty(PARAM_INCLUDED_TEMPLATES), ",");
        List<String> includedTemplates = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            includedTemplates.add(st.nextToken());
        }
        for (String includedTemplate : includedTemplates) {
            addDirectories(new File(nuxeoDefaultConf.getParent(),
                    includedTemplate).listFiles(filter), inputDirectories);
        }

        // add chosen template directories
        addDirectories(templateDefaultConf.getParentFile().listFiles(filter),
                inputDirectories);

        for (File in : inputDirectories) {
            templateParser.processDirectory(in, new File(outputDirectory,
                    in.getName()));
        }
    }

    private void addDirectories(File[] filesToAdd, List<File> inputDirectories) {
        if (filesToAdd != null) {
            for (File in : filesToAdd) {
                inputDirectories.add(in);
            }
        }
    }

    /**
     * @return Properties of configured parameters with user and default values
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected Properties getConfiguration() throws FileNotFoundException,
            IOException {
        // Load default configuration
        Properties defaultConfig = new Properties();
        defaultConfig.load(new FileInputStream(nuxeoDefaultConf));
        // Load user configuration
        Properties userConfig = new Properties(defaultConfig);
        userConfig.load(new FileInputStream(nuxeoConf));
        // Override default configuration with specific configuration of the
        // chosen template
        chosenTemplate = userConfig.getProperty(PARAM_TEMPLATE_NAME);
        templateDefaultConf = new File(nuxeoDefaultConf.getParent(),
                chosenTemplate + File.separator + NUXEO_DEFAULT_CONF);
        defaultConfig.load(new FileInputStream(templateDefaultConf));
        return userConfig;
    }

    /**
     * @return true if "config" files directory already exists
     */
    protected boolean isConfigured() {
        return new File(nuxeoHome, JBOSS_CONFIG).exists();
    }

}
