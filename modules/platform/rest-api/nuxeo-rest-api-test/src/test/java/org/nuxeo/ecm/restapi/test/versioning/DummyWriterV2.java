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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test.versioning;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writer only available in v2+.
 *
 * <pre>
 *  {
 *      "entity-type": "dummy2"
 *  }
 * </pre>
 *
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DummyWriterV2 extends ExtensibleEntityJsonWriter<DummyObjectV2> {

    public static final String ENTITY_TYPE = "dummy2";

    public DummyWriterV2() {
        super(ENTITY_TYPE, DummyObjectV2.class);
    }

    @Override
    protected void writeEntityBody(DummyObjectV2 entity, JsonGenerator jg) { // NOSONAR
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        if (ctx.getAPIVersion().lt(APIVersion.V11)) {
            return false;
        }

        return super.accept(clazz, genericType, mediatype);
    }
}
