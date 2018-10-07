/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core.operations.services;

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
@Operation(id = AuditLog.ID, category = Constants.CAT_SERVICES, label = "Log Event In Audit", description = "Log events into audit for each of the input document. The operation accept as input one ore more documents that are returned back as the output.", aliases = { "Audit.Log" })
public class AuditLog {

    public static final String ID = "Audit.LogEvent";

    @Context
    protected AuditLogger logger;

    @Context
    protected OperationContext ctx;

    @Param(name = "event", widget = Constants.W_AUDIT_EVENT)
    protected String event;

    @Param(name = "category", required = false, values = { "Automation" })
    protected String category = "Automation";

    @Param(name = "comment", required = false, widget = Constants.W_MULTILINE_TEXT)
    protected String comment = "";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        String uname = ctx.getPrincipal().getActingUser();
        LogEntry entry = newEntry(doc, uname, new Date());
        logger.addLogEntries(Collections.singletonList(entry));
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        Date date = new Date();
        String uname = ctx.getPrincipal().getActingUser();
        for (DocumentModel doc : docs) {
            entries.add(newEntry(doc, uname, date));
        }
        logger.addLogEntries(entries);
        return docs;
    }

    protected LogEntry newEntry(DocumentModel doc, String principal, Date date) {
        LogEntry entry = logger.newLogEntry();
        entry.setEventId(event);
        entry.setEventDate(new Date());
        entry.setCategory(category);
        entry.setDocUUID(doc.getId());
        entry.setDocPath(doc.getPathAsString());
        entry.setComment(comment);
        entry.setPrincipalName(principal);
        entry.setDocType(doc.getType());
        entry.setRepositoryId(doc.getRepositoryName());
        entry.setDocLifeCycle(doc.getCurrentLifeCycleState());
        return entry;
    }

}
