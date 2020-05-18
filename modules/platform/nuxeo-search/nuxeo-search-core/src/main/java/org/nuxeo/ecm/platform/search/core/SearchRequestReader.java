/*
 * (C) Copyright 2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.search.core;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.*;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SearchRequestReader extends EntityJsonReader<SearchRequest> {

    public SearchRequestReader() {
        super(SearchRequest.ENTITY_TYPE);
    }

    @Override
    protected SearchRequest readEntity(JsonNode jn) throws IOException {
        List<String> queryParams = null;
        JsonNode node = jn.get("queryParams");
        if (node != null) {
            queryParams = node.isArray() ? getStringListField(jn, "queryParams")
                    : Arrays.asList(getStringField(jn, "queryParams").split(","));
        }
        String query = getStringField(jn, "query");
        String queryLanguage = getStringField(jn, "queryLanguage");
        String pageProviderName = getStringField(jn, "pageProviderName");
        Long pageSize = getLongField(jn, "pageSize");
        Long currentPageIndex = getLongField(jn, "currentPageIndex");
        Long offset = getLongField(jn, "offset");
        Long maxResults = getLongField(jn, "maxResults");
        String sortBy = getStringField(jn, "sortBy");
        String sortOrder = getStringField(jn, "sortOrder");
        String quickfilters = getStringField(jn, "quickfilters");
        String highlights = getStringField(jn, "highlights");
        JsonNode namedParamsNode = jn.has("params") ? jn.get("params") : null;

        Map<String, String> params = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        if (namedParamsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = namedParamsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = fields.next();
                params.put(
                        fieldEntry.getKey(),
                        fieldEntry.getValue().isTextual() ? fieldEntry.getValue().textValue()
                                : mapper.writeValueAsString(fieldEntry.getValue()));
            }
        }

        return new SearchRequest(queryParams, params, query, queryLanguage, pageProviderName, pageSize,
                currentPageIndex, offset, maxResults, sortBy, sortOrder, quickfilters, highlights);
    }
}
