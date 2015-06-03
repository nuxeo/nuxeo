/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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


/**
 * @since 6.0
 */
public class EsResult {
    private final DocumentModelList documents;

    private final IterableQueryResult rows;

    private final List<Aggregate> aggregates;

    private final SearchResponse response;

    public EsResult(DocumentModelList documents, List<Aggregate> aggregates, SearchResponse response) {
        this.documents = documents;
        this.rows = null;
        this.aggregates = aggregates;
        this.response = response;
    }

    public EsResult(IterableQueryResult rows, List<Aggregate> aggregates, SearchResponse response) {
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
     * Get the aggretages list or null if onlyElasticsearchResponse option is set.
     */
    public List<Aggregate> getAggregates() {
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
