/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * An operation to log in log4j.
 *
 * @since 5.6
 */
@Operation(id = LogOperation.ID, category = Constants.CAT_NOTIFICATION, label = "Log", description = "Logging with log4j", aliases = { "LogOperation" })
public class LogOperation {

    public static final String ID = "Log";

    @Param(name = "category", required = false)
    protected String category;

    @Param(name = "message", required = true)
    protected String message;

    @Param(name = "level", required = true, widget = Constants.W_OPTION, values = { "info", "debug", "warn", "error" })
    protected String level = "info";

    @OperationMethod
    public void run() {
        if (category == null) {
            category = "org.nuxeo.ecm.automation.logger";
        }

        Log log = LogFactory.getLog(category);

        if ("debug".equals(level)) {
            log.debug(message);
            return;
        }

        if ("warn".equals(level)) {
            log.warn(message);
            return;
        }

        if ("error".equals(level)) {
            log.error(message);
            return;
        }
        // in any other case, use info log level
        log.info(message);

    }
}
