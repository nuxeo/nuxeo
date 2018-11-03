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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.junit.Test;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.client.ESRestClient;
import org.nuxeo.elasticsearch.client.ESRestClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the ESClient based on Rest Client protocol
 */
public class TestESRestClient extends TestESClient {

    protected ESRestClientFactory factory = new ESRestClientFactory();

    @Override
    public ESClient createClient(ElasticSearchEmbeddedNode embeddedNode) {
        ElasticSearchEmbeddedServerConfig config = new ElasticSearchEmbeddedServerConfig();
        config.setHttpEnabled(true);
        ElasticSearchEmbeddedNode node = new ElasticSearchEmbeddedNode(config);
        return factory.create(node, new ElasticSearchClientConfig());
    }

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

    @Test
    public void testCredentialProvider() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        config.options.put("addressList", "localhost:9200");
        config.options.put(ESRestClientFactory.AUTH_USER_OPT, "bob");
        config.options.put(ESRestClientFactory.AUTH_PASSWORD_OPT, "bob");
        ESRestClient esClient = (ESRestClient) factory.create(null, config);
        assertNotNull(esClient);
        //Its not possible to get a reference to check the configuration, but the absence of an error is itself a test.
    }

    @Test
    public void testAllNulls() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        config.options.put("addressList", "localhost:9200");
        config.options.put(ESRestClientFactory.AUTH_USER_OPT, null);
        config.options.put(ESRestClientFactory.AUTH_PASSWORD_OPT, null);
        config.options.put(ESRestClientFactory.TRUST_STORE_PATH_OPT, null);
        config.options.put(ESRestClientFactory.TRUST_STORE_PASSWORD_OPT, null);
        config.options.put(ESRestClientFactory.TRUST_STORE_TYPE_OPT, null);
        config.options.put(ESRestClientFactory.KEY_STORE_PATH_OPT, null);
        config.options.put(ESRestClientFactory.KEY_STORE_PASSWORD_OPT, null);
        config.options.put(ESRestClientFactory.KEY_STORE_TYPE_OPT, null);
        ESRestClient esClient = (ESRestClient) factory.create(null, config);
        assertNotNull(esClient);
        //Its not possible to get a reference to check the configuration, but the absence of an error is itself a test.
    }


    @Test
    public void testDefaultTrustStore() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();
        String password = "difficultpass";
        File keystoreFile = getKeystoreFile(password, KeyStore.getDefaultType());

        config.options.put("addressList", "localhost:9200");
        config.options.put(ESRestClientFactory.TRUST_STORE_PATH_OPT, keystoreFile.getAbsolutePath());
        config.options.put(ESRestClientFactory.TRUST_STORE_PASSWORD_OPT, password);
        config.options.put(ESRestClientFactory.TRUST_STORE_TYPE_OPT, null);
        config.options.put(ESRestClientFactory.KEY_STORE_PATH_OPT, keystoreFile.getAbsolutePath());
        config.options.put(ESRestClientFactory.KEY_STORE_PASSWORD_OPT, password);
        config.options.put(ESRestClientFactory.KEY_STORE_TYPE_OPT, null);
        ESRestClient esClient = (ESRestClient) factory.create(null, config);
        assertNotNull(esClient);
        //This would error if it couldn't open the keystore.

        keystoreFile.delete();
    }

    @Test
    public void testSslContext() throws Exception {
        ElasticSearchClientConfig config = new ElasticSearchClientConfig();

        String password = "mypass";
        String keystoreType = "pkcs12";
        File keystoreFile = getKeystoreFile(password, keystoreType);

        config.options.put("addressList", "localhost:9200");
        config.options.put(ESRestClientFactory.TRUST_STORE_PATH_OPT, keystoreFile.getAbsolutePath());
        config.options.put(ESRestClientFactory.TRUST_STORE_PASSWORD_OPT, password);
        config.options.put(ESRestClientFactory.TRUST_STORE_TYPE_OPT, keystoreType);
        config.options.put(ESRestClientFactory.KEY_STORE_PATH_OPT, keystoreFile.getAbsolutePath());
        config.options.put(ESRestClientFactory.KEY_STORE_PASSWORD_OPT, password);
        config.options.put(ESRestClientFactory.KEY_STORE_TYPE_OPT, keystoreType);
        ESRestClient esClient = (ESRestClient) factory.create(null, config);
        assertNotNull(esClient);
        //Its not possible to get a reference to check the configuration, but the absence of an error is itself a test.

        keystoreFile.delete();
    }

    /**
     * Sets up a temporary keystore
     */
    public File getKeystoreFile(String password, String keystoreType) throws Exception {
        File keystoreFile = Framework.createTempFile("keystore_", ".tmp");
        KeyStore ks = KeyStore.getInstance(keystoreType);
        ks.load(null, password.toCharArray());
        try (OutputStream os = new FileOutputStream(keystoreFile)) {
           ks.store(os, password.toCharArray());
        }
        return keystoreFile;
    }

}
