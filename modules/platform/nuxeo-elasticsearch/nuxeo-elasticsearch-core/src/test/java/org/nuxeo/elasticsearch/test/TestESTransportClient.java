/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.client.ESTransportClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

/**
 * Test the ESClient based on Transport Client protocol
 *
 * @since 10.2
 */
public class TestESTransportClient extends TestESClient {

    @Override
    public ESClient createClient(ElasticSearchEmbeddedNode embeddedNode) {
        ESTransportClientFactory factory = new ESTransportClientFactory();
        return factory.create(embeddedNode, new ElasticSearchClientConfig());
    }
}
