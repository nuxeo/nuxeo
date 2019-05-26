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
 *     ldoguin
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.dm.operation.CreateRoutingTask.OperationTaskVariableName;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;

/**
 * This operation removes all the tasks created when a given step is run
 *
 * @author mcedica
 * @since 5.6
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@Operation(id = RemoveRoutingTask.ID, category = Constants.CAT_SERVICES, label = "remove task", since = "5.6", description = " This operation removes all the tasks created when a given step is run.", addToStudio = false)
public class RemoveRoutingTask {

    public static final String ID = "Workflow.RemoveRoutingTask";

    private static final Log log = LogFactory.getLog(RemoveRoutingTask.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;

    @Context
    protected TaskService taskService;

    @OperationMethod
    public void removeTasks() {
        List<Task> tasks = fetchTasks();
        for (Task task : tasks) {
            taskService.deleteTask(coreSession, task.getId());
        }
    }

    protected List<Task> fetchTasks() {
        List<Task> tasks = new ArrayList<>();
        DocumentModelList docList = (DocumentModelList) ctx.get(OperationTaskVariableName.taskDocuments.name());
        if (docList != null) {
            for (DocumentModel documentModel : docList) {
                tasks.add(documentModel.getAdapter(Task.class));
            }
        }
        if (tasks.isEmpty()) {
            tasks = fetchTasksFromStep();
        }
        return tasks;
    }

    protected List<Task> fetchTasksFromStep() {
        List<Task> tasks = new ArrayList<>();
        DocumentRouteStep step = (DocumentRouteStep) ctx.get(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
        DocumentModelList list = coreSession.query(String.format(
                "Select * from TaskDoc WHERE nt:task_variables/*/key like '%s' AND nt:task_variables/*/value like '%s'",
                DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, step.getDocument().getId()));
        for (DocumentModel documentModel : list) {
            tasks.add(documentModel.getAdapter(Task.class));
        }
        return tasks;
    }
}
