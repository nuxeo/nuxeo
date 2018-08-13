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
 *     Tiry
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.audit;

import static org.elasticsearch.common.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.CursorResult;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the {@link AuditBackend} interface using Elasticsearch persistence
 *
 * @author tiry
 */
public class ESAuditBackend extends AbstractAuditBackend implements AuditBackend {

    public static final String SEQ_NAME = "audit";

    public static final String MIGRATION_FLAG_PROP = "audit.elasticsearch.migration";

    public static final String MIGRATION_BATCH_SIZE_PROP = "audit.elasticsearch.migration.batchSize";

    public static final String MIGRATION_DONE_EVENT = "sqlToElasticsearchMigrationDone";

    public static final int MIGRATION_DEFAULT_BACTH_SIZE = 1000;

    protected CursorService<Iterator<SearchHit>, SearchHit, String> cursorService;

    public ESAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        super(component, config);
    }

    /**
     * @since 9.3
     */
    public ESAuditBackend() {
        super();
    }

    protected ESClient esClient;

    protected static final Log log = LogFactory.getLog(ESAuditBackend.class);

    protected BaseLogEntryProvider provider = new BaseLogEntryProvider() {

        @Override
        public int removeEntries(String eventId, String pathPattern) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public void addLogEntry(LogEntry logEntry) {
            List<LogEntry> entries = new ArrayList<>();
            entries.add(logEntry);
            addLogEntries(entries);
        }

    };

    protected ESClient getClient() {
        log.info("Activate Elasticsearch backend for Audit");
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        ESClient client = esa.getClient();
        ensureUIDSequencer(client);
        return client;
    }

    protected boolean isMigrationDone() {
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> entries = reader.queryLogs(new String[] { MIGRATION_DONE_EVENT }, null);
        return !entries.isEmpty();
    }

    @Override
    public int getApplicationStartedOrder() {
        int elasticOrder = ((DefaultComponent) Framework.getRuntime().getComponent(
                "org.nuxeo.elasticsearch.ElasticSearchComponent")).getApplicationStartedOrder();
        int uidgenOrder = ((DefaultComponent) Framework.getRuntime().getComponent(
                "org.nuxeo.ecm.core.uidgen.UIDGeneratorService")).getApplicationStartedOrder();
        return Integer.max(elasticOrder, uidgenOrder) + 1;
    }

    @Override
    public void onApplicationStarted() {
        esClient = getClient();
        if (Boolean.parseBoolean(Framework.getProperty(MIGRATION_FLAG_PROP))) {
            if (!isMigrationDone()) {
                log.info(String.format(
                        "Property %s is true and migration is not done yet, processing audit migration from SQL to Elasticsearch index",
                        MIGRATION_FLAG_PROP));
                // Drop audit index first in case of a previous bad migration
                ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
                esa.dropAndInitIndex(getESIndexName());
                int batchSize = MIGRATION_DEFAULT_BACTH_SIZE;
                String batchSizeProp = Framework.getProperty(MIGRATION_BATCH_SIZE_PROP);
                if (batchSizeProp != null) {
                    batchSize = Integer.parseInt(batchSizeProp);
                }
                migrate(batchSize);
            } else {
                log.warn(String.format(
                        "Property %s is true but migration is already done, please set this property to false",
                        MIGRATION_FLAG_PROP));
            }
        } else {
            log.debug(String.format("Property %s is false, not processing any migration", MIGRATION_FLAG_PROP));
        }
        cursorService = new CursorService<>(SearchHit::getSourceAsString);
    }

    @Override
    public void onApplicationStopped() {
        if (esClient == null) {
            return;
        }
        try {
            esClient.close();
            cursorService.clear();
        } catch (Exception e) {
            log.warn("Fail to close esClient: " + e.getMessage(), e);
        } finally {
            esClient = null;
            cursorService = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(org.nuxeo.ecm.core.query.sql.model.QueryBuilder builder) {
        // prepare parameters
        Predicate predicate = builder.predicate();
        OrderByList orders = builder.orders();
        long offset = builder.offset();
        long limit = builder.limit();

        SearchSourceBuilder source = createSearchRequestSource(predicate, orders);

        // Perform search
        List<LogEntry> logEntries;
        SearchRequest request = createSearchRequest();
        request.source(source);
        if (limit == 0) {
            // return all result -> use the scroll api
            // offset is not taking into account when querying all results
            TimeValue keepAlive = TimeValue.timeValueMinutes(1);
            request.scroll(keepAlive);
            // the size here is the size of each scrolls
            source.size(100);

            // run request
            SearchResponse searchResponse = runRequest(request);

            // Build log entries
            logEntries = buildLogEntries(searchResponse);
            // Scroll on next results
            for (; //
            searchResponse.getHits().getHits().length > 0
                    && logEntries.size() < searchResponse.getHits().getTotalHits(); //
                    searchResponse = runNextScroll(searchResponse.getScrollId(), keepAlive)) {
                // Build log entries
                logEntries.addAll(buildLogEntries(searchResponse));
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

    protected SearchSourceBuilder createSearchRequestSource(Predicate predicate, OrderByList orders) {
        // create ES query builder
        QueryBuilder query = createQueryBuilder(predicate);

        // create ES source
        SearchSourceBuilder source = new SearchSourceBuilder().query(QueryBuilders.constantScoreQuery(query)).size(100);

        // create sort
        orders.forEach(order -> source.sort(order.reference.name, order.isDescending ? SortOrder.DESC : SortOrder.ASC));
        return source;
    }

    protected QueryBuilder createQueryBuilder(Predicate andPredicate) {
        // cast parameters
        // current implementation only support a MultiExpression with AND operator
        @SuppressWarnings("unchecked")
        List<Predicate> predicates = (List<Predicate>) ((List<?>) ((MultiExpression) andPredicate).values);
        // current implementation only use Predicate/OrderByExpr with a simple Reference for left and right
        Function<Operand, String> getFieldName = operand -> ((Reference) operand).name;

        QueryBuilder query;
        if (predicates.isEmpty()) {
            query = QueryBuilders.matchAllQuery();
        } else {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (Predicate predicate : predicates) {
                String leftName = getFieldName.apply(predicate.lvalue);
                Operator operator = predicate.operator;
                Object rightValue = Literals.valueOf(predicate.rvalue);
                if (Operator.EQ.equals(operator)) {
                    boolQuery.must(QueryBuilders.termQuery(leftName, rightValue));
                } else if (Operator.NOTEQ.equals(operator)) {
                    boolQuery.mustNot(QueryBuilders.termQuery(leftName, rightValue));
                } else if (Operator.LT.equals(operator)) {
                    boolQuery.must(QueryBuilders.rangeQuery(leftName).lt(rightValue));
                } else if (Operator.LTEQ.equals(operator)) {
                    boolQuery.must(QueryBuilders.rangeQuery(leftName).lte(rightValue));
                } else if (Operator.GTEQ.equals(operator)) {
                    boolQuery.must(QueryBuilders.rangeQuery(leftName).gte(rightValue));
                } else if (Operator.GT.equals(operator)) {
                    boolQuery.must(QueryBuilders.rangeQuery(leftName).gt(rightValue));
                } else if (Operator.IN.equals(operator)) {
                    boolQuery.must(QueryBuilders.termsQuery(leftName, (List<?>) rightValue));
                } else if (Operator.STARTSWITH.equals(operator)) {
                    boolQuery.must(NxqlQueryConverter.makeStartsWithQuery(leftName, rightValue));
                }
            }
            query = boolQuery;
        }
        return query;
    }

    protected List<LogEntry> buildLogEntries(SearchResponse searchResponse) {
        List<LogEntry> entries = new ArrayList<>(searchResponse.getHits().getHits().length);
        ObjectMapper mapper = new ObjectMapper();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(mapper.readValue(hit.getSourceAsString(), LogEntryImpl.class));
            } catch (IOException e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    protected SearchRequest createSearchRequest() {
        return new SearchRequest(getESIndexName()).searchType(SearchType.DFS_QUERY_THEN_FETCH);
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        GetResponse ret = esClient.get(
                new GetRequest(getESIndexName(), ElasticSearchConstants.ENTRY_TYPE, String.valueOf(id)));
        if (!ret.isExists()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(ret.getSourceAsString(), LogEntryImpl.class);
        } catch (IOException e) {
            throw new NuxeoException("Unable to read Entry for id " + id, e);
        }
    }

    public SearchRequest buildQuery(String query, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
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
            try (XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(
                    new NamedXContentRegistry(searchModule.getNamedXContents()), THROW_UNSUPPORTED_OPERATION, query)) {
                searchSourceBuilder.parseXContent(parser);
            }
        } catch (IOException | ParsingException e) {
            log.error("Invalid query: " + query + ": " + e.getMessage(), e);
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
        if (params != null && params.size() > 0) {
            TextTemplate tmpl = new TextTemplate();
            for (String key : params.keySet()) {
                Object val = params.get(key);
                if (val == null) {
                    continue;
                } else if (val instanceof Calendar) {
                    tmpl.setVariable(key, Long.toString(((Calendar) val).getTime().getTime()));
                } else if (val instanceof Date) {
                    tmpl.setVariable(key, Long.toString(((Date) val).getTime()));
                } else {
                    tmpl.setVariable(key, val.toString());
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
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize) {
        SearchRequest request = createSearchRequest();
        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
        if (eventIds != null && eventIds.length > 0) {
            if (eventIds.length == 1) {
                filterBuilder.must(QueryBuilders.termQuery("eventId", eventIds[0]));
            } else {
                filterBuilder.must(QueryBuilders.termsQuery("eventId", eventIds));
            }
        }
        if (categories != null && categories.length > 0) {
            if (categories.length == 1) {
                filterBuilder.must(QueryBuilders.termQuery("category", categories[0]));
            } else {
                filterBuilder.must(QueryBuilders.termsQuery("category", categories));
            }
        }
        if (path != null) {
            filterBuilder.must(QueryBuilders.termQuery("docPath", path));
        }

        if (limit != null) {
            filterBuilder.must(QueryBuilders.rangeQuery("eventDate").lt(convertDate(limit)));
        }
        request.source(new SearchSourceBuilder().query(QueryBuilders.constantScoreQuery(filterBuilder)));
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
    public void addLogEntries(List<LogEntry> entries) {

        if (entries.isEmpty()) {
            return;
        }

        BulkRequest bulkRequest = new BulkRequest();
        JsonFactory factory = new JsonFactory();

        UIDGeneratorService uidGeneratorService = Framework.getService(UIDGeneratorService.class);
        UIDSequencer seq = uidGeneratorService.getSequencer();

        try {
            List<Long> block = seq.getNextBlock(SEQ_NAME, entries.size());
            for (int i = 0; i < entries.size(); i++) {
                LogEntry entry = entries.get(i);
                entry.setId(block.get(i));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Indexing log entry: %s", entry));
                }
                entry.setLogDate(new Date());
                try (OutputStream out = new BytesStreamOutput(); //
                        JsonGenerator jg = factory.createGenerator(out); //
                        XContentBuilder builder = jsonBuilder(out)) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(jg, entry);
                    bulkRequest.add(new IndexRequest(getESIndexName(), ElasticSearchConstants.ENTRY_TYPE,
                            String.valueOf(entry.getId())).source(builder));
                }
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest);
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse response : bulkResponse.getItems()) {
                    if (response.isFailed()) {
                        log.error("Unable to index audit entry " + response.getItemId() + " :"
                                + response.getFailureMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new NuxeoException("Error while indexing Audit entries", e);
        }

    }

    @Override
    public Long getEventsCount(String eventId) {
        SearchResponse res = esClient.search(
                new SearchRequest(getESIndexName()).source(
                        new SearchSourceBuilder().query(
                                QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("eventId", eventId)))
                                                 .size(0)));
        return res.getHits().getTotalHits();
    }

    @Override
    public long syncLogCreationEntries(final String repoId, final String path, final Boolean recurs) {
        return syncLogCreationEntries(provider, repoId, path, recurs);
    }

    public SearchResponse search(SearchRequest request) {
        String[] indices = request.indices();
        if (indices == null || indices.length != 1) {
            throw new IllegalStateException("Search on audit must include index name: " + request);
        }
        if (!getESIndexName().equals(indices[0])) {
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
                if (val[0] instanceof Iterable<?>) {
                    List<String> l = new ArrayList<>();
                    Iterable<?> vals = (Iterable<?>) val[0];

                    for (Object v : vals) {
                        if (v != null) {
                            l.add(v.toString());
                        }
                    }
                    values = l.toArray(new String[l.size()]);
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

    @SuppressWarnings("deprecation")
    public String migrate(final int batchSize) {

        final String MIGRATION_WORK_ID = "AuditMigration";

        WorkManager wm = Framework.getService(WorkManager.class);
        State migrationState = wm.getWorkState(MIGRATION_WORK_ID);
        if (migrationState != null) {
            return "Migration already scheduled : " + migrationState.toString();
        }

        Work migrationWork = new ESAuditMigrationWork(MIGRATION_WORK_ID, batchSize);
        wm.schedule(migrationWork);
        return "Migration work started : " + MIGRATION_WORK_ID;
    }

    SearchResponse runRequest(SearchRequest request) {
        logSearchRequest(request);
        SearchResponse response = esClient.search(request);
        logSearchResponse(response);
        return response;
    }

    SearchResponse runNextScroll(String scrollId, TimeValue keepAlive) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Scroll request: -XGET 'localhost:9200/_search/scroll' -d '{\"scroll\": \"%s\", \"scroll_id\": \"%s\" }'",
                    keepAlive, scrollId));
        }
        SearchResponse response = esClient.searchScroll(new SearchScrollRequest(scrollId).scroll(keepAlive));
        logSearchResponse(response);
        return response;
    }

    protected void logSearchResponse(SearchResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Response: " + response.toString());
        }
    }

    protected void logSearchRequest(SearchRequest request) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Search query: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty' -d '%s'",
                    getESIndexName(), ElasticSearchConstants.ENTRY_TYPE, request.toString()));
        }
    }

    /**
     * Ensures the audit sequence returns an UID greater or equal than the maximum log entry id.
     */
    protected void ensureUIDSequencer(ESClient esClient) {
        boolean auditIndexExists = esClient.indexExists(getESIndexName());
        if (!auditIndexExists) {
            return;
        }

        // Get max log entry id
        SearchRequest request = createSearchRequest();
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery())
                                                .aggregation(AggregationBuilders.max("maxAgg").field("id")));
        SearchResponse searchResponse = esClient.search(request);
        Max agg = searchResponse.getAggregations().get("maxAgg");
        long maxLogEntryId = (long) agg.getValue();

        // Get next sequence id
        UIDGeneratorService uidGeneratorService = Framework.getService(UIDGeneratorService.class);
        UIDSequencer seq = uidGeneratorService.getSequencer();
        seq.init();
        long nextSequenceId = seq.getNextLong(SEQ_NAME);

        // Increment sequence to max log entry id if needed
        if (nextSequenceId < maxLogEntryId) {
            log.info(String.format("Next UID returned by %s sequence is %d, initializing sequence to %d", SEQ_NAME,
                    nextSequenceId, maxLogEntryId));
            seq.initSequence(SEQ_NAME, maxLogEntryId);
        }
    }

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return new ESExtendedInfo(value);
    }

    protected String getESIndexName() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        return esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE);
    }

    @Override
    public void append(List<String> jsonEntries) {
        BulkRequest bulkRequest = new BulkRequest();
        for (String json : jsonEntries) {
            try {
                String entryId = new JSONObject(json).getString(LOG_ID);
                if (StringUtils.isBlank(entryId)) {
                    throw new NuxeoException("A json entry has an empty id. entry=" + json);
                }
                IndexRequest request = new IndexRequest(getESIndexName(), ElasticSearchConstants.ENTRY_TYPE, entryId);
                request.source(json, XContentType.JSON);
                bulkRequest.add(request);
            } catch (JSONException e) {
                throw new NuxeoException("Unable to deserialize json entry=" + json, e);
            }
        }
        esClient.bulk(bulkRequest);
    }

    @Override
    public ScrollResult<String> scroll(org.nuxeo.ecm.core.query.sql.model.QueryBuilder builder, int batchSize,
            int keepAliveSeconds) {
        // prepare parameters
        Predicate predicate = builder.predicate();
        OrderByList orders = builder.orders();

        // create source
        SearchSourceBuilder source = createSearchRequestSource(predicate, orders);
        source.size(batchSize);
        // create request
        SearchRequest request = createSearchRequest();
        request.source(source).scroll(TimeValue.timeValueSeconds(keepAliveSeconds));
        SearchResponse response = runRequest(request);
        // register cursor
        String scrollId = cursorService.registerCursorResult(new ESCursorResult(response, batchSize, keepAliveSeconds));
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return cursorService.scroll(scrollId);
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
            SearchResponse response = ESAuditBackend.this.runNextScroll(scrollId,
                    TimeValue.timeValueSeconds(keepAliveSeconds));
            cursor = response.getHits().iterator();
            end = !cursor.hasNext();
        }

        @Override
        public void close() {
            ClearScrollRequest request = new ClearScrollRequest();
            request.addScrollId(scrollId);
            esClient.clearScroll(request);
            end = true;
            // Call super close to clear cursor
            super.close();
        }

    }

}
