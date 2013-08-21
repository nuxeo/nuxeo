/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Cancels any remaining open tasks created by this workflow when this workflow
 * is done
 *
 * @since 5.7.3
 *
 */
public class DocumentRoutingWorkflowDoneListener implements EventListener {

    private DocumentRoutingService routingService;

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!DocumentRoutingConstants.Events.afterRouteFinish.name().equals(
                event.getName())) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        CoreSession session = docCtx.getCoreSession();

        DocumentModel routeInstanceDoc = docCtx.getSourceDocument();
        DocumentRoute graph = routeInstanceDoc.getAdapter(DocumentRoute.class);
        if (graph == null) {
            throw new ClientRuntimeException("Document "
                    + routeInstanceDoc.getId()
                    + "can not be adapted to DocumentRoute");
        }

        List<Task> openTasks = Framework.getLocalService(TaskService.class).getAllTaskInstances(
                routeInstanceDoc.getId(), session);
        for (Task task : openTasks) {
            getDocumentRoutingService().cancelTask(session, graph, task);
        }
    }

    protected DocumentRoutingService getDocumentRoutingService() {
        if (routingService == null) {
            routingService = Framework.getLocalService(DocumentRoutingService.class);
        }
        return routingService;
    }
}
