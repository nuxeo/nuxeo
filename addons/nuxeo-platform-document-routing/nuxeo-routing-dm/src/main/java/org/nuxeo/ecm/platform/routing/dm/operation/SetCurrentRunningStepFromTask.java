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
 *     Mariana Cedica
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.dm.adapter.RoutingTask;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants;
import org.nuxeo.ecm.platform.task.TaskComment;

/***
 * Set the current running step as <document.routing.step> context variable.
 *
 * @author mcedica
 * @since 5.6
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@Operation(id = SetCurrentRunningStepFromTask.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Set Current Step from Task", description = "Set the current running step as <document.routing.step> context variable. The comments from the task can be mapped to originating step.", addToStudio = false)
public class SetCurrentRunningStepFromTask extends AbstractTaskStepOperation {

    public final static String ID = "Document.Routing.SetRunningStepFromTask";

    private static final Log log = LogFactory.getLog(SetCurrentRunningStepFromTask.class);

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "mappingComments", required = false)
    protected Boolean mappingComments = false;

    @OperationMethod
    public void setStepDocument() {
        String stepDocumentId = getRoutingStepDocumentId(context);
        DocumentModel docStep = session.getDocument(new IdRef(stepDocumentId));
        if (mappingComments) {
            mappCommentsFromTaskToStep(session, docStep);
        }
        context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, docStep.getAdapter(DocumentRouteStep.class));
    }

    protected void mappCommentsFromTaskToStep(CoreSession session, DocumentModel docStep) {
        List<String> comments = new ArrayList<>();

        RoutingTask task = getRoutingTask(context);
        if (task == null) {
            log.error("No task found on the operation context");
            return;
        }
        List<TaskComment> taskComments = task.getComments();
        for (TaskComment taskComment : taskComments) {
            StringBuilder commentBuilder = new StringBuilder();
            commentBuilder.append(taskComment.getAuthor());
            commentBuilder.append(" : ");
            commentBuilder.append(taskComment.getText());
            comments.add(commentBuilder.toString());
        }
        if (docStep.hasFacet(RoutingTaskConstants.TASK_STEP_FACET_NAME)) {
            docStep.setPropertyValue(RoutingTaskConstants.TASK_STEP_COMMENTS_PROPERTY_NAME, (Serializable) comments);
            session.saveDocument(docStep);
        }
    }
}
