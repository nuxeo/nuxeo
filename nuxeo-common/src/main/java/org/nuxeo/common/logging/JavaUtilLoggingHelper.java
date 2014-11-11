/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.common.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper that can redirect all {@code java.util.logging} messages to the Apache
 * Commons Logging implementation.
 *
 * @author Florent Guillaume
 */
public class JavaUtilLoggingHelper {

    private static final Log log = LogFactory.getLog(JavaUtilLoggingHelper.class);

    private static LogHandler activeHandler;

    //Utility class.
    private JavaUtilLoggingHelper() {
    }

    /**
     * Redirects {@code java.util.logging} to Apache Commons Logging
     */
    public static void redirectToApacheCommons() {
        if (activeHandler != null) {
            return;
        }
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
            activeHandler = new LogHandler();
            activeHandler.setLevel(Level.ALL);
            rootLogger.addHandler(activeHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (Exception e) {
            log.error("Handler setup failed", e);
        }
    }

    /**
     * Resets {@code java.util.logging} redirections.
     */
    public static void reset() {
        if (activeHandler == null) {
            return;
        }
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            rootLogger.removeHandler(activeHandler);
        } catch (Exception e) {
            log.error("Handler removal failed", e);
        }
    }

    public static class LogHandler extends Handler {

        private final Map<String, Log> cache = new ConcurrentHashMap<String, Log>();

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            if (level == Level.FINER || level == Level.FINEST) {
                // don't log, too fine
                return;
            }
            String name = record.getLoggerName();
            Log log = cache.get(name);
            if (log == null) {
                log = LogFactory.getLog(name);
                cache.put(name, log);
            }
            if (level == Level.FINE) {
                log.trace(record.getMessage(), record.getThrown());
            } else if (level == Level.CONFIG) {
                log.debug(record.getMessage(), record.getThrown());
            } else if (level == Level.INFO) {
                log.info(record.getMessage(), record.getThrown());
            } else if (level == Level.WARNING) {
                log.warn(record.getMessage(), record.getThrown());
            } else if (level == Level.SEVERE) {
                log.error(record.getMessage(), record.getThrown());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

}
