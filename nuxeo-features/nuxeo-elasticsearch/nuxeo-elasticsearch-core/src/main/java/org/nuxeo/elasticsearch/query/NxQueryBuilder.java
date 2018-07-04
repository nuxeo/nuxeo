/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.query;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.ACL_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.FETCH_DOC_FROM_ES_PROPERTY;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.fetcher.EsFetcher;
import org.nuxeo.elasticsearch.fetcher.Fetcher;
import org.nuxeo.elasticsearch.fetcher.VcsFetcher;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch query builder for the Nuxeo ES api.
 *
 * @since 5.9.5
 */
public class NxQueryBuilder {

    private static final int DEFAULT_LIMIT = 10;

    private static final String AGG_FILTER_SUFFIX = "_filter";

    private final CoreSession session;

    private final List<SortInfo> sortInfos = new ArrayList<>();

    private final List<String> repositories = new ArrayList<>();

    private final List<AggregateEsBase<? extends Bucket>> aggregates = new ArrayList<>();

    private int limit = DEFAULT_LIMIT;

    private int offset = 0;

    private String nxql;

    private org.elasticsearch.index.query.QueryBuilder esQueryBuilder;

    private boolean fetchFromElasticsearch = false;

    private boolean searchOnAllRepo = false;

    private String[] selectFields = { ElasticSearchConstants.ID_FIELD };

    private Map<String, Type> selectFieldsAndTypes;

    private boolean returnsDocuments = true;

    private boolean esOnly = false;

    private List<String> highlightFields;

    private EsFetcher.HitDocConsumer hitDocConsumer;

    public NxQueryBuilder(CoreSession coreSession) {
        session = coreSession;
        repositories.add(coreSession.getRepositoryName());
        fetchFromElasticsearch = Boolean.parseBoolean(Framework.getProperty(FETCH_DOC_FROM_ES_PROPERTY, "false"));
    }

    public static String getAggregateFilterId(Aggregate agg) {
        return agg.getId() + AGG_FILTER_SUFFIX;
    }

    /**
     * No more than that many documents will be returned. Default to {DEFAULT_LIMIT}. Since Nuxeo 8.4 and ES 2.x, we can
     * not give -1 to this method as the default configuration on ES allows to have a search window of 10000 documents
     * at maximum. This settings could be changed on ES by changing {index.max_result_window}, but it is preferable to
     * use the scan & scroll API.
     */
    public NxQueryBuilder limit(int limit) {
        // For compatibility only, deprecated since 8.4
        if (limit < 0) {
            limit = Integer.MAX_VALUE;
        }
        this.limit = limit;
        return this;
    }

    /**
     * Says to skip that many documents before beginning to return documents. If both offset and limit appear, then
     * offset documents are skipped before starting to count the limit documents that are returned.
     */
    public NxQueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public NxQueryBuilder addSort(SortInfo sortInfo) {
        sortInfos.add(sortInfo);
        return this;
    }

    public NxQueryBuilder addSort(SortInfo[] sortInfos) {
        if (sortInfos != null && sortInfos.length > 0) {
            Collections.addAll(this.sortInfos, sortInfos);
        }
        return this;
    }

    /**
     * Build the query from a NXQL string. You should either use nxql, either esQuery, not both.
     */
    public NxQueryBuilder nxql(String nxql) {
        this.nxql = nxql;
        this.esQueryBuilder = null;
        return this;
    }

    /**
     * Build the query using the Elasticsearch QueryBuilder API. You should either use nxql, either esQuery, not both.
     */
    public NxQueryBuilder esQuery(QueryBuilder queryBuilder) {
        esQueryBuilder = addSecurityFilter(queryBuilder);
        nxql = null;
        return this;
    }

    /**
     * Ask for the Elasticsearch _source field, use it to build documents.
     */
    public NxQueryBuilder fetchFromElasticsearch() {
        fetchFromElasticsearch = true;
        return this;
    }

    /**
     * If search results are found, use this SearchHit and DocumentModel consumer on each hit.
     * @since 10.2
     */
    public NxQueryBuilder hitDocConsumer(EsFetcher.HitDocConsumer consumer) {
        hitDocConsumer = consumer;
        return this;
    }
    
    /**
     * Fetch the documents using VCS (database) engine. This is done by default
     */
    public NxQueryBuilder fetchFromDatabase() {
        fetchFromElasticsearch = false;
        return this;
    }

    /**
     * Don't return document model list, aggregates or rows, only the original Elasticsearch response is accessible from
     * {@link EsResult#getElasticsearchResponse()}
     *
     * @since 7.3
     */
    public NxQueryBuilder onlyElasticsearchResponse() {
        esOnly = true;
        return this;
    }

    public NxQueryBuilder addAggregate(AggregateEsBase<? extends Bucket> aggregate) {
        aggregates.add(aggregate);
        return this;
    }

    public NxQueryBuilder addAggregates(List<AggregateEsBase<? extends Bucket>> aggregates) {
        if (aggregates != null && !aggregates.isEmpty()) {
            this.aggregates.addAll(aggregates);
        }
        return this;
    }

    /**
     * @since 9.1
     */
    public NxQueryBuilder highlight(List<String> highlightFields) {
        this.highlightFields = highlightFields;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public List<SortInfo> getSortInfos() {
        return sortInfos;
    }

    public String getNxql() {
        return nxql;
    }

    public boolean isFetchFromElasticsearch() {
        return fetchFromElasticsearch;
    }

    public CoreSession getSession() {
        return session;
    }

    /**
     * Get the Elasticsearch queryBuilder. Note that it returns only the query part without order, limits nor
     * aggregates, use the udpateRequest to get the full request.
     */
    public QueryBuilder makeQuery() {
        if (esQueryBuilder == null) {
            if (nxql != null) {
                esQueryBuilder = NxqlQueryConverter.toESQueryBuilder(nxql, session);
                // handle the built-in order by clause
                if (nxql.toLowerCase().contains("order by")) {
                    List<SortInfo> builtInSortInfos = NxqlQueryConverter.getSortInfo(nxql);
                    sortInfos.addAll(builtInSortInfos);
                }
                if (nxqlHasSelectClause(nxql)) {
                    selectFieldsAndTypes = NxqlQueryConverter.getSelectClauseFields(nxql);
                    Set<String> keySet = selectFieldsAndTypes.keySet();
                    selectFields = keySet.toArray(new String[keySet.size()]);
                    returnsDocuments = false;
                }
                esQueryBuilder = addSecurityFilter(esQueryBuilder);
            }
        }
        return esQueryBuilder;
    }

    protected boolean nxqlHasSelectClause(String nxql) {
        String lowerNxql = nxql.toLowerCase();
        return lowerNxql.startsWith("select") && !lowerNxql.startsWith("select * from");
    }

    public SortBuilder[] getSortBuilders() {
        SortBuilder[] ret;
        if (sortInfos.isEmpty()) {
            return new SortBuilder[0];
        }
        ret = new SortBuilder[sortInfos.size()];
        int i = 0;
        for (SortInfo sortInfo : sortInfos) {
            String fieldType = guessFieldType(sortInfo.getSortColumn());
            ret[i++] = new FieldSortBuilder(sortInfo.getSortColumn())
                                                                     .order(sortInfo.getSortAscending() ? SortOrder.ASC
                                                                             : SortOrder.DESC)
                                                                     .unmappedType(fieldType);
        }
        return ret;
    }

    protected String guessFieldType(String field) {
        String fieldType;
        try {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            fieldType = schemaManager.getField(field).getType().getName();
        } catch (NullPointerException e) {
            // probably an internal field without schema
            fieldType = "string";
        }
        switch (fieldType) {
        case "integer":
        case "long":
        case "boolean":
        case "date":
        case "string":
            return fieldType;
        }
        return "string";
    }

    protected QueryBuilder getAggregateFilter() {
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (AggregateEsBase agg : aggregates) {
            QueryBuilder filter = agg.getEsFilter();
            if (filter != null) {
                ret.must(filter);
            }
        }
        if (!ret.hasClauses()) {
            return null;
        }
        return ret;
    }

    protected QueryBuilder getAggregateFilterExceptFor(String id) {
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (AggregateEsBase agg : aggregates) {
            if (!agg.getId().equals(id)) {
                QueryBuilder filter = agg.getEsFilter();
                if (filter != null) {
                    ret.must(filter);
                }
            }
        }
        if (!ret.hasClauses()) {
            return QueryBuilders.matchAllQuery();
        }
        return ret;
    }

    public List<AggregateEsBase<? extends Bucket>> getAggregates() {
        return aggregates;
    }

    public List<FilterAggregationBuilder> getEsAggregates() {
        List<FilterAggregationBuilder> ret = new ArrayList<>(aggregates.size());
        for (AggregateEsBase agg : aggregates) {
            FilterAggregationBuilder fagg = null;
            fagg = new FilterAggregationBuilder(getAggregateFilterId(agg), getAggregateFilterExceptFor(agg.getId()));
            fagg.subAggregation(agg.getEsAggregate());
            ret.add(fagg);
        }
        return ret;
    }

    public void updateRequest(SearchSourceBuilder request) {
        // Set limits
        request.from(getOffset()).size(getLimit());
        // Build query with security checks
        request.query(makeQuery());
        // Add sort
        for (SortBuilder sortBuilder : getSortBuilders()) {
            request.sort(sortBuilder);
        }
        // Add Aggregate
        for (AbstractAggregationBuilder aggregate : getEsAggregates()) {
            request.aggregation(aggregate);
        }
        // Add Aggregate post filter
        QueryBuilder aggFilter = getAggregateFilter();
        if (aggFilter != null) {
            request.postFilter(aggFilter);
        }

        // Add highlighting
        if (highlightFields != null && !highlightFields.isEmpty()) {
            HighlightBuilder hb = new HighlightBuilder();
            for (String field : highlightFields) {
                hb.field(field);
            }
            hb.requireFieldMatch(false);
            request.highlighter(hb);
        }
        // Fields selection
        if (!isFetchFromElasticsearch()) {
            request.fetchSource(getSelectFields(), null);
        }

    }

    protected QueryBuilder addSecurityFilter(QueryBuilder query) {
        Principal principal = session.getPrincipal();
        if (principal == null
                || (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator())) {
            return query;
        }
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        // we want an ACL that match principals but we discard
        // unsupported ACE that contains negative ACE
        QueryBuilder aclFilter = QueryBuilders.boolQuery()
                                              .must(QueryBuilders.termsQuery(ACL_FIELD, principals))
                                              .mustNot(QueryBuilders.termsQuery(ACL_FIELD, UNSUPPORTED_ACL));
        return QueryBuilders.boolQuery().must(query).filter(aclFilter);
    }

    /**
     * Add a specific repository to search. Default search is done on the session repository only.
     *
     * @since 6.0
     */
    public NxQueryBuilder addSearchRepository(String repositoryName) {
        repositories.add(repositoryName);
        return this;
    }

    /**
     * Search on all available repositories.
     *
     * @since 6.0
     */
    public NxQueryBuilder searchOnAllRepositories() {
        searchOnAllRepo = true;
        return this;
    }

    /**
     * Return the list of repositories to search, or an empty list to search on all available repositories;
     *
     * @since 6.0
     */
    public List<String> getSearchRepositories() {
        if (searchOnAllRepo) {
            return Collections.<String> emptyList();
        }
        return repositories;
    }

    /**
     * @since 6.0
     */
    public Fetcher getFetcher(SearchResponse response, Map<String, String> repoNames) {
        if (isFetchFromElasticsearch()) {
            return new EsFetcher(session, response, repoNames, hitDocConsumer);
        }
        return new VcsFetcher(session, response, repoNames);
    }

    /**
     * @since 7.2
     */
    public String[] getSelectFields() {
        return selectFields;
    }

    /**
     * @since 7.2
     */
    public Map<String, Type> getSelectFieldsAndTypes() {
        return selectFieldsAndTypes;
    }

    /**
     * @since 7.2
     */
    public boolean returnsDocuments() {
        if (esOnly) {
            return false;
        }
        return returnsDocuments;
    }

    public boolean returnsRows() {
        if (esOnly) {
            return false;
        }
        return !returnsDocuments;
    }
}
