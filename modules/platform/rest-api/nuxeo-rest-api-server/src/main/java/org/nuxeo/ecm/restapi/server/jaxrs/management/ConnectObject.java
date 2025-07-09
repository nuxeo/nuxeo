/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.TechnicalInstanceIdentifier;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.ecm.restapi.jaxrs.io.management.ConnectStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to fetch the connect status.
 *
 * @since 2023
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "connect")
@Produces(APPLICATION_JSON)
public class ConnectObject extends AbstractResource<ResourceTypeImpl> {

    private static final Logger log = LogManager.getLogger(ConnectObject.class);

    @GET
    @Path("status")
    public ConnectStatus getStatus(@QueryParam(value = "forceRefresh") boolean forceRefresh) throws IOException {
        var csh = ConnectStatusHolder.instance();
        var service = Framework.getService(ConnectRegistrationService.class);
        String clid;
        try {
            clid = LogicalInstanceIdentifier.instance().getCLID();
        } catch (LogicalInstanceIdentifier.NoCLID e) {
            clid = null;
        }
        return new ConnectStatus(service.isInstanceRegistered(), csh.getRegistrationExpirationTimestamp(),
                csh.getStatus(forceRefresh), clid, TechnicalInstanceIdentifier.instance().getCTID());
    }
}
