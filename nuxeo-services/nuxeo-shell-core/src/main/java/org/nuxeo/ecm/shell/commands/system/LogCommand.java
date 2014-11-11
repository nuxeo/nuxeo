/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     jcarsique
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;

public class LogCommand extends AbstractCommand {

    private static final String CONSOLE_APPENDER_NAME = "CONSOLE";

    private static final String DEFAULT_CATEGORY = "org.nuxeo.ecm.shell";

    private static final Log log = LogFactory.getLog(LogCommand.class);

    private static boolean debug = false;

    private static final String FULL_PATTERN_LAYOUT = "%d{HH:mm:ss,SSS} %-5p [%C{1}] %m%n";

    private static final String LIGHT_PATTERN_LAYOUT = "%m%n";

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: log filename [log level [package or class]]");
        System.out.println("        log off [package or class]");
        System.out.println("        log debug");
        System.out.println(" filename : logging file destination (writes in append mode)");
        System.out.println(" log level : (optionnal, default=INFO): available values are TRACE, DEBUG, INFO, WARN, ERROR, FATAL");
        System.out.println(" package or class (optionnal, default=\"org.nuxeo.ecm.shell\"): category logged (see Log4J doc. for more details)");
        System.out.println("");
        System.out.println("\"log off\" command stops given logger or all custom loggers if none is specified (resets configuration)");
        System.out.println("");
        System.out.println("\"log debug\" command switches DEBUG mode on/off");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the log command must take at least one argument: log [filename|off|debug]");
            printHelp();
            return;
        } else if ("off".equals(elements[0])) {
            removeLogger(elements);
        } else if ("debug".equals(elements[0])) {
            setDebug(!debug);
        } else {
            try {
                setLogger(elements);
            } catch (FileNotFoundException e) {
                log.error("Couldn't create or open " + elements[0], e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeLogger(String[] elements) {
        if (elements.length > 1) {
            Logger.getLogger(elements[1]).removeAllAppenders();
        } else {
            Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
            while (loggers.hasMoreElements()) {
                Logger logger = loggers.nextElement();
                logger.removeAllAppenders();
            }
            Logger.getLogger(DEFAULT_CATEGORY).removeAllAppenders();
        }
    }

    private void setLogger(String[] elements) throws FileNotFoundException {
        Logger logger;
        if (elements.length > 2) {
            logger = Logger.getLogger(elements[2]);
        } else {
            logger = Logger.getLogger(DEFAULT_CATEGORY);
        }
        if (!elements[0].contains(File.separator)) {
            elements[0] = "log" + File.separator + elements[0];
        }
        OutputStream out = new FileOutputStream(elements[0]);
        Appender appender = new WriterAppender(new PatternLayout(
                FULL_PATTERN_LAYOUT), out);
        logger.addAppender(appender);

        if (elements.length > 1) {
            logger.setLevel(Level.toLevel(elements[1]));
        } else {
            logger.setLevel(Level.INFO);
        }
    }

    public static void setDebug(boolean debugMode) {
        debug = debugMode;
        if (debugMode) {
            Appender consoleAppender = Logger.getLogger(DEFAULT_CATEGORY).getAppender(
                    CONSOLE_APPENDER_NAME);
            consoleAppender.setLayout(new PatternLayout(FULL_PATTERN_LAYOUT));
            Logger.getRootLogger().setLevel(Level.DEBUG);
            Logger.getLogger(DEFAULT_CATEGORY).setLevel(Level.DEBUG);
            Logger.getLogger("org.nuxeo").setLevel(Level.DEBUG);
            log.info("Log level set to DEBUG");
        } else {
            Appender consoleAppender = Logger.getLogger(DEFAULT_CATEGORY).getAppender(
                    CONSOLE_APPENDER_NAME);
            consoleAppender.setLayout(new PatternLayout(LIGHT_PATTERN_LAYOUT));
            Logger.getRootLogger().setLevel(Level.WARN);
            Logger.getLogger(DEFAULT_CATEGORY).setLevel(Level.INFO);
            Logger.getLogger("org.nuxeo").setLevel(Level.INFO);
            log.info("Log level reset to WARN for log file and INFO for console");
        }
    }

}
