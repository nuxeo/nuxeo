/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.dm.adapter.RoutingTask;

/**
 * @since 5.6
 * 
 */
public class AbstractTaskStepOperation {

    protected RoutingTask routingTask;

    protected DocumentRouteElement step;

    public RoutingTask getRoutingTask(OperationContext context) {
        if (routingTask == null) {
            DocumentModel taskDoc = (DocumentModel) context.get(OperationTaskVariableName.taskDocument.name());
            if (taskDoc != null) {
                routingTask = taskDoc.getAdapter(RoutingTask.class);
            }
        }
        return routingTask;
    }

    public DocumentRouteElement getRoutingStep(OperationContext context) {
        if (getRoutingTask(context) != null) {
            step = routingTask.getDocumentRouteStep(context.getCoreSession());
            return step;
        }
        return null;
    }

    public String getRoutingStepDocumentId(OperationContext context)
            throws ClientException {
        if (getRoutingTask(context) != null) {
            String stepDocumentId = routingTask.getVariable(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
            return stepDocumentId;
        }
        return null;
    }
}
