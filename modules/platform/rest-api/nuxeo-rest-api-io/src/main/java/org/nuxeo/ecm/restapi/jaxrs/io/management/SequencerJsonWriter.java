/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.0
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SequencerJsonWriter extends ExtensibleEntityJsonWriter<Sequencer> {

    public static final String ENTITY_TYPE = "sequencer";

    public SequencerJsonWriter() {
        super(ENTITY_TYPE);
    }

    @Override
    protected void writeEntityBody(Sequencer entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", entity.name());
        jg.writeStringField("implementation", entity.implementation());
        jg.writeArrayFieldStart("keyValues");
        for (SequencerKeyValue kv : entity.keyValues()) {
            jg.writeStartObject();
            jg.writeStringField("key", kv.key());
            jg.writeNumberField("value", kv.value());
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }
}
