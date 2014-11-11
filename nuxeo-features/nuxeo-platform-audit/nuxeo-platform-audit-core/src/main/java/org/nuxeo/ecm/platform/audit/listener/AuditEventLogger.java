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

package org.nuxeo.ecm.platform.audit.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.runtime.api.Framework;

/**
 * PostCommit async listener that pushes {@link EventBundle} into the Audit log.
 *
 * @author tiry
 */
public class AuditEventLogger implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(AuditEventLogger.class);

    protected AuditLogger getAuditLogger() throws ClientException {
        try {
            return Framework.getService(AuditLogger.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    public void handleEvent(EventBundle events) throws ClientException {
        AuditLogger logger = getAuditLogger();
        if (logger != null) {
            try {
                logger.logEvents(events);
            } catch (AuditException e) {
                log.error("Unable to persist event bundle into audit log", e);
            }
        } else {
            log.error("Can not reach AuditLogger");
        }
    }

}
