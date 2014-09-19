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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import java.io.File;

import java.security.InvalidParameterException;
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

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@Features(RuntimeFeature.class)
public class BareElasticSearchFeature extends SimpleFeature {

    protected Client client;

    protected Node node;

    @Override
    public void start(FeaturesRunner runner) throws Exception {

        File home = Framework.getRuntime().getHome();
        File esDirectory = new File(home, "elasticsearch");
        if (!esDirectory.exists()) {
            if (!esDirectory.mkdir()) {
                throw new InvalidParameterException(
                        "Can not create directory: "
                                + esDirectory.getAbsolutePath());
            }
        }
        Settings settings = ImmutableSettings.settingsBuilder().put(
                "node.http.enabled", true).put("path.logs",
                esDirectory.getPath() + "/logs").put("path.data",
                esDirectory.getPath() + "/data").put("gateway.type", "none").put(
                "index.store.type", "memory").put("index.number_of_shards", 1).put(
                "index.number_of_replicas", 1).build();

        node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        client = node.client();
        super.start(runner);
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        binder.bind(Node.class).toProvider(new Provider<Node>() {
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
