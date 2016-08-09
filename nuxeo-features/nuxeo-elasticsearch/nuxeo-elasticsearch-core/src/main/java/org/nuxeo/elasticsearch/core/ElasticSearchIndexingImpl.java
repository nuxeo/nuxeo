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
import static org.nuxeo.elasticsearch.ElasticSearchConstants.PATH_FIELD;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * @since 6.0
 */
public class ElasticSearchIndexingImpl implements ElasticSearchIndexing {
    private static final Log log = LogFactory.getLog(ElasticSearchIndexingImpl.class);

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
    public void runReindexingWorker(String repositoryName, String nxql) {
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
        Context stopWatch = bulkIndexTimer.time();
        try {
            processBulkIndexCommands(cmds);
        } finally {
            stopWatch.stop();
        }
        esa.totalCommandProcessed.addAndGet(nbCommands);
        refreshIfNeeded(cmds);
    }

    void processBulkDeleteCommands(List<IndexingCommand> cmds) {
        // Can be optimized with a single delete by query
        for (IndexingCommand cmd : cmds) {
            if (cmd.getType() == Type.DELETE) {
                Context stopWatch = deleteTimer.time();
                try {
                    processDeleteCommand(cmd);
                } finally {
                    stopWatch.stop();
                }
            }
        }
    }

    void processBulkIndexCommands(List<IndexingCommand> cmds) {
        BulkRequestBuilder bulkRequest = esa.getClient().prepareBulk();
        Set<String> docIds = new HashSet<>(cmds.size());
        for (IndexingCommand cmd : cmds) {
            if (cmd.getType() == Type.DELETE || cmd.getType() == Type.UPDATE_DIRECT_CHILDREN) {
                continue;
            }
            if (!docIds.add(cmd.getTargetDocumentId())) {
                // do not submit the same doc 2 times
                continue;
            }
            try {
                IndexRequestBuilder idxRequest = buildEsIndexingRequest(cmd);
                if (idxRequest != null) {
                    bulkRequest.add(idxRequest);
                }
            } catch (ConcurrentUpdateException e) {
                throw e; // bubble up, usually until AbstractWork catches it and maybe retries
            } catch (DocumentNotFoundException e) {
                log.info("Ignore indexing command in bulk, doc does not exists anymore: " + cmd);
            } catch (IllegalArgumentException e) {
                log.error("Ignore indexing command in bulk, fail to create request: " + cmd, e);
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Index %d docs in bulk request: curl -XPOST 'http://localhost:9200/_bulk' -d '%s'",
                        bulkRequest.numberOfActions(), bulkRequest.request().requests().toString()));
            }
            BulkResponse response = bulkRequest.execute().actionGet();
            if (response.hasFailures()) {
                logBulkFailure(response);
            }
        }
    }

    protected void logBulkFailure(BulkResponse response) {
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
            log.info(sb);
        }
    }

    protected void refreshIfNeeded(List<IndexingCommand> cmds) {
        for (IndexingCommand cmd : cmds) {
            if (refreshIfNeeded(cmd))
                return;
        }
    }

    private boolean refreshIfNeeded(IndexingCommand cmd) {
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
        Context stopWatch = null;
        try {
            if (type == Type.DELETE) {
                stopWatch = deleteTimer.time();
                processDeleteCommand(cmd);
            } else {
                stopWatch = indexTimer.time();
                processIndexCommand(cmd);
            }
            refreshIfNeeded(cmd);
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
            }
            esa.totalCommandProcessed.incrementAndGet();
        }
    }

    void processIndexCommand(IndexingCommand cmd) {
        IndexRequestBuilder request;
        try {
            request = buildEsIndexingRequest(cmd);
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
            log.debug(String.format("Index request: curl -XPUT 'http://localhost:9200/%s/%s/%s' -d '%s'",
                    esa.getIndexNameForRepository(cmd.getRepositoryName()), DOC_TYPE, cmd.getTargetDocumentId(),
                    request.request().toString()));
        }
        try {
            request.execute().actionGet();
        } catch (VersionConflictEngineException e) {
            log.info("Ignore indexing of doc " + cmd.getTargetDocumentId()
                    + " a more recent version has already been indexed: " + e.getMessage());
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
        String indexName = esa.getIndexNameForRepository(cmd.getRepositoryName());
        DeleteRequestBuilder request = esa.getClient().prepareDelete(indexName, DOC_TYPE, cmd.getTargetDocumentId());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Delete request: curl -XDELETE 'http://localhost:9200/%s/%s/%s'", indexName,
                    DOC_TYPE, cmd.getTargetDocumentId()));
        }
        request.execute().actionGet();
    }

    void processDeleteCommandRecursive(IndexingCommand cmd) {
        String indexName = esa.getIndexNameForRepository(cmd.getRepositoryName());
        // we don't want to rely on target document because the document can be
        // already removed
        String docPath = getPathOfDocFromEs(cmd.getRepositoryName(), cmd.getTargetDocumentId());
        if (docPath == null) {
            if (!Framework.isTestModeSet()) {
                log.warn("Trying to delete a non existing doc: " + cmd.toString());
            }
            return;
        }
        QueryBuilder query = QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(CHILDREN_FIELD, docPath));
        TimeValue keepAlive = TimeValue.timeValueMinutes(1);
        SearchRequestBuilder request = esa.getClient()
                                          .prepareSearch(indexName)
                                          .setTypes(DOC_TYPE)
                                          .setScroll(keepAlive)
                                          .setSize(100)
                                          .setFetchSource(false)
                                          .setQuery(query);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Search with scroll request: curl -XGET 'http://localhost:9200/%s/%s/_search?scroll=%s' -d '%s'",
                    indexName, DOC_TYPE, keepAlive, query.toString()));
        }
        for (SearchResponse response = request.execute().actionGet(); //
        response.getHits().getHits().length > 0; //
        response = runNextScroll(response, keepAlive)) {

            // Build bulk delete request
            BulkRequestBuilder bulkBuilder = esa.getClient().prepareBulk();
            for (SearchHit hit : response.getHits().getHits()) {
                bulkBuilder.add(esa.getClient().prepareDelete(hit.getIndex(), hit.getType(), hit.getId()));
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Bulk delete request on %s elements", bulkBuilder.numberOfActions()));
            }
            // Run bulk delete request
            bulkBuilder.execute().actionGet();
        }
    }

    SearchResponse runNextScroll(SearchResponse response, TimeValue keepAlive) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Scroll request: -XGET 'localhost:9200/_search/scroll' -d '{\"scroll\": \"%s\", \"scroll_id\": \"%s\" }'",
                    keepAlive, response.getScrollId()));
        }
        return esa.getClient().prepareSearchScroll(response.getScrollId()).setScroll(keepAlive).execute().actionGet();
    }

    /**
     * Return the ecm:path of an ES document or null if not found.
     */
    String getPathOfDocFromEs(String repository, String docId) {
        String indexName = esa.getIndexNameForRepository(repository);
        GetRequestBuilder getRequest = esa.getClient().prepareGet(indexName, DOC_TYPE, docId).setFields(PATH_FIELD);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Get path of doc: curl -XGET 'http://localhost:9200/%s/%s/%s?fields=%s'", indexName,
                    DOC_TYPE, docId, PATH_FIELD));
        }
        GetResponse ret = getRequest.execute().actionGet();
        if (!ret.isExists() || ret.getField(PATH_FIELD) == null) {
            // doc not found
            return null;
        }
        return ret.getField(PATH_FIELD).getValue().toString();
    }

    /**
     * Return indexing request or null if the doc does not exists anymore.
     *
     * @throws java.lang.IllegalStateException if the command is not attached to a session
     */
    IndexRequestBuilder buildEsIndexingRequest(IndexingCommand cmd) {
        DocumentModel doc = cmd.getTargetDocument();
        if (doc == null) {
            return null;
        }
        try {
            JsonFactory factory = new JsonFactory();
            OutputStream out = new BytesStreamOutput();
            JsonGenerator jsonGen = factory.createJsonGenerator(out);
            jsonESDocumentWriter.writeESDocument(jsonGen, doc, cmd.getSchemas(), null);
            IndexRequestBuilder ret = esa.getClient()
                                         .prepareIndex(esa.getIndexNameForRepository(cmd.getRepositoryName()), DOC_TYPE,
                                                 cmd.getTargetDocumentId())
                                         .setSource(jsonBuilder(out));
            if (useExternalVersion && cmd.getOrder() > 0) {
                ret.setVersionType(VersionType.EXTERNAL).setVersion(cmd.getOrder());
            }
            return ret;
        } catch (IOException e) {
            throw new NuxeoException("Unable to create index request for Document " + cmd.getTargetDocumentId(), e);
        }
    }

}
