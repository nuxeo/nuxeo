/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.log4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * Provides helper methods for working with log4j
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class Log4JHelper {
    private static final Log log = LogFactory.getLog(Log4JHelper.class);

    /**
     * Returns list of files produced by {@link FileAppender}s defined in a given {@link Configuration}. There's no
     * need for the log4j configuration corresponding to this repository of being active.
     *
     * @param configuration the {@link Configuration} to browse looking for {@link FileAppender}
     * @return {@link FileAppender}s present in configuration
     * @since 10.3
     */
    public static List<String> getFileAppendersFileNames(Configuration configuration) {
        List<String> names = new ArrayList<>();
        for (Appender appender : configuration.getAppenders().values()) {
            if (appender instanceof FileAppender) {
                names.add(((FileAppender) appender).getFileName());
            } else if (appender instanceof RollingFileAppender) {
                names.add(((RollingFileAppender) appender).getFileName());
            }
        }
        return names;
    }

    /**
     * Creates a {@link Configuration} initialized with given log4j configuration file without making this
     * configuration active.
     *
     * @param log4jConfigurationFile the XML configuration file to load
     * @return {@link Configuration} initialized with log4jConfigurationFile
     * @since 10.3
     */
    public static Configuration newConfiguration(File log4jConfigurationFile) {
        if (log4jConfigurationFile == null || !log4jConfigurationFile.exists()) {
            throw new IllegalArgumentException("Missing Log4J configuration: " + log4jConfigurationFile);
        } else {
            XmlConfiguration configuration = new XmlConfiguration(null,
                    ConfigurationSource.fromUri(log4jConfigurationFile.toURI()));
            configuration.initialize();
            log.debug("Log4j configuration " + log4jConfigurationFile + " successfully loaded.");
            return configuration;
        }
    }

    /**
     * @see #getFileAppendersFileNames(Configuration)
     * @param log4jConfigurationFile the XML configuration file to load
     * @return {@link FileAppender}s defined in log4jConfigurationFile
     * @since 10.3
     */
    public static List<String> getFileAppendersFileNames(File log4jConfigurationFile) {
        return getFileAppendersFileNames(newConfiguration(log4jConfigurationFile));
    }

    /**
     * Set DEBUG level on the given logger.
     *
     * @param loggerNames the logger names to change level
     * @param level the level to set
     * @param includeChildren whether or not to change children levels
     * @since 10.3
     */
    public static void setLevel(String[] loggerNames, Level level, boolean includeChildren) {
        if (includeChildren) {
            // don't use Configurator.setAllLevels(String, Level) in order to reload configuration only once
            LoggerContext loggerContext = LoggerContext.getContext(false);
            Configuration config = loggerContext.getConfiguration();
            boolean set = false;
            for (String parentLogger : loggerNames) {
                set |= setLevel(parentLogger, level, config);
                for (final Map.Entry<String, LoggerConfig> entry : config.getLoggers().entrySet()) {
                    if (entry.getKey().startsWith(parentLogger)) {
                        set |= setLevel(entry.getValue(), level);
                    }
                }
            }
            if (set) {
                loggerContext.updateLoggers();
            }
        } else {
            Configurator.setLevel(Stream.of(loggerNames).collect(Collectors.toMap(Function.identity(), l -> level)));
        }
    }

    // Copied from org.apache.logging.log4j.core.config.Configurator
    private static boolean setLevel(final String loggerName, final Level level, final Configuration config) {
        boolean set;
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        if (!loggerName.equals(loggerConfig.getName())) {
            // TODO Should additivity be inherited?
            loggerConfig = new LoggerConfig(loggerName, level, true);
            config.addLogger(loggerName, loggerConfig);
            loggerConfig.setLevel(level);
            set = true;
        } else {
            set = setLevel(loggerConfig, level);
        }
        return set;
    }

    // Copied from org.apache.logging.log4j.core.config.Configurator
    private static boolean setLevel(final LoggerConfig loggerConfig, final Level level) {
        final boolean set = !loggerConfig.getLevel().equals(level);
        if (set) {
            loggerConfig.setLevel(level);
        }
        return set;
    }

}
