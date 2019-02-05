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
package org.nuxeo.ecm.automation.core.events.operations;

import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FireEvent.ID, category = Constants.CAT_NOTIFICATION, label = "Send Event", description = "Send a Nuxeo event.", aliases = { "Notification.SendEvent" })
public class FireEvent {

    public static final String ID = "Event.Fire";

    @Context
    protected OperationContext ctx;

    @Context
    protected EventProducer service;

    @Param(name = "name")
    protected String name;

    /** @since 11.1 */
    @Param(name = "properties", required = false)
    protected Properties properties = new Properties();

    @OperationMethod
    public void run() {
        CoreSession session = ctx.getCoreSession();
        Object input = ctx.getInput();
        if (input instanceof DocumentModel) {
            sendDocumentEvent((DocumentModel) input);
        } else if (input instanceof DocumentRef) {
            sendDocumentEvent(session.getDocument((DocumentRef) input));
        } else if (input instanceof DocumentModelList) {
            DocumentModelList docs = (DocumentModelList) input;
            for (DocumentModel documentModel : docs) {
                sendDocumentEvent(documentModel);
            }
        } else {
            sendUnknownEvent(input);
        }
    }

    protected void sendDocumentEvent(DocumentModel input) {
        CoreSession session = ctx.getCoreSession();
        EventContextImpl evctx = new DocumentEventContext(session, session.getPrincipal(), input);
        sendEvent(evctx);
    }

    protected void sendUnknownEvent(Object input) {
        CoreSession session = ctx.getCoreSession();
        EventContextImpl evctx = new EventContextImpl(session, session.getPrincipal(), input);
        sendEvent(evctx);
    }

    protected void sendEvent(EventContext eventContext) {
        Event event = eventContext.newEvent(name);
        event.getContext().setProperties(
                properties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        service.fireEvent(event);
    }

}
