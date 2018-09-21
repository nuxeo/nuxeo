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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(BulkActionObject.class);

    protected String query;

    @Override
    public void initialize(Object... args) {
        query = (String) args[0];
    }

    @POST
    @Path("{actionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response executeBulkAction(@PathParam("actionId") String actionId, String actionParams)
            throws IOException {

        String repository = getContext().getCoreSession().getRepositoryName();
        String username = getContext().getPrincipal().getName();

        Map<String, Serializable> params = BulkParameters.paramsToMap(actionParams);

        BulkCommand command = new BulkCommand().withAction(actionId)
                                               .withRepository(repository)
                                               .withUsername(username)
                                               .withQuery(query)
                                               .withParams(params);
        String commandId = Framework.getService(BulkService.class).submit(command);

        BulkStatus status = new BulkStatus();
        status.setCommandId(commandId);
        return Response.status(Response.Status.ACCEPTED).entity(status).build();
    }

}
