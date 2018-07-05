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
package org.nuxeo.elasticsearch.test;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.client.ESRestClientFactory;
import org.nuxeo.elasticsearch.client.ESTransportClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

/**
 * This ES client factory uses environment properties to choose the type of client during tests.
 *
 * @since 9.3
 */
public class ESTestClientFactory implements ESClientFactory {
    public static final String TRANSPORT_CLIENT = "TransportClient";

    public static final String REST_CLIENT = "RestClient";

    public static final String DEFAULT_CLIENT = REST_CLIENT;

    public static final String CLIENT_PROPERTY = "nuxeo.test.elasticsearch.client";

    public static final String ADDRESS_LIST_PROPERTY = "nuxeo.test.elasticsearch.addressList";

    public static final String CLUSTER_NAME_PROPERTY = "nuxeo.test.elasticsearch.clusterName";

    private static final Log log = LogFactory.getLog(ESTestClientFactory.class);

    protected Random random = new Random();

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig config) {
        // we don't use the provided config for the client
        String clientType = System.getProperty(CLIENT_PROPERTY);
        switch (clientType != null ? clientType : DEFAULT_CLIENT) {
        case TRANSPORT_CLIENT:
            return createTransportClient(node);
        case REST_CLIENT:
            return createRestClient(node);
        default:
            throw new IllegalArgumentException("Unknown Elasticsearch client type: " + clientType);
        }
    }

    protected ESClient createTransportClient(ElasticSearchEmbeddedNode node) {
        ESTransportClientFactory factory = new ESTransportClientFactory();
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        addOptions(config);
        log.info("Using Elasticsearch TransportClient");
        System.out.println("ElasticSearchClient: TransportClient");
        return factory.create(node, config);
    }

    protected ESClient createRestClient(ElasticSearchEmbeddedNode node) {
        ESRestClientFactory factory = new ESRestClientFactory();
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        addOptions(config);
        log.info("Using Elasticsearch RestClient");
        System.out.println("ElasticSearchClient: RestClient");
        return factory.create(node, config);
    }

    protected void addOptions(ElasticSearchClientConfig config) {
        String addressList = System.getProperty(ADDRESS_LIST_PROPERTY, "localhost:9300");
        if (addressList != null) {
            config.options.put("addressList", addressList);
        }
        String clusterName = System.getProperty(CLUSTER_NAME_PROPERTY);
        if (clusterName != null) {
            config.options.put("clusterName", clusterName);
        }
    }
}
