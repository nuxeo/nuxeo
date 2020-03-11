/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.adobe.cc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_TYPE_NAME;
import static org.nuxeo.ecm.restapi.server.jaxrs.QueryObject.ORDERED_PARAMS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, RepositoryElasticSearchFeature.class, CollectionFeature.class })
@Deploy("org.nuxeo.adobe.cc.nuxeo-adobe-connector-core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
@Deploy("org.nuxeo.ecm.platform.restapi.test:elasticsearch-test-contrib.xml")
public class TestPageProviders extends BaseTest {

    protected String testWorkspacePath;

    protected String testWorkspaceId;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature tf;

    @Before
    public void before() {
        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "test", "Workspace");
        doc = session.createDocument(doc);
        session.save();

        testWorkspacePath = doc.getPathAsString();
        testWorkspaceId = doc.getId();
    }

    @Test
    public void testAllImages() throws Exception {
        DocumentModel doc = session.createDocumentModel(testWorkspacePath, "foo", PICTURE_TYPE_NAME);
        doc = session.createDocument(doc);

        final DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-all-images", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("uid").asText()).isEqualTo(finalDoc.getId());
        });
    }

    @Test
    public void testBrowse() throws Exception {
        String rootId = session.getDocument(new PathRef("/default-domain/workspaces")).getId();

        testPageProvider("adobe-connector-browse", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("path").asText()).isEqualTo(testWorkspacePath);
        }, rootId);
    }

    @Test
    public void testOthers() throws Exception {
        DocumentModel doc = session.createDocumentModel(testWorkspacePath, "foo", PICTURE_TYPE_NAME);
        doc = session.createDocument(doc);

        CollectionManager cm = Framework.getService(CollectionManager.class);
        DocumentModel collection = cm.createCollection(session, "my-collec", "dummy", testWorkspacePath);
        cm.addToCollection(collection, doc, session);

        AtomicReference<String> collectionId = new AtomicReference<>();
        testPageProvider("adobe-connector-other_primary", (entries) -> {
            assertThat(entries).hasSize(1);
            collectionId.set(entries.get(0).get("uid").asText());
        });

        DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-other_secondary", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("uid").asText()).isEqualTo(finalDoc.getId());
        }, collectionId.get());
    }

    @Test
    public void testSearch() throws Exception {
        DocumentModel doc = session.createDocumentModel(testWorkspacePath, "foo", PICTURE_TYPE_NAME);
        doc.setPropertyValue("dc:description", "hello");
        session.createDocument(doc);

        doc = session.createDocumentModel(testWorkspacePath, "bar", PICTURE_TYPE_NAME);
        doc.setPropertyValue("dc:description", "world");
        doc = session.createDocument(doc);

        Map<String, String> params = new HashMap<>();
        params.put("system_fulltext", "world");
        params.put("system_parentId", testWorkspaceId);

        DocumentModel finalDoc = doc;
        testPageProvider("adobe-connector-search", (entries) -> {
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("uid").asText()).isEqualTo(finalDoc.getId());
        }, params);
    }

    protected void testPageProvider(String ppName, Consumer<List<JsonNode>> consumer, Map<String, String> qarams,
            String... parameters) throws Exception {
        tf.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put(ORDERED_PARAMS, Arrays.asList(parameters));
        qarams.forEach(queryParams::putSingle);

        try (CloseableClientResponse res = getResponse(RequestType.GET, "search/pp/" + ppName + "/execute",
                queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
            List<JsonNode> logEntries = getLogEntries(mapper.readTree(res.getEntityInputStream()));

            consumer.accept(logEntries);
        }
    }

    protected void testPageProvider(String ppName, Consumer<List<JsonNode>> consumer, String... parameters)
            throws Exception {
        testPageProvider(ppName, consumer, Collections.emptyMap(), parameters);
    }
}
