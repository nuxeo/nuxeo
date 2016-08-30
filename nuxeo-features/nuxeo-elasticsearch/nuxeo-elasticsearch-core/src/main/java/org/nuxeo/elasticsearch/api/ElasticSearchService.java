/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.api;

import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;

/**
 * Interface to search on documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchService {

    /**
     * Returns a document list using an {@link NxQueryBuilder}.
     *
     * @since 5.9.5
     */
    DocumentModelList query(NxQueryBuilder queryBuilder);

    /**
     * Returns documents and aggregates.
     *
     * @since 6.0
     */
    EsResult queryAndAggregate(NxQueryBuilder queryBuilder);

    /**
     * Performs the initial search of a scrollable search request using an {@link NxQueryBuilder}.
     *
     * @param queryBuilder
     * @param keepAlive the search context lifetime
     * @return an {@link EsScrollResult} including the search results and a scroll id, to be passed to the subsequent
     *         calls to {@link #scroll(EsScrollResult)}
     * @since 8.3
     */
    EsScrollResult scroll(NxQueryBuilder queryBuilder, long keepAlive);

    /**
     * Retrieves the next batch of results of a scrollable search request for the given {@link EsScrollResult}.
     *
     * @return an {@link EsScrollResult} including the search results and a scroll id, to be passed to the subsequent
     *         calls to {@link #scroll(EsScrollResult)}
     * @since 8.3
     */
    EsScrollResult scroll(EsScrollResult scrollResult);

    /**
     * Clear scroll on ElasticSearch cluster for the given {@link EsScrollResult}.
     *
     * @since 8.4
     */
    void clearScroll(EsScrollResult scrollResult);

    /**
     * Returns a document list using an NXQL query. Fetch documents from the VCS repository.
     *
     * @since 5.9.3
     * @deprecated since 6.0, use query with NxQueryBuilder
     */
    @Deprecated
    DocumentModelList query(CoreSession session, String nxql, int limit, int offset, SortInfo... sortInfos);

    /**
     * Returns a document list using an ElasticSearch {@link QueryBuilder}. Fetch documents from the VCS repository.
     *
     * @since 5.9.3
     * @deprecated since 6.0, use query with NxQueryBuilder
     */
    @Deprecated
    DocumentModelList query(CoreSession session, QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos);

}
