/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.client.ESRestClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;

/**
 * Basic test for ESRestClientFactory creation and config
 */
public class TestESRestClient {

    protected ESRestClientFactory factory = new ESRestClientFactory();

    @Test(expected = IllegalArgumentException.class)
    public void testNoClientConfig() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        ESClient esClient = factory.create(null, config);
        //No config so should throw IllegalArgumentException
    }

    @Test
    public void testValidClientConfigURls() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        config.options.put("addressList", "myhost,localhost:9200,local:80,http://localhosted,https://mysecure,https://moresecure:445");
        ESClient esClient = factory.create(null, config);
        assertNotNull(esClient);
        //Its not possible to get a reference to the list of hosts to check they are configured
    }
}
