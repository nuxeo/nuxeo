/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests the REST API document endpoints with multiple repositories.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-multi-repository-contrib.xml")
@ServletContainer(port = 18090)
public class TestMultiRepository extends BaseTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession defaultRepositorySession;

    protected CloseableCoreSession otherRepositorySession;

    protected Map<String, CoreSession> sessions;

    @Before
    public void init() throws Exception {
        otherRepositorySession = CoreInstance.openCoreSession("other");
        sessions = Stream.of(defaultRepositorySession, otherRepositorySession)
                         .collect(Collectors.toMap(CoreSession::getRepositoryName, Function.identity()));
    }

    @After
    public void cleanUp() throws Exception {
        otherRepositorySession.removeChildren(new PathRef("/"));
        otherRepositorySession.close();
    }

    // Tests on the /api/v1/id/{docId} endpoint.
    @Test
    public void testPOST() {
        // default repository
        assertEquals(201, create("test", "newDoc1", "New Doc 1"));

        // other repository, without "X-NXRepository" header
        // expecting 404 as the repository cannot be guessed
        assertEquals(404, create("other", "newDoc1", "New Doc 1"));

        // other repository, with "X-NXRepository" header
        assertEquals(201, createWithRepoHeader("other", "newDoc1", "New Doc 1"));
    }

    @Test
    public void testGET() throws IOException {
        // default repository
        assertEquals(200, read("test"));

        // other repository, without "X-NXRepository" header
        // expecting 404 as the repository cannot be guessed
        assertEquals(404, read("other"));

        // other repository, with "X-NXRepository" header
        assertEquals(200, readWithRepoHeader("other"));
    }

    @Test
    public void testPUT() {
        // default repository, without "uid" field
        assertEquals(200, update("test", "Root Doc Update 1", false));

        // default repository, with "uid" field
        assertEquals(200, update("test", "Root Doc Update 2", true));

        // other repository, without "X-NXRepository" header, without "uid" field
        // expecting 404 as the repository cannot be guessed
        assertEquals(404, update("other", "Root Doc Update 1", false));

        // other repository, without "X-NXRepository" header, with "uid" field
        // expecting 404 as the repository cannot be guessed
        assertEquals(404, update("other", "Root Doc Update 2", true));

        // other repository, with "X-NXRepository" header, without "uid" field
        assertEquals(200, updateWithRepoHeader("other", "Root Doc Update 1", false));

        // other repository, with "X-NXRepository" header, with "uid" field
        assertEquals(200, updateWithRepoHeader("other", "Root Doc Update 2", true));
    }

    @Test
    public void testDELETE() throws IOException {
        // default repository
        assertEquals(204, delete("test"));

        // other repository, without "X-NXRepository" header
        // expecting 404 as the repository cannot be guessed
        assertEquals(404, delete("other"));

        // other repository, with "X-NXRepository" header
        assertEquals(204, deleteWithRepoHeader("other"));
    }

    // Tests on the /api/v1/repo/{repoId}/id/{docId} endpoint.
    @Test
    public void testPOSTWithRepoInPath() {
        // default repository
        assertEquals(201, createWithRepoInPath("test", "newDoc1", "New Doc 1"));

        // other repository
        assertEquals(201, createWithRepoInPath("other", "newDoc1", "New Doc 1"));
    }

    @Test
    public void testGETWithRepoInPath() throws IOException {
        // default repository
        assertEquals(200, readWithRepoInPath("test"));

        // other repository
        assertEquals(200, readWithRepoInPath("other"));
    }

    @Test
    public void testPUTWithRepoInPath() {
        // default repository, without "uid" field
        assertEquals(200, updateWithRepoInPath("test", "Root Doc Update 1", false));

        // default repository, with "uid" field
        assertEquals(200, updateWithRepoInPath("test", "Root Doc Update 2", true));

        // other repository, without "uid" field
        assertEquals(200, updateWithRepoInPath("other", "Root Doc Update 1", false));

        // other repository, with "uid" field
        assertEquals(200, updateWithRepoInPath("other", "Root Doc Update 2", true));
    }

    @Test
    public void testDELETEWithRepoInPath() throws IOException {
        // default repository
        assertEquals(204, deleteWithRepoInPath("test"));

        // other repository
        assertEquals(204, deleteWithRepoInPath("other"));
    }

    protected int create(String repoName, String name, String title) {
        return create(repoName, name, title, false, false);
    }

    protected int createWithRepoHeader(String repoName, String name, String title) {
        return create(repoName, name, title, false, true);
    }

    protected int createWithRepoInPath(String repoName, String name, String title) {
        return create(repoName, name, title, true, false);
    }

    /**
     * Creates a document with the given {@code name} and {@code title} in the root document of the repository with the
     * given {@code repoName} by executing a REST API POST request. Returns the HTTP response status code.
     * <p>
     * If {@code repoInPath} is true, uses the /repo/{repoId}/id/{docId} endpoint, else the /id/{docId} one.
     * <p>
     * If {@code repoHeader} is true, passes the {@link RenderingContext#REPOSITORY_NAME_REQUEST_HEADER} request header.
     */
    protected int create(String repoName, String name, String title, boolean repoInPath, boolean repoHeader) {
        CoreSession session = sessions.get(repoName);
        String rootDocumentId = session.getRootDocument().getId();
        String path = "id/" + rootDocumentId;
        if (repoInPath) {
            path = "repo/" + repoName + "/" + path;
        }
        String data = buildDocumentCreationJSON(name, title);
        Map<String, String> headers = repoHeader
                ? Collections.singletonMap(RenderingContext.REPOSITORY_NAME_REQUEST_HEADER, repoName)
                : Collections.emptyMap();
        try (CloseableClientResponse response = getResponse(RequestType.POST, path, data, headers)) {
            int status = response.getStatus();
            if (status == 201) {
                txFeature.nextTransaction();
                DocumentModel child = session.getChild(session.getRootDocument().getRef(), name);
                assertEquals(title, child.getTitle());
            }
            return status;
        }
    }

    protected int read(String repoName) throws IOException {
        return read(repoName, false, false);
    }

    protected int readWithRepoHeader(String repoName) throws IOException {
        return read(repoName, false, true);
    }

    protected int readWithRepoInPath(String repoName) throws IOException {
        return read(repoName, true, false);
    }

    /**
     * Reads the root document of the repository with the given {@code repoName} by executing a REST API GET request.
     * Returns the HTTP response status code.
     * <p>
     * If {@code repoInPath} is true, uses the /repo/{repoId}/id/{docId} endpoint, else the /id/{docId} one.
     * <p>
     * If {@code repoHeader} is true, passes the {@link RenderingContext#REPOSITORY_NAME_REQUEST_HEADER} request header.
     */
    protected int read(String repoName, boolean repoInPath, boolean repoHeader) throws IOException {
        CoreSession session = sessions.get(repoName);
        String rootDocumentId = session.getRootDocument().getId();
        String path = "id/" + rootDocumentId;
        if (repoInPath) {
            path = "repo/" + repoName + "/" + path;
        }
        Map<String, String> headers = repoHeader ? Collections.singletonMap("X-NXRepository", repoName)
                : Collections.emptyMap();
        try (CloseableClientResponse response = getResponse(RequestType.GET, path, headers)) {
            int status = response.getStatus();
            if (status == 200) {
                JsonNode node = mapper.readTree(response.getEntityInputStream());
                assertEquals(repoName, node.get("repository").textValue());
                assertEquals(rootDocumentId, node.get("uid").textValue());
                assertEquals("/", node.get("path").textValue());
            }
            return status;
        }
    }

    protected int update(String repoName, String title, boolean specifyUID) {
        return update(repoName, title, false, false, specifyUID);
    }

    protected int updateWithRepoHeader(String repoName, String title, boolean specifyUID) {
        return update(repoName, title, false, true, specifyUID);
    }

    protected int updateWithRepoInPath(String repoName, String title, boolean specifyUID) {
        return update(repoName, title, true, false, specifyUID);
    }

    /**
     * Updates the root document of the repository with the given {@code repoName} with the given {@code title} by
     * executing a REST API PUT request. Returns the HTTP response status code.
     * <p>
     * If {@code repoInPath} is true, uses the /repo/{repoId}/id/{docId} endpoint, else the /id/{docId} one.
     * <p>
     * If {@code repoHeader} is true, passes the {@link RenderingContext#REPOSITORY_NAME_REQUEST_HEADER} request header.
     * <p>
     * If {@code specifyUID} is true, passes the "uid" field in the JSON request data.
     */
    protected int update(String repoName, String title, boolean repoInPath, boolean repoHeader, boolean specifyUID) {
        CoreSession session = sessions.get(repoName);
        String rootDocumentId = session.getRootDocument().getId();
        String path = "id/" + rootDocumentId;
        if (repoInPath) {
            path = "repo/" + repoName + "/" + path;
        }
        String data = buildDocumentUpdateJSON(title, specifyUID ? rootDocumentId : null);
        Map<String, String> headers = repoHeader ? Collections.singletonMap("X-NXRepository", repoName)
                : Collections.emptyMap();
        try (CloseableClientResponse response = getResponse(RequestType.PUT, path, data, headers)) {
            int status = response.getStatus();
            if (status == 200) {
                txFeature.nextTransaction();
                assertEquals(title, session.getRootDocument().getTitle());
            }
            return status;
        }
    }

    protected int delete(String repoName) throws IOException {
        return delete(repoName, false, false);
    }

    protected int deleteWithRepoHeader(String repoName) throws IOException {
        return delete(repoName, false, true);
    }

    protected int deleteWithRepoInPath(String repoName) throws IOException {
        return delete(repoName, true, false);
    }

    /**
     * Creates a document in the root document of the repository with the given {@code repoName} and deletes it
     * afterwards by executing a REST API DELETE request. Returns the HTTP response status code.
     * <p>
     * If {@code repoInPath} is true, uses the /repo/{repoId}/id/{docId} endpoint, else the /id/{docId} one.
     * <p>
     * If {@code repoHeader} is true, passes the {@link RenderingContext#REPOSITORY_NAME_REQUEST_HEADER} request header.
     */
    protected int delete(String repoName, boolean repoInPath, boolean repoHeader) throws IOException {
        CoreSession session = sessions.get(repoName);
        // first create a document to be deleted afterwards
        String docId = session.createDocument(session.createDocumentModel("/", "docToDelete", "File")).getId();
        txFeature.nextTransaction();
        String path = "id/" + docId;
        if (repoInPath) {
            path = "repo/" + repoName + "/" + path;
        }
        Map<String, String> headers = repoHeader ? Collections.singletonMap("X-NXRepository", repoName)
                : Collections.emptyMap();
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, path, headers)) {
            int status = response.getStatus();
            if (status == 204) {
                txFeature.nextTransaction();
                assertFalse(session.exists(new IdRef(docId)));
            }
            return status;
        }
    }

    protected String buildDocumentCreationJSON(String name, String title) {
        String json = "{";
        json += "  \"entity-type\": \"document\",";
        json += "  \"name\": \"" + name + "\",";
        json += "  \"type\": \"File\",";
        json += "  \"properties\": {";
        json += "    \"dc:title\": \"" + title + "\"";
        json += "  }";
        json += "}";
        return json;
    }

    protected String buildDocumentUpdateJSON(String title, String uid) {
        String json = "{";
        json += "  \"entity-type\": \"document\",";
        if (uid != null) {
            json += "  \"uid\": \"" + uid + "\",";
        }
        json += "  \"properties\": {";
        json += "    \"dc:title\": \"" + title + "\"";
        json += "  }";
        json += "}";
        return json;
    }

}
