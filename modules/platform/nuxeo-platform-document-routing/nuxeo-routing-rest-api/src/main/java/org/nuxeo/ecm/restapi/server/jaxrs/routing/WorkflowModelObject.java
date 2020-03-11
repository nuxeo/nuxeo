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
