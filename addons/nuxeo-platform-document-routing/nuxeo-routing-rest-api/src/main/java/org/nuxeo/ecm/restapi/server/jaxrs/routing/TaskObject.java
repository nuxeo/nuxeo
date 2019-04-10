/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.restapi.server.jaxrs.routing;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
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
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebObject(type = "task")
public class TaskObject extends DefaultObject {

    public static final String BASE_QUERY = String.format(
            "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
            TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE);

    @PUT
    @Path("{taskId}/reassign")
    @Consumes
    public Response reassignTask(@PathParam("taskId") String taskId, @QueryParam("actors") List<String> actors,
            @QueryParam("comment") String comment) {
        Framework.getLocalService(DocumentRoutingService.class).reassignTask(getContext().getCoreSession(), taskId,
                actors, comment);
        return Response.ok().status(Status.OK).build();
    }

    @PUT
    @Path("{taskId}/delegate")
    public Response delegateTask(@PathParam("taskId") String taskId,
            @QueryParam("delegatedActors") List<String> delegatedActors, @QueryParam("comment") String comment) {
        Framework.getLocalService(DocumentRoutingService.class).delegateTask(getContext().getCoreSession(), taskId,
                delegatedActors, comment);
        return Response.ok().status(Status.OK).build();
    }

    @PUT
    @Path("{taskId}/{taskAction}")
    public Response completeTask(@PathParam("taskId") String taskId, @PathParam("taskAction") String action,
            TaskCompletionRequest taskCompletionRequest) {
        Map<String, Object> data = taskCompletionRequest.getDataMap();
        CoreSession session = getContext().getCoreSession();
        Framework.getLocalService(DocumentRoutingService.class).endTask(session,
                session.getDocument(new IdRef(taskId)).getAdapter(Task.class), data, action);
        Task completedTask = session.getDocument(new IdRef(taskId)).getAdapter(Task.class);
        return Response.ok(completedTask).status(Status.OK).build();
    }

    @GET
    public List<Task> getUserRelatedWorkflowTasks(@QueryParam("userId") String userId,
            @QueryParam("workflowInstanceId") String workflowInstanceId,
            @QueryParam("workflowModelName") String workflowModelName) {
        return Framework.getService(DocumentRoutingService.class).getTasks(null, userId, workflowInstanceId,
                workflowModelName, getContext().getCoreSession());
    }

    @GET
    @Path("{taskId}")
    public Task getTaskById(@PathParam("taskId") String taskId) {
        DocumentModel docModel = getContext().getCoreSession().getDocument(new IdRef(taskId));
        return docModel.getAdapter(Task.class);
    }

}
