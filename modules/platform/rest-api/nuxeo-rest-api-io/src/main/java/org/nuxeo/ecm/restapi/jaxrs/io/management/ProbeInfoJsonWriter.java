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
 *     Nour Al Kotob
 */

package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.common.utils.DateUtils;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.management.api.ProbeInfo;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 11.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ProbeInfoJsonWriter extends ExtensibleEntityJsonWriter<ProbeInfo> {

    public static final String ENTITY_TYPE = "probe";

    public ProbeInfoJsonWriter() {
        super(ENTITY_TYPE, ProbeInfo.class);
    }

    @Override
    protected void writeEntityBody(ProbeInfo entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", entity.getShortcutName());

        writeEntityField("status", entity.getStatus(), jg);

        jg.writeObjectFieldStart("history");
        jg.writeStringField("lastRun", DateUtils.formatISODateTime(entity.getLastRunnedDate()));
        jg.writeStringField("lastSuccess", DateUtils.formatISODateTime(entity.getLastSucceedDate()));
        jg.writeStringField("lastFail", DateUtils.formatISODateTime(entity.getLastFailedDate()));
        jg.writeEndObject();

        jg.writeObjectFieldStart("counts");
        jg.writeNumberField("run", entity.getRunnedCount());
        jg.writeNumberField("success", entity.getSucceedCount());
        jg.writeNumberField("failure", entity.getFailedCount());
        jg.writeEndObject();

        jg.writeNumberField("time", entity.getLastDuration());
    }
}
