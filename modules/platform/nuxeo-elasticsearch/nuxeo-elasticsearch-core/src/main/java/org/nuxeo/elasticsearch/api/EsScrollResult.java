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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.elasticsearch.api;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;

/**
 * Wrapper for the results of a scrollable search request.
 *
 * @see ElasticSearchService#scroll(NxQueryBuilder, long)
 * @see ElasticSearchService#scroll(EsScrollResult)
 * @since 8.3
 */
public class EsScrollResult extends EsResult {

    /**
     * {@link NxQueryBuilder} used for the initial search request.
     */
    protected final NxQueryBuilder queryBuilder;

    /**
     * Scroll id returned by the search request.
     */
    protected final String scrollId;

    /**
     * Timeout for keeping the search context alive.
     */
    protected final long keepAlive;

    public EsScrollResult(DocumentModelList documents, SearchResponse response, NxQueryBuilder queryBuilder,
            String scrollId, long keepAlive) {
        super(documents, null, response);
        this.queryBuilder = queryBuilder;
        this.scrollId = scrollId;
        this.keepAlive = keepAlive;
    }

    public EsScrollResult(IterableQueryResult rows, SearchResponse response, NxQueryBuilder queryBuilder,
            String scrollId, long keepAlive) {
        super(rows, null, response);
        this.queryBuilder = queryBuilder;
        this.scrollId = scrollId;
        this.keepAlive = keepAlive;
    }

    public EsScrollResult(SearchResponse response, NxQueryBuilder queryBuilder, String scrollId, long keepAlive) {
        super(response);
        this.queryBuilder = queryBuilder;
        this.scrollId = scrollId;
        this.keepAlive = keepAlive;
    }

    public EsScrollResult(NxQueryBuilder queryBuilder, String scrollId, long keepAlive) {
        this(null, queryBuilder, scrollId, keepAlive);
    }

    public NxQueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public String getScrollId() {
        return scrollId;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

}
