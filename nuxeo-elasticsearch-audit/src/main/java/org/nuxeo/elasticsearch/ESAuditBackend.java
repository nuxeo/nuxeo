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
package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.ArrayList;
import java.util.Date;
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.io.AuditEntryJSONReader;
import org.nuxeo.elasticsearch.io.AuditEntryJSONWriter;
import org.nuxeo.elasticsearch.seqgen.SequenceGenerator;
import org.nuxeo.runtime.api.Framework;

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

        SearchResponse searchResponse = getClient().prepareSearch(IDX_NAME).setTypes(
                IDX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("docUUID", uuid)) // Query
        .setFrom(0).setSize(60).execute().actionGet();

        // filterMap
        
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

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params,
            int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
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
                
        if (eventIds !=null && eventIds.length>0) {
            if (eventIds.length==1) {
                queryBuilder.must(QueryBuilders.matchQuery("eventId", eventIds[0]));
                nbClauses++;
            } else {
                filterBuilder.must(FilterBuilders.termsFilter("eventId", eventIds));
                nbFilters++;
            }            
        }                
        
        if (categories !=null && categories.length>0) {
            if (categories.length==1) {
                queryBuilder.must(QueryBuilders.matchQuery("category", categories[0]));
                nbClauses++;
            } else {
                filterBuilder.must(FilterBuilders.termsFilter("category", categories));
                nbFilters++;
            }            
        }

        if (path !=null ) {            
           queryBuilder.must(QueryBuilders.matchQuery("docPath", path));
           nbClauses++;
        }

        if (limit !=null ) {                        
            queryBuilder.must(QueryBuilders.rangeQuery("eventDate").lt(limit));
            nbClauses++;
         }
        
        QueryBuilder targetBuilder = null;
        FilterBuilder targetFilter = null;
        
        
        if (nbClauses>0) {        
            targetBuilder = queryBuilder;            
        } else {
            targetBuilder = QueryBuilders.matchAllQuery();            
        }

        if (nbFilters>0) {
            targetFilter = filterBuilder;
        } else {
            targetFilter = FilterBuilders.matchAllFilter();
        }
        
        builder.setQuery(QueryBuilders.filteredQuery(targetBuilder, targetFilter));
        
        if (pageNb>0) {
            builder.setFrom(pageNb*pageSize);
        }
        
        if (pageSize>0) {
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
        if  (dateRange!=null) {
            try {
                limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
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

}
