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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.impl.jsongraph.JsonGraphRoute;
import org.nuxeo.ecm.platform.routing.core.io.WorkflowRequest;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebObject(type = "workflow")
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowObject extends DefaultObject {

    private static Log log = LogFactory.getLog(WorkflowObject.class);

    private DocumentRoutingService documentRoutingService;

    @Override
    protected void initialize(Object... args) {
        documentRoutingService = Framework.getService(DocumentRoutingService.class);
    }

    @POST
    public Response createWorkflowInstance(WorkflowRequest workflowRequest) {
        final String workflowInstanceId = documentRoutingService.createNewInstance(
                workflowRequest.getWorkflowModelName(), workflowRequest.getAttachedDocumentIds(),
                workflowRequest.getVariables(), ctx.getCoreSession(), true);
        DocumentModel workflowInstance = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
        DocumentRoute route = workflowInstance.getAdapter(DocumentRoute.class);
        return Response.ok(route).status(Status.CREATED).build();
    }

    @GET
    @Path("{workflowInstanceId}")
    public DocumentRoute getWorkflowInstance(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel workflowInstance;
        try {
            workflowInstance = getContext().getCoreSession().getDocument(new IdRef(workflowInstanceId));
            return workflowInstance.getAdapter(DocumentRoute.class);
        } catch (NuxeoException e) {
            e.addInfo("Can not get workflow instance with id: " + workflowInstanceId);
            throw e;
        }
    }

    @GET
    @Path("{workflowInstanceId}/graph")
    public JsonGraphRoute getWorkflowGraph(@PathParam("workflowInstanceId") String workflowInstanceId) {
        try {
            return new JsonGraphRoute(getContext().getCoreSession(), workflowInstanceId, getContext().getLocale());
        } catch (NuxeoException e) {
            e.addInfo("Can not get workflow instance graph with id: " + workflowInstanceId);
            throw e;
        }
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
        } catch (NuxeoException e) {
            e.addInfo("Can not get workflow instance with id: " + workflowInstanceId);
            throw e;
        }
        Framework.getService(DocumentRoutingEngineService.class).cancel(route, getContext().getCoreSession());
        return Response.ok().status(Status.NO_CONTENT).build();
    }

    protected void checkCancelGuards(DocumentRoute route) {
        NuxeoPrincipal currentUser = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (currentUser.isAdministrator() || currentUser.isMemberOf("powerusers")) {
            return;
        }
        try {
            if (currentUser.getName().equals(route.getInitiator())) {
                return;
            }
            throw new WebSecurityException("You don't have the permission to cancel this workflow");
        } catch (NuxeoException e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    public List<DocumentRoute> getRunningWorkflowInstancesLaunchedByCurrentUser(
            @QueryParam("workflowModelName") String worflowModelName) {
        return documentRoutingService.getRunningWorkflowInstancesLaunchedByCurrentUser(getContext().getCoreSession(),
                worflowModelName);
    }

}
