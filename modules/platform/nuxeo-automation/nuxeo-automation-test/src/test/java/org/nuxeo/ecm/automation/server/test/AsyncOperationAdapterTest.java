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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static junit.framework.TestCase.assertEquals;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction;
import org.nuxeo.ecm.automation.core.operations.services.bulk.BulkRunAction;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.server.test.actions.DummyExposeBlobAction;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.automation.test:operation-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.test:test-bindings.xml")
@Deploy("org.nuxeo.ecm.automation.test.test:test-page-provider.xml")
@Deploy("org.nuxeo.ecm.automation.test.test:test-expose-blob-action.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class AsyncOperationAdapterTest {

    public static final String VOID_OPERATION = "X-NXVoidOperation";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    protected HttpAutomationClient automationClient;

    @Inject
    protected HttpAutomationSession session;

    protected HttpAutomationSession async;

    @Inject
    public TransactionalFeature txFeature;

    public String getDocRef(JsonNode node) {
        return "doc:" + node.get("uid").asText();
    }

    public String getTitle(JsonNode node) {
        return node.get("title").asText();
    }

    public String getPath(JsonNode node) {
        return node.get("path").asText();
    }

    @Before
    public void setUp() throws IOException {
        async = automationClient.getSession(ADMINISTRATOR, ADMINISTRATOR);
        async.setAsync(true);
    }

    @Test
    public void testAsyncOperation() throws Exception {

        boolean b = async.newRequest(ReturnOperation.ID) //
                         .setInput(Boolean.TRUE)
                         .executeReturningBooleanEntity();
        assertTrue(b);

        // Document
        JsonNode node = async.newRequest(FetchDocument.ID) //
                             .set("value", "/")
                             .executeReturningDocument();
        assertNotNull(node);

        // Documents
        List<JsonNode> nodes = async.newRequest(DocumentPaginatedQuery.ID)
                                    .set("query", "SELECT * from Document")
                                    .executeReturningDocuments();
        assertNotNull(nodes);

        // Blob
        JsonNode root = session.newRequest(FetchDocument.ID) //
                               .set("value", "/")
                               .executeReturningDocument();

        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(root)
                                 .set("type", "Folder")
                                 .set("name", "asyncTest")
                                 .executeReturningDocument();

        JsonNode file = session.newRequest(CreateDocument.ID)
                               .setInput(folder)
                               .set("type", "File")
                               .set("name", "blobs")
                               .executeReturningDocument();

        Blob fb = Blobs.createBlob("<doc>mydoc1</doc>", "text/xml", null, "file.xml");
        session.newRequest(AttachBlob.ID)
               .setHeader(VOID_OPERATION, "true")
               .setInput(fb)
               .set("document", getPath(file))
               .execute();

        Blob blob = async.newRequest(GetDocumentBlob.ID) //
                         .setInput(getDocRef(file))
                         .executeReturningBlob();
        assertNotNull(blob);
    }

    @Test
    public void testAsyncChain() throws Exception {
        // create a folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput("doc:/")
                                 .set("type", "Folder")
                                 .set("name", "chainTest")
                                 .executeReturningDocument();

        JsonNode doc = async.newRequest("testchain") //
                            .setInput(folder)
                            .executeReturningDocument();
        assertEquals("/chainTest/chain.doc", getPath(doc));
        assertEquals("Note", doc.get("type").asText());
    }

    @Test
    public void testError() throws Exception {
        String r = session.newRequest("Test.Exit") //
                          .setInput("Error")
                          .executeReturningStringEntity();
        assertEquals("Error", r);

        String error = async.newRequest("Test.Exit") //
                            .set("error", true)
                            .set("rollback", true)
                            .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation Test.Exit", error);
    }

    @Test
    public void testAsyncBulkAction() throws Exception {
        // create a folder
        session.newRequest(CreateDocument.ID)
               .setInput("doc:/")
               .set("type", "Folder")
               .set("name", "test")
               .executeReturningDocument();

        // param for the automation operation
        HashMap<String, Serializable> automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=foo");

        // param for the automation bulk action
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, automationParams);

        JsonNode r = async.newRequest(BulkRunAction.ID)
                          .set("action", AutomationBulkAction.ACTION_NAME)
                          .set("query", "SELECT * FROM Folder")
                          .set("bucketSize", "10")
                          .set("batchSize", "5")
                          .set("parameters", MAPPER.writeValueAsString(actionParams))
                          .execute();
        assertNotNull(r);

        txFeature.nextTransaction();

        JsonNode folder = session.newRequest(FetchDocument.ID) //
                                 .set("value", "/test")
                                 .executeReturningDocument();

        assertEquals("foo", getTitle(folder));
    }

    /**
     * @since 11.1
     */
    @Test
    public void testAsyncExposeBlob() throws Exception {
        JsonNode r = async.newRequest(BulkRunAction.ID)
                .set("action", DummyExposeBlobAction.ACTION_NAME)
                .set("query", "SELECT * FROM Folder")
                .execute();
        assertNotNull(r);
        assertNotNull(r.get("url"));
        assertTrue(new URI(r.get("url").asText()).isAbsolute());
    }

    /**
     * @since 11.1
     */
    @Test
    public void testAsyncBulkActionWithPPWhereClause() throws IOException {
        testAsyncBulkActionWithPP("QuickFilterPageProvider");
    }

    /**
     * @since 11.1
     */
    @Test
    public void testAsyncBulkActionWithPPPattern() throws IOException {
        testAsyncBulkActionWithPP("PageProvider");
    }

    protected void testAsyncBulkActionWithPP(String pageProviderName) throws IOException {
        // create a folder and a file
        session.newRequest(CreateDocument.ID)
               .setInput("doc:/")
               .set("type", "Folder")
               .set("name", "test")
               .executeReturningDocument();
        session.newRequest(CreateDocument.ID)
               .setInput("doc:/test")
               .set("type", "File")
               .set("name", "file")
               .executeReturningDocument();

        // param for the automation operation
        Map<String, Serializable> automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=foo");

        // param for the automation bulk action -> page provider with quick filter to only hit File document
        Map<String, Serializable> actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, (Serializable) automationParams);

        JsonNode r = async.newRequest(BulkRunAction.ID)
                          .set("action", AutomationBulkAction.ACTION_NAME)
                          .set("providerName", pageProviderName)
                          .set("quickFilters", "FileOnly")
                          .set("bucketSize", "10")
                          .set("batchSize", "5")
                          .set("parameters", MAPPER.writeValueAsString(actionParams))
                          .execute();
        assertNotNull(r);

        txFeature.nextTransaction();

        // expect only File doc to be modified
        JsonNode folder = session.newRequest(FetchDocument.ID) //
                                 .set("value", "/test")
                                 .executeReturningDocument();
        JsonNode file = session.newRequest(FetchDocument.ID) //
                               .set("value", "/test/file")
                               .executeReturningDocument();

        assertEquals("test", getTitle(folder));
        assertEquals("foo", getTitle(file));
    }

}
