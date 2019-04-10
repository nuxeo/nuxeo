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
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.io.WorkflowRequest;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebAdapter(name = WorkflowAdapter.NAME, type = "workflowAdapter")
public class WorkflowAdapter extends DefaultAdapter {

    public static final String NAME = "workflow";

    @POST
    public Response doPost(WorkflowRequest routingRequest) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        final String workflowInstanceId = Framework.getLocalService(DocumentRoutingService.class).createNewInstance(
                routingRequest.getWorkflowModelName(), Arrays.asList(new String[] { doc.getId() }), ctx.getCoreSession(),
                true);
        DocumentModel result = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
        DocumentRoute route = result.getAdapter(DocumentRoute.class);
        return Response.ok(route).status(Status.CREATED).build();
    }

    @GET
    public List<DocumentRoute> doGet() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return Framework.getLocalService(DocumentRoutingService.class).getDocumentRelatedWorkflows(doc,
                getContext().getCoreSession());
    }

    @GET
    @Path("{workflowInstanceId}/task")
    public List<Task> doGetTasks(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return Framework.getLocalService(DocumentRoutingService.class).getTasks(doc,
                null , workflowInstanceId, null, getContext().getCoreSession());
    }

}
