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

package org.nuxeo.ecm.restapi.server.jaxrs.routing;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.io.TaskCompletionRequest;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.PaginableObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebObject(type = "task")
public class TaskObject extends PaginableObject<Task> {

    /**
     * @since 11.1
     */
    public static final String PAGE_PROVIDER_NAME = "nuxeo_tasks_listing";

    public static final String BASE_QUERY = String.format(
            "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
            TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE);

    protected String userId;

    protected String workflowInstanceId;

    /**
     * For backward compatibility only. This parameter forces post-filtering and prevents paginated results.
     * @deprecated since 11.1
     */
    @Deprecated
    protected String workflowModelName;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        HttpServletRequest request = ctx.getRequest();
        userId = request.getParameter("userId");
        workflowInstanceId = request.getParameter("workflowInstanceId");
        workflowModelName = request.getParameter("workflowModelName");
    }

    @PUT
    @Path("{taskId}/reassign")
    @Consumes
    public Response reassignTask(@PathParam("taskId") String taskId, @QueryParam("actors") List<String> actors,
            @QueryParam("comment") String comment) {
        Framework.getService(DocumentRoutingService.class).reassignTask(getContext().getCoreSession(), taskId,
                actors, comment);
        return Response.ok().status(Status.OK).build();
    }

    @PUT
    @Path("{taskId}/delegate")
    public Response delegateTask(@PathParam("taskId") String taskId,
            @QueryParam("delegatedActors") List<String> delegatedActors, @QueryParam("comment") String comment) {
        Framework.getService(DocumentRoutingService.class).delegateTask(getContext().getCoreSession(), taskId,
                delegatedActors, comment);
        return Response.ok().status(Status.OK).build();
    }

    @PUT
    @Path("{taskId}/{taskAction}")
    public Response completeTask(@PathParam("taskId") String taskId, @PathParam("taskAction") String action,
            TaskCompletionRequest taskCompletionRequest) {
        Map<String, Object> data = taskCompletionRequest.getDataMap();
        CoreSession session = getContext().getCoreSession();
        Framework.getService(DocumentRoutingService.class).endTask(session,
                session.getDocument(new IdRef(taskId)).getAdapter(Task.class), data, action);
        Task completedTask = session.getDocument(new IdRef(taskId)).getAdapter(Task.class);
        return Response.ok(completedTask).status(Status.OK).build();
    }

    @GET
    public List<Task> getUserRelatedWorkflowTasks() {
        if (StringUtils.isNotBlank(workflowModelName)) {
            return Framework.getService(DocumentRoutingService.class).getTasks(null, userId, workflowInstanceId,
                    workflowModelName, getContext().getCoreSession());
        } else {
            return getPaginableEntries();
        }
    }

    @GET
    @Path("{taskId}")
    public Task getTaskById(@PathParam("taskId") String taskId) {
        DocumentModel docModel = getContext().getCoreSession().getDocument(new IdRef(taskId));
        return docModel.getAdapter(Task.class);
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        return pageProviderService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { userId, workflowInstanceId };
    }

}
