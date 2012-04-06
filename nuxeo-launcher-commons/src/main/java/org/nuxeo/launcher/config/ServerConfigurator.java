/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
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

    private File pidDir = null;

    protected File libDir = null;

    private File tmpDir = null;

    /**
     * @since 5.4.2
     */
    public static final String[] NUXEO_SYSTEM_PROPERTIES = new String[] {
            "nuxeo.conf", "nuxeo.home" };

    protected static final String DEFAULT_CONTEXT_NAME = "/nuxeo";

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
                "xml,properties,nx"));

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
    protected File getOutputDirectory() {
        return getRuntimeHome();
    }

    /**
     * @return Default data directory path relative to Nuxeo Home
     * @since 5.4.2
     */
    protected abstract String getDefaultDataDir();

    /**
     * Returns the Home of NuxeoRuntime
     * (same as Framework.getRuntime().getHome().getAbsolutePath())
     *
     * @return
     */
    protected abstract File getRuntimeHome();

    /**
     * @return Data directory
     * @since 5.4.2
     */
    public File getDataDir() {
        if (dataDir == null) {
            dataDir = new File(generator.getNuxeoHome(), getDefaultDataDir());
        }
        return dataDir;
    }

    /**
     * @return Log directory
     * @since 5.4.2
     */
    public File getLogDir() {
        if (logDir == null) {
            logDir = new File(generator.getNuxeoHome(), DEFAULT_LOG_DIR);
        }
        return logDir;
    }

    /**
     * @param dataDirStr Data directory path to set
     * @since 5.4.2
     */
    public void setDataDir(String dataDirStr) {
        dataDir = new File(dataDirStr);
        dataDir.mkdirs();
    }

    /**
     * @param logDirStr Log directory path to set
     * @since 5.4.2
     */
    public void setLogDir(String logDirStr) {
        logDir = new File(logDirStr);
        logDir.mkdirs();
    }

    /**
     * Initialize logs
     *
     * @since 5.4.2
     */
    public void initLogs() {
        File logFile = getLogConfFile();
        try {
            System.out.println("Try to configure logs with " + logFile);
            System.setProperty(org.nuxeo.common.Environment.NUXEO_LOG_DIR,
                    getLogDir().getPath());
            DOMConfigurator.configure(logFile.toURI().toURL());
            log.info("Logs successfully configured.");
        } catch (MalformedURLException e) {
            log.error("Could not initialize logs with " + logFile, e);
        }
    }

    /**
     * @return Pid directory (usually known as "run directory"); Returns log
     *         directory if not set by configuration.
     * @since 5.4.2
     */
    public File getPidDir() {
        if (pidDir == null) {
            pidDir = getLogDir();
        }
        return pidDir;
    }

    /**
     * @param pidDirStr Pid directory path to set
     * @since 5.4.2
     */
    public void setPidDir(String pidDirStr) {
        pidDir = new File(pidDirStr);
        pidDir.mkdirs();
    }

    /**
     * Check server paths; warn if existing deprecated paths.
     * Override this method to perform server specific checks.
     *
     * @throws ConfigurationException If deprecated paths have been detected
     *
     * @since 5.4.2
     */
    public void checkPaths() throws ConfigurationException {
        File badInstanceClid = new File(generator.getNuxeoHome(),
                getDefaultDataDir() + File.separator + "instance.clid");
        if (badInstanceClid.exists()
                && !getDataDir().equals(
                        new File(generator.getNuxeoHome(), getDefaultDataDir()))) {
            log.warn("Moving " + badInstanceClid + " to " + getDataDir() + ".");
            try {
                FileUtils.moveFileToDirectory(badInstanceClid, getDataDir(),
                        true);
            } catch (IOException e) {
                throw new ConfigurationException("Move failed.", e);
            }
        }
    }

    /**
     * @return Temporary directory
     * @since 5.4.2
     */
    public File getTmpDir() {
        if (tmpDir == null) {
            tmpDir = new File(generator.getNuxeoHome(), getDefaultTmpDir());
        }
        return tmpDir;
    }

    /**
     * @return Default temporary directory path relative to Nuxeo Home
     * @since 5.4.2
     */
    public abstract String getDefaultTmpDir();

    /**
     * @param tmpDirStr Temporary directory path to set
     * @since 5.4.2
     */
    public void setTmpDir(String tmpDirStr) {
        tmpDir = new File(tmpDirStr);
        tmpDir.mkdirs();
    }

    /**
     * @see Environment
     * @param key directory system key
     * @param directory absolute or relative directory path
     * @since 5.4.2
     */
    public void setDirectory(String key, String directory) {
        String absoluteDirectory = setAbsolutePath(key, directory);
        if (org.nuxeo.common.Environment.NUXEO_DATA_DIR.equals(key)) {
            setDataDir(absoluteDirectory);
        } else if (org.nuxeo.common.Environment.NUXEO_LOG_DIR.equals(key)) {
            setLogDir(absoluteDirectory);
        } else if (org.nuxeo.common.Environment.NUXEO_PID_DIR.equals(key)) {
            setPidDir(absoluteDirectory);
        } else if (org.nuxeo.common.Environment.NUXEO_TMP_DIR.equals(key)) {
            setTmpDir(absoluteDirectory);
        } else {
            log.error("Unknown directory key: " + key);
        }
    }

    /**
     * Make absolute the directory passed in parameter. If it was relative, then
     * store absolute path in user config instead of relative and return value
     *
     * @param key Directory system key
     * @param directory absolute or relative directory path
     * @return absolute directory path
     * @since 5.4.2
     */
    private String setAbsolutePath(String key, String directory) {
        if (!new File(directory).isAbsolute()) {
            directory = new File(generator.getNuxeoHome(), directory).getPath();
            generator.getUserConfig().setProperty(key, directory);
        }
        return directory;
    }

    /**
     * @see Environment
     * @param key directory system key
     * @return Directory denoted by key
     * @since 5.4.2
     */
    public File getDirectory(String key) {
        if (org.nuxeo.common.Environment.NUXEO_DATA_DIR.equals(key)) {
            return getDataDir();
        } else if (org.nuxeo.common.Environment.NUXEO_LOG_DIR.equals(key)) {
            return getLogDir();
        } else if (org.nuxeo.common.Environment.NUXEO_PID_DIR.equals(key)) {
            return getPidDir();
        } else if (org.nuxeo.common.Environment.NUXEO_TMP_DIR.equals(key)) {
            return getTmpDir();
        } else {
            log.error("Unknown directory key: " + key);
            return null;
        }
    }

    /**
     * Check if oldPath exist; if so, then raise a ConfigurationException with
     * information for fixing issue
     *
     * @param oldPath Path that must NOT exist
     * @param message Error message thrown with exception
     * @throws ConfigurationException If an old path has been discovered
     */
    protected void checkPath(File oldPath, String message)
            throws ConfigurationException {
        if (oldPath.exists()) {
            log.error("Deprecated paths used (NXP-5370, NXP-5460).");
            throw new ConfigurationException(message);
        }
    }

    /**
     * @return Log4J configuration file
     * @since 5.4.2
     */
    public abstract File getLogConfFile();

    /**
     * Remove locks on file system (dedicated to Lucene locks)
     *
     * @since 5.4.2
     */
    public void removeExistingLocks() {
        File lockFile = new File(getDataDir(), "h2" + File.separator
                + "nuxeo.lucene" + File.separator + "write.lock");
        if (lockFile.exists()) {
            log.info("Removing lock file " + lockFile);
            lockFile.delete();
        }
    }

    /**
     * @return Nuxeo config directory
     * @since 5.4.2
     */
    public abstract File getConfigDir();

    /**
     * @since 5.4.2
     */
    public abstract void prepareWizardStart();

    /**
     * @since 5.4.2
     */

    public abstract void cleanupPostWizard();

    /**
     * @return true if configuration wizard is required before starting Nuxeo
     * @since 5.4.2
     */
    public abstract boolean isWizardAvailable();

    /**
     * @param userConfig Properties to dump into config directory
     * @since 5.4.2
     */
    public void dumpProperties(Properties userConfig) {
        Properties dumpedProperties = filterSystemProperties(userConfig);
        File dumpedFile = new File(generator.getConfigDir(),
                "configuration.properties");
        OutputStream os = null;
        try {
            os = new FileOutputStream(dumpedFile, false);
            dumpedProperties.store(os, "Generated by " + getClass());
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error("Could not dump properties to " + dumpedFile, e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * Extract Nuxeo properties from given Properties (System properties are
     * removed, except those set by Nuxeo)
     *
     * @param properties Properties to be filtered
     * @return copy of given properties filtered out of System properties
     * @since 5.4.2
     */
    public Properties filterSystemProperties(Properties properties) {
        Properties dumpedProperties = new Properties();
        for (@SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = (Enumeration<String>) properties.propertyNames(); propertyNames.hasMoreElements();) {
            String key = propertyNames.nextElement();
            dumpedProperties.setProperty(key, properties.getProperty(key));
        }
        // Remove System properties
        for (@SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = (Enumeration<String>) System.getProperties().propertyNames(); propertyNames.hasMoreElements();) {
            String key = propertyNames.nextElement();
            dumpedProperties.remove(key);
        }
        // Re-add Nuxeo's System properties
        for (String key : NUXEO_SYSTEM_PROPERTIES) {
            dumpedProperties.setProperty(key, properties.getProperty(key));
        }
        return dumpedProperties;
    }

    /**
     * @return Nuxeo's third party libraries directory
     * @since 5.4.1
     */
    public File getNuxeoLibDir() {
        return new File(getRuntimeHome(), "lib");
    }

    /**
     * @return Server's third party libraries directory
     * @since 5.4.1
     */
    public abstract File getServerLibDir();

}
