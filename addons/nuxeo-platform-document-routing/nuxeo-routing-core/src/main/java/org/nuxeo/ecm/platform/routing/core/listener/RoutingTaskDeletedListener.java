/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.listener;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;

/**
 * @since 7.4
 */
public class RoutingTaskDeletedListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (!DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            return;
        }
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        CoreSession session = docCtx.getCoreSession();
        if (doc.hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME)) {
            Task task = doc.getAdapter(Task.class);
            String routeId = task.getProcessId();
            IdRef routeIdRef = new IdRef(routeId);
            if (StringUtils.isNotBlank(routeId) && session.exists(routeIdRef)) {
                GraphRoute graphRoute = session.getDocument(routeIdRef).getAdapter(GraphRoute.class);
                String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
                if (StringUtils.isNotBlank(nodeId)) {
                    GraphNode graphNode = graphRoute.getNode(nodeId);
                    graphNode.removeTaskInfo(task.getId());
                }
            }

        }
    }

}
