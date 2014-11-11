/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jetty;

import org.apache.commons.logging.Log;
import org.mortbay.log.Logger;

/**
 * Dumb logger to see what appends in Jetty.
 *
 * @author Thierry Delprat
 */
public class Log4JLogger implements Logger {

    protected final Log logger;

    public Log4JLogger(Log logger) {
        this.logger = logger;
    }

    public void debug(String msg, Throwable th) {
        logger.debug(msg, th);
    }

    public void debug(String msg, Object arg0, Object arg1) {
        logger.debug(String.format(msg, arg0, arg1));
    }

    public Logger getLogger(String name) {
        return this;
    }

    public void info(String msg, Object arg0, Object arg1) {
        logger.info(String.format(msg, arg0, arg1));
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void setDebugEnabled(boolean enabled) {
    }

    public void warn(String msg, Throwable th) {
        logger.warn(msg, th);
    }

    public void warn(String msg, Object arg0, Object arg1) {
        logger.warn(String.format(msg, arg0, arg1));
    }

}
