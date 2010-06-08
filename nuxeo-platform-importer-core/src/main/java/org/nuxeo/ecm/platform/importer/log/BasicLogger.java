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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.log;

import org.apache.commons.logging.Log;

/**
 *
 * Simple logger that wraps a bufferized string logger (for remote retrieval)
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
        } else {
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
