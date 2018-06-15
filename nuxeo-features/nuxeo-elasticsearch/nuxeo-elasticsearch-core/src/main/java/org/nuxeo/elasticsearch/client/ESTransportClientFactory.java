/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

/**
 * @since 9.3
 */
public class ESTransportClientFactory implements ESClientFactory {
    private static final Log log = LogFactory.getLog(ESTransportClientFactory.class);

    public ESTransportClientFactory() {
    }

    public Settings.Builder getSetting(ElasticSearchClientConfig config) {
        return Settings.builder()
                       .put("cluster.name", config.getOption("clusterName", "elasticsearch"))
                       .put("client.transport.nodes_sampler_interval",
                               config.getOption("clientTransportNodesSamplerInterval", "5s"))
                       .put("client.transport.ping_timeout", config.getOption("clientTransportPingTimeout", "5s"))
                       .put("client.transport.ignore_cluster_name",
                               config.getOption("clientTransportIgnoreClusterName", "false"))
                       .put("client.transport.sniff", config.getOption("clientTransportSniff", "true"));
    }

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig config) {
        log.info("Creating an Elasticsearch TransportClient");
        if (node == null) {
            return createRemoteClient(config);
        }
        return createLocalClient(node);
    }

    @SuppressWarnings("resource")
    protected ESClient createRemoteClient(ElasticSearchClientConfig config) {
        Settings settings = getSetting(config).build();
        log.debug("Using settings: " + settings.toDelimitedString(','));
        TransportClient client = new PreBuiltTransportClient(settings); // not closed here
        String[] addresses = config.getOption("addressList", "").split(",");
        if (addresses.length == 0) {
            throw new IllegalArgumentException("No addressList option provided cannot connect TransportClient");
        } else {
            for (String item : addresses) {
                String[] address = item.split(":");
                log.debug("Add transport address: " + item);
                try {
                    InetAddress inet = InetAddress.getByName(address[0]);
                    client.addTransportAddress(new TransportAddress(inet, Integer.parseInt(address[1])));
                } catch (UnknownHostException e) {
                    log.error("Unable to resolve host " + address[0], e);
                }
            }
        }
        return new ESTransportClient(client);
    }

    protected ESClient createLocalClient(ElasticSearchEmbeddedNode node) {
        log.info("Creating a TransportClient to a local Elasticsearch");
        return new ESTransportClient(node.getNode().client());
    }
}
