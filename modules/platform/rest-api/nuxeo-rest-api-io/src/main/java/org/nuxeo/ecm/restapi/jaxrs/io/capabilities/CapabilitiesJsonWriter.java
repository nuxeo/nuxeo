/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.jaxrs.io.capabilities;

import static org.nuxeo.common.function.ThrowableBiConsumer.asBiConsumer;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.capabilities.Capabilities;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 11.5
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class CapabilitiesJsonWriter extends ExtensibleEntityJsonWriter<Capabilities> {

    public static final String ENTITY_TYPE = "capabilities";

    public CapabilitiesJsonWriter() {
        super(ENTITY_TYPE, Capabilities.class);
    }

    @Override
    protected void writeEntityBody(Capabilities entity, JsonGenerator jg) throws IOException {
        entity.get().forEach(asBiConsumer(jg::writeObjectField));
    }
}
