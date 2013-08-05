package org.nuxeo.elasticsearch.test;

import java.io.File;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

@Features(RuntimeFeature.class)
public class BareElasticSearchFeature extends SimpleFeature {

    protected Client client;
    protected Node node;

    @Override
    public void start(FeaturesRunner runner) throws Exception {

        String root = Framework.getProperty("nuxeo.data.dir");
        File esDirectory = new File(root + "/elasticsearch");
        esDirectory.mkdir();

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("node.http.enabled", true)
                .put("path.logs", esDirectory.getPath() + "/logs")
                .put("path.data", esDirectory.getPath() + "/data")
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1).build();

        node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        client = node.client();
        super.start(runner);
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        binder.bind(Node.class).toProvider(
                new Provider<Node>() {
                    @Override
                    public Node get() {
                        return node;
                    }
                }).in(Scopes.SINGLETON);
        binder.bind(Client.class).toProvider(new Provider<Client>() {
            @Override
            public Client get() {
                return client;
            }
        }).in(Scopes.SINGLETON);
    }


    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        super.afterRun(runner);
        node.close();
    }

}
