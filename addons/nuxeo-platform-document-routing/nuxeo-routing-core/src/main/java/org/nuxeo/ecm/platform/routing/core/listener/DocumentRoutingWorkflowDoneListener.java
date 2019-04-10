/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Cancels any remaining open tasks created by this workflow when this workflow is done
 *
 * @since 5.7.3
 */
public class DocumentRoutingWorkflowDoneListener implements EventListener {

    private DocumentRoutingService routingService;

    @Override
    public void handleEvent(Event event) {
        if (!DocumentRoutingConstants.Events.afterRouteFinish.name().equals(event.getName())) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        CoreSession session = docCtx.getCoreSession();
        DocumentModel routeInstanceDoc = docCtx.getSourceDocument();

        List<Task> openTasks = Framework.getService(TaskService.class).getAllTaskInstances(
                routeInstanceDoc.getId(), session);
        for (Task task : openTasks) {
            getDocumentRoutingService().cancelTask(session, task.getId());
        }
    }

    protected DocumentRoutingService getDocumentRoutingService() {
        if (routingService == null) {
            routingService = Framework.getService(DocumentRoutingService.class);
        }
        return routingService;
    }
}
