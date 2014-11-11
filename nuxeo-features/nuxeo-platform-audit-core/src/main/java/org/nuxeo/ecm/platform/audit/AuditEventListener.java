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
 *     matic
 */
package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;

public class AuditEventListener extends AbstractEventListener {

    protected NXAuditEventsService guardedService() {
        NXAuditEventsService service;
        service = (NXAuditEventsService) Framework.getRuntime().getComponent(NXAuditEventsService.NAME);
        if (service == null) {
            throw new AuditRuntimeException("Cannot get audit service");
        }
        return service;
    }

    @Override
    public void handleEvent(CoreEvent coreEvent) throws Exception {
        guardedService().logEvent(coreEvent);
    }

    @Override
    public boolean accepts(String eventId) {
        return guardedService().getAuditableEventNames().contains(eventId);
    }

    @Override
    public void addEventId(String eventId) {
        throw new UnsupportedOperationException();
    }

}