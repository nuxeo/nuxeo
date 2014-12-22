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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
@WebObject(type = "workflow")
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowObject extends DefaultObject {

    private DocumentRoutingService documentRoutingService;

    @Override
    protected void initialize(Object... args) {
        documentRoutingService = Framework.getLocalService(DocumentRoutingService.class);
    }

    @POST
    @Consumes({"application/json+nxentity" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response startWorkflow(RoutingRequest rr) {
        String routeId = ""; //= documentRoutingService.createNewInstance(routingRequest.getRoutingId(), routingRequest.getDocumentIds(), ctx.getCoreSession(), routingRequest.isShouldStart());
        return Response.ok(routeId).status(Status.CREATED).build();
    }

    @GET
    @Path("{workflowId}")
    public DocumentModel getWorkflow(@PathParam("workflowId") String workflowId) {
        DocumentRoute result = documentRoutingService.getRouteModelWithId(getContext().getCoreSession(), workflowId);
        return result.getDocument();
    }

}
