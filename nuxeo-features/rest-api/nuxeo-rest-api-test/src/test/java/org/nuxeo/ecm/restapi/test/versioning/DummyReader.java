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

import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reader behaving differently in v1 and v2+.
 * <p>
 * In v1, reads only the field {@code fieldV1}. In v2+ reads the fields {@code fieldV1} and {@code fieldV2}.
 *
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DummyReader extends EntityJsonReader<DummyObject> {

    public DummyReader() {
        super(DummyWriter.ENTITY_TYPE);
    }

    @Override
    protected DummyObject readEntity(JsonNode jn) {
        DummyObject dummyObject = new DummyObject();
        dummyObject.fieldV1 = getStringField(jn, "fieldV1");

        if (ctx.getAPIVersion().gte(APIVersion.V11)) {
            dummyObject.fieldV2 = getStringField(jn, "fieldV2");
        }
        return dummyObject;
    }
}
