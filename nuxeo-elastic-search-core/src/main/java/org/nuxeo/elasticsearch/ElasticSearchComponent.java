package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.admin.cluster.node.shutdown.NodesShutdownRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
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
            config.setLocal(true);
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
        if (config.isLocal()) {
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
    public void index(DocumentLocation docLocation, boolean children) {
        // TODO Auto-generated method stub

    }

    protected void flushIfLocal() {
        if (getConfig().isLocal()) {
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
            localNode = NodeBuilder.nodeBuilder().local(config.isLocal()).settings(
                    settings).node();
        }
        return localNode;
    }


    @Override
    public Client getClient() {
        if (client == null) {
            if (getConfig().isLocal()) {
                client = getLocalNode().client();
            } else {
                TransportClient tClient = new TransportClient(getSettings());
                for (String remoteNode : getConfig().getRemoteNodes()) {
                    String[] address = remoteNode.split(":");
                    tClient.addTransportAddress(new InetSocketTransportAddress(
                            address[0], Integer.parseInt(address[1])));
                }
                client = tClient;
            }
        }
        return client;
    }

    @Override
    public DocumentModelList query(CoreSession session, String query,
            int pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        if(getConfig()!=null && !getConfig().isLocal() && getConfig().autostartLocalNode()) {
            ElasticSearchControler controler = new ElasticSearchControler(config);
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
            if(getConfig()!=null && !getConfig().isLocal() && getConfig().autostartLocalNode()) {
                client.admin().cluster().nodesShutdown(new NodesShutdownRequest(getConfig().getNodeName())).actionGet();
            }
            client.close();
        }
        if (localNode != null) {
            localNode.close();
        }
        client = null;
        localNode = null;
    }
}
