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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
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
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.listener.EventConstants;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component used to configure and manage ElasticSearch integration
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSearchService, ElasticSearchIndexing, ElasticSearchAdmin {

    protected static final Log log = LogFactory.getLog(ElasticSearchComponent.class);

    protected NuxeoElasticSearchConfig config;

    protected Node localNode;

    protected Client client;

    // temporary hack until we are able to list pending indexing jobs cluster
    // wide
    protected final CopyOnWriteArrayList<String> pendingWork = new CopyOnWriteArrayList<String>();
    protected final CopyOnWriteArrayList<String> pendingCommands = new CopyOnWriteArrayList<String>();

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
            String cname = config.getClusterName();
            if (cname == null) {
                cname = "NuxeoESCluster";
            }
            settings = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp()).put("path.logs",
                    config.getLogPath()).put("path.data", config.getDataPath()).put(
                    "gateway.type", "none").put("index.store.type",
                    config.getIndexStorageType()).put("index.number_of_shards",
                    1).put("index.number_of_replicas", 1).put("cluster.name",
                    cname).put("node.name", config.getNodeName()).build();
        } else {
            settings = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp()).put(
                    "cluster.name", config.getClusterName()).build();
        }
        return settings;
    }

    protected void schedulePostCommitIndexing(IndexingCommand cmd) throws ClientException {

        try {
            CoreSession session = cmd.getTargetDocument().getCoreSession();
            EventProducer evtProducer = Framework.getLocalService(EventProducer.class);

            EventContextImpl context = new EventContextImpl(session,
                    session.getPrincipal());
            context.getProperties().put(cmd.getId(),cmd.toJSON());

            Event indexingEvent = context.newEvent(EventConstants.ES_INDEX_EVENT_SYNC);
            evtProducer.fireEvent(indexingEvent);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public String indexNow(IndexingCommand cmd) throws ClientException {
        DocumentModel doc = cmd.getTargetDocument();
        try {
            JsonFactory factory = new JsonFactory();
            XContentBuilder builder = jsonBuilder();

            JsonGenerator jsonGen = factory.createJsonGenerator(builder.stream());
            JsonESDocumentWriter.writeESDocument(jsonGen, doc, doc.getSchemas(),
                    null);
            IndexResponse response = getClient().prepareIndex(MAIN_IDX,
                    NX_DOCUMENT, doc.getId()).setSource(builder).execute().actionGet();

            pendingWork.remove(getWorkKey(doc));
            pendingCommands.remove(cmd.getId());

            return response.getId();
        } catch (Exception e) {
            throw new ClientException("Unable to index Document " + doc.getId(), e);
        }
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
            schedulePostCommitIndexing(cmd);
        } else {
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            IndexingWorker idxWork = new IndexingWorker(cmd);
            wm.schedule(idxWork, true);
        }
    }

    protected void flushIfLocal() {
        if (getConfig().isInProcess()) {
            // do the refresh
            flush();
        }
    }

    public void flush() {
        getClient().admin().indices().prepareRefresh().execute().actionGet();
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
    public DocumentModelList query(CoreSession session,
            QueryBuilder queryBuilder, int pageSize, int pageIdx)
            throws ClientException {

        DocumentModelList result = new DocumentModelListImpl();
        boolean completed = false;
        int fetch = 0;

        while (result.size() < pageSize && !completed) {
            int start = (pageIdx + fetch) * pageSize;
            int end = (pageIdx + fetch + 1) * pageSize;
            SearchResponse searchResponse = getClient().prepareSearch(MAIN_IDX).setTypes(
                    "doc").setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                    queryBuilder).setFrom(start).setSize(end).execute().actionGet();

            if (searchResponse.getHits().getTotalHits() < pageSize) {
                completed = true;
            }
            Iterator<SearchHit> hits = searchResponse.getHits().iterator();
            while (hits.hasNext()) {
                SearchHit hit = hits.next();
                IdRef ref = new IdRef(hit.getId());
                if (session.exists(ref)) {
                    result.add(session.getDocument(ref));
                }
            }
            fetch += 1;
        }
        return result;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        if (getConfig() != null && !getConfig().isInProcess()
                && getConfig().autostartLocalNode()) {
            ElasticSearchController controler = new ElasticSearchController(
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

    public int getPendingDocs() {
        return pendingWork.size();
    }

    public int getPendingCommands() {
        return pendingCommands.size();
    }

}
