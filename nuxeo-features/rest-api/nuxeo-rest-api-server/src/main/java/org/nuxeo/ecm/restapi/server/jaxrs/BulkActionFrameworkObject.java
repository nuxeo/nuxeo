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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Bulk endpoint to expose {@link BulkService}.
 *
 * @since 10.3
 */
@WebObject(type = "bulkActionFramework")
public class BulkActionFrameworkObject extends DefaultObject {

    @GET
    @Path("{commandId}")
    @Produces(MediaType.APPLICATION_JSON)
    public BulkStatus executeBulkAction(@PathParam("commandId") String commandId) {
        BulkStatus status = Framework.getService(BulkService.class).getStatus(commandId);
        if (status.getState() == State.UNKNOWN) {
            throw new WebResourceNotFoundException("Bulk command with id=" + commandId + " doesn't exist");
        }
        return status;
    }

}
