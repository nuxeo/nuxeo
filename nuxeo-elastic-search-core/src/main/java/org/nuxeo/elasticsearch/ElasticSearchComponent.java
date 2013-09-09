package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.admin.cluster.node.shutdown.NodesShutdownRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Assert;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSearchService, ElasticSearchAdmin {

    protected static final Log log = LogFactory.getLog(ElasticSearchComponent.class);

    protected NuxeoElasticSearchConfig config;

    protected Node localNode;

    protected Client client;

    protected final CopyOnWriteArrayList<String> pendingWork = new CopyOnWriteArrayList<String>();

    public static final String EP_Config = "elasticSearchConfig";

    public static final String MAIN_IDX = "nxmain";

    public static final String NX_DOCUMENT = "doc";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (EP_Config.equals(extensionPoint)) {
            release();
            config = (NuxeoElasticSearchConfig) contribution;
        }
    }

    public NuxeoElasticSearchConfig getConfig() {
        if (Framework.isTestModeSet() && config == null) {
            // automatically generate test config
            config = new NuxeoElasticSearchConfig();
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
        NuxeoElasticSearchConfig config = getConfig();
        Settings settings = null;
        if (config.isInProcess()) {
            settings = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp()).put("path.logs",
                    config.getLogPath()).put("path.data", config.getDataPath()).put(
                    "gateway.type", "none").put("index.store.type",
                    config.getIndexStorageType()).put("index.number_of_shards",
                    1).put("index.number_of_replicas", 1).build();
        } else {
            settings = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp()).put(
                    "cluster.name", config.getClusterName()).build();
        }
        return settings;
    }

    @Override
    public void index(DocumentModel doc, boolean recurse) {

        boolean added = pendingWork.addIfAbsent(getWorkKey(doc));
        if (!added) {
            log.debug("Skip indexing for " + doc
                    + " since it is already scheduled");
            return;
        }

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        IndexingWorker idxWork = new IndexingWorker(doc, recurse);
        wm.schedule(idxWork, true);
    }

    protected void flushIfLocal() {
        if (getConfig().isInProcess()) {
            // do the refresh
            getClient().admin().indices().prepareRefresh().execute().actionGet();
        }
    }

    @Override
    public String indexNow(DocumentModel doc) throws ClientException {
        try {
            JsonFactory factory = new JsonFactory();
            XContentBuilder builder = jsonBuilder();
            JsonGenerator jsonGen = factory.createJsonGenerator(builder.stream());
            JsonDocumentWriter.writeDocument(jsonGen, doc, doc.getSchemas(),
                    null);
            IndexResponse response = getClient().prepareIndex(MAIN_IDX,
                    NX_DOCUMENT, doc.getId()).setSource(builder).execute().actionGet();
            flushIfLocal();
            pendingWork.remove(getWorkKey(doc));
            return response.getId();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Node getLocalNode() {
        if (localNode == null) {
            NuxeoElasticSearchConfig config = getConfig();
            Settings settings = getSettings();
            localNode = NodeBuilder.nodeBuilder().local(config.isInProcess()).settings(
                    settings).node();
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
                        address[1] = "9300"; // Hard coded for now !
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
    public DocumentModelList query(CoreSession session, QueryBuilder queryBuilder,
            int pageSize, int pageIdx) throws ClientException {

        DocumentModelList result = new DocumentModelListImpl();
        boolean completed = false;
        int fetch = 0;

        while (result.size()<pageSize && ! completed) {
            int start = (pageIdx+fetch)*pageSize;
            int end = (pageIdx+fetch+1)*pageSize;
            SearchResponse searchResponse = getClient().prepareSearch(MAIN_IDX)
                    .setTypes("doc")
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setQuery(queryBuilder)
                    .setFrom(start).setSize(end)
                    .execute()
                    .actionGet();

            if (searchResponse.getHits().getTotalHits() < pageSize) {
                completed = true;
            }
            Iterator<SearchHit> hits =  searchResponse.getHits().iterator();
            while (hits.hasNext()) {
                SearchHit hit = hits.next();
                IdRef ref = new IdRef(hit.getId());
                if (session.exists(ref)) {
                    result.add(session.getDocument(ref));
                }
            }
            fetch+=1;
        }
        return result;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        if (getConfig() != null && !getConfig().isInProcess()
                && getConfig().autostartLocalNode()) {
            ElasticSearchControler controler = new ElasticSearchControler(
                    config);
            if (controler.start()) {
                log.info("Started Elastic Search");
            } else {
                log.error("Failed to start ElasticSearch");
            }
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
                client.admin().cluster().nodesShutdown(
                        new NodesShutdownRequest(getConfig().getNodeName())).actionGet();
            }
            client.close();
        }
        if (localNode != null) {
            localNode.close();
        }
        client = null;
        localNode = null;
    }

    protected String getWorkKey(DocumentModel doc) {
        return doc.getRepositoryName() + ":" + doc.getId();
    }

    public boolean isAlreadyScheduledForIndexing(DocumentModel doc) {
        if (pendingWork.contains(getWorkKey(doc))) {
            return true;
        }
        return false;
    }

    public int getPendingIndexingTasksCount() {
        return pendingWork.size();
    }
}
