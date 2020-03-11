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

import java.io.IOException;

import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enricher behaving differently in v1 and v2+.
 * <p>
 * In v1:
 *
 * <pre>
 * { "message":"foo" }
 * </pre>
 * <p>
 * In v2+:
 *
 * <pre>
 * { "message":"bar" }
 * </pre>
 *
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DummyEnricher extends AbstractJsonEnricher<DummyObject> {

    public static final String NAME = "dummy";

    public DummyEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DummyObject enriched) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        String message = ctx.getAPIVersion().eq(APIVersion.V1) ? "foo" : "bar";
        jg.writeStringField("message", message);
        jg.writeEndObject();
    }
}
