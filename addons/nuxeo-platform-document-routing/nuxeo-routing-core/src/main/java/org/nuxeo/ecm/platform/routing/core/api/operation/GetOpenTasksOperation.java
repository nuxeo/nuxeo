/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Returns all open tasks on the input document(s). If the operation is invoked with parameters, all tasks instances for
 * the given 'processId' originating from the given 'nodeId' are returned. The 'processId' is the id of the document
 * representing the workflow instance. The parameter 'username' is used to fetch only tasks assigned to the given user.
 * Tasks are queried using an unrestricted session.
 *
 * @since 5.7.2
 */
@Operation(id = GetOpenTasksOperation.ID, category = Constants.CAT_WORKFLOW, label = "Get open tasks", requires = Constants.WORKFLOW_CONTEXT, description = "Returns all open tasks for the input document(s). "
        + "If the operation is invoked with parameters, all tasks instances for the given 'processId' "
        + "originating from the given 'nodeId' are returned. The 'processId' is the id of the document representing the workflow instance. The parameter 'username' is used to fetch only tasks assigned to the given user. "
        + "Tasks are queried using an unrestricted session.", aliases = { "Context.GetOpenTasks" })
public class GetOpenTasksOperation {
    public static final String ID = "Workflow.GetOpenTasks";

    @Context
    protected CoreSession session;

    @Param(name = "nodeId", required = false)
    protected String nodeId;

    @Param(name = "processId", required = false)
    protected String processId;

    @Param(name = "username", required = false)
    protected String username;

    @Context
    protected TaskService taskService;

    @Context
    protected UserManager userManager;

    @OperationMethod
    public DocumentModelList getAllTasks(DocumentModel doc) {
        DocumentModelList taskDocs = new DocumentModelListImpl();
        List<Task> tasks;
        if (nodeId == null && processId == null) {
            NuxeoPrincipal principal = username != null ? userManager.getPrincipal(username) : null;
            tasks = taskService.getTaskInstances(doc, principal, session);
            for (Task task : tasks) {
                taskDocs.add(task.getDocument());
            }
            return taskDocs;
        }
        if (nodeId == null || processId == null) {
            throw new NuxeoException("Need both nodeId and processId to invoke the operation with parameters");
        }
        tasks = taskService.getAllTaskInstances(processId, nodeId, session);
        for (Task task : tasks) {
            if (task.getTargetDocumentsIds().contains(doc.getId())) {
                if (username == null || task.getActors().contains(username)) {
                    taskDocs.add(task.getDocument());
                }
            }
        }
        return taskDocs;
    }

    @OperationMethod
    public DocumentModelList getAllTasks(DocumentModelList docs) {
        DocumentModelList taskDocs = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            taskDocs.addAll(getAllTasks(doc));
        }
        return taskDocs;
    }
}
