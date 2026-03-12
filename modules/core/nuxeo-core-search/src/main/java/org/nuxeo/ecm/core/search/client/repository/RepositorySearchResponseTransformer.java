/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.search.client.repository;

import static org.nuxeo.ecm.core.api.AbstractSession.LIMIT_RESULTS_PROPERTY;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter.REPOSITORY_PROP;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.search.AbstractSearchResponseTransformer;
import org.nuxeo.ecm.core.search.SearchClient;
import org.nuxeo.ecm.core.search.SearchHit;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class RepositorySearchResponseTransformer
        extends AbstractSearchResponseTransformer<PartialList<Map<String, Serializable>>> {

    protected final boolean limitedResults;

    protected RepositorySearchResponseTransformer(Set<SearchClient.Capability> supportedCapabilities) {
        super(supportedCapabilities);
        this.limitedResults = Boolean.parseBoolean(Framework.getProperty(LIMIT_RESULTS_PROPERTY));
    }

    @Override
    public SearchResponse apply(SearchQuery searchQuery, PartialList<Map<String, Serializable>> projections) {
        return SearchResponse.builder(makeSearchHits(searchQuery, projections))
                             .total(projections.totalSize())
                             .totalAccurate(!limitedResults || projections.totalSize() < 0)
                             .limitations(getLimitations(searchQuery))
                             .build();
    }

    protected List<SearchHit> makeSearchHits(SearchQuery searchQuery,
            PartialList<Map<String, Serializable>> projections) {
        var searchIndex = searchQuery.getSearchIndexes().getFirst();
        return projections.stream().map(projection -> {
            String repository = searchIndex.repository();
            String docId = projection.get(ECM_UUID).toString();
            var fields = new HashMap<>(projection);
            fields.put(REPOSITORY_PROP, repository);
            return SearchHit.builder(searchIndex.index(), docId)
                            .repository(repository)
                            .docId(docId)
                            .fields(fields)
                            .build();
        }).toList();
    }
}
