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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.introspection;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.11
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionStreamJsonWriter extends AbstractJsonWriter<StreamIntrospection.Stream> {

    @Override
    public void write(StreamIntrospection.Stream entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", entity.name());
        jg.writeNumberField("partitions", entity.partitions());
        jg.writeStringField("codec", entity.codec());
        jg.writeEndObject();
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class ListJsonWriter extends AbstractJsonWriter<List<StreamIntrospection.Stream>> {

        @Override
        public void write(List<StreamIntrospection.Stream> streams, JsonGenerator jg) throws IOException {
            jg.writeStartArray();
            streams.forEach(ThrowableConsumer.asConsumer(stream -> writeEntity(stream, jg)));
            jg.writeEndArray();
        }
    }
}
