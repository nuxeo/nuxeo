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
package org.nuxeo.ecm.platform.routing.core.listener;

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
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Grants the READ/WRITE permissions on the route instance to all task actors.
 * This is needed beacuse an user having a task assigned should be able to see
 * the relatedRoute and to set global workflow variables.
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
        List<String> actors = null;

        if (TaskEventNames.WORKFLOW_TASK_ASSIGNED.equals(event.getName())
                || TaskEventNames.WORKFLOW_TASK_REASSIGNED.equals(event.getName())) {
            actors = task.getActors();
        }

        if (TaskEventNames.WORKFLOW_TASK_DELEGATED.equals(event.getName())) {
            actors = task.getDelegatedActors();
        }
        if (actors == null || actors.isEmpty()) {
            return;
        }
        String routeDocId = task.getVariables().get(
                DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (routeDocId == null) {
            return;
        }
        DocumentModel routeDoc = session.getDocument(new IdRef(routeDocId));
        for (String userName : actors) {
            if (userName.startsWith(NotificationConstants.GROUP_PREFIX)
                    || userName.startsWith(NotificationConstants.USER_PREFIX)) {
                // prefixed assignees with "user:" or "group:"
                userName = userName.substring(userName.indexOf(":") + 1);
            }

            ACP acp = routeDoc.getACP();
            ACL routeACL = acp.getOrCreateACL(DocumentRoutingConstants.ROUTE_TASK_LOCAL_ACL);
            ACE ace = new ACE(userName, SecurityConstants.READ_WRITE, true);
            if (!routeACL.contains(ace)) {
                routeACL.add(ace);
            }
            acp.addACL(routeACL);
            session.setACP(routeDoc.getRef(), acp, false);
        }
        session.saveDocument(routeDoc);
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
