/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * Updates the security of the {@link DocumentRoute} so the user responsible for starting the route on a document can
 * see the route.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class DocumentRoutingSecurityListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentRoute route = (DocumentRoute) docCtx.getProperty(DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY);
        String initiator = (String) docCtx.getProperty(DocumentRoutingConstants.INITIATOR_EVENT_CONTEXT_KEY);
        CoreSession session = docCtx.getCoreSession();
        // initiator is a step validator
        route.setCanValidateStep(session, initiator);
        // initiator can see the route
        ACP acp = route.getDocument().getACP();
        ACL acl = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
        acl.add(new ACE(initiator, SecurityConstants.READ, true));
        session.setACP(route.getDocument().getRef(), acp, true);
    }

}
