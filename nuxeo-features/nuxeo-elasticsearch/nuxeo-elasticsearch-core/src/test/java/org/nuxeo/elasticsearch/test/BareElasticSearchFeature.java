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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import java.io.File;
import java.security.InvalidParameterException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Scopes;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Features(RuntimeFeature.class)
public class BareElasticSearchFeature extends SimpleFeature {

    protected Client client;

    protected Node node;

    @Override
    public void start(FeaturesRunner runner) throws Exception {

        File home = Framework.getRuntime().getHome();
        File esDirectory = new File(home, "elasticsearch");
        if (!esDirectory.exists() && !esDirectory.mkdir()) {
            throw new InvalidParameterException("Can not create directory: " + esDirectory.getAbsolutePath());
        }
        Settings settings = Settings.settingsBuilder()
                                    .put("node.http.enabled", true)
                                    .put("path.home", esDirectory.getPath())
                                    .put("path.logs", esDirectory.getPath() + "/logs")
                                    .put("path.data", esDirectory.getPath() + "/data")
                                    .put("index.store.type", "mmapfs")
                                    .put("index.number_of_shards", 1)
                                    .put("index.number_of_replicas", 1)
                                    .build();
        node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        client = node.client();
        super.start(runner);
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        binder.bind(Node.class).toProvider(() -> node).in(Scopes.SINGLETON);
        binder.bind(Client.class).toProvider(() -> client).in(Scopes.SINGLETON);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        super.afterRun(runner);
        node.close();
    }

}
