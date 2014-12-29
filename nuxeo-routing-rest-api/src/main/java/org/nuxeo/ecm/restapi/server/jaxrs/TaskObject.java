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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.TaskCompletion;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @since 7.1
 */
@WebObject(type = "task")
@Produces(MediaType.APPLICATION_JSON)
public class TaskObject extends DefaultObject {

    public static final String BASE_QUERY = String.format(
            "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
            TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE);

    private static Log log = LogFactory.getLog(TaskObject.class);

    @PUT
    @Path("{taskId}/complete")
    @Consumes({ "application/json+nxentity" })
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentModel completeTask(@PathParam("taskId") String taskId, TaskCompletion taskCompletion) {
        throw new  UnsupportedOperationException();
    }

    @GET
    public Response getUserRelatedWorkflowTasks(@QueryParam("userId") String userId,
            @QueryParam("workflowInstanceId") String workflowInstanceId) {
        StringBuilder query = new StringBuilder(BASE_QUERY);
        String.format(
                "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND nt:processId = '%s' AND nt:actors = '%s' AND ecm:currentLifeCycleState = '%s'",
                TaskConstants.TASK_FACET_NAME, workflowInstanceId, userId, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE);
        if (StringUtils.isNotBlank(userId)) {
            query.append(String.format(" AND nt:actors = '%s'", userId));
        }
        if (StringUtils.isNotBlank(workflowInstanceId)) {
            query.append(String.format(" AND nt:processId = '%s'", workflowInstanceId));
        }
        return redirect("/api/v1/query?query=" + query.toString().replaceAll(" ", "%20"));
    }

}
