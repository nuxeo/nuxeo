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
package org.nuxeo.ecm.automation.core.events.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FireEvent.ID, category = Constants.CAT_NOTIFICATION, label = "Send Event", description = "Send a Nuxeo event.")
public class FireEvent {

    public static final String ID = "Notification.SendEvent";

    @Context
    protected OperationContext ctx;

    @Context
    protected EventProducer service;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public void run() throws Exception {
        CoreSession session = ctx.getCoreSession();
        Object input = ctx.getInput();
        if (input instanceof DocumentModel) {
            sendDocumentEvent((DocumentModel) input);
        } else if (input instanceof DocumentRef) {
            sendDocumentEvent(session.getDocument((DocumentRef) input));
        } else {
            sendUnknownEvent(input);
        }
    }

    protected void sendDocumentEvent(DocumentModel input) throws Exception {
        CoreSession session = ctx.getCoreSession();
        EventContextImpl evctx = new DocumentEventContext(session,
                session.getPrincipal(), input);
        Event event = evctx.newEvent(name);
        service.fireEvent(event);
    }

    protected void sendUnknownEvent(Object input) throws Exception {
        CoreSession session = ctx.getCoreSession();
        EventContextImpl evctx = new EventContextImpl(session,
                session.getPrincipal(), input);
        Event event = evctx.newEvent(name);
        service.fireEvent(event);
    }

}
