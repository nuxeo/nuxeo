/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter;


import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.io.TaskCompletionRequest;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebAdapter(name = TaskAdapter.NAME, type = "taskAdapter")
public class TaskAdapter extends DefaultAdapter {

    public static final String NAME = "task";

    @PUT
    @Path("{taskId}/{action}")
    public Response completeTask(@PathParam("taskId") String taskId, @PathParam("action") String action,
            TaskCompletionRequest taskCompletionRequest) {
        Map<String, Object> data = taskCompletionRequest.getDataMap();
        CoreSession session = getContext().getCoreSession();
        Framework.getLocalService(DocumentRoutingService.class).endTask(session,
                session.getDocument(new IdRef(taskId)).getAdapter(Task.class), data, action);
        Task completedTask = session.getDocument(new IdRef(taskId)).getAdapter(Task.class);
        return Response.ok(completedTask).status(Status.OK).build();
    }

    @GET
    public List<Task> doGet(@QueryParam("userId") String userId,
            @QueryParam("workflowInstanceId") String workflowInstanceId,
            @QueryParam("workflowModelName") String workflowModelName) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return Framework.getLocalService(DocumentRoutingService.class).getTasks(doc, userId, workflowInstanceId,
                workflowModelName, getContext().getCoreSession());
    }

}
