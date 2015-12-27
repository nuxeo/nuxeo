/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.jetty;

import org.apache.commons.logging.Log;
import org.mortbay.log.Logger;

/**
 * Dumb logger to see what happens in Jetty.
 *
 * @author Thierry Delprat
 */
public class Log4JLogger implements Logger {

    protected final Log logger;

    public Log4JLogger(Log logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String msg, Throwable th) {
        logger.debug(msg, th);
    }

    @Override
    public void debug(String msg, Object arg0, Object arg1) {
        logger.debug(String.format(msg, arg0, arg1));
    }

    @Override
    public Logger getLogger(String name) {
        return this;
    }

    @Override
    public void info(String msg, Object arg0, Object arg1) {
        logger.info(String.format(msg, arg0, arg1));
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
    }

    @Override
    public void warn(String msg, Throwable th) {
        logger.warn(msg, th);
    }

    @Override
    public void warn(String msg, Object arg0, Object arg1) {
        logger.warn(String.format(msg, arg0, arg1));
    }

}
