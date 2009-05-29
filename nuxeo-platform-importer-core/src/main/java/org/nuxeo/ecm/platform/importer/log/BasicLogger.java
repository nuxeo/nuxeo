package org.nuxeo.ecm.platform.importer.log;

import org.apache.commons.logging.Log;

/**
 *
 * Simple logger that wraps
 * a bufferized string logger (for remote retrieval)
 * and a log4J logger
 *
 * @author tiry
 *
 */
public class BasicLogger implements ImporterLogger {

    protected Log javaLogger;

    protected boolean bufferActive = false;

    public BasicLogger(Log javaLogger) {
        this.javaLogger = javaLogger;
    }

    public void info(String message) {
        javaLogger.info(message);
    }

    public void warn(String message) {
        javaLogger.warn(message);
    }

    public void debug(String message) {
        javaLogger.debug(message);
    }

    public void debug(String message, Throwable t) {
        javaLogger.debug(message, t);
    }

    public void error(String message) {
        javaLogger.error(message);
    }

    public void error(String message, Throwable t) {
        javaLogger.error(message, t);
    }

    public String getLoggerBuffer(String sep) {
        return "";
    }

    public String getLoggerBuffer() {
        if (bufferActive) {
            return getLoggerBuffer("\n");
        }
        else {
            return "Buffer is not active";
        }
    }

    public boolean isBufferActive() {
        return bufferActive;
    }

    public void setBufferActive(boolean active) {
        bufferActive = active;
    }

}
