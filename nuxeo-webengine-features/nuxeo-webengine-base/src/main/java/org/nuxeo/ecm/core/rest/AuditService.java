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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.util.List;

import javax.ws.rs.GET;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.View;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit Service - manage document versions
 * TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get audit records
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "audits", type = "AuditService", targetType = "Document")
public class AuditService extends DefaultAdapter {

    protected Logs guardedService() {
        Logs service;
        try {
            service = Framework.getService(Logs.class);
        } catch (Exception e) {
            throw WebException.wrap("Failed to get audit service", e);
        }
        if (service == null) {
            throw new WebException("Failed to get audit service");
        }
        return service;
    }

    @GET
    public Object doGet() {
        return new View(getTarget(), "audits").resolve();
    }

    public List<LogEntry> getAudits() {
        Logs logs = guardedService();
        DocumentObject document = (DocumentObject) getTarget();
        DocumentModel model = document.getAdapter(DocumentModel.class);
        String id = model.getId();
        return logs.getLogEntriesFor(id);
    }

}
