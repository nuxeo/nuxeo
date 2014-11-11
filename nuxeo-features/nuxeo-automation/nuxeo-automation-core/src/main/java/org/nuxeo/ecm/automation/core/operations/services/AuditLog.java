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
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = AuditLog.ID, category = Constants.CAT_SERVICES, label = "Log Event In Audit", description = "Log events into audit for each of the input document. The operation accept as input one ore more documents that are returned back as the output.")
public class AuditLog {

    public static final String ID = "Audit.Log";

    @Context
    protected AuditLogger logger;

    @Context
    protected OperationContext ctx;

    @Param(name = "event", widget = Constants.W_AUDIT_EVENT)
    protected String event;

    @Param(name = "category", required = false, values = "Automation")
    protected String category = "Automation";

    @Param(name = "comment", required = false, widget = Constants.W_MULTILINE_TEXT)
    protected String comment = "";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        Principal principal = ctx.getPrincipal();
        LogEntry entry = newEntry(doc, principal != null ? principal.getName()
                : null, new Date());
        logger.addLogEntries(Collections.singletonList(entry));
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        Date date = new Date();
        Principal principal = ctx.getPrincipal();
        String uname = principal != null ? principal.getName() : null;
        for (DocumentModel doc : docs) {
            entries.add(newEntry(doc, uname, date));
        }
        logger.addLogEntries(entries);
        return docs;
    }

    protected LogEntry newEntry(DocumentModel doc, String principal, Date date) {
        LogEntry entry = new LogEntry();
        entry.setEventId(event);
        entry.setEventDate(new Date());
        entry.setCategory(category);
        entry.setDocUUID(doc.getId());
        entry.setDocPath(doc.getPathAsString());
        entry.setComment(comment);
        entry.setPrincipalName(principal);
        entry.setDocType(doc.getType());
        try {
            entry.setDocLifeCycle(doc.getCurrentLifeCycleState());
        } catch (Exception e) {
            // ignore error
        }
        return entry;
    }

}
