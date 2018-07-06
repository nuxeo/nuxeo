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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

/**
 * Low level test on ESClient
 *
 * @since 10.2
 */
public abstract class TestESClient {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    protected static ElasticSearchEmbeddedNode embeddedNode;

    protected ESClient client;

    public abstract ESClient createClient(ElasticSearchEmbeddedNode embeddedNode);

    protected static void createEmbeddedNode() throws IOException {
        ElasticSearchEmbeddedServerConfig config = new ElasticSearchEmbeddedServerConfig();
        config.setHttpEnabled(true);
        config.setClusterName("nuxeoESClientCluster");
        config.setIndexStorageType("mmapfs");
        config.setNodeName("nuxeoESClientTestNode");
        config.setDataPath(folder.newFolder().getAbsolutePath());
        config.setHomePath(folder.newFolder().getAbsolutePath());
        embeddedNode = new ElasticSearchEmbeddedNode(config);
        embeddedNode.start();
    }

    @BeforeClass
    public static void initEmbeddedNode() throws IOException {
        createEmbeddedNode();
    }

    @AfterClass
    public static void stopEmbeddedNode() throws IOException {
        if (embeddedNode != null) {
            embeddedNode.close();
        }
        embeddedNode = null;
    }

    @Before
    public void initESClient() {
        client = createClient(embeddedNode);
    }

    @After
    public void closeESClient() throws Exception {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void testIndexOptimisticConcurrency() {
        // when using external version, indexing command in disorder should be rejected
        IndexRequest request1 = new IndexRequest("foo-index", "_doc", "123");
        request1.versionType(VersionType.EXTERNAL).version(100);
        request1.source("{\"foo\": \"v1\"}", XContentType.JSON);

        IndexRequest request2 = new IndexRequest("foo-index", "_doc", "123");
        request2.versionType(VersionType.EXTERNAL).version(200);
        request2.source("{\"foo\": \"v2\"}", XContentType.JSON);

        // index version 2 of the document
        IndexResponse response = client.index(request2);
        assertEquals(request2.version(), response.getVersion());

        try {
            // now index an older version of the document
            client.index(request1);
            fail("index command in disorder should raise exception");
        } catch (ConcurrentUpdateException e) {
            // expected
        }

        // confirms that the version 2 is the indexed version
        GetResponse ret = client.get(new GetRequest("foo-index", "_doc", "123"));
        assertEquals(request2.version(), ret.getVersion());
    }

    // TODO: do check all API of ESClient
}
