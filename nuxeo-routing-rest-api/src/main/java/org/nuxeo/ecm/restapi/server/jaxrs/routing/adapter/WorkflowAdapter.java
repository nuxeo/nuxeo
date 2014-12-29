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

package org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.RoutingRequest;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
@WebAdapter(name = WorkflowAdapter.NAME, type = "workflowAdapter")
public class WorkflowAdapter extends DefaultAdapter {

    public static final String NAME = "workflow";

    @POST
    public Response doPost(RoutingRequest routingRequest) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        final String workflowInstanceId = Framework.getLocalService(DocumentRoutingService.class).createNewInstance(
                routingRequest.getRouteModelId(), Arrays.asList(new String[] { doc.getId() }), ctx.getCoreSession(),
                true);
        DocumentModel result = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
        return Response.ok(result).status(Status.CREATED).build();
    }

    @GET
    public Response doGet() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        final String query = String.format(
                "SELECT * FROM %s WHERE docri:participatingDocuments = '%s' AND ecm:currentLifeCycleState = '%s'",
                DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE, doc.getId(),
                DocumentRouteElement.ElementLifeCycleState.running).replaceAll(" ", "%20");
        return redirect("/api/v1/query?query=" + query);
    }

    @GET
    @Path("{workflowInstanceId}/task")
    public Response doGetTasks(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        final String query = String.format(
                "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s' AND nt:targetDocumentId = '%s' AND nt:processId = '%s'",
                TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE, doc.getId(),
                workflowInstanceId).replaceAll(" ", "%20");
        return redirect("/api/v1/query?query=" + query);
    }

}
