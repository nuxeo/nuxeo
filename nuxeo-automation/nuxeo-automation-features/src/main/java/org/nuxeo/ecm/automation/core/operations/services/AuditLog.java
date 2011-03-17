/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public DocumentModel run(DocumentModel doc) {
        Principal principal = ctx.getPrincipal();
        LogEntry entry = newEntry(doc, principal != null ? principal.getName()
                : null, new Date());
        logger.addLogEntries(Collections.singletonList(entry));
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
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
        LogEntry entry = logger.newLogEntry();
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
