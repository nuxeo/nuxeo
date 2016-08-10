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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 6.0
 */
public class EsResult {
    private final DocumentModelList documents;

    private final IterableQueryResult rows;

    private final List<Aggregate<Bucket>> aggregates;

    private final SearchResponse response;

    public EsResult(DocumentModelList documents, List<Aggregate<Bucket>> aggregates, SearchResponse response) {
        this.documents = documents;
        this.rows = null;
        this.aggregates = aggregates;
        this.response = response;
    }

    public EsResult(IterableQueryResult rows, List<Aggregate<Bucket>> aggregates, SearchResponse response) {
        this.documents = null;
        this.rows = rows;
        this.aggregates = aggregates;
        this.response = response;
    }

    public EsResult(SearchResponse response) {
        this.documents = null;
        this.rows = null;
        this.aggregates = null;
        this.response = response;
    }

    /**
     * Get the list of Nuxeo documents, this is populated when using a SELECT * clause, or when submitting esQuery.
     *
     * @return null if the query returns fields or if the onlyElasticsearchResponse option is set.
     */
    public DocumentModelList getDocuments() {
        return documents;
    }

    /**
     * Iterator to use when selecting fields: SELECT ecm:uuid ...
     *
     * @since 7.2
     * @return null if the query returns documents or if the onlyElasticsearchResponse option is set.
     */
    public IterableQueryResult getRows() {
        return rows;
    }

    /**
     * Get the aggregates list or null if onlyElasticsearchResponse option is set.
     */
    public List<Aggregate<Bucket>> getAggregates() {
        return aggregates;
    }

    /**
     * Returns the original Elasticsearch response.
     *
     * Use it at your own risk
     * @since 7.3
     */
    public SearchResponse getElasticsearchResponse() {
        return response;
    }
}
