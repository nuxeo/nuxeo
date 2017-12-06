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

package org.nuxeo.ecm.platform.audit.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
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
        AuditLogger logger = Framework.getService(AuditLogger.class);
        if (logger == null) {
            return false;
        }
        return logger.getAuditableEventNames().contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle events) {
        AuditLogger logger = Framework.getService(AuditLogger.class);
        if (logger != null) {
            logger.logEvents(events);
        } else {
            log.error("Can not reach AuditLogger");
        }
    }

}
