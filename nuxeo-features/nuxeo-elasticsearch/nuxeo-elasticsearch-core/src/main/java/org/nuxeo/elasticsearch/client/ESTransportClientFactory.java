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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @since 9.3
 */
public class ESTransportClientFactory implements ESClientFactory {
    private static final Log log = LogFactory.getLog(ESTransportClientFactory.class);

    public ESTransportClientFactory() {
    }

    public Settings.Builder getSetting(ElasticSearchRemoteConfig serverConfig) {
        return Settings.builder()
                .put("cluster.name", serverConfig.getClusterName())
                .put("client.transport.nodes_sampler_interval", serverConfig.getSamplerInterval())
                .put("client.transport.ping_timeout", serverConfig.getPingTimeout())
                .put("client.transport.ignore_cluster_name", serverConfig.isIgnoreClusterName())
                .put("client.transport.sniff", serverConfig.isClusterSniff());
    }

    @Override
    public ESClient create(ElasticSearchRemoteConfig remoteConfig, ElasticSearchClientConfig clientConfig) {
        log.info("Creating a TransportClient to a remote Elasticsearch");
        Settings settings = getSetting(remoteConfig).build();
        log.debug("Using settings: " + settings.toDelimitedString(','));
        TransportClient client = new PreBuiltTransportClient(settings);
        String[] addresses = remoteConfig.getAddresses();
        if (addresses == null) {
            log.error("You need to provide an addressList to join a cluster");
        } else {
            for (String item : remoteConfig.getAddresses()) {
                String[] address = item.split(":");
                log.debug("Add transport address: " + item);
                try {
                    InetAddress inet = InetAddress.getByName(address[0]);
                    client.addTransportAddress(new InetSocketTransportAddress(inet, Integer.parseInt(address[1])));
                } catch (UnknownHostException e) {
                    log.error("Unable to resolve host " + address[0], e);
                }
            }
        }
        return new ESTransportClient(client);
    }

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig clientConfig) {
        log.info("Creating a TransportClient to a local Elasticsearch");
        return new ESTransportClient(node.getNode().client());
    }
}
