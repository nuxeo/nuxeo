/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.Migration;
import org.nuxeo.runtime.migration.MigrationService;

/**
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "migration")
@Produces(APPLICATION_JSON)
public class MigrationObject extends AbstractResource<ResourceTypeImpl> {

    @GET
    @Path("{migrationId}")
    public Migration doGet(@PathParam("migrationId") String migrationId) {
        return Framework.getService(MigrationService.class).getMigration(migrationId);
    }

    @GET
    public List<Migration> doGetList() {
        return Framework.getService(MigrationService.class).getMigrations();
    }

    @POST
    @Path("{migrationId}/probe")
    public Migration doProbe(@PathParam("migrationId") String migrationId) {
        Framework.getService(MigrationService.class).probeAndSetState(migrationId);
        return doGet(migrationId);
    }

    @POST
    @Path("{migrationId}/run")
    public Response doRun(@PathParam("migrationId") String migrationId) {
        Framework.getService(MigrationService.class).probeAndRun(migrationId);
        return Response.status(SC_ACCEPTED).build();
    }

    @POST
    @Path("{migrationId}/run/{stepId}")
    public Response doRun(@PathParam("migrationId") String migrationId, @PathParam("stepId") String stepId) {
        Framework.getService(MigrationService.class).runStep(migrationId, stepId);
        return Response.status(SC_ACCEPTED).build();
    }

}
