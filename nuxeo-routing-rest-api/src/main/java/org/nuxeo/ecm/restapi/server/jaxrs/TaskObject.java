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

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.TaskCompletionRequest;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebObject(type = "task")
@Produces(MediaType.APPLICATION_JSON)
public class TaskObject extends DefaultObject {

    public static final String BASE_QUERY = String.format(
            "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
            TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE);

    @PUT
    @Path("{taskId}/{action}")
    @Consumes({ "application/json+nxentity" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeTask(@PathParam("taskId") String taskId, @PathParam("action") String action, TaskCompletionRequest taskCompletionRequest) {
        Map<String, Object> data = new HashMap<String, Object>();
        if (taskCompletionRequest.getWorkflowVariables() != null) {
            data.put(Constants.VAR_WORKFLOW, taskCompletionRequest.getWorkflowVariables());
        }
        if (taskCompletionRequest.getNodeVariables() != null) {
            data.put(Constants.VAR_WORKFLOW_NODE, taskCompletionRequest.getNodeVariables());
        }
        data.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, Boolean.TRUE);
        if (StringUtils.isNotBlank(taskCompletionRequest.getComment())) {
            data.put(GraphNode.NODE_VARIABLE_COMMENT, taskCompletionRequest.getComment());
        }
        CoreSession session = getContext().getCoreSession();
        Framework.getLocalService(DocumentRoutingService.class).endTask(session, session.getDocument(new IdRef(taskId)).getAdapter(Task.class), data, action);
        return Response.ok(null).status(Status.OK).build();
    }

    @GET
    public List<Task> getUserRelatedWorkflowTasks(@QueryParam("userId") String userId,
            @QueryParam("workflowInstanceId") String workflowInstanceId) {
        return Framework.getLocalService(DocumentRoutingService.class).getUserRelatedWorkflowTasks(userId,
                workflowInstanceId, getContext().getCoreSession());
    }

}
