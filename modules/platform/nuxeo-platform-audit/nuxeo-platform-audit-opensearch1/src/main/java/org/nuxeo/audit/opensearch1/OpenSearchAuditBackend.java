/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.audit.opensearch1;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;
import static org.nuxeo.audit.io.LogEntryJsonWriter.EXTENDED_INFO_JSON_STRING_AS_JSON;
import static org.opensearch.common.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION;
import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.audit.service.AbstractAuditBackend;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.CursorResult;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;
import org.nuxeo.runtime.opensearch1.OpenSearchComponent;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.action.search.SearchType;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchModule;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.Max;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

/**
 * Implementation of the {@link AuditBackend} interface using OpenSearch persistence
 *
 * @author tiry
 */
public class OpenSearchAuditBackend extends AbstractAuditBackend
        implements AuditBackend.CursorServiceScroll<Iterator<SearchHit>, SearchHit> {

    private static final Logger log = LogManager.getLogger(OpenSearchAuditBackend.class);

    public static final String SEQ_NAME = "audit";

    // @since 2021.21
    protected static final String AUDIT_LATEST_LOG_ID_AFTER_DATE_PROP = "nuxeo.audit.opensearch1.latestLogId.afterDate";

    protected final OpenSearchClient client;

    protected final String indexName;

    protected final CursorService<Iterator<SearchHit>, SearchHit, String> cursorService;

    protected final CursorService<Iterator<SearchHit>, SearchHit, String> storageCursorService;

    // @since 2021.21
    protected String latestLogIdAfterDate;

    protected OpenSearchAuditBackend(OpenSearchClient client, String indexName) {
        this.client = client;
        this.indexName = indexName;
        initUIDSequencer(client, indexName);
        this.cursorService = new CursorService<>(SearchHit::getId);
        this.storageCursorService = new CursorService<>(SearchHit::getSourceAsString);
    }

    @Override
    public LogEntryList queryLogs(org.nuxeo.ecm.core.query.sql.model.QueryBuilder builder) {
        // prepare parameters
        MultiExpression predicate = builder.predicate();
        OrderByList orders = builder.orders();
        long offset = builder.offset();
        long limit = builder.limit();

        SearchSourceBuilder source = createSearchRequestSource(predicate, orders);

        // Perform search
        LogEntryList logEntries;
        SearchRequest request = createSearchRequest();
        request.source(source);
        if (limit == 0) {
            // return all result -> use the scroll api
            // offset is not taking into account when querying all results
            TimeValue keepAlive = TimeValue.timeValueMinutes(1);
            request.scroll(keepAlive);
            // the size here is the size of each scrolls
            source.size(100);
            SearchResponse searchResponse = null;
            try {
                // run request
                searchResponse = runRequest(request);

                // Build log entries
                logEntries = buildLogEntries(searchResponse);
                // Scroll on next results
                for (; //
                        searchResponse.getHits().getHits().length > 0
                                && logEntries.size() < searchResponse.getHits().getTotalHits().value; //
                        searchResponse = runNextScroll(searchResponse.getScrollId(), keepAlive)) {
                    // Build log entries
                    logEntries.addAll(buildLogEntries(searchResponse));
                }
            } finally {
                clearScrollContext(searchResponse);
            }
        } else {
            // return a page -> use a regular search
            source.from((int) offset).size((int) limit);

            // run request
            SearchResponse searchResponse = runRequest(request);

            // Build log entries
            logEntries = buildLogEntries(searchResponse);
        }

        return logEntries;
    }

    /** @since 2025.18 */
    @Override
    public CursorResult<Iterator<SearchHit>, SearchHit> scrollLogIdsAsCursor(
            org.nuxeo.ecm.core.query.sql.model.QueryBuilder builder, int batchSize, Duration keepAlive) {
        // prepare parameters
        MultiExpression predicate = builder.predicate();
        OrderByList orders = builder.orders();

        // create source
        SearchSourceBuilder source = createSearchRequestSource(predicate, orders);
        source.fetchSource(false);
        source.size(batchSize);
        // create request
        SearchRequest request = createSearchRequest();
        request.source(source).scroll(TimeValue.timeValueSeconds(keepAlive.toSeconds()));
        SearchResponse response = runRequest(request);
        // register cursor
        return new ESCursorResult(response, batchSize, (int) keepAlive.toSeconds());
    }

    protected void clearScrollContext(SearchResponse response) {
        if (response != null && response.getScrollId() != null) {
            ClearScrollRequest closeScrollRequest = new ClearScrollRequest();
            closeScrollRequest.addScrollId(response.getScrollId());
            client.clearScroll(closeScrollRequest);
        }
    }

    protected SearchSourceBuilder createSearchRequestSource(MultiExpression predicate, OrderByList orders) {
        // create ES query builder
        QueryBuilder query = createQueryBuilder(predicate);

        // create ES source
        SearchSourceBuilder source = new SearchSourceBuilder().query(QueryBuilders.constantScoreQuery(query)).size(100);

        // create sort
        orders.forEach(order -> source.sort(order.reference.name, order.isDescending ? SortOrder.DESC : SortOrder.ASC));
        return source;
    }

    @SuppressWarnings("unchecked")
    protected QueryBuilder createQueryBuilder(Predicate queryPredicate) {
        Operator operator = queryPredicate.operator;
        if (queryPredicate instanceof MultiExpression multiExpression) {
            return multiExpression.predicates.stream()
                                             .map(this::createQueryBuilder)
                                             .reduce(QueryBuilders.boolQuery(),
                                                     operator == Operator.OR ? BoolQueryBuilder::should
                                                             : BoolQueryBuilder::must,
                                                     operator == Operator.OR ? BoolQueryBuilder::should
                                                             : BoolQueryBuilder::must);
        } else if (operator == Operator.AND) {
            var boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(createQueryBuilder((Predicate) queryPredicate.lvalue));
            boolQuery.must(createQueryBuilder((Predicate) queryPredicate.rvalue));
            return boolQuery;
        } else if (operator == Operator.OR) {
            var boolQuery = QueryBuilders.boolQuery();
            boolQuery.should(createQueryBuilder((Predicate) queryPredicate.lvalue));
            boolQuery.should(createQueryBuilder((Predicate) queryPredicate.rvalue));
            return boolQuery;
        } else {
            // current implementation only uses Predicate/OrderByExpr with a simple Reference for left and right
            String leftName = ((Reference) queryPredicate.lvalue).name;
            // do a basic replace with no support for arrays nor correlated queries
            leftName = leftName.replaceAll("/", ".");
            if (operator == Operator.ISNULL) {
                return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(leftName));
            } else if (operator == Operator.ISNOTNULL) {
                return QueryBuilders.existsQuery(leftName);
            }
            Object rightValue = Literals.valueOf(queryPredicate.rvalue);
            if (operator == Operator.EQ) {
                return QueryBuilders.termQuery(leftName, rightValue);
            } else if (operator == Operator.NOTEQ) {
                return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(leftName, rightValue));
            } else if (operator == Operator.LT) {
                return QueryBuilders.rangeQuery(leftName).lt(rightValue);
            } else if (operator == Operator.LTEQ) {
                return QueryBuilders.rangeQuery(leftName).lte(rightValue);
            } else if (operator == Operator.GTEQ) {
                return QueryBuilders.rangeQuery(leftName).gte(rightValue);
            } else if (operator == Operator.GT) {
                return QueryBuilders.rangeQuery(leftName).gt(rightValue);
            } else if (operator == Operator.IN) {
                return QueryBuilders.termsQuery(leftName, (List<?>) rightValue);
            } else if (operator == Operator.STARTSWITH) {
                return makeStartsWithQuery(leftName, rightValue);
            } else if (operator == Operator.BETWEEN) {
                var lowerBound = ((List<ZonedDateTime>) rightValue).getFirst();
                var upperBound = ((List<ZonedDateTime>) rightValue).getLast();
                return QueryBuilders.rangeQuery(leftName).from(lowerBound).to(upperBound);
            } else if (operator == Operator.NOTBETWEEN) {
                var lowerBound = ((List<ZonedDateTime>) rightValue).getFirst();
                var upperBound = ((List<ZonedDateTime>) rightValue).getLast();
                return QueryBuilders.boolQuery()
                                    .mustNot(QueryBuilders.rangeQuery(leftName).from(lowerBound).to(upperBound));
            } else {
                throw new IllegalArgumentException("Unsupported operator: " + operator + " on field: " + leftName);
            }
        }
    }

    protected LogEntryList buildLogEntries(SearchResponse searchResponse) {
        var hits = searchResponse.getHits();
        List<LogEntry> entries = new ArrayList<>(hits.getHits().length);
        for (SearchHit hit : hits) {
            try {
                entries.add(MarshallerHelper.jsonToObject(LogEntry.class, hit.getSourceAsString(),
                        RenderingContext.CtxBuilder.get()));
            } catch (IOException e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return new LogEntryList(entries, hits.getTotalHits().value);
    }

    protected SearchRequest createSearchRequest() {
        return new SearchRequest(indexName).searchType(SearchType.DFS_QUERY_THEN_FETCH);
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        GetResponse ret = client.get(new GetRequest(indexName, String.valueOf(id)));
        if (!ret.isExists()) {
            return null;
        }
        try {
            return MarshallerHelper.jsonToObject(LogEntry.class, ret.getSourceAsString(),
                    RenderingContext.CtxBuilder.get());
        } catch (IOException e) {
            throw new NuxeoException("Unable to read Entry for id " + id, e);
        }
    }

    public SearchRequest buildQuery(String query, Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            query = expandQueryVariables(query, params);
        }
        SearchRequest request = createSearchRequest();
        SearchSourceBuilder sourceBuilder = createSearchSourceBuilder(query);
        return request.source(sourceBuilder);
    }

    protected SearchSourceBuilder createSearchSourceBuilder(String query) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        try {
            try (XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                                                        .createParser(
                                                                new NamedXContentRegistry(
                                                                        searchModule.getNamedXContents()),
                                                                THROW_UNSUPPORTED_OPERATION, query)) {
                searchSourceBuilder.parseXContent(parser);
            }
        } catch (IOException | ParsingException e) {
            log.error("Invalid query: {}: {}", query, e.getMessage(), e);
            throw new IllegalArgumentException("Bad query: " + query);
        }
        return searchSourceBuilder;
    }

    public String expandQueryVariables(String query, Object[] params) {
        Map<String, Object> qParams = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            query = query.replaceFirst("\\?", "\\${param" + i + "}");
            qParams.put("param" + i, params[i]);
        }
        return expandQueryVariables(query, qParams);
    }

    public String expandQueryVariables(String query, Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            TextTemplate tmpl = new TextTemplate();
            for (String key : params.keySet()) {
                Object val = params.get(key);
                switch (val) {
                    case Calendar calendar -> tmpl.setVariable(key, Long.toString(calendar.getTime().getTime()));
                    case Date date -> tmpl.setVariable(key, Long.toString(date.getTime()));
                    case null -> {
                    }
                    default -> tmpl.setVariable(key, val.toString());
                }
            }
            query = tmpl.processText(query);
        }
        return query;
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize) {
        SearchRequest request = buildQuery(query, params);
        if (pageNb > 0) {
            request.source().from(pageNb * pageSize);
        }
        if (pageSize > 0) {
            request.source().size(pageSize);
        }
        SearchResponse searchResponse = runRequest(request);
        return buildLogEntries(searchResponse);
    }

    @Override
    public void insertLogs(Collection<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        BulkRequest bulkRequest = new BulkRequest();
        try {
            for (var entry : entries) {
                if (entry.getId() == 0L || entry.getLogDate() == null) {
                    throw new IllegalArgumentException("Log entry must have an id and log date to be inserted");
                }
                log.debug("Indexing log entry: {}", entry);
                try (OutputStream out = new BytesStreamOutput(); //
                        XContentBuilder builder = jsonBuilder(out)) {
                    var renderingContext = RenderingContext.CtxBuilder.get();
                    renderingContext.setParameterValues(EXTENDED_INFO_JSON_STRING_AS_JSON, true);
                    var writer = Framework.getService(MarshallerRegistry.class)
                                          .getWriter(renderingContext, LogEntry.class, APPLICATION_JSON_TYPE);
                    writer.write(entry, LogEntry.class, LogEntry.class, APPLICATION_JSON_TYPE, out);
                    bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(entry.getId())).source(builder));
                }
            }

            BulkResponse bulkResponse = client.bulk(bulkRequest);
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse response : bulkResponse.getItems()) {
                    if (response.isFailed()) {
                        log.error("Unable to index audit entry {} : {}", response.getItemId(),
                                response.getFailureMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new NuxeoException("Error while indexing Audit entries", e);
        }

    }

    @Override
    public Long getEventsCount(String eventId) {
        SearchResponse res = client.search(new SearchRequest(indexName).source(new SearchSourceBuilder().query(
                QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("eventId", eventId))).size(0)));
        return res.getHits().getTotalHits().value;
    }

    public SearchResponse search(SearchRequest request) {
        String[] indices = request.indices();
        if (indices == null || indices.length != 1) {
            throw new IllegalStateException("Search on audit must include index name: " + request);
        }
        if (!indexName.equals(indices[0])) {
            throw new IllegalStateException("Search on audit must be on audit index: " + request);
        }
        return runRequest(request);
    }

    protected QueryBuilder buildFilter(PredicateDefinition[] predicates, DocumentModel searchDocumentModel) {
        if (searchDocumentModel == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();

        int nbFilters = 0;

        for (PredicateDefinition predicate : predicates) {

            // extract data from DocumentModel
            PredicateFieldDefinition[] fieldDef = predicate.getValues();
            Object[] val = new Object[fieldDef.length];
            for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                if (fieldDef[fidx].getXpath() != null) {
                    val[fidx] = searchDocumentModel.getPropertyValue(fieldDef[fidx].getXpath());
                } else {
                    val[fidx] = searchDocumentModel.getProperty(fieldDef[fidx].getSchema(), fieldDef[fidx].getName());
                }
            }

            if (!isNonNullParam(val)) {
                // skip predicate where all values are null
                continue;
            }

            nbFilters++;

            String op = predicate.getOperator();
            if (op.equalsIgnoreCase("IN")) {

                String[] values = null;
                if (val[0] instanceof Iterable<?> vals) {
                    List<String> l = new ArrayList<>();

                    for (Object v : vals) {
                        if (v != null) {
                            l.add(v.toString());
                        }
                    }
                    values = l.toArray(String[]::new);
                } else if (val[0] instanceof Object[]) {
                    values = (String[]) val[0];
                }
                filterBuilder.must(QueryBuilders.termsQuery(predicate.getParameter(), values));
            } else if (op.equalsIgnoreCase("BETWEEN")) {
                filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).gt(convertDate(val[0])));
                if (val.length > 1) {
                    filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).lt(convertDate(val[1])));
                }
            } else if (">".equals(op)) {
                filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).gt(convertDate(val[0])));
            } else if (">=".equals(op)) {
                filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).gte(convertDate(val[0])));
            } else if ("<".equals(op)) {
                filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).lt(convertDate(val[0])));
            } else if ("<=".equals(op)) {
                filterBuilder.must(QueryBuilders.rangeQuery(predicate.getParameter()).lte(convertDate(val[0])));
            } else {
                filterBuilder.must(QueryBuilders.termQuery(predicate.getParameter(), convertDate(val[0])));
            }
        }

        if (nbFilters == 0) {
            return QueryBuilders.matchAllQuery();
        }
        return filterBuilder;
    }

    protected Object convertDate(Object o) {
        // Date are convert to timestamp ms which is a known format by default for ES
        if (o instanceof Calendar) {
            return Long.valueOf(((Calendar) o).getTime().getTime());
        } else if (o instanceof Date) {
            return Long.valueOf(((Date) o).getTime());
        }
        return o;
    }

    public SearchRequest buildSearchQuery(String fixedPart, PredicateDefinition[] predicates,
            DocumentModel searchDocumentModel) {
        SearchRequest request = createSearchRequest();
        QueryBuilder queryBuilder = QueryBuilders.wrapperQuery(fixedPart);
        QueryBuilder filterBuilder = buildFilter(predicates, searchDocumentModel);
        request.source(
                new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(queryBuilder).filter(filterBuilder)));
        return request;
    }

    protected boolean isNonNullParam(Object[] val) {
        if (val == null) {
            return false;
        }
        for (Object v : val) {
            if (v != null) {
                if (v instanceof String) {
                    if (!((String) v).isEmpty()) {
                        return true;
                    }
                } else if (v instanceof String[]) {
                    if (((String[]) v).length > 0) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    SearchResponse runRequest(SearchRequest request) {
        logSearchRequest(request);
        SearchResponse response = client.search(request);
        logSearchResponse(response);
        return response;
    }

    protected SearchResponse runNextScroll(String scrollId, TimeValue keepAlive) {
        log.debug(
                "Scroll request: -XGET 'localhost:9200/_search/scroll' -d '{\"scroll\": \"{}\", \"scroll_id\": \"{}\" }'",
                keepAlive, scrollId);
        SearchResponse response = client.scroll(new SearchScrollRequest(scrollId).scroll(keepAlive));
        logSearchResponse(response);
        return response;
    }

    protected void logSearchResponse(SearchResponse response) {
        log.debug("Response: {}", response);
    }

    protected void logSearchRequest(SearchRequest request) {
        log.debug("Search query: curl -XGET 'http://localhost:9200/{}/_search?pretty' -d '{}'", indexName, request);
    }

    /**
     * Ensures the audit sequence returns an UID greater or equal than the maximum log entry id.
     */
    protected static void initUIDSequencer(OpenSearchClient esClient, String indexName) {
        boolean auditIndexExists = esClient.indexExists(indexName);
        if (!auditIndexExists) {
            return;
        }

        // Get max log entry id
        SearchRequest request = new SearchRequest(indexName).searchType(SearchType.DFS_QUERY_THEN_FETCH);
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery())
                                                .aggregation(AggregationBuilders.max("maxAgg").field("id")));
        SearchResponse searchResponse = esClient.search(request);
        Max agg = searchResponse.getAggregations().get("maxAgg");
        long maxLogEntryId = (long) agg.getValue();

        // Get next sequence id
        UIDGeneratorService uidGeneratorService = Framework.getService(UIDGeneratorService.class);
        UIDSequencer seq = uidGeneratorService.getSequencer();
        long currentSequenceValue = seq.getCurrent(SEQ_NAME);

        // Increment sequence to max log entry id if needed
        if (currentSequenceValue < maxLogEntryId) {
            log.info("UID returned by sequence: {} is: {}, initializing sequence to: {}", SEQ_NAME,
                    currentSequenceValue, maxLogEntryId);
            seq.initSequence(SEQ_NAME, maxLogEntryId);
        }
    }

    public String getIndexName() {
        return indexName;
    }

    @Override
    public void append(List<String> jsonEntries) {
        BulkRequest bulkRequest = new BulkRequest();
        for (String json : jsonEntries) {
            try {
                Object entryId = new JSONObject(json).opt(LOG_ID);
                if (entryId == null) {
                    throw new NuxeoException("A json entry has an empty id. entry=" + json);
                }
                IndexRequest request = new IndexRequest(indexName).id(entryId.toString());
                request.source(json, XContentType.JSON);
                bulkRequest.add(request);
            } catch (JSONException e) {
                throw new NuxeoException("Unable to deserialize json entry=" + json, e);
            }
        }
        client.bulk(bulkRequest);
    }

    @SuppressWarnings("resource") // CursorResult is being registered, must not be closed
    @Override
    public ScrollResult<String> scroll(org.nuxeo.ecm.core.query.sql.model.QueryBuilder builder, int batchSize,
            int keepAliveSeconds) {
        // prepare parameters
        MultiExpression predicate = builder.predicate();
        OrderByList orders = builder.orders();

        // create source
        SearchSourceBuilder source = createSearchRequestSource(predicate, orders);
        source.size(batchSize);
        // create request
        SearchRequest request = createSearchRequest();
        request.source(source).scroll(TimeValue.timeValueSeconds(keepAliveSeconds));
        SearchResponse response = runRequest(request);
        // register cursor
        String scrollId = storageCursorService.registerCursorResult(
                new ESCursorResult(response, batchSize, keepAliveSeconds));
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return storageCursorService.scroll(scrollId);
    }

    public class ESCursorResult extends CursorResult<Iterator<SearchHit>, SearchHit> {

        protected final String scrollId;

        protected boolean end;

        public ESCursorResult(SearchResponse response, int batchSize, int keepAliveSeconds) {
            super(response.getHits().iterator(), batchSize, keepAliveSeconds);
            this.scrollId = response.getScrollId();
        }

        @Override
        public boolean hasNext() {
            if (cursor == null || end) {
                return false;
            } else if (cursor.hasNext()) {
                return true;
            } else {
                runNextScroll();
                return !end;
            }
        }

        @Override
        public SearchHit next() {
            if (cursor != null && !cursor.hasNext() && !end) {
                // try to run a next scroll
                runNextScroll();
            }
            return super.next();
        }

        protected void runNextScroll() {
            SearchResponse response = OpenSearchAuditBackend.this.runNextScroll(scrollId,
                    TimeValue.timeValueSeconds(keepAliveSeconds));
            cursor = response.getHits().iterator();
            end = !cursor.hasNext();
        }

        @Override
        public void close() {
            ClearScrollRequest request = new ClearScrollRequest();
            request.addScrollId(scrollId);
            client.clearScroll(request);
            end = true;
            // Call super close to clear cursor
            super.close();
        }

    }

    @Override
    public long getLatestLogId(String repositoryId, String... eventIds) {
        if (getLatestLogIdAfterDate() == null) {
            return super.getLatestLogId(repositoryId, eventIds);
        }
        // limit to recent events to avoid performance problem related to total hits
        var builder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                             .and(Predicates.in(LOG_EVENT_ID, eventIds))
                                             .and(Predicates.gt(LOG_EVENT_DATE, getLatestLogIdAfterDate()))
                                             .order(OrderByExprs.desc(LOG_ID))
                                             .limit(1);
        return queryLogs(builder).stream().mapToLong(LogEntry::getId).findFirst().orElse(0L);
    }

    // @since 2021.21
    protected String getLatestLogIdAfterDate() {
        if (latestLogIdAfterDate == null) {
            latestLogIdAfterDate = Framework.getProperty(AUDIT_LATEST_LOG_ID_AFTER_DATE_PROP);
            if (latestLogIdAfterDate == null) {
                latestLogIdAfterDate = "";
            }
        }
        return latestLogIdAfterDate.isEmpty() ? null : latestLogIdAfterDate;
    }

    @Override
    protected void clearEntries() {
        ((OpenSearchComponent) Framework.getService(OpenSearchClientService.class)).dropAndInitIndex(indexName);
    }

    // copied from NxqlQueryConverter
    protected static QueryBuilder makeStartsWithQuery(String name, Object value) {
        QueryBuilder filter;
        String indexName = name + ".children";
        if ("/".equals(value)) {
            if (NXQL.ECM_PATH.equals(name)) {
                // any non orphan|place-less document must have a path starting with "/"
                filter = QueryBuilders.existsQuery(NXQL.ECM_PARENTID);
            } else {
                // match any document with a populated field
                filter = QueryBuilders.existsQuery(indexName);
            }
        } else {
            String v = String.valueOf(value);
            if (v.endsWith("/")) {
                v = v.replaceAll("/$", "");
            }
            if (NXQL.ECM_PATH.equals(name)) {
                // we don't want to return the parent when searching on ecm:path, see NXP-18955
                filter = QueryBuilders.boolQuery()
                                      .must(QueryBuilders.termQuery(indexName, v))
                                      .mustNot(QueryBuilders.termQuery(name, value));
            } else {
                filter = QueryBuilders.termQuery(indexName, v);
            }
        }
        return filter;
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case EXTENDED_INFO_SEARCH -> true;
            case STARTS_WITH_PARTIAL_MATCH -> false;
        };
    }

    @Override
    public CursorService<Iterator<SearchHit>, SearchHit, String> getCursorService() {
        return cursorService;
    }
}
