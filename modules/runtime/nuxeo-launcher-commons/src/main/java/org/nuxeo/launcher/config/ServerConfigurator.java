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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_PROFILES;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_BIND_ADDRESS;
import static org.nuxeo.launcher.config.ConfigurationGenerator.TEMPLATE_SEPARATOR;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.PackageInfo;

/**
 * @author jcarsique
 * @implNote since 11.1, configurator only handles Tomcat and is no more abstract
 */
public class ServerConfigurator {

    private static final Logger log = LogManager.getLogger(ServerConfigurator.class);

    /** @since 5.4.2 */
    public static final String TOMCAT_STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    /** @since 5.6 */
    public static final String TOMCAT_HOME = "tomcat.home";

    /** @since 5.7 */
    public static final String PARAM_HTTP_TOMCAT_ADMIN_PORT = "nuxeo.server.tomcat_admin.port";

    protected static final String DEFAULT_CONTEXT_NAME = "/nuxeo";

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
     * @return true if server configuration files already exist
     */
    protected boolean isConfigured() {
        Path nuxeoContext = Path.of("conf", "Catalina", "localhost", getContextName() + ".xml");
        return Files.exists(configHolder.getHomePath().resolve(nuxeoContext));
    }

    /**
     * @return Configured context name
     * @since 5.4.2
     */
    public String getContextName() {
        if (contextName == null) {
            contextName = configHolder.getProperty(ConfigurationGenerator.PARAM_CONTEXT_PATH, DEFAULT_CONTEXT_NAME);
            contextName = contextName.substring(1);
        }
        return contextName;
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
     * Check server paths; warn if existing deprecated paths. Override this method to perform server specific checks.
     *
     * @throws ConfigurationException If deprecated paths have been detected
     * @since 5.4.2
     */
    public void checkPaths() throws ConfigurationException {
        Path badInstanceClid = configHolder.getRuntimeHomePath()
                                           .resolve(Environment.DEFAULT_DATA_DIR)
                                           .resolve("instance.clid");
        Path dataPath = configHolder.getDataPath();
        if (Files.exists(badInstanceClid) && !badInstanceClid.startsWith(dataPath)) {
            log.warn("Moving {} to {}.", badInstanceClid, dataPath);
            try {
                FileUtils.moveFileToDirectory(badInstanceClid.toFile(), dataPath.toFile(), true);
            } catch (IOException e) {
                throw new ConfigurationException("NXP-6722 move failed: " + e.getMessage(), e);
            }
        }

        Path oldPackagesPath = dataPath.resolve(Environment.DEFAULT_MP_DIR);
        Path packagesPath = configHolder.getPackagesPath();
        if (Files.exists(oldPackagesPath) && !oldPackagesPath.equals(packagesPath)) {
            log.warn("NXP-8014 Packages cache location changed. You can safely delete {} or move its content to {}",
                    oldPackagesPath, packagesPath);
        }
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
     * @since 5.7
     */
    public void verifyInstallation() throws ConfigurationException {
        checkPaths();
        checkNetwork();
    }

    /**
     * Perform server specific checks, not already done by {@link ConfigurationGenerator#checkAddressesAndPorts()}
     *
     * @since 5.7
     * @see ConfigurationGenerator#checkAddressesAndPorts()
     */
    protected void checkNetwork() throws ConfigurationException {
        InetAddress bindAddress = ConfigurationGenerator.getBindAddress(configHolder.getProperty(PARAM_BIND_ADDRESS));
        ConfigurationGenerator.checkPortAvailable(bindAddress,
                Integer.parseInt(configHolder.getProperty(PARAM_HTTP_TOMCAT_ADMIN_PORT)));
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

    /**
     * Introspect the server and builds the instance info
     *
     * @since 8.3
     */
    public InstanceInfo getInfo(String clid, List<LocalPackage> pkgs) {
        InstanceInfo nxInstance = new InstanceInfo();
        nxInstance.NUXEO_CONF = configHolder.getNuxeoConfPath().toString();
        nxInstance.NUXEO_HOME = configHolder.getHomePath().toString();
        // distribution
        Path distFile = configHolder.getConfigurationPath().resolve("distribution.properties");
        if (Files.notExists(distFile)) {
            // fallback in the file in templates
            distFile = configHolder.getTemplatesPath().resolve(Path.of("common", "config", "distribution.properties"));
        }
        try {
            nxInstance.distribution = new DistributionInfo(distFile.toFile());
        } catch (IOException e) {
            nxInstance.distribution = new DistributionInfo();
        }
        // packages
        nxInstance.clid = clid;
        Set<String> pkgTemplates = new HashSet<>();
        for (LocalPackage pkg : pkgs) {
            final PackageInfo info = new PackageInfo(pkg);
            nxInstance.packages.add(info);
            pkgTemplates.addAll(info.templates);
        }
        nxInstance.config = new ConfigurationInfo();
        // profiles
        String profiles = System.getenv(NUXEO_PROFILES);
        if (isNotBlank(profiles)) {
            nxInstance.config.profiles.addAll(Arrays.asList(profiles.split(TEMPLATE_SEPARATOR)));
        }
        // templates
        nxInstance.config.dbtemplate = generator.extractDatabaseTemplateName();
        List<String> userTemplates = configHolder.getIncludedTemplateNames();
        for (String template : userTemplates) {
            if (template.equals(nxInstance.config.dbtemplate)) {
                continue;
            }
            if (pkgTemplates.contains(template)) {
                nxInstance.config.pkgtemplates.add(template);
            } else {
                if (Files.exists(configHolder.getTemplatesPath().resolve(template))) {
                    nxInstance.config.basetemplates.add(template);
                } else {
                    nxInstance.config.usertemplates.add(template);
                }
            }
        }
        CryptoProperties userConfig = configHolder.userConfig;
        // Settings from nuxeo.conf
        computeKeyVals(nxInstance.config.keyvals, userConfig, userConfig.keySet());
        // Effective configuration for environment and profiles
        computeKeyVals(nxInstance.config.allkeyvals, userConfig, userConfig.stringPropertyNames());
        return nxInstance;
    }

    protected void computeKeyVals(List<KeyValueInfo> keyVals, CryptoProperties userConfig, Set<?> keys) {
        for (Object item : new TreeSet<>(keys)) {
            String key = (String) item;
            String value = userConfig.getRawProperty(key);
            if (JAVA_OPTS.equals(key)) {
                value = generator.getJavaOptsString();
            }
            if (ConfigurationGenerator.SECRET_KEYS.contains(key) || key.contains("password")
                    || key.equals(Environment.SERVER_STATUS_KEY) || Crypto.isEncrypted(value)) {
                value = "********";
            }
            keyVals.add(new KeyValueInfo(key, value));
        }
    }
}
