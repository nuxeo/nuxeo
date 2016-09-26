/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.webengine.model.View;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit Service - manage document versions TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get audit records
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "audits", type = "AuditService", targetType = "Document")
public class AuditService extends DefaultAdapter {

    @GET
    public Object doGet() {
        return new View(getTarget(), "audits").resolve();
    }

    public List<LogEntry> getAudits() {
        Logs logs = Framework.getService(Logs.class);
        DocumentObject document = (DocumentObject) getTarget();
        DocumentModel model = document.getAdapter(DocumentModel.class);
        String id = model.getId();
        String repo = model.getRepositoryName();
        return logs.getLogEntriesFor(id, repo);
    }

}
