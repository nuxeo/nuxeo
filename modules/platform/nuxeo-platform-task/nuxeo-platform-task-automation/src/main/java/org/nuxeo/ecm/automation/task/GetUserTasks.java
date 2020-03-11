/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.automation.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;

/**
 * Returns tasks assigned to current user or one of its groups.
 *
 * @since 5.5
 */
@Operation(id = GetUserTasks.ID, category = Constants.CAT_SERVICES, label = "Get user tasks", since = "5.4", description = "List tasks assigned to this user or one of its group."
        + "Task properties are serialized using JSON and returned in a Blob.", aliases = { "Workflow.GetTask" })
public class GetUserTasks {

    public static final String ID = "Task.GetAssigned";

    private static final Log log = LogFactory.getLog(Log.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession repo;

    @Context
    protected TaskService taskService;

    @OperationMethod
    public Blob run() throws IOException {
        List<Task> tasks = taskService.getCurrentTaskInstances(repo);
        if (tasks == null) {
            return null;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Task task : tasks) {
            DocumentModel doc = taskService.getTargetDocumentModel(task, repo);
            if (doc == null) {
                log.warn(String.format("User '%s' has a task of type '%s' on an " + "unexisting or invisible document",
                        ctx.getPrincipal().getName(), task.getName()));
                continue;
            }

            DashBoardItem item = new DashBoardItemImpl(task, doc, null);
            Map<String, Object> obj = item.asMap();
            rows.add(obj);
        }
        return Blobs.createJSONBlobFromValue(rows);
    }

}
