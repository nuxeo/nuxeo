/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.automation.task;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;

/**
 * Returns tasks assigned to current user or one of its groups.
 * @since 5.5
 */
@Operation(id = GetUserTasks.ID, category = Constants.CAT_SERVICES, label = "Get user tasks", since = "5.4", description = "List tasks assigned to this user or one of its group."
        + "Task properties are serialized using JSON and returned in a Blob.")
public class GetUserTasks {

    public static final String ID = "Workflow.GetTask";

    private static final Log log = LogFactory.getLog(Log.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession repo;

    @Context
    protected TaskService taskService;

    @OperationMethod
    public Blob run() throws Exception {
        List<Task> tasks = taskService.getCurrentTaskInstances(repo);
        if (tasks == null) {
            return null;
        }
        JSONArray rows = new JSONArray();
        for (Task task : tasks) {
            DocumentModel doc = null;
            try {
                doc = taskService.getTargetDocumentModel(task, repo);
            } catch (Exception e) {
                log.warn("Cannot get doc for task " + task.getId(), e);
            }
            if (doc == null) {
                log.warn(String.format(
                        "User '%s' has a task of type '%s' on an "
                                + "unexisting or invisible document",
                        principal().getName(), task.getName()));
                continue;
            }

            DashBoardItem item = new DashBoardItemImpl(task, doc, null);
            JSONObject obj = item.asJSON();
            rows.add(obj);
        }
        return new StringBlob(rows.toString(), "application/json");
    }

    protected NuxeoPrincipal principal() {
        return (NuxeoPrincipal) ctx.getPrincipal();
    }

}
