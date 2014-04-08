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
 *     bdelbosc
 */

package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.ACL_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.CHILDREN_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.DEFAULT_FULLTEXT_FIELDS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.admin.cluster.node.shutdown.NodesShutdownRequest;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.service.PendingClusterTask;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchServerConfig;
import org.nuxeo.elasticsearch.nxql.NxqlQueryConverter;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * Component used to configure and manage ElasticSearch integration
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSearchService, ElasticSearchIndexing, ElasticSearchAdmin {

    private static final Log log = LogFactory
            .getLog(ElasticSearchComponent.class);

    public static final String EP_CONFIG = "elasticSearchServer";

    public static final String EP_INDEX = "elasticSearchIndex";

    public static final String ID_FIELD = "_id";

    protected ElasticSearchServerConfig config;

    protected Node localNode;

    protected Client client;

    protected boolean indexInitDone = false;

    // indexing command that where received before the index initialization
    protected List<IndexingCommand> stackedCommands = new ArrayList<>();

    // temporary hack until we are able to list pending indexing jobs cluster
    // wide
    protected final CopyOnWriteArrayList<String> pendingWork = new CopyOnWriteArrayList<String>();

    protected final CopyOnWriteArrayList<String> pendingCommands = new CopyOnWriteArrayList<String>();

    protected Map<String, ElasticSearchIndexConfig> indexes = new HashMap<String, ElasticSearchIndexConfig>();

    protected List<String> fulltextFields;

    protected String docIndexName;

    // Metrics
    protected final MetricRegistry registry = SharedMetricRegistries
            .getOrCreate(MetricsService.class.getName());

    protected Timer searchTimer;

    protected Timer fetchTimer;


    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (EP_CONFIG.equals(extensionPoint)) {
            release();
            config = (ElasticSearchServerConfig) contribution;
        } else if (EP_INDEX.equals(extensionPoint)) {
            ElasticSearchIndexConfig idx = (ElasticSearchIndexConfig) contribution;
            ElasticSearchIndexConfig previous = indexes.put(idx.getIndexType(), idx);
            idx.merge(previous);
            if (DOC_TYPE.equals(idx.getIndexType())) {
                docIndexName = idx.getIndexName();
            }
        }
    }

    @Override
    public ElasticSearchServerConfig getConfig() {
        if (Framework.isTestModeSet() && config == null) {
            // automatically generate test config
            config = new ElasticSearchServerConfig();
            config.setInProcess(true);
            config.enableHttp(true);
            File home = Framework.getRuntime().getHome();
            File esDirectory = new File(home, "elasticsearch");
            esDirectory.mkdir();
            config.setLogPath(esDirectory.getPath() + "/logs");
            config.setDataPath(esDirectory.getPath() + "/data");
            config.setIndexStorageType("memory");
        }
        return config;
    }

    protected Settings getSettings() {
        ElasticSearchServerConfig config = getConfig();
        Settings settings = null;
        String cname = config.getClusterName();
        if (config.isInProcess()) {
            if (cname == null) {
                cname = "NuxeoESCluster";
            }
            Builder sBuilder = ImmutableSettings.settingsBuilder();
            sBuilder.put("node.http.enabled", config.enableHttp())
                    .put("path.logs", config.getLogPath())
                    .put("path.data", config.getDataPath())
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 1)
                    .put("cluster.name", cname)
                    .put("node.name", config.getNodeName());
            if (config.getIndexStorageType() != null) {
                sBuilder.put("index.store.type", config.getIndexStorageType());
                if (config.getIndexStorageType().equals("memory")) {
                    sBuilder.put("gateway.type", "none");
                }
            }
            settings = sBuilder.build();
        } else {
            Builder builder = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp());
            if (cname != null) {
                builder.put("cluster.name", cname);
            }
            settings = builder.build();
        }
        return settings;
    }

    protected void schedulePostCommitIndexing(IndexingCommand cmd)
            throws ClientException {
        try {
            EventProducer evtProducer = Framework
                    .getLocalService(EventProducer.class);
            Event indexingEvent = cmd.asIndexingEvent();
            evtProducer.fireEvent(indexingEvent);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public void indexNow(List<IndexingCommand> cmds) throws ClientException {

        if (!indexInitDone) {
            stackedCommands.addAll(cmds);
            log.debug("Delaying indexing request : waiting for Index to be initialized");
            return;
        }

        BulkRequestBuilder bulkRequest = getClient().prepareBulk();
        for (IndexingCommand cmd : cmds) {
            if (IndexingCommand.DELETE.equals(cmd.getName())) {
                indexNow(cmd);
            } else {
                log.debug("Sending bulk indexing request to ElasticSearch "
                        + cmd.toString());
                IndexRequestBuilder idxRequest = buildESIndexingRequest(cmd);
                bulkRequest.add(idxRequest);
            }
        }
        // execute bulk index if any
        if (bulkRequest.numberOfActions() > 0) {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Index bulk request: curl -XPOST 'http://localhost:9200/_bulk' -d '%s'",
                                bulkRequest.request().requests().toString()));
            }
            bulkRequest.execute().actionGet();
        }

        for (IndexingCommand cmd : cmds) {
            markCommandExecuted(cmd);
        }
    }

    @Override
    public void indexNow(IndexingCommand cmd) throws ClientException {

        if (!indexInitDone) {
            stackedCommands.add(cmd);
            log.debug("Delaying indexing request : waiting for Index to be initialized");
            return;
        }

        log.debug("Sending indexing request to ElasticSearch " + cmd.toString());
        DocumentModel doc = cmd.getTargetDocument();
        if (IndexingCommand.DELETE.equals(cmd.getName())) {
            DeleteRequestBuilder request = getClient().prepareDelete(getDocIndex(),
                    DOC_TYPE, doc.getId());
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Delete request: curl -XDELETE 'http://localhost:9200/%s/%s/%s' -d '%s'",
                                getDocIndex(), DOC_TYPE, cmd.getTargetDocument()
                                        .getId(), request.request().toString()));
            }
            request.execute().actionGet();

            if (cmd.isRecurse()) {
                DeleteByQueryRequestBuilder deleteRequest = getClient()
                        .prepareDeleteByQuery(getDocIndex()).setQuery(
                                QueryBuilders.constantScoreQuery(FilterBuilders
                                        .termFilter(CHILDREN_FIELD,
                                                doc.getPathAsString())));
                if (log.isDebugEnabled()) {
                    log.debug(String
                            .format("Delete byQuery request: curl -XDELETE 'http://localhost:9200/%s/%s/_query' -d '%s'",
                                    getDocIndex(), DOC_TYPE, request.request()
                                            .toString()));
                }
                deleteRequest.execute().actionGet();
            }
        } else {
            IndexRequestBuilder request = buildESIndexingRequest(cmd);
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Index request: curl -XPUT 'http://localhost:9200/%s/%s/%s' -d '%s'",
                                getDocIndex(), DOC_TYPE, cmd.getTargetDocument()
                                        .getId(), request.request().toString()));
            }
            request.execute().actionGet();
        }
        markCommandExecuted(cmd);
    }

    /**
     * Get the elastic search index name for the documents
     */
    protected String getDocIndex() {
        return docIndexName;
    }

    protected IndexRequestBuilder buildESIndexingRequest(IndexingCommand cmd)
            throws ClientException {
        DocumentModel doc = cmd.getTargetDocument();
        try {
            JsonFactory factory = new JsonFactory();
            XContentBuilder builder = jsonBuilder();

            JsonGenerator jsonGen = factory.createJsonGenerator(builder
                    .stream());
            JsonESDocumentWriter.writeESDocument(jsonGen, doc,
                    cmd.getSchemas(), null);
            return getClient().prepareIndex(getDocIndex(), DOC_TYPE, doc.getId())
                    .setSource(builder);
        } catch (Exception e) {
            throw new ClientException(
                    "Unable to create index request for Document "
                            + doc.getId(), e);
        }
    }

    protected void markCommandExecuted(IndexingCommand cmd) {
        pendingWork.remove(getWorkKey(cmd.getTargetDocument()));
        pendingCommands.remove(cmd.getId());
    }

    @Override
    public void scheduleIndexing(IndexingCommand cmd) throws ClientException {
        DocumentModel doc = cmd.getTargetDocument();
        boolean added = pendingCommands.addIfAbsent(cmd.getId());
        if (!added) {
            log.debug("Skip indexing for " + doc
                    + " since it is already scheduled");
            return;
        }

        added = pendingWork.addIfAbsent(getWorkKey(doc));
        if (!added) {
            log.debug("Skip indexing for " + doc
                    + " since it is already scheduled");
            return;
        }

        if (cmd.isSync()) {
            log.debug("Schedule PostCommit indexing request " + cmd.toString());
            schedulePostCommitIndexing(cmd);
        } else {
            log.debug("Schedule Async indexing request  " + cmd.toString());
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            IndexingWorker idxWork = new IndexingWorker(cmd);
            wm.schedule(idxWork, true);
        }
    }

    @Override
    public void flush() {
        flush(false);
    }

    @Override
    public void flush(boolean commit) {
        // refresh indexes
        getClient().admin().indices().prepareRefresh(getDocIndex()).execute()
                .actionGet();
        if (commit) {
            getClient().admin().indices().prepareFlush(getDocIndex()).execute()
                    .actionGet();
        }
    }

    protected Node getLocalNode() {
        if (localNode == null) {
            if (log.isDebugEnabled()) {
                log.debug("Create a local ES node inside the Nuxeo JVM");
            }
            ElasticSearchServerConfig config = getConfig();
            Settings settings = getSettings();
            localNode = NodeBuilder.nodeBuilder().local(config.isInProcess())
                    .settings(settings).node();
            localNode.start();
        }
        return localNode;
    }

    @Override
    public Client getClient() {
        if (client == null) {
            if (getConfig().isInProcess()) {
                client = getLocalNode().client();
            } else {
                TransportClient tClient = new TransportClient(getSettings());
                for (String remoteNode : getConfig().getRemoteNodes()) {
                    String[] address = remoteNode.split(":");
                    try {
                        InetAddress inet = InetAddress.getByName(address[0]);
                        if (log.isDebugEnabled()) {
                            log.debug("Use a remote ES node: " + remoteNode);
                        }
                        tClient.addTransportAddress(new InetSocketTransportAddress(
                                inet, Integer.parseInt(address[1])));
                    } catch (UnknownHostException e) {
                        log.error("Unable to resolve host " + address[0], e);
                    }
                }
                client = tClient;
            }
        }
        return client;
    }

    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit,
            int offset, SortInfo... sortInfos) throws ClientException {
        QueryBuilder queryBuilder = NxqlQueryConverter.toESQueryBuilder(nxql,
                getFulltextFields());

        // handle the built-in order by clause
        if (nxql.toLowerCase().contains("order by")) {
            List<SortInfo> builtInSortInfos = NxqlQueryConverter
                    .getSortInfo(nxql);
            if (sortInfos != null) {
                for (SortInfo si : sortInfos) {
                    builtInSortInfos.add(si);
                }
            }
            sortInfos = builtInSortInfos.toArray(new SortInfo[builtInSortInfos
                    .size()]);
        }
        return query(session, queryBuilder, limit, offset, sortInfos);
    }

    @Override
    public DocumentModelList query(CoreSession session,
            QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) throws ClientException {
        long totalSize;
        List<String> ids;
        Context stopWatch = searchTimer.time();
        try {
            // Initialize request
            SearchRequestBuilder request = getClient().prepareSearch(getDocIndex())
                    .setTypes(DOC_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addField(ID_FIELD).setFrom(offset).setSize(limit);
            // Add security filter
            AndFilterBuilder aclFilter = null;
            Principal principal = session.getPrincipal();
            if (principal != null) {
                if (!(principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal)
                        .isAdministrator())) {
                    String[] principals = SecurityService
                            .getPrincipalsToCheck(principal);
                    // we want an ACL that match principals but we discard
                    // unsupported ACE that contains negative ACE
                    aclFilter = FilterBuilders.andFilter(FilterBuilders
                            .inFilter(ACL_FIELD, principals), FilterBuilders
                            .notFilter(FilterBuilders.inFilter(ACL_FIELD,
                                    UNSUPPORTED_ACL)));
                }
            }
            if (aclFilter == null) {
                request.setQuery(queryBuilder);
            } else {
                request.setQuery(QueryBuilders.filteredQuery(queryBuilder,
                        aclFilter));
            }
            // Add sort
            if (sortInfos != null) {
                for (SortInfo sortInfo : sortInfos) {
                    request.addSort(sortInfo.getSortColumn(), sortInfo
                            .getSortAscending() ? SortOrder.ASC
                            : SortOrder.DESC);
                }
            }
            // Execute the ES query
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Search query: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty' -d '%s'",
                                getDocIndex(), DOC_TYPE, request.toString()));
            }
            SearchResponse response = request.execute().actionGet();
            if (log.isDebugEnabled()) {
                log.debug("Response: " + response.toString());
            }
            // Get the list of ids
            ids = new ArrayList<String>(limit - offset);
            for (SearchHit hit : response.getHits()) {
                ids.add(hit.getId());
            }
            totalSize = response.getHits().getTotalHits();
        } finally {
            stopWatch.stop();
        }
        DocumentModelList ret = new DocumentModelListImpl(ids.size());
        stopWatch = fetchTimer.time();
        try {
            ((DocumentModelListImpl) ret).setTotalSize(totalSize);
            // Fetch the document model
            if (!ids.isEmpty()) {
                try {
                    ret.addAll(fetchDocuments(ids, session));
                } catch (ClientException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } finally {
            stopWatch.stop();
        }
        return ret;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {

        super.applicationStarted(context);
        if (getConfig() == null) {
            log.warn("Unable to initialize ElasticSearch service : no configuration is provided");
            return;
        }
        // init metrics
        searchTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "search"));
        fetchTimer = registry.timer(MetricRegistry.name("nuxeo",
                "elasticsearch", "service", "fetch"));
        // start Server if needed
        if (getConfig() != null && !getConfig().isInProcess()
                && getConfig().autostartLocalNode()) {
            startESServer(getConfig());
        }
        // init client
        getClient();
        // init indexes if needed
        initIndexes(false);
    }

    protected void startESServer(ElasticSearchServerConfig config)
            throws Exception {
        ElasticSearchController controler = new ElasticSearchController(config);
        if (controler.start()) {
            log.info("Started Elasticsearch");
        } else {
            log.error("Failed to start ElasticSearch");
        }
    }

    @Override
    public void initIndexes(boolean recreate) throws Exception {
        for (ElasticSearchIndexConfig idx : indexes.values()) {
            initIndex(idx, recreate);
        }
        indexInitDone = true;
        if (!stackedCommands.isEmpty()) {
            boolean txCreated = false;
            if (!TransactionHelper.isTransactionActive()) {
                txCreated = TransactionHelper.startTransaction();
            }
            try {
                for (final IndexingCommand cmd : stackedCommands) {
                    new UnrestrictedSessionRunner(cmd.getRepository()) {
                        @Override
                        public void run() throws ClientException {
                            cmd.refresh(session);
                            indexNow(cmd);
                        }
                    };
                }
            } catch (Exception e) {
                log.error("Unable to flush pending indexing commands", e);
            } finally {
                if (txCreated) {
                    TransactionHelper.commitOrRollbackTransaction();
                }
                stackedCommands.clear();
            }
        }
    }

    protected void initIndex(ElasticSearchIndexConfig conf, boolean dropIfExists)
            throws Exception {
        if (! conf.mustCreate()) {
            return;
        }
        log.info(String.format("Initialize index: %s, type: %s",
                conf.getIndexName(), conf.getIndexType()));
        boolean mappingExists = false;
        boolean indexExists = getClient().admin().indices()
                .prepareExists(conf.getIndexName()).execute().actionGet()
                .isExists();
        if (indexExists) {
            if (!dropIfExists) {
                log.debug("Index " + conf.getIndexName() + " already exists");
                mappingExists = getClient().admin().indices()
                        .prepareGetMappings(conf.getIndexName()).execute()
                        .actionGet().getMappings().containsKey(DOC_TYPE);
            } else {
                log.warn(String.format("Initializing index: %s, type: %s with "
                        + "dropIfExists flag, deleting an existing index",
                        conf.getIndexName(), conf.getIndexType()));
                getClient().admin().indices()
                        .delete(new DeleteIndexRequest(conf.getIndexName()))
                        .actionGet();
                indexExists = false;
            }
        }
        if (!indexExists) {
            log.info(String.format("Creating index: %s", conf.getIndexName()));
            getClient().admin().indices().prepareCreate(conf.getIndexName())
                    .setSettings(conf.getSettings()).execute().actionGet();
        }
        if (!mappingExists) {
            log.info(String.format("Creating mapping type: %s on index: %s",
                    conf.getIndexType(), conf.getIndexName()));
            getClient().admin().indices()
                    .preparePutMapping(conf.getIndexName())
                    .setType(conf.getIndexType()).setSource(conf.getMapping())
                    .execute().actionGet();

        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        release();
    }

    protected void release() {
        if (client != null) {
            if (getConfig() != null && !getConfig().isInProcess()
                    && getConfig().autostartLocalNode()) {
                client.admin()
                        .cluster()
                        .nodesShutdown(
                                new NodesShutdownRequest(getConfig()
                                        .getNodeName())).actionGet();
            }
            client.close();
        }
        if (localNode != null) {
            localNode.stop();
            localNode.close();
        }
        client = null;
        localNode = null;
    }

    protected String getWorkKey(DocumentModel doc) {
        return doc.getRepositoryName() + ":" + doc.getId();
    }

    @Override
    public boolean isAlreadyScheduledForIndexing(DocumentModel doc) {
        if (pendingWork.contains(getWorkKey(doc))) {
            return true;
        }
        return false;
    }

    @Override
    public int getPendingDocs() {
        return pendingWork.size();
    }

    @Override
    public int getPendingCommands() {
        return pendingCommands.size();
    }

    @Override
    public List<PendingClusterTask> getPendingTasks() {
        PendingClusterTasksResponse response = getClient().admin().cluster()
                .preparePendingClusterTasks().execute().actionGet();
        return response.getPendingTasks();
    }

    /**
     * Fetch document models from VCS, return results in the same order.
     *
     */
    protected List<DocumentModel> fetchDocuments(final List<String> ids,
            CoreSession session) throws ClientException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document WHERE ecm:uuid IN (");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(NXQL.escapeString(ids.get(i)));
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        DocumentModelList ret = session.query(sb.toString());
        // Order the results
        Collections.sort(ret, new Comparator<DocumentModel>() {
            @Override
            public int compare(DocumentModel a, DocumentModel b) {
                return ids.indexOf(a.getId()) - ids.indexOf(b.getId());
            }
        });
        return ret;
    }

    @Override
    public List<String> getFulltextFields() {
        if (fulltextFields != null) {
            return fulltextFields;
        }
        ElasticSearchIndexConfig idxConfig = indexes.get(getDocIndex());
        if (idxConfig != null && !idxConfig.getFulltextFields().isEmpty()) {
            fulltextFields = idxConfig.getFulltextFields();
        } else {
            fulltextFields = Arrays.asList(DEFAULT_FULLTEXT_FIELDS);
        }
        return fulltextFields;
    }

}
