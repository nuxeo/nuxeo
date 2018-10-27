/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.bulk.BulkAdminService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.io.BulkParameters;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Bulk endpoint to perform bulk actions on a set of documents.
 *
 * @since 10.3
 */
@WebObject(type = "bulkAction")
public class BulkActionObject extends DefaultObject {

    protected String query;

    @Override
    public void initialize(Object... args) {
        query = (String) args[0];
    }

    @POST
    @Path("{actionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeBulkAction(@PathParam("actionId") String actionId, String actionParams)
            throws IOException {

        BulkAdminService admin = Framework.getService(BulkAdminService.class);
        if (!admin.getActions().contains(actionId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!admin.isHttpEnabled(actionId) && !getContext().getPrincipal().isAdministrator()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String repository = getContext().getCoreSession().getRepositoryName();
        String username = getContext().getPrincipal().getName();

        BulkCommand command = new BulkCommand.Builder(actionId, query).repository(repository)
                                                                      .user(username)
                                                                      .params(BulkParameters.paramsToMap(actionParams))
                                                                      .build();

        BulkService service = Framework.getService(BulkService.class);
        String commandId = service.submit(command);
        BulkStatus status = service.getStatus(commandId);
        return Response.status(Response.Status.ACCEPTED).entity(status).build();
    }

}
