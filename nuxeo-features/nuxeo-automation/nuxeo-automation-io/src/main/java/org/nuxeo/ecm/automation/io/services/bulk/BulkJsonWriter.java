/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.automation.io.services.bulk;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.bulk.BulkStatus;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BulkJsonWriter extends ExtensibleEntityJsonWriter<BulkStatus> {

    public BulkJsonWriter() {
        super("bulk", BulkStatus.class);
    }

    @Override
    public void writeEntityBody(BulkStatus entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("state", entity.getState().toString());
        jg.writeStringField("creation", String.valueOf(entity.getCreationDate().toString()));
        jg.writeStringField("repository", entity.getCommand().getRepository());
        jg.writeStringField("operation", entity.getCommand().getOperation());
        jg.writeStringField("username", entity.getCommand().getUsername());
        jg.writeStringField("query", entity.getCommand().getQuery());
        if (entity.getScrolledDocumentCount() != null) {
            jg.writeNumberField("scrolledDocumentCount", entity.getScrolledDocumentCount().longValue());
        }
        jg.writeEndObject();
    }

}
