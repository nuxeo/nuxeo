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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.jsongraph.JsonGraphRoute;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebObject(type = "workflowModel")
public class WorkflowModelObject extends DefaultObject {

    @GET
    public List<DocumentRoute> getWorkflowModels(@Context UriInfo uriInfo) {
        return Framework.getService(DocumentRoutingService.class).getAvailableDocumentRouteModel(
                getContext().getCoreSession());
    }

    @GET
    @Path("{modelName}")
    public DocumentRoute getWorkflowModel(@PathParam("modelName") String modelName) {
        DocumentRoute result = Framework.getService(DocumentRoutingService.class).getRouteModelWithId(
                getContext().getCoreSession(), modelName);
        return result;
    }

    @GET
    @Path("{modelName}/graph")
    public JsonGraphRoute getWorkflowModelGraph(@PathParam("modelName") String modelName) {
        try {
            final String id = Framework.getService(DocumentRoutingService.class).getRouteModelDocIdWithId(
                    getContext().getCoreSession(), modelName);
            return new JsonGraphRoute(getContext().getCoreSession(), id, getContext().getLocale());
        } catch (NuxeoException e) {
            e.addInfo("Can not get workflow model graph with name: " + modelName);
            throw e;
        }
    }

}
