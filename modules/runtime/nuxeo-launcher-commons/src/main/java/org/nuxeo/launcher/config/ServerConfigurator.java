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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.nuxeo.log4j.Log4JHelper;

import freemarker.template.TemplateException;

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

    /**
     * @since 5.4.2
     */
    public static final List<String> NUXEO_SYSTEM_PROPERTIES = List.of("nuxeo.conf", "nuxeo.home", "log.id");

    protected static final String DEFAULT_CONTEXT_NAME = "/nuxeo";

    /** @since 9.3 */
    public static final String JAVA_OPTS = "JAVA_OPTS";

    private static final String NEW_FILES = ConfigurationGenerator.TEMPLATES + File.separator + "files.list";

    protected final ConfigurationGenerator generator;

    protected File dataDir = null;

    protected File logDir = null;

    protected File pidDir = null;

    protected File tmpDir = null;

    protected File packagesDir = null;

    private String contextName = null;

    public ServerConfigurator(ConfigurationGenerator configurationGenerator) {
        generator = configurationGenerator;
    }

    /**
     * @return true if server configuration files already exist
     */
    protected boolean isConfigured() {
        Path tomcatNuxeoConf = Path.of("conf", "Catalina", "localhost", getContextName() + ".xml");
        return Files.exists(generator.getNuxeoHome().toPath().resolve(tomcatNuxeoConf));
    }

    /**
     * @return Configured context name
     * @since 5.4.2
     */
    public String getContextName() {
        if (contextName == null) {
            Properties userConfig = generator.getUserConfig();
            if (userConfig != null) {
                contextName = userConfig.getProperty(ConfigurationGenerator.PARAM_CONTEXT_PATH, DEFAULT_CONTEXT_NAME)
                                        .substring(1);
            } else {
                contextName = DEFAULT_CONTEXT_NAME.substring(1);
            }
        }
        return contextName;
    }

    /**
     * Generate configuration files from templates and given configuration parameters
     *
     * @param config Properties with configuration parameters for template replacement
     */
    protected void parseAndCopy(Properties config) throws IOException, TemplateException, ConfigurationException {
        // FilenameFilter for excluding "nuxeo.defaults" files from copy
        final FilenameFilter filter = (dir, name) -> !ConfigurationGenerator.NUXEO_DEFAULT_CONF.equals(name);
        final TextTemplate templateParser = new TextTemplate(config);
        templateParser.setKeepEncryptedAsVar(true);
        templateParser.setTrim(true);
        templateParser.setTextParsingExtensions(
                config.getProperty(ConfigurationGenerator.PARAM_TEMPLATES_PARSING_EXTENSIONS, "xml,properties,nx"));
        templateParser.setFreemarkerParsingExtensions(
                config.getProperty(ConfigurationGenerator.PARAM_TEMPLATES_FREEMARKER_EXTENSIONS, "nxftl"));

        deleteTemplateFiles();
        // add included templates directories
        List<String> newFilesList = new ArrayList<>();
        for (File includedTemplate : generator.getIncludedTemplates()) {
            File[] listFiles = includedTemplate.listFiles(filter);
            if (listFiles != null) {
                String templateName = includedTemplate.getName();
                log.debug("Parsing {}... {}", () -> templateName, () -> Arrays.toString(listFiles));
                // Check for deprecation
                boolean isDeprecated = Boolean.parseBoolean(config.getProperty(templateName + ".deprecated"));
                if (isDeprecated) {
                    log.warn("WARNING: Template {} is deprecated.", templateName);
                    String deprecationMessage = config.getProperty(templateName + ".deprecation");
                    if (deprecationMessage != null) {
                        log.warn(deprecationMessage);
                    }
                }
                // Retrieve optional target directory if defined
                String outputDirectoryStr = config.getProperty(templateName + ".target");
                File outputDirectory = (outputDirectoryStr != null)
                        ? new File(generator.getNuxeoHome(), outputDirectoryStr)
                        : getOutputDirectory();
                for (File in : listFiles) {
                    // copy template(s) directories parsing properties
                    newFilesList.addAll(templateParser.processDirectory(in, new File(outputDirectory, in.getName())));
                }
            }
        }
        storeNewFilesList(newFilesList);
    }

    /**
     * Delete files previously deployed by templates. If a file had been overwritten by a template, it will be restored.
     * Helps the server returning to the state before any template was applied.
     */
    private void deleteTemplateFiles() throws IOException, ConfigurationException {
        File newFiles = new File(generator.getNuxeoHome(), NEW_FILES);
        if (!newFiles.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(newFiles))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".bak")) {
                    log.debug("Restore {}", line);
                    String originalName = line.substring(0, line.length() - 4);
                    try {
                        File backup = new File(generator.getNuxeoHome(), line);
                        File original = new File(generator.getNuxeoHome(), originalName);
                        FileUtils.copyFile(backup, original);
                        backup.delete();
                    } catch (IOException e) {
                        throw new ConfigurationException(
                                String.format("Failed to restore %s from %s\nEdit or delete %s to bypass that error.",
                                        originalName, line, newFiles),
                                e);
                    }
                } else {
                    log.debug("Remove {}", line);
                    new File(generator.getNuxeoHome(), line).delete();
                }
            }
        }
        newFiles.delete();
    }

    /**
     * Store into {@link #NEW_FILES} the list of new files deployed by the templates. For later use by
     * {@link #deleteTemplateFiles()}
     */
    private void storeNewFilesList(List<String> newFilesList) throws IOException {
        File newFiles = new File(generator.getNuxeoHome(), NEW_FILES);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(newFiles, false), UTF_8))) {
            // Store new files listing
            int index = generator.getNuxeoHome().getCanonicalPath().length() + 1;
            for (String filepath : newFilesList) {
                writer.write(new File(filepath).getCanonicalPath().substring(index));
                writer.newLine();
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
    protected String getDefaultDataDir() {
        return "nxserver" + File.separator + Environment.DEFAULT_DATA_DIR;
    }

    /**
     * Returns the Home of NuxeoRuntime (same as Framework.getRuntime().getHome().getAbsolutePath())
     */
    protected File getRuntimeHome() {
        return new File(generator.getNuxeoHome(), "nxserver");
    }

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
            logDir = new File(generator.getNuxeoHome(), Environment.DEFAULT_LOG_DIR);
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
     * Initialize logs. This is called before {@link ConfigurationGenerator#init()} so the {@code logDir} field is not
     * yet initialized
     *
     * @since 5.4.2
     */
    public void initLogs() {
        File logFile = getLogConfFile();
        String logDirectory = System.getProperty(Environment.NUXEO_LOG_DIR);
        if (logDirectory == null) {
            System.setProperty(Environment.NUXEO_LOG_DIR, getLogDir().getPath());
        }
        if (logFile == null || !logFile.exists()) {
            System.out.println("No logs configuration, will setup a basic one.");
            Configurator.initialize(new DefaultConfiguration());
        } else {
            System.out.println("Try to configure logs with " + logFile);
            Configurator.initialize(Log4JHelper.newConfiguration(logFile));
        }
        log.info("Logs successfully configured.");
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
        File badInstanceClid = new File(generator.getNuxeoHome(),
                getDefaultDataDir() + File.separator + "instance.clid");
        if (badInstanceClid.exists() && !getDataDir().equals(badInstanceClid.getParentFile())) {
            log.warn("Moving {} to {}.", () -> badInstanceClid, this::getDataDir);
            try {
                FileUtils.moveFileToDirectory(badInstanceClid, getDataDir(), true);
            } catch (IOException e) {
                throw new ConfigurationException("NXP-6722 move failed: " + e.getMessage(), e);
            }
        }

        File oldPackagesPath = new File(getDataDir(), getDefaultPackagesDir());
        if (oldPackagesPath.exists() && !oldPackagesPath.equals(getPackagesDir())) {
            log.warn("NXP-8014 Packages cache location changed. You can safely delete {} or move its content to {}",
                    () -> oldPackagesPath, this::getPackagesDir);
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
     * Check if oldPath exist; if so, then raise a ConfigurationException with information for fixing issue
     *
     * @param oldPath Path that must NOT exist
     * @param message Error message thrown with exception
     * @throws ConfigurationException If an old path has been discovered
     */
    protected void checkPath(File oldPath, String message) throws ConfigurationException {
        if (oldPath.exists()) {
            log.error("Deprecated paths used.");
            throw new ConfigurationException(message);
        }
    }

    /**
     * @return Log4J configuration file
     * @since 5.4.2
     */
    public File getLogConfFile() {
        return new File(getServerLibDir(), "log4j2.xml");
    }

    /**
     * @return Nuxeo config directory
     * @since 5.4.2
     */
    public File getConfigDir() {
        return new File(getRuntimeHome(), Environment.DEFAULT_CONFIG_DIR);
    }

    /**
     * @param userConfig Properties to dump into config directory
     * @since 5.4.2
     */
    public void dumpProperties(CryptoProperties userConfig) {
        Properties dumpedProperties = filterSystemProperties(userConfig);
        File dumpedFile = generator.getDumpedConfig();
        try (OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(dumpedFile, false), UTF_8)) {
            dumpedProperties.store(os, "Generated by " + getClass());
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error("Could not dump properties to {}", dumpedFile, e);
        }
    }

    /**
     * Extract Nuxeo properties from given Properties (System properties are removed, except those set by Nuxeo)
     *
     * @param properties Properties to be filtered
     * @return copy of given properties filtered out of System properties
     * @since 5.4.2
     */
    public Properties filterSystemProperties(CryptoProperties properties) {
        Properties dumpedProperties = new Properties();
        for (@SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = (Enumeration<String>) properties.propertyNames(); propertyNames.hasMoreElements();) {
            String key = propertyNames.nextElement();
            // Exclude System properties except Nuxeo's System properties
            if (!System.getProperties().containsKey(key) || NUXEO_SYSTEM_PROPERTIES.contains(key)) {
                dumpedProperties.setProperty(key, properties.getRawProperty(key));
            }
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
    public File getServerLibDir() {
        return new File(generator.getNuxeoHome(), "lib");
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
        InetAddress bindAddress = generator.getBindAddress();
        ConfigurationGenerator.checkPortAvailable(bindAddress,
                Integer.parseInt(generator.getUserConfig().getProperty(PARAM_HTTP_TOMCAT_ADMIN_PORT)));
    }

    /**
     * @return Marketplace Packages directory
     * @since 5.9.4
     */
    public File getPackagesDir() {
        if (packagesDir == null) {
            packagesDir = new File(generator.getNuxeoHome(), getDefaultPackagesDir());
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
        nxInstance.NUXEO_CONF = generator.getNuxeoConf().getPath();
        nxInstance.NUXEO_HOME = generator.getNuxeoHome().getPath();
        // distribution
        File distFile = new File(generator.getConfigDir(), "distribution.properties");
        if (!distFile.exists()) {
            // fallback in the file in templates
            distFile = new File(generator.getNuxeoHome(), "templates");
            distFile = new File(distFile, "common");
            distFile = new File(distFile, "config");
            distFile = new File(distFile, "distribution.properties");
        }
        try {
            nxInstance.distribution = new DistributionInfo(distFile);
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
        // templates
        nxInstance.config = new ConfigurationInfo();
        nxInstance.config.dbtemplate = generator.extractDatabaseTemplateName();
        String userTemplates = generator.getUserTemplates();
        StringTokenizer st = new StringTokenizer(userTemplates, ",");
        while (st.hasMoreTokens()) {
            String template = st.nextToken();
            if (template.equals(nxInstance.config.dbtemplate)) {
                continue;
            }
            if (pkgTemplates.contains(template)) {
                nxInstance.config.pkgtemplates.add(template);
            } else {
                File testBase = new File(generator.getNuxeoHome(),
                        ConfigurationGenerator.TEMPLATES + File.separator + template);
                if (testBase.exists()) {
                    nxInstance.config.basetemplates.add(template);
                } else {
                    nxInstance.config.usertemplates.add(template);
                }
            }
        }
        // Settings from nuxeo.conf
        CryptoProperties userConfig = generator.getUserConfig();
        for (Object item : new TreeSet<>(userConfig.keySet())) {
            String key = (String) item;
            String value = userConfig.getRawProperty(key);
            if (JAVA_OPTS.equals(key)) {
                value = generator.getJavaOptsString();
            }
            if (ConfigurationGenerator.SECRET_KEYS.contains(key) || key.contains("password")
                    || key.equals(Environment.SERVER_STATUS_KEY) || Crypto.isEncrypted(value)) {
                value = "********";
            }
            nxInstance.config.keyvals.add(new KeyValueInfo(key, value));
        }
        return nxInstance;
    }
}
