/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere <targuillere@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * This helper writes in the log as browsers object console.log(), console.error(), console.warn() in Automation
 * Scripting. Usage is with an uppercase "C". If logs info or trace are deactivated, Dev mode has to be set to display
 * Automation scripting logs.
 *
 * @since 7.10
 */
public class Console implements ContextHelper {

    private static final Log log = LogFactory.getLog(Console.class);

    protected static boolean infoEnabled = log.isInfoEnabled();

    protected static boolean traceEnabled = log.isTraceEnabled();

    public void error(String inWhat) {
        log.error(inWhat);
    }

    public void warn(String inWhat) {
        log.warn(inWhat);
    }

    public void log(String inWhat) {
        if (infoEnabled) {
            log.info(inWhat);
        } else if (Framework.isDevModeSet()) {
            log.warn("[LOG] " + inWhat);
        }
    }

    /*
     * info() and log() are handled the same way
     */
    public void info(String inWhat) {
        if (infoEnabled) {
            log.info(inWhat);
        } else if (Framework.isDevModeSet()) {
            log.warn("[INFO] " + inWhat);
        }
    }

    public void trace(String inWhat) {
        if (traceEnabled) {
            log.trace(inWhat);
        } else if (Framework.isDevModeSet()) {
            log.warn("[TRACE] " + inWhat);
        }
    }

}