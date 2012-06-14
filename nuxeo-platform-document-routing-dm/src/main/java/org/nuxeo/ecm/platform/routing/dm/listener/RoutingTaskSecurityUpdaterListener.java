/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.routing.dm.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;

/**
 * Grants the READ permission on the route instance to all task actors. This is
 * needed beacuse an user having a task assigned should be able to see the
 * relatedRoute
 *
 * @author mcedica
 *
 */
public class RoutingTaskSecurityUpdaterListener implements EventListener {

    private DocumentRoutingService routingService;

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext eventCtx = event.getContext();
        if (!(eventCtx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docEventCtx = (DocumentEventContext) eventCtx;
        Task task = (Task) docEventCtx.getProperties().get("taskInstance");
        if (task == null) {
            return;
        }
        CoreSession session = eventCtx.getCoreSession();
        List<String> actors = task.getActors();
        if (actors == null || actors.isEmpty()) {
            return;
        }

        String stepId = task.getVariables().get(
                DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
        if (stepId == null) {
            return;
        }
        DocumentModel stepDoc = session.getDocument(new IdRef(stepId));
        DocumentRouteElement stepElement = stepDoc.getAdapter(DocumentRouteElement.class);
        DocumentRoute route = stepElement.getDocumentRoute(session);

        for (String userName : actors) {
            DocumentModel routeDoc = route.getDocument();
            ACP acp = routeDoc.getACP();
            ACL routeACL = acp.getOrCreateACL(RoutingTaskConstants.ROUTE_TASK_LOCAL_ACL);
            routeACL.add(new ACE(userName, SecurityConstants.READ, true));
            acp.addACL(routeACL);
            session.setACP(routeDoc.getRef(), acp, true);
        }
    }

    protected DocumentRoutingService getDocumentRoutingService() {
        try {
            if (routingService == null) {
                routingService = Framework.getService(DocumentRoutingService.class);
            }
            return routingService;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
