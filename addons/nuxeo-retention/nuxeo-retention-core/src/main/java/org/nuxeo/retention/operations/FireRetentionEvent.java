/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.operations;

import java.util.Collections;
import java.util.Date;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.event.RetentionEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
@Operation(id = FireRetentionEvent.ID, category = Constants.CAT_NOTIFICATION, label = "Fire Retention Event", description = "Fire a retention business related event.")
public class FireRetentionEvent {

    public static final String ID = "Retention.FireEvent";

    @Context
    protected OperationContext ctx;

    @Context
    protected EventProducer service;

    @Param(name = "name")
    protected String name;

    @Param(name = "audit", required = false)
    protected boolean audit = true;

    @OperationMethod
    public void run() {
        CoreSession session = ctx.getCoreSession();
        Object input = ctx.getInput();
        RetentionEventContext evctx = new RetentionEventContext(session.getPrincipal());
        if (input != null) {
            if (input instanceof String) {
                evctx.setInput((String) input);
            }

        }
        Event event = evctx.newEvent(name);
        service.fireEvent(event);
        if (audit) {
            AuditLogger logger = Framework.getService(AuditLogger.class);
            LogEntry entry = logger.newLogEntry();
            entry.setEventId(name);
            entry.setEventDate(new Date());
            entry.setCategory(RetentionConstants.EVENT_CATEGORY);
            entry.setPrincipalName(session.getPrincipal().getName());
            entry.setComment(evctx.getInput());
            logger.addLogEntries(Collections.singletonList(entry));
        }
    }

}
