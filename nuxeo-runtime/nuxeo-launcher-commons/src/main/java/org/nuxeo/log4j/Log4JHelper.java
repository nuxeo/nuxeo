/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.log4j;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Provides helper methods for working with log4j
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class Log4JHelper {
    private static final Log log = LogFactory.getLog(Log4JHelper.class);

    /**
     * Returns list of files produced by {@link FileAppender}s defined in a
     * given {@link LoggerRepository}. There's no need for the log4j
     * configuration corresponding to this repository of being active.
     *
     * @param loggerRepository {@link LoggerRepository} to browse looking for
     *            {@link FileAppender}
     * @return {@link FileAppender}s configured in loggerRepository
     */
    public static ArrayList<String> getFileAppendersFiles(
            LoggerRepository loggerRepository) {
        ArrayList<String> logFiles = new ArrayList<String>();
        for (@SuppressWarnings("unchecked")
        Enumeration<Appender> appenders = loggerRepository.getRootLogger().getAllAppenders(); appenders.hasMoreElements();) {
            Appender appender = appenders.nextElement();
            if (appender instanceof FileAppender) {
                FileAppender fileAppender = (FileAppender) appender;
                logFiles.add(fileAppender.getFile());
            }
        }
        return logFiles;
    }

    /**
     * Creates a {@link LoggerRepository} initialized with given log4j
     * configuration file without making this configuration active.
     *
     * @param log4jConfigurationFile XML Log4J configuration file to load.
     * @return {@link LoggerRepository} initialized with log4jConfigurationFile
     */
    public static LoggerRepository getNewLoggerRepository(
            File log4jConfigurationFile) {
        LoggerRepository loggerRepository = null;
        try {
            loggerRepository = new DefaultRepositorySelector(new Hierarchy(
                    new RootLogger(Level.DEBUG))).getLoggerRepository();
            new DOMConfigurator().doConfigure(
                    log4jConfigurationFile.toURI().toURL(), loggerRepository);
            log.debug("Log4j configuration " + log4jConfigurationFile
                    + " succesfully loaded.");
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
    public static ArrayList<String> getFileAppendersFiles(
            File log4jConfigurationFile) {
        return getFileAppendersFiles(getNewLoggerRepository(log4jConfigurationFile));
    }
}
