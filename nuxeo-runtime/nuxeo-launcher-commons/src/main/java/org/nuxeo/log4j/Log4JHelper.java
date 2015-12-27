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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Provides helper methods for working with log4j
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class Log4JHelper {
    private static final Log log = LogFactory.getLog(Log4JHelper.class);

    public static final String CONSOLE_APPENDER_NAME = "CONSOLE";

    protected static final String FULL_PATTERN_LAYOUT = "%d{HH:mm:ss,SSS} %-5p [%l] %m%n";

    protected static final String LIGHT_PATTERN_LAYOUT = "%m%n";

    /**
     * Returns list of files produced by {@link FileAppender}s defined in a given {@link LoggerRepository}. There's no
     * need for the log4j configuration corresponding to this repository of being active.
     *
     * @param loggerRepository {@link LoggerRepository} to browse looking for {@link FileAppender}
     * @return {@link FileAppender}s configured in loggerRepository
     */
    public static ArrayList<String> getFileAppendersFiles(LoggerRepository loggerRepository) {
        ArrayList<String> logFiles = new ArrayList<>();
        for (Enumeration<Appender> appenders = loggerRepository.getRootLogger().getAllAppenders(); appenders.hasMoreElements();) {
            Appender appender = appenders.nextElement();
            if (appender instanceof FileAppender) {
                FileAppender fileAppender = (FileAppender) appender;
                logFiles.add(fileAppender.getFile());
            }
        }
        Enumeration<Logger> currentLoggers = loggerRepository.getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            Logger logger = (currentLoggers.nextElement());
            for (Enumeration<Appender> appenders = logger.getAllAppenders(); appenders.hasMoreElements();) {
                Appender appender = appenders.nextElement();
                if (appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;
                    logFiles.add(fileAppender.getFile());
                }
            }
        }
        return logFiles;
    }

    /**
     * Creates a {@link LoggerRepository} initialized with given log4j configuration file without making this
     * configuration active.
     *
     * @param log4jConfigurationFile XML Log4J configuration file to load.
     * @return {@link LoggerRepository} initialized with log4jConfigurationFile
     */
    public static LoggerRepository getNewLoggerRepository(File log4jConfigurationFile) {
        LoggerRepository loggerRepository = null;
        try {
            loggerRepository = new DefaultRepositorySelector(new Hierarchy(new RootLogger(Level.DEBUG))).getLoggerRepository();
            if (log4jConfigurationFile == null || !log4jConfigurationFile.exists()) {
                log.error("Missing Log4J configuration: " + log4jConfigurationFile);
            } else {
                new DOMConfigurator().doConfigure(log4jConfigurationFile.toURI().toURL(), loggerRepository);
                log.debug("Log4j configuration " + log4jConfigurationFile + " successfully loaded.");
            }
        } catch (MalformedURLException e) {
            log.error("Could not load " + log4jConfigurationFile, e);
        }
        return loggerRepository;
    }

    /**
     * @see #getFileAppendersFiles(LoggerRepository)
     * @param log4jConfigurationFile
     * @return {@link FileAppender}s defined in log4jConfigurationFile
     */
    public static ArrayList<String> getFileAppendersFiles(File log4jConfigurationFile) {
        return getFileAppendersFiles(getNewLoggerRepository(log4jConfigurationFile));
    }

    /**
     * Set DEBUG level on the given category and the children categories. Also change the pattern layout of the given
     * appenderName.
     *
     * @since 5.6
     * @param categories Log4J categories for which to switch debug log level (comma separated values)
     * @param debug set debug log level to true or false
     * @param includeChildren Also set/unset debug mode on children categories
     * @param appenderNames Appender names on which to set a detailed pattern layout. Ignored if null.
     */
    public static void setDebug(String categories, boolean debug, boolean includeChildren, String[] appenderNames) {
        setDebug(categories.split(","), debug, includeChildren, appenderNames);
    }

    /**
     * @param categories
     * @param debug
     * @param includeChildren
     * @param appenderNames
     * @since 7.4
     */
    public static void setDebug(String[] categories, boolean debug, boolean includeChildren, String[] appenderNames) {
        Level newLevel = debug ? Level.DEBUG : Level.INFO;

        // Manage categories
        for (String category : categories) { // Create non existing loggers
            Logger logger = Logger.getLogger(category);
            logger.setLevel(newLevel);
            log.info("Log level set to " + newLevel + " for: " + logger.getName());
        }
        if (includeChildren) { // Also change children categories' level
            for (Enumeration<Logger> loggers = LogManager.getCurrentLoggers(); loggers.hasMoreElements();) {
                Logger logger = loggers.nextElement();
                if (logger.getLevel() == newLevel) {
                    continue;
                }
                for (String category : categories) {
                    if (logger.getName().startsWith(category)) {
                        logger.setLevel(newLevel);
                        log.info("Log level set to " + newLevel + " for: " + logger.getName());
                        break;
                    }
                }
            }
        }

        // Manage appenders
        if (ArrayUtils.isEmpty(appenderNames)) {
            return;
        }
        for (String appenderName : appenderNames) {
            Appender consoleAppender = Logger.getRootLogger().getAppender(appenderName);
            if (consoleAppender != null) {
                Filter filter = consoleAppender.getFilter();
                while (filter != null && !(filter instanceof LevelRangeFilter)) {
                    filter = filter.getNext();
                }
                if (filter != null) {
                    LevelRangeFilter levelRangeFilter = (LevelRangeFilter) filter;
                    levelRangeFilter.setLevelMin(newLevel);
                    log.debug(String.format("Log level filter set to %s for appender %s", newLevel, appenderName));
                }
                String patternLayout = debug ? FULL_PATTERN_LAYOUT : LIGHT_PATTERN_LAYOUT;
                consoleAppender.setLayout(new PatternLayout(patternLayout));
                log.info(String.format("Set pattern layout of %s to %s", appenderName, patternLayout));
            }
        }
    }

    /**
     * Set DEBUG level on the given category and change pattern layout of {@link #CONSOLE_APPENDER_NAME} if defined.
     * Children categories are unchanged.
     *
     * @since 5.5
     * @param category Log4J category for which to switch debug log level
     * @param debug set debug log level to true or false
     * @see #setDebug(String, boolean, boolean, String[])
     */
    public static void setDebug(String category, boolean debug) {
        setDebug(category, debug, false, new String[] { CONSOLE_APPENDER_NAME });
    }

    /**
     * Set "quiet" mode: set log level to WARN for the given Log4J appender.
     *
     * @param appenderName Log4J appender to switch to WARN
     * @since 5.5
     */
    public static void setQuiet(String appenderName) {
        Appender appender = Logger.getRootLogger().getAppender(appenderName);
        if (appender == null) {
            return;
        }
        Filter filter = appender.getFilter();
        while (filter != null && !(filter instanceof LevelRangeFilter)) {
            filter = filter.getNext();
        }
        if (filter != null) {
            LevelRangeFilter levelRangeFilter = (LevelRangeFilter) filter;
            levelRangeFilter.setLevelMin(Level.WARN);
            log.debug("Log level filter set to WARN for appender " + appenderName);
        }
    }

}
