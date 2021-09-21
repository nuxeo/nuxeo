/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.CryptoProperties;

/**
 * Class used to hold the configuration for Nuxeo.
 *
 * @since 11.5
 */
public class ConfigurationHolder {

    protected static final List<String> DB_LIST = asList("default", "mongodb", "postgresql", "oracle", "mysql",
            "mariadb", "mssql", "db2");

    protected static final Set<String> DIRECTORY_PARAMETERS = Set.of(Environment.NUXEO_CONFIG_DIR,
            Environment.NUXEO_DATA_DIR, Environment.NUXEO_LOG_DIR, Environment.NUXEO_MP_DIR, Environment.NUXEO_PID_DIR,
            Environment.NUXEO_TMP_DIR);

    protected static final Path LOG4J2_CONF = Path.of("lib", "log4j2.xml");

    protected static final Path NXSERVER = Path.of("nxserver");

    protected static final Path TEMPLATES = Path.of("templates");

    /** Path representing the Nuxeo Server home directory. Usually {@code /opt/nuxeo/server}. */
    protected final Path home;

    /** Path representing the Nuxeo Server configuration file. Usually {@code /opt/nuxeo/server/bin/nuxeo.conf}. */
    protected final Path nuxeoConf;

    /**
     * Properties representing concatenation of {@code templates/nuxeo.defaults}, {@link System#getProperties()} and
     * {@code nuxeo.defaults} from each template.
     */
    protected final Properties defaultConfig;

    /** Properties representing {@code nuxeo.conf} with a fallback on {@link #defaultConfig}. */
    protected final CryptoProperties userConfig;

    /** Templates included in this configuration. */
    protected final List<Path> templates;

    public ConfigurationHolder(Path home, Path nuxeoConf) {
        this.home = requireNonNull(home).toAbsolutePath();
        this.nuxeoConf = requireNonNull(nuxeoConf).toAbsolutePath();
        // initialize some defaults
        var basicConfig = new Properties();
        UnaryOperator<String> homeResolver = k -> getHomePath().resolve(k).toString();
        UnaryOperator<String> runtimeResolver = k -> getRuntimeHomePath().resolve(k).toString();
        basicConfig.put(Environment.NUXEO_CONFIG_DIR, runtimeResolver.apply(Environment.DEFAULT_CONFIG_DIR));
        basicConfig.put(Environment.NUXEO_DATA_DIR, runtimeResolver.apply(Environment.DEFAULT_DATA_DIR));
        basicConfig.put(Environment.NUXEO_LOG_DIR, homeResolver.apply(Environment.DEFAULT_LOG_DIR));
        basicConfig.put(Environment.NUXEO_MP_DIR, homeResolver.apply(Environment.DEFAULT_MP_DIR));
        basicConfig.put(Environment.NUXEO_PID_DIR, homeResolver.apply(Environment.DEFAULT_LOG_DIR));
        basicConfig.put(Environment.NUXEO_TMP_DIR, homeResolver.apply(Environment.DEFAULT_TMP_DIR));
        defaultConfig = new Properties(basicConfig);
        userConfig = new CryptoProperties(defaultConfig);
        templates = new ArrayList<>();
    }

    public Path getHomePath() {
        return home;
    }

    public Path getNuxeoConfPath() {
        return nuxeoConf;
    }

    public Path getTemplatesPath() {
        return home.resolve(TEMPLATES);
    }

    /**
     * Returns the Home of NuxeoRuntime (same as {@code Framework.getRuntime().getHome()}).
     */
    public Path getRuntimeHomePath() {
        return home.resolve(NXSERVER);
    }

    public Path getConfigurationPath() {
        return getPropertyAsPath(Environment.NUXEO_CONFIG_DIR);
    }

    public Path getDataPath() {
        return getPropertyAsPath(Environment.NUXEO_DATA_DIR);
    }

    public Path getDumpedConfigurationPath() {
        return getConfigurationPath().resolve(ConfigurationConstants.FILE_CONFIGURATION_PROPERTIES);
    }

    public Path getLogPath() {
        return getPropertyAsPath(Environment.NUXEO_LOG_DIR);
    }

    public Path getLogConfigPath() {
        return getHomePath().resolve(LOG4J2_CONF);
    }

    protected Path getPackagesPath() {
        return getPropertyAsPath(Environment.NUXEO_MP_DIR);
    }

    public Path getPidDirPath() {
        return getPropertyAsPath(Environment.NUXEO_PID_DIR);
    }

    public Path getTmpPath() {
        return getPropertyAsPath(Environment.NUXEO_TMP_DIR);
    }

    public boolean isLoaded() {
        return !userConfig.isEmpty();
    }

    /**
     * Returns the property value associated with the given {@code key} from the default configuration.
     */
    public String getDefaultProperty(String key) {
        return defaultConfig.getProperty(key);
    }

    /**
     * Returns the property value associated with the given {@code key} from the user configuration.
     * <p>
     * The method falls back on the default configuration if the user configuration doesn't contain a property value
     * associated with the given {@code key}.
     */
    public String getProperty(String key) {
        return userConfig.getProperty(key);
    }

    /**
     * Returns the property value associated with the given {@code key} from the user configuration.
     * <p>
     * The method falls back on the default configuration if the user configuration doesn't contain a property value
     * associated with the given {@code key}, then falls back on the given {@code defaultValue} if the default
     * configuration doesn't contain a property value associated with then {@code key}.
     */
    public String getProperty(String key, String defaultValue) {
        return userConfig.getProperty(key, defaultValue);
    }

    /**
     * Returns the property value associated with the given {@code key} from the user configuration.
     */
    public Optional<String> getOptProperty(String key) {
        return Optional.ofNullable(userConfig.getProperty(key));
    }

    /**
     * Returns the property value as a boolean associated with the given {@code key} from the user configuration.
     */
    public boolean getPropertyAsBoolean(String key) {
        return Boolean.parseBoolean(getProperty(key, "false"));
    }

    /**
     * Returns the property value as an int associated with the given {@code key} from the user configuration.
     *
     * @throws NumberFormatException – if the string does not contain a parsable integer
     */
    public int getPropertyAsInteger(String key, int defaultValue) {
        return Integer.parseInt(getProperty(key, String.valueOf(defaultValue)));
    }

    /**
     * Returns the property value as a {@link Path} associated with the given {@code key} from the user configuration.
     *
     * @throws InvalidPathException – if the path string cannot be converted to a Path
     */
    public Path getPropertyAsPath(String key) {
        return Path.of(getProperty(key));
    }

    /**
     * Returns the raw property value associated with the given {@code key} from the user configuration.
     */
    public String getRawProperty(String key) {
        return userConfig.getRawProperty(key);
    }

    /**
     * Returns the user configuration key set.
     *
     * @see Properties#keySet()
     */
    @SuppressWarnings("unchecked")
    public Set<String> keySet() {
        return (Set<String>) (Set<?>) userConfig.keySet();
    }

    /**
     * Returns the user configuration property names.
     *
     * @see Properties#stringPropertyNames()
     */
    public Set<String> stringPropertyNames() {
        return userConfig.stringPropertyNames();
    }

    public boolean isForceGenerationOnce() {
        return "once".equals(userConfig.getProperty(ConfigurationConstants.PARAM_FORCE_GENERATION));
    }

    public boolean isForceGeneration() {
        return isForceGenerationOnce()
                || "true".equals(userConfig.getProperty(ConfigurationConstants.PARAM_FORCE_GENERATION));
    }

    /**
     * Puts the given property into default configuration.
     */
    public String putDefault(String key, String value) {
        return (String) defaultConfig.put(key, makePathAbsolute(key, value));
    }

    /**
     * Puts all given {@code properties} into default configuration.
     */
    public void putDefaultAll(Properties properties) {
        defaultConfig.putAll(makePathAbsolute(properties));
    }

    /**
     * Puts the given property into user configuration.
     */
    public String put(String key, String value) {
        return (String) userConfig.put(key, makePathAbsolute(key, value));
    }

    /**
     * Puts all given {@code properties} into user configuration.
     */
    public void putAll(Properties properties) {
        userConfig.putAll(makePathAbsolute(properties));
    }

    /**
     * Puts all given {@code properties} into default configuration for the given template.
     */
    public void putTemplateAll(Path template, Properties properties) {
        templates.add(template);
        defaultConfig.putAll(makePathAbsolute(properties));
    }

    protected Properties makePathAbsolute(Properties properties) {
        properties.replaceAll((k, v) -> makePathAbsolute((String) k, (String) v));
        return properties;
    }

    protected String makePathAbsolute(String key, String value) {
        if (DIRECTORY_PARAMETERS.contains(key)) {
            return home.resolve(value).toString();
        }
        return value;
    }

    /**
     * @return the absolute {@link Path}s of the included templates
     */
    public List<Path> getIncludedTemplates() {
        return Collections.unmodifiableList(templates);
    }

    /**
     * @return the names of the included templates (ie: name of template directory)
     */
    public List<String> getIncludedTemplateNames() {
        return templates.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.toUnmodifiableList());
    }

    /**
     * @return the name of the included DB template
     */
    public String getIncludedDBTemplateName() {
        return templates.stream()
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(DB_LIST::contains)
                        .reduce("unknown", (first, second) -> second);
    }

    protected void clear() {
        defaultConfig.clear();
        userConfig.clear();
        templates.clear();
    }
}
