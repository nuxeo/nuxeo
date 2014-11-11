/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *     Benoit Delbosc
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

    // Utility class.
    private JavaUtilLoggingHelper() {
    }

    /**
     * Redirects {@code java.util.logging} to Apache Commons Logging do not log
     * below INFO level.
     */
    public static synchronized void redirectToApacheCommons() {
        redirectToApacheCommons(Level.INFO);
    }

    /**
     * Redirects {@code java.util.logging} to Apache Commons Logging do not log
     * below the threshold level.
     *
     * @since 5.4.2
     */
    public static synchronized void redirectToApacheCommons(Level threshold) {
        if (activeHandler != null) {
            return;
        }
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
            activeHandler = new LogHandler();
            activeHandler.setLevel(threshold);
            rootLogger.addHandler(activeHandler);
            rootLogger.setLevel(threshold);
            log.info("Redirecting java.util.logging to Apache Commons Logging, threshold is "
                    + threshold.toString());
        } catch (Exception e) {
            log.error("Handler setup failed", e);
        }
    }

    /**
     * Resets {@code java.util.logging} redirections.
     */
    public static synchronized void reset() {
        if (activeHandler == null) {
            return;
        }
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            rootLogger.removeHandler(activeHandler);
        } catch (Exception e) {
            log.error("Handler removal failed", e);
        }
        activeHandler = null;
    }

    public static class LogHandler extends Handler {

        final ThreadLocal<LogRecord> holder = new ThreadLocal<LogRecord>();

        private final Map<String, Log> cache = new ConcurrentHashMap<String, Log>();

        protected void doPublish(LogRecord record) {
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
        public void publish(LogRecord record) {
            if (holder.get() != null) {
                return;
            }
            holder.set(record);
            try {
                doPublish(record);
            } finally {
                holder.remove();
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
