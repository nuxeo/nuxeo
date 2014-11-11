/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * @since 5.9.6
 */
public class ElasticSearchIndexingImpl implements ElasticSearchIndexing {
    private static final Log log = LogFactory
            .getLog(ElasticSearchIndexingImpl.class);

    private final ElasticSearchAdminImpl esa;
    private final Timer deleteTimer;
    private final Timer indexTimer;
    private final Timer bulkIndexTimer;

    public ElasticSearchIndexingImpl(ElasticSearchAdminImpl esa) {
        this.esa = esa;
        MetricRegistry registry = SharedMetricRegistries
                .getOrCreate(MetricsService.class.getName());
        indexTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "index"));
        deleteTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "delete"));
        bulkIndexTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "bulkIndex"));
    }

    @Override
    public void indexNow(List<IndexingCommand> cmds) throws ClientException {
        // we count all commands even those coming from async children worker
        // which are not scheduled
        int nbCommands = cmds.size();
        esa.totalCommandRunning.addAndGet(nbCommands);
        try {
            // uncomment to simulate long indexing which timeout postcommit
            // try {Thread.sleep(1000);} catch (InterruptedException e) { }
            processBulkDeleteCommands(cmds);
            Context stopWatch = bulkIndexTimer.time();
            try {
                processBulkIndexCommands(cmds);
            } finally {
                stopWatch.stop();
            }
        } finally {
            esa.totalCommandRunning.addAndGet(-nbCommands);
        }
        esa.totalCommandProcessed.addAndGet(nbCommands);
    }

    void processBulkDeleteCommands(List<IndexingCommand> cmds) {
        // Can be optimized with a single delete by query
        for (IndexingCommand cmd : cmds) {
            if (IndexingCommand.DELETE.equals(cmd.getName())) {
                Context stopWatch = deleteTimer.time();
                try {
                    processDeleteCommand(cmd);
                } finally {
                    stopWatch.stop();
                }
            }
        }
    }

    void processBulkIndexCommands(List<IndexingCommand> cmds)
            throws ClientException {
        BulkRequestBuilder bulkRequest = esa.getClient().prepareBulk();
        for (IndexingCommand cmd : cmds) {
            String id = cmd.getDocId();
            if (IndexingCommand.UNKOWN_DOCUMENT_ID.equals(id)
                    || (IndexingCommand.DELETE.equals(cmd.getName()))) {
                continue;
            }
            if (log.isTraceEnabled()) {
                log.trace("Sending bulk indexing request to Elasticsearch: "
                        + cmd.toString());
            }
            IndexRequestBuilder idxRequest = buildEsIndexingRequest(cmd);
            bulkRequest.add(idxRequest);
        }
        if (bulkRequest.numberOfActions() > 0) {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Index %d docs in bulk request: curl -XPOST 'http://localhost:9200/_bulk' -d '%s'",
                                bulkRequest.numberOfActions(), bulkRequest
                                        .request().requests().toString()));
            }
            BulkResponse response = bulkRequest.execute().actionGet();
            if (response.hasFailures()) {
                log.error(response.buildFailureMessage());
            }
        }
    }

    @Override
    public void indexNow(IndexingCommand cmd) throws ClientException {
        if (cmd.getTargetDocument() == null
                && IndexingCommand.UNKOWN_DOCUMENT_ID.equals(cmd.getDocId())) {
            esa.totalCommandProcessed.addAndGet(1);
            return;
        }
        esa.totalCommandRunning.incrementAndGet();
        if (log.isTraceEnabled()) {
            log.trace("Sending indexing request to Elasticsearch: "
                    + cmd.toString());
        }
        if (IndexingCommand.DELETE.equals(cmd.getName())) {
            Context stopWatch = deleteTimer.time();
            try {
                processDeleteCommand(cmd);
            } finally {
                stopWatch.stop();
                esa.totalCommandProcessed.addAndGet(1);
                esa.totalCommandRunning.decrementAndGet();
            }
        } else {
            Context stopWatch = indexTimer.time();
            try {
                processIndexCommand(cmd);
            } finally {
                stopWatch.stop();
                esa.totalCommandProcessed.addAndGet(1);
                esa.totalCommandRunning.decrementAndGet();
            }
        }
    }

    void processIndexCommand(IndexingCommand cmd)
            throws ClientException {
        String docId = cmd.getDocId();
        assert (!cmd.getDocId().equals(IndexingCommand.UNKOWN_DOCUMENT_ID));
        IndexRequestBuilder request = buildEsIndexingRequest(cmd);
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Index request: curl -XPUT 'http://localhost:9200/%s/%s/%s' -d '%s'",
                            esa.getRepositoryIndex(cmd.getRepository()),
                            DOC_TYPE, docId, request.request().toString()));
        }
        request.execute().actionGet();
    }

    void processDeleteCommand(IndexingCommand cmd) {
        if (cmd.isRecurse()) {
            processDeleteCommandRecursive(cmd);
        } else {
            processDeleteCommandNonRecursive(cmd);
        }
    }

    void processDeleteCommandNonRecursive(IndexingCommand cmd) {
        String indexName = esa.getRepositoryIndex(cmd.getRepository());
        DeleteRequestBuilder request = esa.getClient().prepareDelete(indexName,
                DOC_TYPE, cmd.getDocId());
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Delete request: curl -XDELETE 'http://localhost:9200/%s/%s/%s'",
                            indexName, DOC_TYPE, cmd.getDocId()));
        }
        request.execute().actionGet();
    }

    void processDeleteCommandRecursive(IndexingCommand cmd) {
        String indexName = esa.getRepositoryIndex(cmd.getRepository());
        // we don't want to rely on target document because the document can be
        // already removed
        String docPath = getPathOfDocFromEs(cmd.getRepository(), cmd.getDocId());
        if (docPath == null) {
            if (!Framework.isTestModeSet()) {
                log.warn("Trying to delete a non existing doc: "
                        + cmd.toString());
            }
            return;
        }
        QueryBuilder query = QueryBuilders.constantScoreQuery(FilterBuilders
                .termFilter(CHILDREN_FIELD, docPath));
        DeleteByQueryRequestBuilder deleteRequest = esa.getClient()
                .prepareDeleteByQuery(indexName).setTypes(DOC_TYPE)
                .setQuery(query);
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Delete byQuery request: curl -XDELETE 'http://localhost:9200/%s/%s/_query' -d '%s'",
                            indexName, DOC_TYPE, query.toString()));
        }
        DeleteByQueryResponse responses = deleteRequest.execute().actionGet();
        for (IndexDeleteByQueryResponse response : responses) {
            // there is no way to trace how many docs are removed
            if (response.getFailedShards() > 0) {
                log.error(String.format(
                        "Delete byQuery fails on shard: %d out of %d",
                        response.getFailedShards(), response.getTotalShards()));
            }
        }
    }

    /**
     * Return the ecm:path of an ES document or null if not found.
     */
    String getPathOfDocFromEs(String repository, String docId) {
        String indexName = esa.getRepositoryIndex(repository);
        GetRequestBuilder getRequest = esa.getClient()
                .prepareGet(indexName, DOC_TYPE, docId).setFields(PATH_FIELD);
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Get path of doc: curl -XGET 'http://localhost:9200/%s/%s/%s?fields=%s'",
                            indexName, DOC_TYPE, docId, PATH_FIELD));
        }
        GetResponse ret = getRequest.execute().actionGet();
        if (!ret.isExists()) {
            // doc not found
            return null;
        }
        return ret.getField(PATH_FIELD).getValue().toString();
    }

    IndexRequestBuilder buildEsIndexingRequest(IndexingCommand cmd)
            throws ClientException {
        DocumentModel doc = cmd.getTargetDocument();
        try {
            JsonFactory factory = new JsonFactory();
            XContentBuilder builder = jsonBuilder();
            JsonGenerator jsonGen = factory.createJsonGenerator(builder
                    .stream());
            JsonESDocumentWriter.writeESDocument(jsonGen, doc,
                    cmd.getSchemas(), null);
            return esa
                    .getClient()
                    .prepareIndex(esa.getRepositoryIndex(cmd.getRepository()),
                            DOC_TYPE, cmd.getDocId()).setSource(builder);
        } catch (Exception e) {
            throw new ClientException(
                    "Unable to create index request for Document "
                            + cmd.getDocId(), e);
        }
    }

    @Override
    public void scheduleIndexing(IndexingCommand cmd) throws ClientException {
        // impl of scheduling is left to the ESService
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isAlreadyScheduled(IndexingCommand cmd) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
