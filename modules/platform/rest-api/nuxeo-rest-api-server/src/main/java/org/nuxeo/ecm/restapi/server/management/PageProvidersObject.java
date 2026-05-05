/*
 * (C) Copyright 2023-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.management;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider.SEARCH_ON_ALL_REPOSITORIES_PROPERTY;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.SearchServicePageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Endpoint to list PageProviders.
 *
 * @since 2023.34
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "page-providers")
@Produces(APPLICATION_JSON)
public class PageProvidersObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Lists the registered page providers.
     */
    @GET
    public String listPageProviders() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode res = mapper.createArrayNode();
        PageProviderService pps = Framework.getService(PageProviderService.class);
        SearchService searchService = Framework.getService(SearchService.class);
        String defaultSearchClient = searchService.getSearchIndex(searchService.getDefaultIndexName()).client();
        for (String ppName : pps.getPageProviderDefinitionNames()) {
            ObjectNode node = mapper.createObjectNode();
            PageProviderDefinition def = pps.getPageProviderDefinition(ppName);
            // Create an instance so class replacer is taken in account
            PageProvider<?> pp = pps.getPageProvider(ppName, def, null, null, 0L, 0L, null);
            String klass = pp.getClass().getCanonicalName();
            node.put("name", pp.getName());
            node.put("class", klass);
            node.put("maxPageSize", pp.getMaxPageSize());
            if (pp instanceof SearchServicePageProvider) {
                if (def instanceof SearchServicePageProviderDescriptor ssdef) {
                    if (isBlank(ssdef.getSearchClient())) {
                        node.put("defaultSearchClient", defaultSearchClient);
                    } else {
                        node.put("searchClient", ssdef.getSearchClient());
                    }
                    if (ssdef.getSearchIndexes() != null) {
                        node.put("searchIndexes", ssdef.getSearchIndexes().toString());
                    }
                } else {
                    node.put("defaultSearchClient", defaultSearchClient);
                }
                if (Boolean.parseBoolean((String) pp.getProperties().get(SEARCH_ON_ALL_REPOSITORIES_PROPERTY))) {
                    node.put("searchAllRepositories", "true");
                }
            }
            res.add(node);
        }
        return res.toString();
    }
}
