/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
