/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Collection;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

/**
 * Enrich {@link DocumentModel} JSON object with an array of the document types that can be created under the current
 * document.
 *
 * @since 8.4
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SubtypesJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "subtypes";

    public SubtypesJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel enriched) throws IOException {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Collection<String> subtypes = enriched.getDocumentType().getAllowedSubtypes();
        jg.writeFieldName(NAME);
        jg.writeStartArray();
        for (String subtype : subtypes) {
            jg.writeStartObject();
            jg.writeStringField("type", subtype);
            jg.writeArrayFieldStart("facets");
            for (String facet : schemaManager.getDocumentType(subtype).getFacets()) {
                jg.writeString(facet);
            }
            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }
}
