/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.log;

import org.apache.commons.logging.Log;

/**
 * Simple logger that wraps a bufferized string logger (for remote retrieval) and a log4J logger
 *
 * @author tiry
 */
public class BasicLogger implements ImporterLogger {

    protected Log javaLogger;

    protected boolean bufferActive = false;

    public BasicLogger(Log javaLogger) {
        this.javaLogger = javaLogger;
    }

    @Override
    public void info(String message) {
        javaLogger.info(message);
    }

    @Override
    public void warn(String message) {
        javaLogger.warn(message);
    }

    @Override
    public void debug(String message) {
        javaLogger.debug(message);
    }

    @Override
    public void debug(String message, Throwable t) {
        javaLogger.debug(message, t);
    }

    @Override
    public void error(String message) {
        javaLogger.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        javaLogger.error(message, t);
    }

    @Override
    public String getLoggerBuffer(String sep) {
        return "";
    }

    @Override
    public String getLoggerBuffer() {
        if (bufferActive) {
            return getLoggerBuffer("\n");
        } else {
            return "Buffer is not active";
        }
    }

    @Override
    public boolean isBufferActive() {
        return bufferActive;
    }

    @Override
    public void setBufferActive(boolean active) {
        bufferActive = active;
    }

}
