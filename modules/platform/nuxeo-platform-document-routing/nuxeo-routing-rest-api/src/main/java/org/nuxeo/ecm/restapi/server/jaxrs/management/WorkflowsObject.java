/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ALL_WORKFLOWS_QUERY;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.GC_ROUTES_ACTION_NAME;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage the worflows.
 *
 * @since 2023
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "workflows")
@Produces(APPLICATION_JSON)
public class WorkflowsObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Garbage collect the routes in state done or canceled as well as the orphan ones.
     */
    @DELETE
    @Path("orphaned")
    public BulkStatus garbageCollectRoutes() {
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(
                new BulkCommand.Builder(GC_ROUTES_ACTION_NAME, ALL_WORKFLOWS_QUERY, SYSTEM_USERNAME).repository(
                        ctx.getCoreSession().getRepositoryName()).build());
        return bulkService.getStatus(commandId);
    }
}
