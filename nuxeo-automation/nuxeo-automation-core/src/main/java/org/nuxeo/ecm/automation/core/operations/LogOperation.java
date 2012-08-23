/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
@Operation(id = LogOperation.ID, category = Constants.CAT_NOTIFICATION, label = "LogOperation", description = "")
public class LogOperation {

    public static final String ID = "LogOperation";

    @Param(name = "category", required = false)
    protected String category;

    @Param(name = "message", required = true)
    protected String message;

    @Param(name = "priority", required = false)
    protected String priority;

    @OperationMethod
    public void run() {
        if (category == null) {
            category = "org.nuxeo.ecm.automation.logger";
        }

        Log log = LogFactory.getLog(category);

        if ("debug".equals(priority)) {
            log.debug(message);
            return;
        }

        if ("warn".equals(priority)) {
            log.warn(message);
            return;
        }

        if ("error".equals(priority)) {
            log.error(message);
            return;
        }
        // in any other case, use info priority
        log.info(message);

    }
}
