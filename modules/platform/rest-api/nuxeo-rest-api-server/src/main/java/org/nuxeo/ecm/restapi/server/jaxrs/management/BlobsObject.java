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
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.ACTION_NAME;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.DRY_RUN_PARAM;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.core.blob.scroll.RepositoryBlobScroll;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage the blobs.
 *
 * @since 2023
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "blobs")
@Produces(APPLICATION_JSON)
public class BlobsObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Garbage collect the orphaned blobs.
     */
    @DELETE
    @Path("orphaned")
    public BulkStatus garbageCollectBlobs(@QueryParam(value = DRY_RUN_PARAM) boolean dryRun) {
        String repository = ctx.getCoreSession().getRepositoryName();
        BulkService bulkService = Framework.getService(BulkService.class);
        BulkCommand command = new BulkCommand.Builder(ACTION_NAME, repository,
                SYSTEM_USERNAME).repository(repository)
                                .useGenericScroller()
                                .param(DRY_RUN_PARAM, dryRun)
                                .scroller(RepositoryBlobScroll.SCROLL_NAME)
                                .build();
        String commandId = bulkService.submit(command);
        return bulkService.getStatus(commandId);

    }
}
