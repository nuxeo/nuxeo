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
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.core;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.CHILDREN_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEX_BULK_MAX_SIZE_PROPERTY;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.PATH_FIELD;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.BlobNotFoundException;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 6.0
 */
public class ElasticSearchIndexingImpl implements ElasticSearchIndexing {
    private static final Log log = LogFactory.getLog(ElasticSearchIndexingImpl.class);

    // debug curl line max size
    private static final int MAX_CURL_LINE = 8 * 1024;

    // send the bulk indexing command when this size is reached, optimal is 5-10m
    private static final int DEFAULT_MAX_BULK_SIZE = 5 * 1024 * 1024;

    private final ElasticSearchAdminImpl esa;

    private final Timer deleteTimer;

    private final Timer indexTimer;

    private final Timer bulkIndexTimer;

    private final boolean useExternalVersion;

    private JsonESDocumentWriter jsonESDocumentWriter;

    public ElasticSearchIndexingImpl(ElasticSearchAdminImpl esa) {
        this.esa = esa;
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        indexTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "index"));
        deleteTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "delete"));
        bulkIndexTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "bulkIndex"));
        this.jsonESDocumentWriter = new JsonESDocumentWriter();// default writer
        this.useExternalVersion = esa.useExternalVersion();
    }

    /**
     * @since 7.2
     */
    public ElasticSearchIndexingImpl(ElasticSearchAdminImpl esa, JsonESDocumentWriter jsonESDocumentWriter) {
        this(esa);
        this.jsonESDocumentWriter = jsonESDocumentWriter;
    }

    @Override
    public void runIndexingWorker(List<IndexingCommand> cmds) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void runReindexingWorker(String repositoryName, String nxql, boolean syncAlias) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void reindexRepository(String repositoryName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void indexNonRecursive(List<IndexingCommand> cmds) {
        int nbCommands = cmds.size();
        if (nbCommands == 1) {
            indexNonRecursive(cmds.get(0));
            return;
        }
        // simulate long indexing
        // try {Thread.sleep(1000);} catch (InterruptedException e) { }

        processBulkDeleteCommands(cmds);
        try (Context ignored = bulkIndexTimer.time()) {
            processBulkIndexCommands(cmds);
        }
        esa.totalCommandProcessed.addAndGet(nbCommands);
        refreshIfNeeded(cmds);
    }

    void processBulkDeleteCommands(List<IndexingCommand> cmds) {
        // Can be optimized with a single delete by query
        for (IndexingCommand cmd : cmds) {
            if (cmd.getType() == Type.DELETE) {
                try (Context ignored = deleteTimer.time()) {
                    processDeleteCommand(cmd);
                }
            }
        }
    }

    void processBulkIndexCommands(List<IndexingCommand> cmds) {
        BulkRequest bulkRequest = new BulkRequest();
        Set<String> docIds = new HashSet<>(cmds.size());
        int bulkSize = 0;
        final int maxBulkSize = getMaxBulkSize();
        for (IndexingCommand cmd : cmds) {
            if (cmd.getType() == Type.DELETE || cmd.getType() == Type.UPDATE_DIRECT_CHILDREN) {
                continue;
            }
            if (!docIds.add(cmd.getTargetDocumentId())) {
                // do not submit the same doc 2 times
                continue;
            }
            try {
                IndexRequest idxRequest = buildEsIndexingRequest(cmd);
                if (idxRequest != null) {
                    bulkSize += idxRequest.source().length();
                    bulkRequest.add(idxRequest);
                }
            } catch (BlobNotFoundException be) {
                log.info("Ignore indexing command in bulk, blob does not exists anymore: " + cmd);
            } catch (ConcurrentUpdateException e) {
                throw e; // bubble up, usually until AbstractWork catches it and maybe retries
            } catch (DocumentNotFoundException e) {
                log.info("Ignore indexing command in bulk, doc does not exists anymore: " + cmd);
            } catch (IllegalArgumentException e) {
                log.error("Ignore indexing command in bulk, fail to create request: " + cmd, e);
            }
            if (bulkSize > maxBulkSize) {
                log.warn("Max bulk size reached " + bulkSize + ", sending bulk command");
                sendBulkCommand(bulkRequest, bulkSize);
                bulkRequest = new BulkRequest();
                bulkSize = 0;
            }
        }
        sendBulkCommand(bulkRequest, bulkSize);
    }

    int getMaxBulkSize() {
        String value = Framework.getProperty(INDEX_BULK_MAX_SIZE_PROPERTY, String.valueOf(DEFAULT_MAX_BULK_SIZE));
        return Integer.parseInt(value);
    }

    void sendBulkCommand(BulkRequest bulkRequest, int bulkSize) {
        if (bulkRequest.numberOfActions() > 0) {
            if (log.isDebugEnabled()) {
                logDebugMessageTruncated(String.format(
                        "Index %d docs (%d bytes) in bulk request: curl -XPOST 'http://localhost:9200/_bulk' -d '%s'",
                        bulkRequest.numberOfActions(), bulkSize, bulkRequest.requests().toString()), MAX_CURL_LINE);
            }
            BulkResponse response = esa.getClient().bulk(bulkRequest);
            if (response.hasFailures()) {
                logBulkFailure(response);
            }
        }
    }

    void logBulkFailure(BulkResponse response) {
        boolean isError = false;
        StringBuilder sb = new StringBuilder();
        sb.append("Ignore indexing of some docs more recent versions has already been indexed");
        for (BulkItemResponse item : response.getItems()) {
            if (item.isFailed()) {
                if (item.getFailure().getStatus() == RestStatus.CONFLICT) {
                    sb.append("\n  ").append(item.getFailureMessage());
                } else {
                    isError = true;
                }
            }
        }
        if (isError) {
            log.error(response.buildFailureMessage());
        } else {
            log.debug(sb);
        }
    }

    void refreshIfNeeded(List<IndexingCommand> cmds) {
        for (IndexingCommand cmd : cmds) {
            if (refreshIfNeeded(cmd))
                return;
        }
    }

    boolean refreshIfNeeded(IndexingCommand cmd) {
        if (cmd.isSync()) {
            esa.refresh();
            return true;
        }
        return false;
    }

    @Override
    public void indexNonRecursive(IndexingCommand cmd) {
        Type type = cmd.getType();
        if (type == Type.UPDATE_DIRECT_CHILDREN) {
            // the parent don't need to be indexed
            return;
        }
        if (type == Type.DELETE) {
            try (Context ignored = deleteTimer.time()) {
                processDeleteCommand(cmd);
            }
        } else {
            try (Context ignored = indexTimer.time()) {
                processIndexCommand(cmd);
            }
        }
        refreshIfNeeded(cmd);
        esa.totalCommandProcessed.incrementAndGet();
    }

    void processIndexCommand(IndexingCommand cmd) {
        IndexRequest request;
        try {
            request = buildEsIndexingRequest(cmd);
        } catch (BlobNotFoundException pe) {
            request = null;
        } catch (DocumentNotFoundException e) {
            request = null;
        } catch (IllegalStateException e) {
            log.error("Fail to create request for indexing command: " + cmd, e);
            return;
        }
        if (request == null) {
            log.info("Cancel indexing command because target document does not exists anymore: " + cmd);
            return;
        }
        if (log.isDebugEnabled()) {
            logDebugMessageTruncated(String.format("Index request: curl -XPUT 'http://localhost:9200/%s/%s/%s' -d '%s'",
                    getWriteIndexForRepository(cmd.getRepositoryName()), DOC_TYPE, cmd.getTargetDocumentId(),
                    request.toString()), MAX_CURL_LINE);
        }
        try {
            esa.getClient().index(request);
        } catch (VersionConflictEngineException e) {
            SequenceTracer.addNote("Ignore indexing of doc " + cmd.getTargetDocumentId());
            log.info("Ignore indexing of doc " + cmd.getTargetDocumentId()
                    + " a more recent version has already been indexed: " + e.getMessage());
        }
    }

    void logDebugMessageTruncated(String msg, int maxSize) {
        if (log.isTraceEnabled() || msg.length() < maxSize) {
            // in trace mode we output the full message
            log.debug(msg);
        } else {
            log.debug(msg.substring(0, maxSize) + "...");
        }
    }

    void processDeleteCommand(IndexingCommand cmd) {
        if (cmd.isRecurse()) {
            processDeleteCommandRecursive(cmd);
        } else {
            processDeleteCommandNonRecursive(cmd);
        }
    }

    void processDeleteCommandNonRecursive(IndexingCommand cmd) {
        String indexName = getWriteIndexForRepository(cmd.getRepositoryName());
        DeleteRequest request = new DeleteRequest(indexName, DOC_TYPE, cmd.getTargetDocumentId());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Delete request: curl -XDELETE 'http://localhost:9200/%s/%s/%s'", indexName,
                    DOC_TYPE, cmd.getTargetDocumentId()));
        }
        esa.getClient().delete(request);
    }

    void processDeleteCommandRecursive(IndexingCommand cmd) {
        String indexName = getWriteIndexForRepository(cmd.getRepositoryName());
        // we don't want to rely on target document because the document can be
        // already removed
        String docPath = getPathOfDocFromEs(cmd.getRepositoryName(), cmd.getTargetDocumentId());
        if (docPath == null) {
            if (!Framework.isTestModeSet()) {
                log.warn("Trying to delete a non existing doc: " + cmd.toString());
            }
            return;
        }
        // Refresh index before bulk delete
        esa.getClient().refresh(indexName);

        // Run the scroll query
        QueryBuilder query = QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(CHILDREN_FIELD, docPath));
        TimeValue keepAlive = TimeValue.timeValueMinutes(1);
        SearchSourceBuilder search = new SearchSourceBuilder().size(100).query(query).fetchSource(false);
        SearchRequest request = new SearchRequest(indexName).scroll(keepAlive).source(search);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Search with scroll request: curl -XGET 'http://localhost:9200/%s/%s/_search?scroll=%s' -d '%s'",
                    indexName, DOC_TYPE, keepAlive, query.toString()));
        }
        for (SearchResponse response = esa.getClient().search(request); //
                response.getHits().getHits().length > 0; //
                response = runNextScroll(response, keepAlive)) {

            // Build bulk delete request
            BulkRequest bulkRequest = new BulkRequest();
            for (SearchHit hit : response.getHits().getHits()) {
                bulkRequest.add(new DeleteRequest(hit.getIndex(), hit.getType(), hit.getId()));
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Bulk delete request on %s elements", bulkRequest.numberOfActions()));
            }
            // Run bulk delete request
            esa.getClient().bulk(bulkRequest);
        }
    }

    SearchResponse runNextScroll(SearchResponse response, TimeValue keepAlive) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Scroll request: -XGET 'localhost:9200/_search/scroll' -d '{\"scroll\": \"%s\", \"scroll_id\": \"%s\" }'",
                    keepAlive, response.getScrollId()));
        }
        SearchScrollRequest request = new SearchScrollRequest(response.getScrollId()).scroll(keepAlive);
        return esa.getClient().searchScroll(request);
    }

    /**
     * Return the ecm:path of an ES document or null if not found.
     */
    String getPathOfDocFromEs(String repository, String docId) {
        String indexName = getWriteIndexForRepository(repository);
        GetRequest request = new GetRequest(indexName, DOC_TYPE, docId).fetchSourceContext(
                new FetchSourceContext(true, new String[] { PATH_FIELD }, null));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Get path of doc: curl -XGET 'http://localhost:9200/%s/%s/%s?fields=%s'", indexName,
                    DOC_TYPE, docId, PATH_FIELD));
        }
        GetResponse ret = esa.getClient().get(request);
        if (!ret.isExists() || ret.getSource() == null || ret.getSource().get(PATH_FIELD) == null) {
            // doc not found
            return null;
        }
        return ret.getSource().get(PATH_FIELD).toString();
    }

    /**
     * Return indexing request or null if the doc does not exists anymore.
     *
     * @throws java.lang.IllegalStateException if the command is not attached to a session
     */
    IndexRequest buildEsIndexingRequest(IndexingCommand cmd) {
        DocumentModel doc = cmd.getTargetDocument();
        if (doc == null) {
            return null;
        }
        try {
            JsonFactory factory = new JsonFactory();
            OutputStream out = new BytesStreamOutput();
            JsonGenerator jsonGen = factory.createGenerator(out);
            jsonESDocumentWriter.writeESDocument(jsonGen, doc, cmd.getSchemas(), null);
            IndexRequest request = new IndexRequest(getWriteIndexForRepository(cmd.getRepositoryName()), DOC_TYPE,
                    cmd.getTargetDocumentId()).source(jsonBuilder(out));
            if (useExternalVersion && cmd.getOrder() > 0) {
                request.versionType(VersionType.EXTERNAL).version(cmd.getOrder());
            }
            return request;
        } catch (IOException e) {
            throw new NuxeoException("Unable to create index request for Document " + cmd.getTargetDocumentId(), e);
        }
    }

    protected String getWriteIndexForRepository(String repository) {
        return esa.getWriteIndexName(esa.getIndexNameForRepository(repository));
    }

}
