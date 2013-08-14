package org.nuxeo.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;

public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSearchService {

    protected NuxeoElasticSearchConfig config;

    protected Node node;

    protected Client client;

    protected NuxeoElasticSearchConfig getConfig() {
        if (Framework.isTestModeSet() && config == null) {
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

    public Node getNode() {
        if (node == null) {
            NuxeoElasticSearchConfig config = getConfig();
            Settings settings = ImmutableSettings.settingsBuilder().put(
                    "node.http.enabled", config.enableHttp()).put("path.logs",
                    config.getLogPath()).put("path.data", config.getDataPath()).put(
                    "gateway.type", "none").put("index.store.type",
                    config.getIndexStorageType()).put("index.number_of_shards",
                    1).put("index.number_of_replicas", 1).build();
            node = NodeBuilder.nodeBuilder().local(config.isLocal()).settings(
                    settings).node();
        }
        return node;
    }

    @Override
    public void index(DocumentLocation docLocation, boolean children) {
        // TODO Auto-generated method stub

    }

    protected void flushIfLocal() {
        if (Framework.isTestModeSet()) {
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
            JsonDocumentWriter.writeDocument(jsonGen, doc, doc.getSchemas(), null);
            IndexResponse response = getClient().prepareIndex("nxmain", "doc", doc.getId())
                    .setSource(builder).execute()
                    .actionGet();
            flushIfLocal();
            return response.getId();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Client getClient() {
        if (client == null && getNode() != null) {
            client = getNode().client();
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
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        if (client != null) {
            client.close();
        }
        if (node != null) {
            node.close();
        }
        client = null;
        node = null;
    }

}
