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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 2025.0
 */
public interface SearchResponse {

    /**
     * Get the total number of match for the query. Limit is not taken in account.
     *
     * @return {@code -1} if unknown.
     */
    long getTotal();

    /**
     * Returns {@code true} if the total is accurate, i.e. not an estimation.
     */
    boolean isTotalAccurate();

    /**
     * Returns the number of returned search hits, taking in account limits.
     */
    long getHitsCount();

    /**
     * Returns the search hits.
     */
    List<SearchHit> getHits();

    /**
     * Returns search hits as a partial list.
     */
    PartialList<Map<String, Serializable>> getHitsAsMap();

    /**
     * Returns search hits as an iterator, the query must be a scroll query.
     *
     * @throws IllegalArgumentException if the query is not of scroll type.
     */
    IterableQueryResult getHitsAsIterator();

    /**
     * Returns {@code true} if the response is provided but search client lacks some search capabilities.
     */
    default boolean isMissingCapabilities() {
        return !getLimitations().isEmpty();
    }

    /**
     * Returns the list of {@link SearchClient.Capability} that are missing to fully perform the SearchQuery.
     */
    default List<SearchClient.Capability> getMissingCapabilities() {
        return getLimitations().stream().map(SearchLimitation::getAffectedCapability).distinct().toList();
    }

    /**
     * Returns structured limitations encountered during search execution. Enables consumers to distinguish root causes
     * (e.g. client unsupported vs. index mapping vs. operator not supported) and take appropriate action.
     * <p>
     * When non-empty, these limitations are the source of truth and {@link #getMissingCapabilities()} is derived from
     * the unique {@link SearchLimitation#getAffectedCapability()} values.
     *
     * @since 2025.17
     */
    // deprecated since 2021.x, turn into abstract method on deprecation removal
    default List<SearchLimitation> getLimitations() {
        return List.of();
    }

    /**
     * Returns a scroll context to use in {@link SearchService#searchScroll(SearchScrollContext)} to fetch the next
     * batch of results.
     */
    @Nullable
    SearchScrollContext getScrollContext();

    /**
     * Returns the aggregates.
     */
    List<Aggregate<? extends Bucket>> getAggregates();

    /**
     * Loads search hits from repository as a DocumentModelList using the provided session. Can be an expensive
     * operation. In the case of a multi repository search, additional sessions are created using the defaultSession
     * principal.
     */
    DocumentModelList loadDocuments(CoreSession defaultSession);

    /**
     * @return a {@link SearchResponse} builder.
     */
    static SearchResponseImpl.Builder builder(List<SearchHit> hits) {
        return new SearchResponseImpl.Builder(hits);
    }
}
