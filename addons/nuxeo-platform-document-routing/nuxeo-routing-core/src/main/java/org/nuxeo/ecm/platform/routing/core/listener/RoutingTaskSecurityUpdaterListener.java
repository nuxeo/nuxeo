/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

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
 * Grants the READ/WRITE permissions on the route instance to all task actors. This is needed beacuse an user having a
 * task assigned should be able to see the relatedRoute and to set global workflow variables.
 *
 * @author mcedica
 */
public class RoutingTaskSecurityUpdaterListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
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
        String routeDocId = task.getVariables().get(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
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
        return Framework.getService(DocumentRoutingService.class);
    }

}
