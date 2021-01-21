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
 */
package org.nuxeo.launcher.config;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;

/**
 * @author jcarsique
 * @implNote since 11.1, configurator only handles Tomcat and is no more abstract
 */
public class ServerConfigurator {

    private static final Logger log = LogManager.getLogger(ServerConfigurator.class);

    /** @since 9.3 */
    public static final String JAVA_OPTS = "JAVA_OPTS";

    protected final ConfigurationGenerator generator;

    /** @since 11.5 */
    protected final ConfigurationHolder configHolder;

    protected File dataDir = null;

    protected File logDir = null;

    protected File pidDir = null;

    protected File tmpDir = null;

    protected File packagesDir = null;

    private String contextName = null;

    public ServerConfigurator(ConfigurationHolder configHolder) {
        this.configHolder = configHolder;
        generator = null;
    }

    public ServerConfigurator(ConfigurationGenerator configurationGenerator, ConfigurationHolder configHolder) {
        this.configHolder = configHolder;
        generator = configurationGenerator;
    }

    /**
     * @return Data directory
     * @since 5.4.2
     */
    public File getDataDir() {
        if (dataDir == null) {
            dataDir = configHolder.getDataPath().toFile();
        }
        return dataDir;
    }

    /**
     * @return Log directory
     * @since 5.4.2
     */
    public File getLogDir() {
        if (logDir == null) {
            logDir = configHolder.getLogPath().toFile();
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
     * @return Pid directory (usually known as "run directory"); Returns log directory if not set by configuration.
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
     * @return Temporary directory
     * @since 5.4.2
     */
    public File getTmpDir() {
        if (tmpDir == null) {
            tmpDir = configHolder.getTmpPath().toFile();
        }
        return tmpDir;
    }

    /**
     * @return Default temporary directory path relative to Nuxeo Home
     * @since 5.4.2
     */
    public String getDefaultTmpDir() {
        return Environment.DEFAULT_TMP_DIR;
    }

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
        if (Environment.NUXEO_DATA_DIR.equals(key)) {
            setDataDir(absoluteDirectory);
        } else if (Environment.NUXEO_LOG_DIR.equals(key)) {
            setLogDir(absoluteDirectory);
        } else if (Environment.NUXEO_PID_DIR.equals(key)) {
            setPidDir(absoluteDirectory);
        } else if (Environment.NUXEO_TMP_DIR.equals(key)) {
            setTmpDir(absoluteDirectory);
        } else if (Environment.NUXEO_MP_DIR.equals(key)) {
            setPackagesDir(absoluteDirectory);
        } else {
            log.error("Unknown directory key: {}", key);
        }
    }

    /**
     * @since 5.9.4
     */
    private void setPackagesDir(String packagesDirStr) {
        packagesDir = new File(packagesDirStr);
        packagesDir.mkdirs();
    }

    /**
     * Make absolute the directory passed in parameter. If it was relative, then store absolute path in user config
     * instead of relative and return value
     *
     * @param key Directory system key
     * @param directory absolute or relative directory path
     * @return absolute directory path
     * @since 5.4.2
     */
    private String setAbsolutePath(String key, String directory) {
        if (!new File(directory).isAbsolute()) {
            directory = configHolder.getHomePath().resolve(directory).toString();
            configHolder.userConfig.setProperty(key, directory);
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
        if (Environment.NUXEO_DATA_DIR.equals(key)) {
            return getDataDir();
        } else if (Environment.NUXEO_LOG_DIR.equals(key)) {
            return getLogDir();
        } else if (Environment.NUXEO_PID_DIR.equals(key)) {
            return getPidDir();
        } else if (Environment.NUXEO_TMP_DIR.equals(key)) {
            return getTmpDir();
        } else if (Environment.NUXEO_MP_DIR.equals(key)) {
            return getPackagesDir();
        } else {
            log.error("Unknown directory key: {}", key);
            return null;
        }
    }

    /**
     * @return Nuxeo's third party libraries directory
     * @since 5.4.1
     */
    public File getNuxeoLibDir() {
        return configHolder.getRuntimeHomePath().resolve("lib").toFile();
    }

    /**
     * @return Server's third party libraries directory
     * @since 5.4.1
     */
    public File getServerLibDir() {
        return configHolder.getHomePath().resolve("lib").toFile();
    }

    /**
     * @return Marketplace Packages directory
     * @since 5.9.4
     */
    public File getPackagesDir() {
        if (packagesDir == null) {
            packagesDir = configHolder.getPackagesPath().toFile();
        }
        return packagesDir;
    }

    /**
     * @return Default MP directory path relative to Nuxeo Home
     * @since 5.9.4
     */
    public String getDefaultPackagesDir() {
        return Environment.DEFAULT_MP_DIR;
    }
}
