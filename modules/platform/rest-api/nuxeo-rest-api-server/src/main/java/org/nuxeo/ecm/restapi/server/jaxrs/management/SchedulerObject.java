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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.scheduler.Schedule;
import org.nuxeo.ecm.core.scheduler.SchedulerService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage the scheduler tasks.
 *
 * @since 2023.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "scheduler")
@Produces(APPLICATION_JSON)
public class SchedulerObject extends AbstractResource<ResourceTypeImpl> {

    @GET
    public List<Schedule> getSchedules() {
        return Framework.getService(SchedulerService.class).getSchedules();
    }

    @PUT
    @Path("start")
    public void start() {
        Framework.getService(SchedulerService.class).resume();
    }

    /**
     * Pauses the scheduler service. The pause is cluster-wide and survives a node restart. See {@link #start()} to
     * resume the schedules service.
     */
    @PUT
    @Path("stop")
    public void stop() {
        Framework.getService(SchedulerService.class).pause();
    }
}
