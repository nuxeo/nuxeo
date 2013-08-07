package org.nuxeo.elasticsearch;

import java.io.File;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSeachService {

    protected NuxeoElasticSearchConfig config;

    protected Node node;

    protected Client client;

    protected NuxeoElasticSearchConfig getConfig() {
        if (Framework.isTestModeSet() && config == null) {
            config = new NuxeoElasticSearchConfig();
            config.setLocal(true);
            config.enableHttp(true);
            String root = Framework.getProperty("nuxeo.data.dir");
            File esDirectory = new File(root + "/elasticsearch");
            esDirectory.mkdir();
            config.setLogPath(esDirectory.getPath() + "/logs");
            config.setLogPath(esDirectory.getPath() + "/data");
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
