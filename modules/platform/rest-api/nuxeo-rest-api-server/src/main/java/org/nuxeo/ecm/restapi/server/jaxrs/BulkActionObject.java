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
import java.io.Serializable;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.bulk.BulkAdminService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.io.BulkParameters;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.AuditAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;

/**
 * Bulk endpoint to perform bulk actions on a set of documents.
 *
 * @since 10.3
 */
@WebObject(type = "bulkAction")
public class BulkActionObject extends DefaultObject {

    private static Logger log = LogManager.getLogger(BulkActionObject.class);

    protected String query;

    protected String scroller = null;

    @Override
    public void initialize(Object... args) {
        query = (String) args[0];
        if (args.length == 2 && args[1] != null) {
            scroller = (String) args[1];
        }
    }

    @POST
    @Path("{actionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeBulkAction(@PathParam("actionId") String actionId, String actionParams)
            throws IOException {
        BulkAdminService admin = Framework.getService(BulkAdminService.class);
        String actionName = Name.ofId(actionId).getUrn();
        if (!admin.getActions().contains(actionName)) {
            log.debug("Action not found, id: {}, name: {}.", actionId, actionName);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!admin.isHttpEnabled(actionName) && !getContext().getPrincipal().isAdministrator()) {
            log.debug("Action name: {} is not exposed to HTTP", actionId);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String repository = getContext().getCoreSession().getRepositoryName();
        String username = getContext().getPrincipal().getName();
        Map<String, Serializable> params = BulkParameters.paramsToMap(actionParams);

        BulkCommand command = new BulkCommand.Builder(actionName, query, username).repository(repository)
                                                                                  .params(params)
                                                                                  .scroller(scroller)
                                                                                  .build();
        BulkService service = Framework.getService(BulkService.class);
        String commandId = service.submit(command);
        BulkStatus status = service.getStatus(commandId);
        log.debug("Action {} submitted in commandId {}", actionId, commandId);
        return Response.status(Response.Status.ACCEPTED).entity(status).build();
    }
}
