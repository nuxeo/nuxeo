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
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.lib.stream.log.LogLag;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.12
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamLagJsonWriter extends AbstractJsonWriter<StreamLag> {

    @Override
    public void write(StreamLag entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("stream", entity.stream());
        jg.writeStringField("consumer", entity.consumer());
        jg.writeNumberField("lag", LogLag.of(entity.lags()).lag());
        jg.writeArrayFieldStart("lags");
        for (int i = 0; i < entity.lags().size(); i++) {
            var lag = entity.lags().get(i);
            jg.writeStartObject();
            jg.writeNumberField("partition", i);
            jg.writeNumberField("pos", lag.lowerOffset());
            jg.writeNumberField("end", lag.upperOffset());
            jg.writeNumberField("lag", lag.lag());
            jg.writeEndObject();
        }
        jg.writeEndArray();
        jg.writeEndObject();
    }
}
