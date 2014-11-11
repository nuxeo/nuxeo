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
package org.nuxeo.ecm.automation.core.operations.notification;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.AutomationComponent;
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
 * @deprecated Not used for now. It may be enabled later. (to enable it remove
 *             the deprecation mark and uncomment the registration from
 *             {@link AutomationComponent#activate(org.nuxeo.runtime.model.ComponentContext)}
 */
@Deprecated
@Operation(id = FireEvent.ID, category = Constants.CAT_NOTIFICATION, label = "Send Event", description = "Send a Nuxeo event.")
public class FireEvent {

    public final static String ID = "Notification.SendEvent";

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
