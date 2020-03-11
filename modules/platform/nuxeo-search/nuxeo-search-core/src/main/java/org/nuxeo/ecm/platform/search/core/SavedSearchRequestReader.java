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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 8.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SavedSearchRequestReader extends EntityJsonReader<SavedSearchRequest> {

    public SavedSearchRequestReader() {
        super(SavedSearchWriter.ENTITY_TYPE);
    }

    @Override
    protected SavedSearchRequest readEntity(JsonNode jn) throws IOException {
        String id = getStringField(jn, "id");
        String title = getStringField(jn, "title");
        String queryParams = getStringField(jn, "queryParams");
        String query = getStringField(jn, "query");
        String queryLanguage = getStringField(jn, "queryLanguage");
        String pageProviderName = getStringField(jn, "pageProviderName");
        Long pageSize = getLongField(jn, "pageSize");
        Long currentPageIndex = getLongField(jn, "currentPageIndex");
        Long maxResults = getLongField(jn, "maxResults");
        String sortBy = getStringField(jn, "sortBy");
        String sortOrder = getStringField(jn, "sortOrder");
        String contentViewData = getStringField(jn, "contentViewData");
        JsonNode queryParamsNode = jn.has("params") ? jn.get("params") : null;

        Map<String, String> params = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        if (queryParamsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = queryParamsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = fields.next();
                params.put(
                        fieldEntry.getKey(),
                        fieldEntry.getValue().isTextual() ? fieldEntry.getValue().textValue()
                                : mapper.writeValueAsString(fieldEntry.getValue()));
            }
        }

        return new SavedSearchRequest(id, title, queryParams, params, query, queryLanguage, pageProviderName, pageSize,
                currentPageIndex, maxResults, sortBy, sortOrder, contentViewData);
    }
}
