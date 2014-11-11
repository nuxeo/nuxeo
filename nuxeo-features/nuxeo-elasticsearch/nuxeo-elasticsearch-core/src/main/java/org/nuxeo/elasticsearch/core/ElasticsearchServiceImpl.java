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
 *     Tiry
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.core;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.fetcher.Fetcher;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * @since 5.9.6
 */
public class ElasticsearchServiceImpl implements ElasticSearchService {
    private static final Log log = LogFactory
            .getLog(ElasticsearchServiceImpl.class);
    // Metrics
    protected final MetricRegistry registry = SharedMetricRegistries
            .getOrCreate(MetricsService.class.getName());
    protected final Timer searchTimer;
    protected final Timer fetchTimer;
    private final ElasticSearchAdminImpl esa;

    public ElasticsearchServiceImpl(ElasticSearchAdminImpl esa) {
        this.esa = esa;
        searchTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "search"));
        fetchTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "fetch"));
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit,
            int offset, SortInfo... sortInfos) throws ClientException {
        NxQueryBuilder query = new NxQueryBuilder(session).nxql(nxql)
                .limit(limit).offset(offset).addSort(sortInfos);
        return query(query);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session,
            QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) throws ClientException {
        NxQueryBuilder query = new NxQueryBuilder(session)
                .esQuery(queryBuilder).limit(limit).offset(offset)
                .addSort(sortInfos);
        return query(query);
    }

    @Override
    public DocumentModelList query(NxQueryBuilder queryBuilder)
            throws ClientException {
        return queryAndAggregate(queryBuilder).getDocuments();
    }

    @Override
    public EsResult queryAndAggregate(NxQueryBuilder queryBuilder)
            throws ClientException {
        SearchResponse response = search(queryBuilder);
        DocumentModelListImpl docs = getDocumentModels(queryBuilder, response);
        List<Aggregate> aggs = getAggregates(queryBuilder, response);
        return new EsResult(docs, aggs);
    }

    protected DocumentModelListImpl getDocumentModels(
            NxQueryBuilder queryBuilder, SearchResponse response) {
        DocumentModelListImpl ret;
        if (response.getHits().getHits().length == 0) {
            ret = new DocumentModelListImpl(0);
            ret.setTotalSize(0);
            return ret;
        }
        Context stopWatch = fetchTimer.time();
        Fetcher fetcher = queryBuilder.getFetcher(response,
                esa.getRepositoryMap());
        try {
            ret = fetcher.fetchDocuments();
        } finally {
            stopWatch.stop();
        }
        long totalSize = response.getHits().getTotalHits();
        ret.setTotalSize(totalSize);
        return ret;
    }

    protected List<Aggregate> getAggregates(NxQueryBuilder queryBuilder,
            SearchResponse response) {
        for (AggregateEsBase<? extends Bucket> agg : queryBuilder
                .getAggregates()) {
            InternalFilter filter = response.getAggregations().get(
                    NxQueryBuilder.getAggregateFilterId(agg));
            if (filter == null) {
                continue;
            }
            MultiBucketsAggregation mba = filter.getAggregations().get(
                    agg.getId());
            if (mba == null) {
                continue;
            }
            agg.parseEsBuckets(mba.getBuckets());
        }
        @SuppressWarnings("unchecked")
        List<Aggregate> ret = (List<Aggregate>) (List<?>) queryBuilder
                .getAggregates();
        return ret;
    }

    protected SearchResponse search(NxQueryBuilder query) {
        Context stopWatch = searchTimer.time();
        try {
            SearchRequestBuilder request = buildEsSearchRequest(query);
            logSearchRequest(request, query);
            SearchResponse response = request.execute().actionGet();
            logSearchResponse(response);
            return response;
        } finally {
            stopWatch.stop();
        }
    }

    protected SearchRequestBuilder buildEsSearchRequest(NxQueryBuilder query) {
        SearchRequestBuilder request = esa
                .getClient()
                .prepareSearch(
                        esa.getSearchIndexes(query.getSearchRepositories()))
                .setTypes(DOC_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        if (query.isFetchFromElasticsearch()) {
            // fetch the _source without the binaryfulltext field
            request.setFetchSource(esa.getIncludeSourceFields(),
                    esa.getExcludeSourceFields());
        } else {
            request.addField(ElasticSearchConstants.ID_FIELD);
        }
        query.updateRequest(request);
        return request;
    }

    protected void logSearchResponse(SearchResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Response: " + response.toString());
        }
    }

    protected void logSearchRequest(SearchRequestBuilder request,
            NxQueryBuilder query) {
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Search query: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty' -d '%s'",
                            getSearchIndexesAsString(query), DOC_TYPE,
                            request.toString()));
        }
    }

    protected String getSearchIndexesAsString(NxQueryBuilder query) {
        return StringUtils.join(
                esa.getSearchIndexes(query.getSearchRepositories()), ',');
    }

}
