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
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.runtime.api.Framework;

/**
 * PostCommit async listener that pushes {@link EventBundle} into the Audit log.
 *
 * @author tiry
 */
public class AuditEventLogger implements PostCommitFilteringEventListener {

    private static final Log log = LogFactory.getLog(AuditEventLogger.class);

    @Override
    public boolean acceptEvent(Event event) {
        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        if (logger == null) {
            return false;
        }
        return logger.getAuditableEventNames().contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
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
