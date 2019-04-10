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
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Returns all open tasks on the input document(s). If the operation is invoked
 * with parameters, all tasks instances for the given 'processId' originating
 * from the given 'nodeId' are returned. The 'processId' is the id of the
 * document representing the workflow instance. The parameter 'username' is used
 * to fetch only tasks assigned to the given user. Tasks are queried using an
 * unrestricted session.
 *
 * @since 5.7.2
 */
@Operation(id = GetOpenTasksOperation.ID, category = Constants.CAT_WORKFLOW, label = "Get open tasks", requires = Constants.WORKFLOW_CONTEXT, description = "Returns all open tasks for the input document(s). "
        + "If the operation is invoked with parameters, all tasks instances for the given 'processId' "
        + "originating from the given 'nodeId' are returned. The 'processId' is the id of the document representing the workflow instance. The parameter 'username' is used to fetch only tasks assigned to the given user. "
        + "Tasks are queried using an unrestricted session.")
public class GetOpenTasksOperation {
    public static final String ID = "Context.GetOpenTasks";

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
    public DocumentModelList getAllTasks(DocumentModel doc)
            throws ClientException {
        DocumentModelList taskDocs = new DocumentModelListImpl();
        List<Task> tasks = new ArrayList<Task>();
        if (nodeId == null && processId == null) {
            NuxeoPrincipal principal = username != null ? userManager.getPrincipal(username)
                    : null;
            tasks = taskService.getTaskInstances(doc, principal, session);
            for (Task task : tasks) {
                taskDocs.add(task.getDocument());
            }
            return taskDocs;
        }
        if (nodeId == null || processId == null) {
            throw new ClientException(
                    "Need both nodeId and processId to invoke the operation with parameters");
        }
        tasks = taskService.getAllTaskInstances(processId, nodeId, session);
        for (Task task : tasks) {
            if (doc.getId().equals(task.getTargetDocumentId())) {
                if (username == null) {
                    taskDocs.add(task.getDocument());
                } else {
                    if (task.getActors().contains(username)) {
                        taskDocs.add(task.getDocument());
                    }
                }
            }
        }
        return taskDocs;
    }

    @OperationMethod
    public DocumentModelList getAllTasks(DocumentModelList docs)
            throws ClientException {
        DocumentModelList taskDocs = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            taskDocs.addAll(getAllTasks(doc));
        }
        return taskDocs;
    }
}