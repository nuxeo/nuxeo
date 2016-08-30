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
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.fetcher.Fetcher;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * @since 6.0
 */
public class ElasticSearchServiceImpl implements ElasticSearchService {
    private static final Log log = LogFactory.getLog(ElasticSearchServiceImpl.class);

    private static final java.lang.String LOG_MIN_DURATION_FETCH_KEY = "org.nuxeo.elasticsearch.core.log_min_duration_fetch_ms";

    private static final long LOG_MIN_DURATION_FETCH_NS = Long.parseLong(
            Framework.getProperty(LOG_MIN_DURATION_FETCH_KEY, "200")) * 1000000;

    // Metrics
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Timer searchTimer;

    protected final Timer scrollTimer;

    protected final Timer fetchTimer;

    private final ElasticSearchAdminImpl esa;

    public ElasticSearchServiceImpl(ElasticSearchAdminImpl esa) {
        this.esa = esa;
        searchTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "search"));
        scrollTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "scroll"));
        fetchTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "fetch"));
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit, int offset, SortInfo... sortInfos) {
        NxQueryBuilder query = new NxQueryBuilder(session).nxql(nxql).limit(limit).offset(offset).addSort(sortInfos);
        return query(query);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) {
        NxQueryBuilder query = new NxQueryBuilder(session).esQuery(queryBuilder)
                                                          .limit(limit)
                                                          .offset(offset)
                                                          .addSort(sortInfos);
        return query(query);
    }

    @Override
    public DocumentModelList query(NxQueryBuilder queryBuilder) {
        return queryAndAggregate(queryBuilder).getDocuments();
    }

    @Override
    public EsResult queryAndAggregate(NxQueryBuilder queryBuilder) {
        SearchResponse response = search(queryBuilder);
        List<Aggregate<Bucket>> aggs = getAggregates(queryBuilder, response);
        if (queryBuilder.returnsDocuments()) {
            DocumentModelListImpl docs = getDocumentModels(queryBuilder, response);
            return new EsResult(docs, aggs, response);
        } else if (queryBuilder.returnsRows()) {
            IterableQueryResult rows = getRows(queryBuilder, response);
            return new EsResult(rows, aggs, response);
        }
        return new EsResult(response);
    }

    @Override
    public EsScrollResult scroll(NxQueryBuilder queryBuilder, long keepAlive) {
        return scroll(queryBuilder, SearchType.DFS_QUERY_THEN_FETCH, keepAlive);
    }

    protected EsScrollResult scroll(NxQueryBuilder queryBuilder, SearchType searchType, long keepAlive) {
        SearchResponse response = searchScroll(queryBuilder, searchType, keepAlive);
        return getScrollResults(queryBuilder, response, response.getScrollId(), keepAlive);
    }

    @Override
    public EsScrollResult scroll(EsScrollResult scrollResult) {
        SearchResponse response = nextScroll(scrollResult.getScrollId(), scrollResult.getKeepAlive());
        return getScrollResults(scrollResult.getQueryBuilder(), response, response.getScrollId(),
                scrollResult.getKeepAlive());
    }

    @Override
    public void clearScroll(EsScrollResult scrollResult) {
        clearScroll(scrollResult.getScrollId());
    }

    protected void clearScroll(String scrollId) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Clear scroll : curl -XDELETE 'http://localhost:9200/_search/scroll' -d '{\"scroll_id\" : [\"%s\"]}'",
                    scrollId));
        }
        esa.getClient().prepareClearScroll().addScrollId(scrollId).execute().actionGet();
    }

    protected EsScrollResult getScrollResults(NxQueryBuilder queryBuilder, SearchResponse response, String scrollId,
            long keepAlive) {
        if (queryBuilder.returnsDocuments()) {
            DocumentModelListImpl docs = getDocumentModels(queryBuilder, response);
            return new EsScrollResult(docs, response, queryBuilder, scrollId, keepAlive);
        } else if (queryBuilder.returnsRows()) {
            IterableQueryResult rows = getRows(queryBuilder, response);
            return new EsScrollResult(rows, response, queryBuilder, scrollId, keepAlive);
        }
        return new EsScrollResult(response, queryBuilder, scrollId, keepAlive);
    }

    protected DocumentModelListImpl getDocumentModels(NxQueryBuilder queryBuilder, SearchResponse response) {
        DocumentModelListImpl ret;
        long totalSize = response.getHits().getTotalHits();
        if (!queryBuilder.returnsDocuments() || response.getHits().getHits().length == 0) {
            ret = new DocumentModelListImpl(0);
            ret.setTotalSize(totalSize);
            return ret;
        }
        Context stopWatch = fetchTimer.time();
        Fetcher fetcher = queryBuilder.getFetcher(response, esa.getRepositoryMap());
        try {
            ret = fetcher.fetchDocuments();
        } finally {
            logMinDurationFetch(stopWatch.stop(), totalSize);
        }
        ret.setTotalSize(totalSize);
        return ret;
    }

    private void logMinDurationFetch(long duration, long totalSize) {
        if (log.isDebugEnabled() && (duration > LOG_MIN_DURATION_FETCH_NS)) {
            String msg = String.format("Slow fetch duration_ms:\t%.2f\treturning:\t%d documents", duration / 1000000.0,
                    totalSize);
            if (log.isTraceEnabled()) {
                log.trace(msg, new Throwable("Slow fetch document stack trace"));
            } else {
                log.debug(msg);
            }
        }
    }

    protected List<Aggregate<Bucket>> getAggregates(NxQueryBuilder queryBuilder, SearchResponse response) {
        for (AggregateEsBase<? extends Bucket> agg : queryBuilder.getAggregates()) {
            InternalFilter filter = response.getAggregations().get(NxQueryBuilder.getAggregateFilterId(agg));
            if (filter == null) {
                continue;
            }
            MultiBucketsAggregation mba = filter.getAggregations().get(agg.getId());
            if (mba == null) {
                continue;
            }
            agg.parseEsBuckets(mba.getBuckets());
        }
        @SuppressWarnings("unchecked")
        List<Aggregate<Bucket>> ret = (List<Aggregate<Bucket>>) (List<?>) queryBuilder.getAggregates();
        return ret;
    }

    private IterableQueryResult getRows(NxQueryBuilder queryBuilder, SearchResponse response) {
        return new EsResultSetImpl(response, queryBuilder.getSelectFieldsAndTypes());
    }

    protected SearchResponse search(NxQueryBuilder query) {
        Context stopWatch = searchTimer.time();
        try {
            SearchType searchType = SearchType.DFS_QUERY_THEN_FETCH;
            SearchRequestBuilder request = buildEsSearchRequest(query, searchType);
            logSearchRequest(request, query, searchType);
            SearchResponse response = request.execute().actionGet();
            logSearchResponse(response);
            return response;
        } finally {
            stopWatch.stop();
        }
    }

    protected SearchResponse searchScroll(NxQueryBuilder query, SearchType searchType, long keepAlive) {
        Context stopWatch = searchTimer.time();
        try {
            SearchRequestBuilder request = buildEsSearchScrollRequest(query, searchType, keepAlive);
            logSearchRequest(request, query, searchType, keepAlive);
            SearchResponse response = request.execute().actionGet();
            logSearchResponse(response);
            return response;
        } finally {
            stopWatch.stop();
        }
    }

    protected SearchResponse nextScroll(String scrollId, long keepAlive) {
        Context stopWatch = scrollTimer.time();
        try {
            SearchScrollRequestBuilder request = buildEsScrollRequest(scrollId, keepAlive);
            logScrollRequest(scrollId, keepAlive);
            SearchResponse response = request.execute().actionGet();
            logSearchResponse(response);
            return response;
        } finally {
            stopWatch.stop();
        }
    }

    protected SearchRequestBuilder buildEsSearchRequest(NxQueryBuilder query, SearchType searchType) {
        SearchRequestBuilder request = esa.getClient()
                                          .prepareSearch(esa.getSearchIndexes(query.getSearchRepositories()))
                                          .setTypes(DOC_TYPE)
                                          .setSearchType(searchType);
        query.updateRequest(request);
        if (query.isFetchFromElasticsearch()) {
            // fetch the _source without the binaryfulltext field
            request.setFetchSource(esa.getIncludeSourceFields(), esa.getExcludeSourceFields());
        }
        return request;
    }

    protected SearchRequestBuilder buildEsSearchScrollRequest(NxQueryBuilder query, SearchType searchType,
            long keepAlive) {
        return buildEsSearchRequest(query, searchType).setScroll(new TimeValue(keepAlive)).setSize(query.getLimit());
    }

    protected SearchScrollRequestBuilder buildEsScrollRequest(String scrollId, long keepAlive) {
        return esa.getClient().prepareSearchScroll(scrollId).setScroll(new TimeValue(keepAlive));
    }

    protected void logSearchResponse(SearchResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Response: " + response.toString());
        }
    }

    protected void logSearchRequest(SearchRequestBuilder request, NxQueryBuilder query, SearchType searchType) {
        logSearchRequest(request, query, searchType, null);
    }

    protected void logSearchRequest(SearchRequestBuilder request, NxQueryBuilder query, SearchType searchType,
            Long keepAlive) {
        if (log.isDebugEnabled()) {
            String scroll = keepAlive != null ? "&scroll=" + keepAlive : "";
            log.debug(String.format(
                    "Search query: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty&search_type=%s%s' -d '%s'",
                    getSearchIndexesAsString(query), DOC_TYPE, searchType.toString().toLowerCase(), scroll,
                    request.toString()));
        }
    }

    protected void logScrollRequest(String scrollId, long keepAlive) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Scroll search: curl -XGET 'http://localhost:9200/_search/scroll?pretty' -d '{\"scroll\" : \"%d\", \"scroll_id\" : \"%s\"}'",
                    keepAlive, scrollId));
        }
    }

    protected String getSearchIndexesAsString(NxQueryBuilder query) {
        return StringUtils.join(esa.getSearchIndexes(query.getSearchRepositories()), ',');
    }

}
