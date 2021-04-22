/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Overrides the {@link DummyEnricher} with a higher priority to validate precedence.
 *
 * @since 11.5
 */
@Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
public class OverrideDummyEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "dummyEnricher";

    public OverrideDummyEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel enriched) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        jg.writeStringField("joe", "doe");
        jg.writeEndObject();
    }
}
