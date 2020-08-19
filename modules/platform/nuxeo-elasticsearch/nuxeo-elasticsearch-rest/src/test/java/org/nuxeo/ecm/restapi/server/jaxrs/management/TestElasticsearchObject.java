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
 *     Salem Aouana
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.restapi.server.jaxrs.management.ElasticsearchObject.GET_ALL_DOCUMENTS_QUERY;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryLightElasticSearchFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features(RepositoryLightElasticSearchFeature.class)
@Deploy("org.nuxeo.elasticsearch.rest")
public class TestElasticsearchObject extends ManagementBaseTest {

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void shouldRunIndexing() throws IOException {
        // Init indexes and drop if any
        esa.initIndexes(true);

        // Initial docs count
        long initialDocCount = coreSession.query(GET_ALL_DOCUMENTS_QUERY).totalSize();

        // Nothing indexed because the index was dropped
        assertEquals(0, ess.query(new NxQueryBuilder(coreSession).nxql(GET_ALL_DOCUMENTS_QUERY)).totalSize());

        // Create new documents without indexing them
        createDocuments();

        // Wait for an eventual indexing
        waitForIndexing();

        // Nothing indexed because of disable indexing flag
        assertEquals(0, ess.query(new NxQueryBuilder(coreSession).nxql(GET_ALL_DOCUMENTS_QUERY)).totalSize());

        // Start the ES indexing of all document of the coreSession repository
        try (CloseableClientResponse response = httpClientRule.post("/management/elasticsearch/reindex", null)) {
            verifyIndexingResponse(response, 3 + initialDocCount);
        }
    }

    @Test
    public void shouldRunIndexingByNXQLQuery() throws IOException {
        // Init indexes and drop if any
        esa.initIndexes(true);

        // Create new documents without indexing them
        createDocuments();

        String query = "SELECT * FROM Document WHERE dc:title LIKE 'Title of my-file%'";
        // Start the ES indexing of document that match the nxql query (2 files)
        try (CloseableClientResponse response = httpClientRule.post("/management/elasticsearch/reindex?query=" + query,
                null)) {
            verifyIndexingResponse(response, 2);
        }
    }

    @Test
    public void shouldRunIndexingOnDocumentAndItsChildren() throws IOException {
        // Init indexes and drop if any
        esa.initIndexes(true);

        // Create new documents without indexing them
        createDocuments();

        // Retrieve the Root document (our Folder) and build the resource path
        String docId = coreSession.getDocument(new PathRef("/default-domain/workspaces/Folder")).getId();

        // Start the ES indexing for a given document (folder) and its children (2 files)
        try (CloseableClientResponse response = httpClientRule.post("/management/elasticsearch/" + docId + "/reindex",
                null)) {
            verifyIndexingResponse(response, 3);
        }
    }

    @Test
    public void shouldFailRunningIndexingWhenRepositoryNotExists() {
        // Launch the ES indexing
        try (CloseableClientResponse response = httpClientRule.post(
                "/repo/unExistingRepository/management/elasticsearch/reindex", null)) {
            assertEquals(SC_NOT_FOUND, response.getStatus());
        }
    }

    @Test
    public void shouldRunFlushOnRepository() {
        try (CloseableClientResponse response = httpClientRule.post("/management/elasticsearch/flush", null)) {
            assertEquals(SC_NO_CONTENT, response.getStatus());
        }
    }

    @Test
    public void shouldRunOptimizeOnRepository() {
        try (CloseableClientResponse response = httpClientRule.post("/management/elasticsearch/optimize", null)) {
            assertEquals(SC_NO_CONTENT, response.getStatus());
        }
    }

    /**
     * Allows us to verify the indexing process.
     */
    protected void verifyIndexingResponse(CloseableClientResponse response, long expectedIndexedDocuments)
            throws IOException {
        assertEquals(SC_OK, response.getStatus());
        JsonNode jsonNode = mapper.readTree(response.getEntityInputStream());
        assertTrue(jsonNode.has("commandId"));
        assertTrue(jsonNode.has("state"));

        // Check the indexing status: at this step the indexing is launched but we are not sure about the exactly
        // value of its progress status
        assertEquals(SC_OK, response.getStatus());
        assertFalse(Arrays.asList("UNKNOWN", "ABORTED").contains(jsonNode.get("state").asText()));

        // Wait until the end of the ES indexing and then our expected indexed documents
        waitForIndexing();
        assertEquals(expectedIndexedDocuments,
                ess.query(new NxQueryBuilder(coreSession).nxql(GET_ALL_DOCUMENTS_QUERY)).totalSize());

    }

    /**
     * Creates documents, this method creates two files under a folder (3 documents)
     */
    protected void createDocuments() {
        DocumentModel folder = coreSession.createDocumentModel("/default-domain/workspaces/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "My Folder");
        folder.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        folder = coreSession.createDocument(folder);

        final String folderPath = folder.getPathAsString();
        Stream.of("my-file-1", "my-file-2").forEach(fileName -> {
            DocumentModel doc = coreSession.createDocumentModel(folderPath, fileName, "File");
            doc.setPropertyValue("dc:title", String.format("Title of %s", fileName));
            doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
            coreSession.createDocument(doc);
        });
        coreSession.save();
    }

    protected void waitForIndexing() {
        txFeature.nextTransaction();
        esa.refresh();
    }
}
