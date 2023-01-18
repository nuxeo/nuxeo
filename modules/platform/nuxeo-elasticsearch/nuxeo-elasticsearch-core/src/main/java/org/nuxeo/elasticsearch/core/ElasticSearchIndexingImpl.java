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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.CHILDREN_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEX_BULK_MAX_SIZE_PROPERTY;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.PATH_FIELD;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.BlobNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.io.JsonESDocumentWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.VersionType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;
import io.dropwizard.metrics5.Timer.Context;

/**
 * @since 6.0
 */
public class ElasticSearchIndexingImpl implements ElasticSearchIndexing {

    private static final Logger log = LogManager.getLogger(ElasticSearchIndexingImpl.class);

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

    protected static final JsonFactory JSON_FACTORY = new JsonFactory();

    public ElasticSearchIndexingImpl(ElasticSearchAdminImpl esa) {
        this.esa = esa;
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        indexTimer = registry.timer(MetricName.build("nuxeo.elasticsearch.service.timer").tagged("service", "index"));
        deleteTimer = registry.timer(MetricName.build("nuxeo.elasticsearch.service.timer").tagged("service", "delete"));
        bulkIndexTimer = registry.timer(
                MetricName.build("nuxeo.elasticsearch.service.timer").tagged("service", "bulkIndex"));
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
            String secondaryIndex = getSecondaryWriteIndexForRepository(cmd.getRepositoryName());
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
                    if (secondaryIndex != null) {

                        IndexRequest idxRequestBis = new IndexRequest(secondaryIndex).id(cmd.getTargetDocumentId())
                                                                                     .source(idxRequest.source(),
                                                                                             XContentType.JSON);
                        if (useExternalVersion && cmd.getOrder() > 0) {
                            idxRequestBis.versionType(VersionType.EXTERNAL).version(cmd.getOrder());
                        }
                        bulkSize += idxRequestBis.source().length();
                        bulkRequest.add(idxRequestBis);
                    }
                }
            } catch (BlobNotFoundException be) {
                log.info("Ignore indexing command in bulk, blob does not exists anymore: {}", cmd);
            } catch (ConcurrentUpdateException e) {
                throw e; // bubble up, usually until AbstractWork catches it and maybe retries
            } catch (DocumentNotFoundException e) {
                log.info("Ignore indexing command in bulk, doc does not exists anymore: {}", cmd);
            } catch (IllegalArgumentException e) {
                log.error("Ignore indexing command in bulk, fail to create request: {}", cmd, e);
            }
            if (bulkSize > maxBulkSize) {
                log.warn("Max bulk size reached: {}, sending bulk command", bulkSize);
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
        } catch (BlobNotFoundException | DocumentNotFoundException pe) {
            request = null;
        } catch (IllegalStateException e) {
            log.error("Fail to create request for indexing command: {}", cmd, e);
            return;
        }
        if (request == null) {
            log.info("Cancel indexing command because target document does not exists anymore: {}", cmd);
            return;
        }
        String repository = cmd.getRepositoryName();
        processIndexRequest(cmd.getTargetDocumentId(), request);
        String secondaryIndex = getSecondaryWriteIndexForRepository(repository);
        if (secondaryIndex != null) {
            request.index(secondaryIndex);
            processIndexRequest(cmd.getTargetDocumentId(), request);
        }
    }

    protected void processIndexRequest(String documentId, IndexRequest request) {
        if (log.isDebugEnabled()) {
            logDebugMessageTruncated(String.format("Index request: curl -XPUT 'http://localhost:9200/%s/%s' -d '%s'",
                    request.index(), documentId, request), MAX_CURL_LINE);
        }
        try {
            esa.getClient().index(request);
        } catch (ConcurrentUpdateException e) {
            log.info("Ignore indexing of doc: {} a more recent version has already been indexed: {}", documentId,
                    e.getMessage());
        }
    }

    void logDebugMessageTruncated(String msg, int maxSize) {
        if (log.isTraceEnabled() || msg.length() < maxSize) {
            // in trace mode we output the full message
            log.debug(msg);
        } else {
            log.debug(() -> msg.substring(0, maxSize) + "...");
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
        String repository = cmd.getRepositoryName();
        processDeleteCommandNonRecursive(cmd, getWriteIndexForRepository(repository));
        String secondaryIndex = getSecondaryWriteIndexForRepository(repository);
        if (secondaryIndex != null) {
            processDeleteCommandNonRecursive(cmd, secondaryIndex);
        }
    }

    void processDeleteCommandNonRecursive(IndexingCommand cmd, String indexName) {
        DeleteRequest request = new DeleteRequest(indexName, cmd.getTargetDocumentId());
        log.debug("Delete request: curl -XDELETE 'http://localhost:9200/{}/{}'", indexName, cmd.getTargetDocumentId());
        esa.getClient().delete(request);
    }

    void processDeleteCommandRecursive(IndexingCommand cmd) {
        String repository = cmd.getRepositoryName();
        String indexName = getWriteIndexForRepository(repository);
        processDeleteCommandRecursive(cmd, indexName);
        String secondaryIndex = getSecondaryWriteIndexForRepository(repository);
        if (secondaryIndex != null) {
            processDeleteCommandRecursive(cmd, secondaryIndex);
        }
    }

    void processDeleteCommandRecursive(IndexingCommand cmd, String indexName) {
        // we don't want to rely on target document because the document can be
        // already removed
        String docPath = getPathOfDocFromEs(cmd.getRepositoryName(), indexName, cmd.getTargetDocumentId());
        if (docPath == null) {
            if (!Framework.isTestModeSet()) {
                log.warn("Trying to delete a non existing doc: {}", cmd);
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
        log.debug("Search with scroll request: curl -XGET 'http://localhost:9200/{}/_search?scroll={}' -d '{}'",
                indexName, keepAlive, query);
        SearchResponse response;
        for (response = esa.getClient().search(request); //
                response.getHits().getHits().length > 0; //
                response = runNextScroll(response, keepAlive)) {

            // Build bulk delete request
            BulkRequest bulkRequest = new BulkRequest();
            for (SearchHit hit : response.getHits().getHits()) {
                bulkRequest.add(new DeleteRequest(hit.getIndex(), hit.getId()));
            }
            log.debug("Bulk delete request on {} elements", bulkRequest.numberOfActions());
            // Run bulk delete request
            esa.getClient().bulk(bulkRequest);
        }
        // Close the scroll
        ClearScrollRequest closeScrollRequest = new ClearScrollRequest();
        closeScrollRequest.addScrollId(response.getScrollId());
        esa.getClient().clearScroll(closeScrollRequest);
    }

    SearchResponse runNextScroll(SearchResponse response, TimeValue keepAlive) {
        log.debug(
                "Scroll request: -XGET 'localhost:9200/_search/scroll' -d '{\"scroll\": \"{}\", \"scroll_id\": \"{}\" }'",
                keepAlive, response.getScrollId());
        SearchScrollRequest request = new SearchScrollRequest(response.getScrollId()).scroll(keepAlive);
        return esa.getClient().searchScroll(request);
    }

    /**
     * Returns the ecm:path of an ES document or null if not found.
     */
    String getPathOfDocFromEs(String repository, String docId) {
        return getPathOfDocFromEs(repository, null, docId);
    }

    /**
     * Returns the ecm:path of an ES document or null if not found.
     *
     * @since 2021.12
     */
    protected String getPathOfDocFromEs(String repository, String indexName, String docId) {
        if (indexName == null) {
            indexName = getWriteIndexForRepository(repository);
        }
        GetRequest request = new GetRequest(indexName, docId).fetchSourceContext(
                new FetchSourceContext(true, new String[] { PATH_FIELD }, null));
        log.debug("Get path of doc: curl -XGET 'http://localhost:9200/{}/{}?fields={}'", indexName, docId, PATH_FIELD);
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
            IndexRequest request = new IndexRequest(getWriteIndexForRepository(cmd.getRepositoryName())).id(
                    cmd.getTargetDocumentId()).source(source(doc), XContentType.JSON);
            if (useExternalVersion && cmd.getOrder() > 0) {
                request.versionType(VersionType.EXTERNAL).version(cmd.getOrder());
            }
            return request;
        } catch (IOException e) {
            throw new NuxeoException("Unable to create index request for Document " + cmd.getTargetDocumentId(), e);
        } catch (PropertyConversionException e) {
            log.error("Skipping indexing of a corrupted doc: {}", cmd.getTargetDocumentId(), e);
            return null;
        }
    }

    protected String getWriteIndexForRepository(String repository) {
        return esa.getWriteIndexName(esa.getIndexNameForRepository(repository));
    }

    protected String getSecondaryWriteIndexForRepository(String repository) {
        return esa.getSecondaryWriteIndexName(esa.getIndexNameForRepository(repository));
    }

    @Override
    public BytesReference source(DocumentModel doc) throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        try (JsonGenerator jsonGen = JSON_FACTORY.createGenerator(out)) {
            jsonESDocumentWriter.writeESDocument(jsonGen, doc, null, null);
            return out.bytes();
        }
    }
}
