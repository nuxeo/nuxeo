/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

import java.io.IOException;

/**
 * @since 9.3
 */
public class ESRestClientFactory implements ESClientFactory {
    private static final Log log = LogFactory.getLog(ESRestClientFactory.class);

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig config) {
        if (node != null) {
            return createLocalRestClient(node.getConfig());
        }
        return createRestClient(config);
    }

    protected ESClient createLocalRestClient(ElasticSearchEmbeddedServerConfig serverConfig) {
        if (! serverConfig.httpEnabled()) {
            throw new IllegalArgumentException("Embedded configuration has no HTTP port enable, use TransportClient instead of Rest");
        }
        RestClient lowLevelRestClient = RestClient.builder(
                new HttpHost("localhost", Integer.parseInt(serverConfig.getHttpPort()))).build();
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);
        //checkConnection(client);
        return new ESRestClient(lowLevelRestClient, client);
    }

    protected ESClient createRestClient(ElasticSearchClientConfig config) {
        String[] hosts = config.getOption("addressList", "").split(",");
        if (hosts.length == 0) {
            throw new IllegalArgumentException("No addressList option provided cannot connect RestClient");
        }
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        int i = 0;
        for (String host : hosts) {
            String[] address = host.split(":");
            httpHosts[i++] = new HttpHost(address[0], Integer.parseInt(address[1]));
        }
        RestClient lowLevelRestClient = RestClient.builder(httpHosts).build();
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);
        //checkConnection(client);
        return new ESRestClient(lowLevelRestClient, client);
    }

    protected void checkConnection(RestHighLevelClient client) {
        boolean ping = false;
        try {
            ping = client.ping();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        if (!ping) {
            throw new IllegalStateException("Fail to ping rest node");
        }
    }
}
