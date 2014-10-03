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
 *     Tiry
 * 
 */
package org.nuxeo.elasticsearch.audit;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONReader;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter;
import org.nuxeo.elasticsearch.seqgen.SequenceGenerator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.star.uno.RuntimeException;

/**
 * Implementation of the {@link AuditBackend} interface using Elasticsearch
 * persistence
 * 
 * @author tiry
 * 
 */
public class ESAuditBackend extends AbstractAuditBackend implements
        AuditBackend {

    public static final String IDX_NAME = "audit";

    public static final String IDX_TYPE = "entry";

    public static final String SEQ_NAME = "audit";

    protected Client esClient = null;

    protected BaseLogEntryProvider provider = null;

    protected Client getClient() {
        if (esClient == null) {
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esClient = esa.getClient();
        }
        return esClient;
    }

    @Override
    public void deactivate() throws Exception {
        if (esClient != null) {
            esClient.close();
        }
    }

    @Override
    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {

        SearchRequestBuilder builder = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        if (filterMap == null || filterMap.size() == 0) {
            builder.setQuery(QueryBuilders.matchQuery("docUUID", uuid));
        } else {
            BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
            for (String key : filterMap.keySet()) {
                FilterMapEntry entry = filterMap.get(key);
                filterBuilder.must(FilterBuilders.termFilter(
                        entry.getColumnName(), entry.getObject()));
            }
            builder.setQuery(QueryBuilders.filteredQuery(
                    QueryBuilders.matchQuery("docUUID", uuid), filterBuilder));
        }

        SearchResponse searchResponse = builder.setFrom(0).setSize(60).execute().actionGet();

        List<LogEntry> entries = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    @Override
    public LogEntry getLogEntryByID(long id) {

        SearchResponse searchResponse = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.idsQuery(String.valueOf(id))).setFrom(0).setSize(
                10).execute().actionGet();

        SearchHits hits = searchResponse.getHits();
        if (hits.getTotalHits() > 1) {
            throw new RuntimeException(
                    "Found several match for the same ID : there is something wrong");
        }
        try {
            return AuditEntryJSONReader.read(hits.getAt(0).getSourceAsString());
        } catch (Exception e) {
            throw new RuntimeException("Unable to read Entry for id " + id, e);
        }
    }

    public SearchRequestBuilder buildQuery(String query, Map<String, Object> params) {
        if (params != null && params.size() > 0) {            
            query = expandQueryVariables(query, params);
        }

        SearchRequestBuilder builder = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        builder.setQuery(query);

        return builder;
    }
    
    public String expandQueryVariables(String query, Object[] params) {        
        Map<String, Object> qParams = new HashMap<String, Object>();
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
                if (val==null) {
                    continue; 
                } else if (val instanceof Calendar) {                         
                    tmpl.setVariable(key,ISODateTimeFormat.dateTime().print(
                            new DateTime((Calendar)val)));
                } else if (val instanceof Date) {                    
                    tmpl.setVariable(key,ISODateTimeFormat.dateTime().print(
                            new DateTime((Date)val)));
                } else {
                    tmpl.setVariable(key, val.toString());
                }
            }
            query = tmpl.process(query);
        }       
        return query;
    }
    
    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params,
            int pageNb, int pageSize) {

        SearchRequestBuilder builder = buildQuery(query, params);

        if (pageNb > 0) {
            builder.setFrom(pageNb * pageSize);
        }

        if (pageSize > 0) {
            builder.setSize(pageSize);
        }
        
        SearchResponse searchResponse = builder.execute().actionGet();
        List<LogEntry> entries = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String[] categories, String path, int pageNb, int pageSize) {

        SearchRequestBuilder builder = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
        int nbClauses = 0;
        int nbFilters = 0;

        if (eventIds != null && eventIds.length > 0) {
            if (eventIds.length == 1) {
                queryBuilder.must(QueryBuilders.matchQuery("eventId",
                        eventIds[0]));
                nbClauses++;
            } else {
                filterBuilder.must(FilterBuilders.termsFilter("eventId",
                        eventIds));
                nbFilters++;
            }
        }

        if (categories != null && categories.length > 0) {
            if (categories.length == 1) {
                queryBuilder.must(QueryBuilders.matchQuery("category",
                        categories[0]));
                nbClauses++;
            } else {
                filterBuilder.must(FilterBuilders.termsFilter("category",
                        categories));
                nbFilters++;
            }
        }

        if (path != null) {
            queryBuilder.must(QueryBuilders.matchQuery("docPath", path));
            nbClauses++;
        }

        if (limit != null) {
            queryBuilder.must(QueryBuilders.rangeQuery("eventDate").lt(limit));
            nbClauses++;
        }

        QueryBuilder targetBuilder = null;
        FilterBuilder targetFilter = null;

        if (nbClauses > 0) {
            targetBuilder = queryBuilder;
        } else {
            targetBuilder = QueryBuilders.matchAllQuery();
        }

        if (nbFilters > 0) {
            targetFilter = filterBuilder;
        } else {
            targetFilter = FilterBuilders.matchAllFilter();
        }

        builder.setQuery(QueryBuilders.filteredQuery(targetBuilder,
                targetFilter));

        if (pageNb > 0) {
            builder.setFrom(pageNb * pageSize);
        }

        if (pageSize > 0) {
            builder.setSize(pageSize);
        }

        SearchResponse searchResponse = builder.execute().actionGet();
        List<LogEntry> entries = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String[] categories, String path, int pageNb, int pageSize) {

        Date limit = null;
        if (dateRange != null) {
            try {
                limit = DateRangeParser.parseDateRangeQuery(new Date(),
                        dateRange);
            } catch (AuditQueryException aqe) {
                throw new AuditRuntimeException(
                        "Wrong date range query. Query was " + dateRange, aqe);
            }
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb,
                pageSize);
    }

    @Override
    public void addLogEntries(List<LogEntry> entries) {

        BulkRequestBuilder bulkRequest = getClient().prepareBulk();
        JsonFactory factory = new JsonFactory();

        SequenceGenerator sg = Framework.getService(SequenceGenerator.class);

        try {

            for (LogEntry entry : entries) {
                entry.setId(sg.getNextId(SEQ_NAME));
                XContentBuilder builder = jsonBuilder();
                JsonGenerator jsonGen = factory.createJsonGenerator(builder.stream());
                AuditEntryJSONWriter.asJSON(jsonGen, entry);
                bulkRequest.add(getClient().prepareIndex(IDX_NAME, IDX_TYPE,
                        String.valueOf(entry.getId())).setSource(builder));
            }

            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse response : bulkResponse.getItems()) {
                    if (response.isFailed()) {
                        log.error("Unable to index audit entry "
                                + response.getItemId() + " :"
                                + response.getFailureMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new ClientException("Error while indexing Audit entries", e);
        }

    }

    @Override
    public Long getEventsCount(String eventId) {
        CountResponse res = getClient().prepareCount(IDX_NAME).setTypes(
                IDX_TYPE).setQuery(QueryBuilders.matchQuery("eventId", eventId)).execute().actionGet();
        return res.getCount();
    }

    protected BaseLogEntryProvider getProvider() {

        if (provider == null) {
            provider = new BaseLogEntryProvider() {

                @Override
                public int removeEntries(String eventId, String pathPattern) {
                    throw new UnsupportedOperationException(
                            "Not implemented yet!");
                }

                @Override
                public void addLogEntry(LogEntry logEntry) {
                    List<LogEntry> entries = new ArrayList<>();
                    entries.add(logEntry);
                    addLogEntries(entries);
                }
            };
        }
        return provider;
    }

    @Override
    public long syncLogCreationEntries(final String repoId, final String path,
            final Boolean recurs) {
        return syncLogCreationEntries(getProvider(), repoId, path, recurs);
    }

    protected FilterBuilder buildFilter(PredicateDefinition[] predicates,
            DocumentModel searchDocumentModel) {        
        
        BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();

        int nbFilters = 0;
        
        for (PredicateDefinition predicate : predicates) {

            // extract data from DocumentModel
            Object[] val;
            try {
                PredicateFieldDefinition[] fieldDef = predicate.getValues();
                val = new Object[fieldDef.length];

                for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                    if (fieldDef[fidx].getXpath() != null) {
                        val[fidx] = searchDocumentModel.getPropertyValue(fieldDef[fidx].getXpath());
                    } else {
                        val[fidx] = searchDocumentModel.getProperty(
                                fieldDef[fidx].getSchema(),
                                fieldDef[fidx].getName());
                    }
                }
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
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
                    Iterator<?> valueIterator = vals.iterator();

                    while (valueIterator.hasNext()) {

                        Object v = valueIterator.next();
                        if (v != null) {
                            l.add(v.toString());
                        }
                    }
                    values = l.toArray(new String[l.size()]);
                } else if (val[0] instanceof Object[]) {
                    values = (String[]) val[0];
                }
                filterBuilder.must(FilterBuilders.termsFilter(
                        predicate.getParameter(), values));
            } else if (op.equalsIgnoreCase("BETWEEN")) {
                filterBuilder.must(FilterBuilders.rangeFilter(
                        predicate.getParameter()).gt(val[0]));
                if (val.length > 1) {
                    filterBuilder.must(FilterBuilders.rangeFilter(
                            predicate.getParameter()).lt(val[1]));
                }
            } else if (">".equals(op)) {
                filterBuilder.must(FilterBuilders.rangeFilter(
                        predicate.getParameter()).gt(val[0]));                
            } else if (">=".equals(op)) {
                filterBuilder.must(FilterBuilders.rangeFilter(
                        predicate.getParameter()).gte(val[0]));                
            } else if ("<".equals(op)) {
                filterBuilder.must(FilterBuilders.rangeFilter(
                        predicate.getParameter()).lt(val[0]));                
            } else if ("<=".equals(op)) {
                filterBuilder.must(FilterBuilders.rangeFilter(
                        predicate.getParameter()).lte(val[0]));                
            } else {
                filterBuilder.must(FilterBuilders.termFilter(
                        predicate.getParameter(), val[0]));
            }
        }

        if (nbFilters==0) {
            return FilterBuilders.matchAllFilter();
        }
        return filterBuilder;
    }

    public SearchRequestBuilder buildSearchQuery(String fixedPart,
            PredicateDefinition[] predicates,
            DocumentModel searchDocumentModel) {

        SearchRequestBuilder builder = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        QueryBuilder queryBuilder = QueryBuilders.wrapperQuery(fixedPart);
        FilterBuilder filterBuilder = buildFilter(predicates,
                searchDocumentModel);
        builder.setQuery(QueryBuilders.filteredQuery(queryBuilder,
                filterBuilder));
        return builder;
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
            
    public void migrate(final int batchSize) throws Exception {
        
        final AuditBackend sourceBackend = new DefaultAuditBackend();
        sourceBackend.activate(component);
                        
        List<Long> res = (List<Long>) sourceBackend.nativeQuery("select count(*) from LogEntry", 1,20);
        
        final long nbEntriesToMigrate = res.get(0).longValue();
        
        Work migrationWork = new AbstractWork("AuditMigration") {
            
            @Override
            public String getTitle() {
                return "Audit migration worker";
            }
            
            @Override
            public void work() throws Exception {
                TransactionHelper.commitOrRollbackTransaction();
                try {
        
                    long nbEntriesMigrated=0;
                    int pageIdx =  0;
                    
                    while (nbEntriesMigrated < nbEntriesToMigrate) {
                        List<LogEntry> entries = (List<LogEntry>)sourceBackend.nativeQuery("from LogEntry log order by log.id asc", pageIdx, batchSize);
                        
                        if (entries.size()==0) {
                            log.warn("Migration ending after " + nbEntriesMigrated + " entries");
                            break;
                        }                        
                        setProgress(new Progress(nbEntriesMigrated, nbEntriesToMigrate));
                        addLogEntries(entries);
                        pageIdx++;
                        nbEntriesMigrated+=entries.size();
                        log.debug("migrated " + nbEntriesMigrated + " log entries on " + nbEntriesToMigrate);
                    }
                } finally {
                    TransactionHelper.startTransaction();
                }                
            }
        };
                
        Framework.getService(WorkManager.class).schedule (migrationWork);        
                
    }
}
