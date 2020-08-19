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
 *     Thomas Roger
 */
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * since 11.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BinaryManagerStatusJsonWriter extends ExtensibleEntityJsonWriter<BinaryManagerStatus> {

    public static final String ENTITY_TYPE = "binaryManagerStatus";

    public BinaryManagerStatusJsonWriter() {
        super(ENTITY_TYPE, BinaryManagerStatus.class);
    }

    @Override
    protected void writeEntityBody(BinaryManagerStatus entity, JsonGenerator jg) throws IOException {
        jg.writeNumberField("gcDuration", entity.getGCDuration());
        jg.writeNumberField("numBinaries", entity.getNumBinaries());
        jg.writeNumberField("sizeBinaries", entity.getSizeBinaries());
        jg.writeNumberField("numBinariesGC", entity.getNumBinariesGC());
        jg.writeNumberField("sizeBinariesGC", entity.getSizeBinariesGC());
    }
}
