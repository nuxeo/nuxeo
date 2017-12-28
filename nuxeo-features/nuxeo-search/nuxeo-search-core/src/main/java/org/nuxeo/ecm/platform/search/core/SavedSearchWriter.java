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
package org.nuxeo.ecm.platform.search.core;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.WILDCARD_VALUE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 8.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SavedSearchWriter extends ExtensibleEntityJsonWriter<SavedSearch> {

    public static final String ENTITY_TYPE = "savedSearch";

    @Inject
    private SchemaManager schemaManager;

    public SavedSearchWriter() {
        super(ENTITY_TYPE, SavedSearch.class);
    }

    @Override
    protected void writeEntityBody(SavedSearch search, JsonGenerator jg) throws IOException {
        jg.writeStringField("id", search.getId());
        jg.writeStringField("title", search.getTitle());
        jg.writeStringField("queryParams", search.getQueryParams());
        jg.writeStringField("query", search.getQuery());
        jg.writeStringField("queryLanguage", search.getQueryLanguage());
        jg.writeStringField("pageProviderName", search.getPageProviderName());
        jg.writeStringField("pageSize", search.getPageSize() == null ? null : search.getPageSize().toString());
        jg.writeStringField("currentPageIndex", search.getCurrentPageIndex() == null ? null
                : search.getCurrentPageIndex().toString());
        jg.writeStringField("maxResults", search.getMaxResults() == null ? null : search.getMaxResults().toString());
        jg.writeStringField("sortBy", search.getSortBy());
        jg.writeStringField("sortOrder", search.getSortOrder());
        jg.writeStringField("contentViewData", search.getContentViewData());

        Map<String, String> params = search.getNamedParams();
        if (params == null) {
            params = new HashMap<>();
        }

        jg.writeObjectFieldStart("params");
        Iterator<String> it = params.keySet().iterator();
        while (it.hasNext()) {
            String param = it.next();
            jg.writeStringField(param, search.getNamedParams().get(param));
        }

        Set<String> schemas = ctx.getProperties();
        if (schemas.size() > 0) {
            DocumentModel doc = search.getDocument();
            if (schemas.contains(WILDCARD_VALUE)) {
                // full document
                for (String schema : doc.getSchemas()) {
                    writeSchemaProperties(jg, doc, schema);
                }
            } else {
                for (String schema : schemas) {
                    if (doc.hasSchema(schema)) {
                        writeSchemaProperties(jg, doc, schema);
                    }
                }
            }
        }

        jg.writeEndObject();
    }

    // taken from DocumentModelJsonWriter
    private void writeSchemaProperties(JsonGenerator jg, DocumentModel doc, String schemaName) throws IOException {
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        // provides the current document to the property marshaller
        try (Closeable resource = ctx.wrap().with(ENTITY_TYPE, doc).open()) {
            Schema schema = schemaManager.getSchema(schemaName);
            String prefix = schema.getNamespace().prefix;
            if (prefix == null || prefix.length() == 0) {
                prefix = schemaName;
            }
            prefix = prefix + ":";
            for (Field field : schema.getFields()) {
                String prefixedName = prefix + field.getName().getLocalName();
                jg.writeFieldName(prefixedName);
                Property property = doc.getProperty(prefixedName);
                OutputStream out = new OutputStreamWithJsonWriter(jg);
                propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
            }
        }
    }
}
