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

package org.nuxeo.launcher.config;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.commons.text.TextTemplate;

/**
 * @author jcarsique
 */
public abstract class ServerConfigurator {

    protected static final Log log = LogFactory.getLog(ServerConfigurator.class);

    protected static final String DEFAULT_LOG_DIR = "log";

    protected final ConfigurationGenerator generator;

    protected File dataDir = null;

    protected File logDir = null;

    private File runDir = null;

    protected File libDir = null;

    public ServerConfigurator(ConfigurationGenerator configurationGenerator) {
        generator = configurationGenerator;
    }

    /**
     * @return true if server configuration files already exist
     */
    abstract boolean isConfigured();

    /**
     * Generate configuration files from templates and given configuration
     * parameters
     *
     * @param config Properties with configuration parameters for template
     *            replacement
     */
    protected void parseAndCopy(Properties config) throws IOException {
        // FilenameFilter for excluding "nuxeo.defaults" files from copy
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !"nuxeo.defaults".equals(name);
            }
        };
        final TextTemplate templateParser = new TextTemplate(config);
        templateParser.setTrim(true);
        templateParser.setParsingExtensions(config.getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_PARSING_EXTENSIONS,
                "xml,properties"));

        // add included templates directories
        for (File includedTemplate : generator.getIncludedTemplates()) {
            if (includedTemplate.listFiles(filter) != null) {
                // Retrieve optional target directory if defined
                String outputDirectoryStr = config.getProperty(includedTemplate.getName()
                        + ".target");
                File outputDirectory = (outputDirectoryStr != null) ? new File(
                        generator.getNuxeoHome(), outputDirectoryStr)
                        : getOutputDirectory();
                for (File in : includedTemplate.listFiles(filter)) {
                    // copy template(s) directories parsing properties
                    templateParser.processDirectory(in, new File(
                            outputDirectory, in.getName()));
                }
            }
        }
    }

    /**
     * @return output directory for files generation
     */
    protected abstract File getOutputDirectory();

    /**
     * @return Default data directory path relative to Nuxeo Home
     * @since 5.4.1
     */
    protected abstract String getDefaultDataDir();

    /**
     * @return Data directory
     * @since 5.4.1
     */
    public File getDataDir() {
        if (dataDir == null) {
            dataDir = new File(generator.getNuxeoHome(), getDefaultDataDir());
        }
        return dataDir;
    }

    /**
     * @return Log directory
     * @since 5.4.1
     */
    public File getLogDir() {
        if (logDir == null) {
            logDir = new File(generator.getNuxeoHome(), DEFAULT_LOG_DIR);
        }
        return logDir;
    }

    /**
     * @param dataDirStr Data directory path to set
     * @since 5.4.1
     */
    public void setDataDir(String dataDirStr) {
        dataDir = new File(dataDirStr);
        dataDir.mkdirs();
    }

    /**
     * @param logDirStr Log directory path to set
     * @since 5.4.1
     */
    public void setLogDir(String logDirStr) {
        logDir = new File(logDirStr);
        logDir.mkdirs();
    }

    /**
     * Initialize logs
     *
     * @since 5.4.1
     */
    public abstract void initLogs();

    /**
     * @return Main lib directory
     * @since 5.4.1
     */
    public abstract File getLibDir();

    /**
     * @return Run directory; Returns log directory if not set by configuration.
     * @since 5.4.1
     */
    public File getRunDir() {
        if (runDir == null) {
            runDir = getLogDir();
        }
        return runDir;
    }

    /**
     * @return Server bootstrap file
     * @since 5.4.1
     */
    public abstract File getBootstrap();

}
