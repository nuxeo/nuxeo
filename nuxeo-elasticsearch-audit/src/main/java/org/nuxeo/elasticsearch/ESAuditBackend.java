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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.io.AuditEntryJSONReader;
import org.nuxeo.elasticsearch.io.AuditEntryJSONWriter;
import org.nuxeo.elasticsearch.seqgen.SequenceGenerator;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link AuditBackend} interface using Elasticsearch persistence
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
                
        SearchResponse searchResponse = getClient().prepareSearch(IDX_NAME).setTypes(IDX_TYPE)        
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("docUUID", uuid)) // Query
        .setFrom(0).setSize(60).execute().actionGet();
        
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
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params,
            int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String[] categories, String path, int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String[] categories, String path, int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
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
                bulkRequest.add(getClient().prepareIndex(IDX_NAME, IDX_TYPE, String.valueOf(entry.getId())).setSource(builder));                                                                
            }                
            
            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse response : bulkResponse.getItems()) {
                    if (response.isFailed()) {
                        log.error("Unable to index audit entry " + response.getItemId() + " :" + response.getFailureMessage());
                    }
                }
            }       
        } catch (Exception e) {
            throw new ClientException(
                    "Error while indexing Audit entries", e);
        }

    }

    @Override
    public Long getEventsCount(String eventId) {
        throw new UnsupportedOperationException("Not implemented yet!");
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
