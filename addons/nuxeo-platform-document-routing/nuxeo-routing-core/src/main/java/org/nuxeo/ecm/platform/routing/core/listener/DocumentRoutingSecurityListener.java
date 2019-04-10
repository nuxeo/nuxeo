/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.nuxeo.ecm.core.api.ClientException;
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
 * Updates the security of the {@link DocumentRoute} so the user responsible for
 * starting the route on a document can see the route.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class DocumentRoutingSecurityListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
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
