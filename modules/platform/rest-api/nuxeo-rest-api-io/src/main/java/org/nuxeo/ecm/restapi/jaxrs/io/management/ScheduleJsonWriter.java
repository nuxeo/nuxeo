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

package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.scheduler.Schedule;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2023.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ScheduleJsonWriter extends ExtensibleEntityJsonWriter<Schedule> {

    public static final String ENTITY_TYPE = "schedule";

    public ScheduleJsonWriter() {
        super(ENTITY_TYPE, Schedule.class);
    }

    @Override
    protected void writeEntityBody(Schedule schedule, JsonGenerator jg) throws IOException {
        jg.writeStringField("id", schedule.getId());
        jg.writeStringField("jobFactoryClass", schedule.getJobFactory().getClass().getCanonicalName());
        jg.writeStringField("eventId", schedule.getEventId());
        jg.writeStringField("eventCategory", schedule.getEventCategory());
        jg.writeStringField("cronExpression", schedule.getCronExpression());
        jg.writeStringField("username", schedule.getUsername());
        jg.writeBooleanField("enabled", schedule.isEnabled());
        jg.writeStringField("timeZone", schedule.getTimeZone());
    }
}
