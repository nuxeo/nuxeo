/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.action.GarbageCollectOrphanVersionsAction;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage the versions.
 *
 * @since 2023
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "versions")
@Produces(APPLICATION_JSON)
public class VersionsObject extends AbstractResource<ResourceTypeImpl> {

    public static final String ALL_VERSIONS_QUERY = "SELECT * FROM Document WHERE ecm:isVersion = 1";

    /**
     * Garbage collect the orphaned versions.
     * <p>
     * A version stays referenced, and therefore is not removed, if any proxy points to a version in the version history
     * of any live document, or in the case of tree snapshot if there is a snapshot containing a version in the version
     * history of any live document.
     */
    @DELETE
    @Path("orphaned")
    public BulkStatus garbageCollectVersions() {
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(new BulkCommand.Builder(GarbageCollectOrphanVersionsAction.ACTION_NAME,
                ALL_VERSIONS_QUERY, SYSTEM_USERNAME).repository(ctx.getCoreSession().getRepositoryName()).build());
        return bulkService.getStatus(commandId);
    }
}
