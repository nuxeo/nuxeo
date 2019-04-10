/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.dm.adapter.RoutingTask;

/**
 * @since 5.6
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
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

    public String getRoutingStepDocumentId(OperationContext context) {
        if (getRoutingTask(context) != null) {
            String stepDocumentId = routingTask.getVariable(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
            return stepDocumentId;
        }
        return null;
    }
}
