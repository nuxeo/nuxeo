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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.RoutingRequest;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
@WebObject(type = "workflow")
@Produces(MediaType.APPLICATION_JSON)
public class RoutingObject extends DefaultObject {

    private static Log log = LogFactory.getLog(RoutingObject.class);

    private DocumentRoutingService documentRoutingService;

    @Override
    protected void initialize(Object... args) {
        documentRoutingService = Framework.getService(DocumentRoutingService.class);
    }

    @POST
    @Consumes({ "application/json+nxentity" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWorkflowInstance(RoutingRequest routingRequest) {
        final String workflowInstanceId = documentRoutingService.createNewInstance(routingRequest.getRouteModelId(),
                routingRequest.getDocumentIds(), ctx.getCoreSession(), true);
        DocumentModel result = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
        return Response.ok(result).status(Status.CREATED).build();
    }

    @GET
    @Path("{workflowInstanceId}")
    public DocumentModel getWorkflowInstance(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel workflowInstance;
        try {
            workflowInstance = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
            workflowInstance.getAdapter(DocumentRoute.class);
            return workflowInstance;
        } catch (ClientException e) {
            log.error("Can not get workflow instance with id" + workflowInstanceId);
            throw new ClientException(e);
        }
    }

    @GET
    @Path("models")
    public Response getWorkflowModels(@Context UriInfo uriInfo) {
        String query = String.format("SELECT * FROM %s", DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE).replaceAll(
                " ", "%20");
        return redirect("/api/v1/query?query=" + query);
    }

    @GET
    @Path("models/{modelId}")
    public DocumentModel getWorkflowModel(@PathParam("modelId") String modelId) {
        DocumentRoute result = documentRoutingService.getRouteModelWithId(getContext().getCoreSession(), modelId);
        return result.getDocument();
    }

    @DELETE
    @Path("{workflowInstanceId}")
    public Response cancelWorkflowInstance(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel workflowInstance;
        DocumentRoute route;
        try {
            workflowInstance = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
            route = workflowInstance.getAdapter(DocumentRoute.class);
            checkCancelGuards(route);
        } catch (ClientException e) {
            log.error("Can not get workflow instance with id" + workflowInstanceId);
            throw new ClientException(e);
        }
        Framework.getService(DocumentRoutingEngineService.class).cancel(route, getContext().getCoreSession());
        return Response.ok().status(Status.NO_CONTENT).build();
    }

    protected void checkCancelGuards(DocumentRoute route) {
        NuxeoPrincipal currentUser = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!(currentUser.isAdministrator() || currentUser.isMemberOf("powerusers"))) {
            throw new WebSecurityException("Not allowed to cancel workflow");
        }

        try {
            if (!currentUser.getName().equals(route.getInitiator())) {
                throw new WebSecurityException("You don't have the permission to cancel this workflow");
            }
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    public Response getRunningWorkflowInstancesLaunchedByCurrentUser() {
        final String query = String.format("SELECT * FROM %s WHERE docri:initiator = '%s' AND ecm:currentLifeCycleState = '%s'",
                DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE, getContext().getPrincipal().getName(),
                DocumentRouteElement.ElementLifeCycleState.running).replaceAll(" ", "%20");
        return redirect("/api/v1/query?query=" + query);
    }

}
