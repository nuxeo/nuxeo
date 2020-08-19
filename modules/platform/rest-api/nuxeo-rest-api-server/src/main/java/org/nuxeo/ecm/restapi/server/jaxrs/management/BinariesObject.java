/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage the binaries.
 *
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "binaries")
@Produces(APPLICATION_JSON)
public class BinariesObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Garbage collect the unused (orphaned) binaries.
     * 
     * @return {@link BinaryManagerStatus} if no gc is in progress, otherwise a
     *         {@link javax.ws.rs.core.Response.Status#CONFLICT}
     */
    @DELETE
    @Path("orphaned")
    public BinaryManagerStatus garbageCollectBinaries() {
        DocumentBlobManager documentBlobManager = Framework.getService(DocumentBlobManager.class);

        if (documentBlobManager.isBinariesGarbageCollectionInProgress()) {
            throw new NuxeoException(HttpServletResponse.SC_CONFLICT);
        }

        return documentBlobManager.garbageCollectBinaries(true);
    }
}
