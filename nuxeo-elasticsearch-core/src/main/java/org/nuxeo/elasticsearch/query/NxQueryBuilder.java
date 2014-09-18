/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.query;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.ACL_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXCLUDE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXTENDED_BOUND_MAX_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXTENDED_BOUND_MIN_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INCLUDE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INTERVAL_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_TERM_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_TERM_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_POST_ZONE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_PRE_ZONE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_SIZE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TIME_ZONE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_RANGE;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_SIGNIFICANT_TERMS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_TERMS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.FETCH_DOC_FROM_ES_PROPERTY;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BaseFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeBuilder;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.elasticsearch.fetcher.EsFetcher;
import org.nuxeo.elasticsearch.fetcher.Fetcher;
import org.nuxeo.elasticsearch.fetcher.VcsFetcher;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch query buidler for the Nuxeo ES api.
 *
 * @since 5.9.5
 */
public class NxQueryBuilder {

    private static final int DEFAULT_LIMIT = 10;
    private int limit = DEFAULT_LIMIT;
    private static final String AGG_FILTER_SUFFIX = "_filter";
    private final CoreSession session;
    private final List<SortInfo> sortInfos = new ArrayList<SortInfo>();
    private final List<AggregateQuery> aggregates = new ArrayList<AggregateQuery>();
    private final List<String> repositories = new ArrayList<String>();
    private int offset = 0;
    private String nxql;
    private org.elasticsearch.index.query.QueryBuilder esQueryBuilder;
    private boolean fetchFromElasticsearch = false;
    private boolean searchOnAllRepo = false;

    public NxQueryBuilder(CoreSession coreSession) {
        session = coreSession;
        repositories.add(coreSession.getRepositoryName());
        fetchFromElasticsearch = Boolean.parseBoolean(Framework.getProperty(
                FETCH_DOC_FROM_ES_PROPERTY, "false"));
    }

    public static String getAggregateFilderId(AggregateQuery aggQuery) {
        return aggQuery.getId() + AGG_FILTER_SUFFIX;
    }

    /**
     * No more than that many documents will be returned.
     *
     * Default to {DEFAULT_LIMIT}
     *
     */
    public NxQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Says to skip that many documents before beginning to return documents.
     *
     * If both offset and limit appear, then offset documents are skipped before
     * starting to count the limit documents that are returned.
     *
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
     * Build the query from a NXQL string.
     *
     * You should either use nxql, either esQuery, not both.
     */
    public NxQueryBuilder nxql(String nxql) {
        this.nxql = nxql;
        this.esQueryBuilder = null;
        return this;
    }

    /**
     * Build the query using the Elasticsearch QueryBuilder API.
     *
     * You should either use nxql, either esQuery, not both.
     */
    public NxQueryBuilder esQuery(QueryBuilder queryBuilder) {
        this.esQueryBuilder = queryBuilder;
        this.nxql = null;
        return this;
    }

    /**
     * Fetch the documents from the Elasticsearch _source field.
     *
     */
    public NxQueryBuilder fetchFromElasticsearch() {
        this.fetchFromElasticsearch = true;
        return this;
    }

    /**
     * Fetch the documents using VCS (database) engine.
     *
     * This is done by default
     */
    public NxQueryBuilder fetchFromDatabase() {
        this.fetchFromElasticsearch = false;
        return this;
    }

    public NxQueryBuilder addAggregate(AggregateQuery aggregate) {
        aggregates.add(aggregate);
        return this;
    }

    public NxQueryBuilder addAggregates(List<AggregateQuery> aggregates) {
        if (aggregates != null && !aggregates.isEmpty()) {
            this.aggregates.addAll(aggregates);
        }
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
     * Get the Elasticsearch queryBuilder.
     */
    public QueryBuilder makeQuery() {
        if (esQueryBuilder == null) {
            if (nxql != null) {
                esQueryBuilder = NxqlQueryConverter.toESQueryBuilder(nxql);
                // handle the built-in order by clause
                if (nxql.toLowerCase().contains("order by")) {
                    List<SortInfo> builtInSortInfos = NxqlQueryConverter
                            .getSortInfo(nxql);
                    sortInfos.addAll(builtInSortInfos);
                }
                esQueryBuilder = addSecurityFilter(esQueryBuilder);
            }
        }
        return esQueryBuilder;
    }

    public SortBuilder[] getSortBuilders() {
        SortBuilder[] ret;
        if (sortInfos.isEmpty()) {
            return new SortBuilder[0];
        }
        ret = new SortBuilder[sortInfos.size()];
        int i = 0;
        for (SortInfo sortInfo : sortInfos) {
            ret[i++] = new FieldSortBuilder(sortInfo.getSortColumn())
                    .order(sortInfo.getSortAscending() ? SortOrder.ASC
                            : SortOrder.DESC);
        }
        return ret;
    }

    protected FilterBuilder getAggregateFilter() {
        boolean hasFilter = false;
        AndFilterBuilder ret = FilterBuilders.andFilter();
        for (AggregateQuery aggQuery : aggregates) {
            if (!aggQuery.getSelection().isEmpty()) {
                BaseFilterBuilder filter = getFilterForSelection(aggQuery);
                if (filter != null) {
                    ret.add(filter);
                    hasFilter = true;
                }
            }
        }
        if (!hasFilter) {
            return null;
        }
        return ret;
    }

    protected FilterBuilder getAggregateFilterExceptFor(String id) {
        boolean hasFilter = false;
        AndFilterBuilder ret = FilterBuilders.andFilter();
        for (AggregateQuery aggQuery : aggregates) {
            if (!aggQuery.getId().equals(id)) {
                BaseFilterBuilder filter = getFilterForSelection(aggQuery);
                if (filter != null) {
                    ret.add(filter);
                    hasFilter = true;
                }
            }
        }
        if (!hasFilter) {
            return FilterBuilders.matchAllFilter();
        }
        return ret;
    }

    private BaseFilterBuilder getFilterForSelection(AggregateQuery aggQuery) {
        if (aggQuery.getSelection().isEmpty()) {
            return null;
        }
        BaseFilterBuilder ret = null;
        switch (aggQuery.getType()) {
        case AGG_TYPE_TERMS:
        case AGG_TYPE_SIGNIFICANT_TERMS:
            ret = FilterBuilders.termsFilter(aggQuery.getField(),
                    aggQuery.getSelection());
            break;
        case AGG_TYPE_RANGE:
            OrFilterBuilder orFilter = FilterBuilders.orFilter();
            for (AggregateRangeDefinition range : aggQuery.getRanges()) {
                if (aggQuery.getSelection().contains(range.getKey())) {
                    RangeFilterBuilder rangeFilter = FilterBuilders
                            .rangeFilter(aggQuery.getField());
                    if (range.getFrom() != null) {
                        rangeFilter.gte(range.getFrom());
                    }
                    if (range.getTo() != null) {
                        rangeFilter.lt(range.getTo());
                    }
                    orFilter.add(rangeFilter);
                }
            }
            ret = orFilter;
            break;
        case AGG_TYPE_DATE_RANGE:
            OrFilterBuilder orDateFilter = FilterBuilders.orFilter();
            for (AggregateRangeDateDefinition range : aggQuery.getDateRanges()) {
                if (aggQuery.getSelection().contains(range.getKey())) {
                    RangeFilterBuilder rangeFilter = FilterBuilders
                            .rangeFilter(aggQuery.getField());
                    if (range.getFromAsString() != null) {
                        rangeFilter.gte(range.getFromAsString());
                    }
                    if (range.getToAsString() != null) {
                        rangeFilter.lt(range.getToAsString());
                    }
                    orDateFilter.add(rangeFilter);
                }
            }
            ret = orDateFilter;
            break;
        case AGG_TYPE_DATE_HISTOGRAM:
        case AGG_TYPE_HISTOGRAM:
            // Selection not supported
            break;
        }
        return ret;
    }

    public List<AggregateQuery> getAggregatesQuery() {
        return aggregates;
    }

    public List<AbstractAggregationBuilder> getAggregates() {
        List<AbstractAggregationBuilder> ret = new ArrayList<AbstractAggregationBuilder>(
                aggregates.size());
        for (AggregateQuery aggQuery : aggregates) {
            FilterAggregationBuilder fagg = new FilterAggregationBuilder(
                    getAggregateFilderId(aggQuery));
            fagg.filter(getAggregateFilterExceptFor(aggQuery.getId()));
            switch (aggQuery.getType()) {
            case AGG_TYPE_TERMS:
                fagg.subAggregation(getTermsBuilder(aggQuery));
                break;
            case AGG_TYPE_SIGNIFICANT_TERMS:
                fagg.subAggregation(getSignificantTermsBuilder(aggQuery));
                break;
            case AGG_TYPE_RANGE:
                fagg.subAggregation(getRangeBuilder(aggQuery));
                break;
            case AGG_TYPE_DATE_RANGE:
                fagg.subAggregation(getRangeDateBuilder(aggQuery));
                break;
            case AGG_TYPE_HISTOGRAM:
                fagg.subAggregation(getHistogramBuilder(aggQuery));
                break;
            case AGG_TYPE_DATE_HISTOGRAM:
                fagg.subAggregation(getHistogramDateBuilder(aggQuery));
                break;
            default:
                fagg.subAggregation(getRangeBuilder(aggQuery));
                throw new NotImplementedException(String.format(
                        "%s aggregation type is unknown for agg: %s",
                        aggQuery.getType(), aggQuery));
            }
            ret.add(fagg);
        }
        return ret;
    }

    protected TermsBuilder getTermsBuilder(AggregateQuery aggQuery) {
        TermsBuilder ret = AggregationBuilders.terms(aggQuery.getId()).field(
                aggQuery.getField());
        Map<String, String> props = aggQuery.getProperties();
        if (props.containsKey(AGG_SIZE_PROP)) {
            ret.size(Integer.parseInt(props.get(AGG_SIZE_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_EXCLUDE_PROP)) {
            ret.exclude(props.get(AGG_EXCLUDE_PROP));
        }
        if (props.containsKey(AGG_INCLUDE_PROP)) {
            ret.include(props.get(AGG_INCLUDE_PROP));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(Terms.Order.count(false));
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(Terms.Order.count(true));
                break;
            case AGG_ORDER_TERM_DESC:
                ret.order(Terms.Order.term(false));
                break;
            case AGG_ORDER_TERM_ASC:
                ret.order(Terms.Order.term(true));
                break;
            }
        }
        return ret;
    }

    protected SignificantTermsBuilder getSignificantTermsBuilder(
            AggregateQuery aggQuery) {
        SignificantTermsBuilder ret = AggregationBuilders.significantTerms(
                aggQuery.getId()).field(aggQuery.getField());
        Map<String, String> props = aggQuery.getProperties();
        if (props.containsKey(AGG_SIZE_PROP)) {
            ret.size(Integer.parseInt(props.get(AGG_SIZE_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Integer.parseInt(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        return ret;
    }

    protected RangeBuilder getRangeBuilder(AggregateQuery aggQuery) {
        RangeBuilder ret = AggregationBuilders.range(aggQuery.getId()).field(
                aggQuery.getField());
        for (AggregateRangeDefinition range : aggQuery.getRanges()) {
            if (range.getFrom() != null) {
                if (range.getTo() != null) {
                    ret.addRange(range.getKey(), range.getFrom(), range.getTo());
                } else {
                    ret.addUnboundedFrom(range.getKey(), range.getFrom());
                }
            } else if (range.getTo() != null) {
                ret.addUnboundedTo(range.getKey(), range.getTo());
            }
        }
        return ret;
    }

    protected DateRangeBuilder getRangeDateBuilder(AggregateQuery aggQuery) {
        DateRangeBuilder ret = AggregationBuilders.dateRange(aggQuery.getId())
                .field(aggQuery.getField());
        for (AggregateRangeDateDefinition range : aggQuery.getDateRanges()) {
            if (range.getFromAsString() != null) {
                if (range.getToAsString() != null) {
                    ret.addRange(range.getKey(), range.getFromAsString(),
                            range.getToAsString());
                } else {
                    ret.addUnboundedFrom(range.getKey(),
                            range.getFromAsString());
                }
            } else if (range.getToAsString() != null) {
                ret.addUnboundedTo(range.getKey(), range.getToAsString());
            }
        }
        return ret;
    }

    protected HistogramBuilder getHistogramBuilder(AggregateQuery aggQuery) {
        HistogramBuilder ret = AggregationBuilders.histogram(aggQuery.getId())
                .field(aggQuery.getField());
        Map<String, String> props = aggQuery.getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret.interval(Integer.parseInt(props.get(AGG_INTERVAL_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(Histogram.Order.COUNT_DESC);
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(Histogram.Order.COUNT_ASC);
                break;
            case AGG_ORDER_KEY_DESC:
                ret.order(Histogram.Order.KEY_DESC);
                break;
            case AGG_ORDER_KEY_ASC:
                ret.order(Histogram.Order.KEY_ASC);
                break;
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP)
                && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MIN_PROP)),
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MAX_PROP)));
        }
        return ret;
    }

    protected DateHistogramBuilder getHistogramDateBuilder(
            AggregateQuery aggQuery) {
        DateHistogramBuilder ret = AggregationBuilders.dateHistogram(
                aggQuery.getId()).field(aggQuery.getField());
        Map<String, String> props = aggQuery.getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret.interval(new DateHistogram.Interval(props
                    .get(AGG_INTERVAL_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(Histogram.Order.COUNT_DESC);
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(Histogram.Order.COUNT_ASC);
                break;
            case AGG_ORDER_KEY_DESC:
                ret.order(Histogram.Order.KEY_DESC);
                break;
            case AGG_ORDER_KEY_ASC:
                ret.order(Histogram.Order.KEY_ASC);
                break;
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP)
                && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(props.get(AGG_EXTENDED_BOUND_MIN_PROP),
                    props.get(AGG_EXTENDED_BOUND_MAX_PROP));
        }
        if (props.containsKey(AGG_TIME_ZONE_PROP)) {
            ret.preZone(props.get(AGG_TIME_ZONE_PROP));
        }
        if (props.containsKey(AGG_PRE_ZONE_PROP)) {
            ret.preZone(props.get(AGG_PRE_ZONE_PROP));
        }
        if (props.containsKey(AGG_POST_ZONE_PROP)) {
            ret.postZone(props.get(AGG_POST_ZONE_PROP));
        }
        return ret;
    }

    public void updateRequest(SearchRequestBuilder request) {
        // Set limits
        request.setFrom(getOffset()).setSize(getLimit());
        // Build query with security checks
        request.setQuery(makeQuery());
        // Add sort
        for (SortBuilder sortBuilder : getSortBuilders()) {
            request.addSort(sortBuilder);
        }
        // Add Aggregate
        for (AbstractAggregationBuilder aggregate : getAggregates()) {
            request.addAggregation(aggregate);
        }
        // Add Aggregate post filter
        FilterBuilder aggFilter = getAggregateFilter();
        if (aggFilter != null) {
            request.setPostFilter(aggFilter);
        }
    }

    protected QueryBuilder addSecurityFilter(QueryBuilder query) {
        AndFilterBuilder aclFilter;
        Principal principal = session.getPrincipal();
        if (principal == null
                || (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal)
                        .isAdministrator())) {
            return query;
        }
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        // we want an ACL that match principals but we discard
        // unsupported ACE that contains negative ACE
        aclFilter = FilterBuilders.andFilter(FilterBuilders.inFilter(ACL_FIELD,
                principals), FilterBuilders.notFilter(FilterBuilders.inFilter(
                ACL_FIELD, UNSUPPORTED_ACL)));
        return QueryBuilders.filteredQuery(query, aclFilter);
    }

    /**
     * Add a specific repository to search.
     *
     * Default search is done on the session repository only.
     *
     * @since 5.9.6
     */
    public NxQueryBuilder addSearchRepository(String repositoryName) {
        repositories.add(repositoryName);
        return this;
    }

    /**
     * Search on all available repositories.
     *
     * @since 5.9.6
     */
    public NxQueryBuilder searchOnAllRepositories() {
        searchOnAllRepo = true;
        return this;
    }

    /**
     * Return the list of repositories to search, or an empty list to search on
     * all available repositories;
     *
     * @since 5.9.6
     */
    public List<String> getSearchRepositories() {
        if (searchOnAllRepo) {
            return Collections.<String> emptyList();
        }
        return repositories;
    }

    /**
     *
     * @since 5.9.6
     */
    public Fetcher getFetcher(SearchResponse response,
            Map<String, String> repoNames) {
        if (isFetchFromElasticsearch()) {
            return new EsFetcher(session, response, repoNames);
        }
        return new VcsFetcher(session, response, repoNames);
    }
}
