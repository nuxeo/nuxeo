/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.automation.test.HttpAutomationRequest.ENTITY_TYPE;
import static org.nuxeo.ecm.automation.test.HttpAutomationRequest.ENTITY_TYPE_DOCUMENT;
import static org.nuxeo.ecm.automation.test.HttpAutomationRequest.ENTITY_TYPE_EXCEPTION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.core.operations.services.query.ResultSetPaginatedQuery;
import org.nuxeo.ecm.automation.core.operations.traces.JsonStackToggleDisplayOperation;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationRequest;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractAutomationClientTest {

    public static final String VOID_OPERATION = "X-NXVoidOperation";

    public static final String DOCUMENT_PROPERTIES = "X-NXDocumentProperties";

    protected JsonNode automationTestFolder;

    @Inject
    HttpAutomationSession session;

    @Inject
    HttpAutomationClient client;

    public String getId(JsonNode node) {
        return node.get("uid").asText();
    }

    public String getTitle(JsonNode node) {
        return node.get("title").asText();
    }

    public String getDescription(JsonNode node) {
        return getProperties(node).get("dc:description").asText();
    }

    public String getPath(JsonNode node) {
        return node.get("path").asText();
    }

    public JsonNode getProperties(JsonNode node) {
        return node.get("properties");
    }

    public Object getDocRef(JsonNode node) {
        return Map.of(ENTITY_TYPE, ENTITY_TYPE_DOCUMENT, "uid", getId(node));
    }

    @Before
    public void setupTestFolder() throws IOException {
        JsonNode root = session.newRequest(FetchDocument.ID) //
                               .set("value", "/")
                               .executeReturningDocument();
        assertNotNull(root);
        assertEquals("/", getPath(root));
        automationTestFolder = session.newRequest(CreateDocument.ID)
                                      .setInput(root)
                                      .set("type", "Folder")
                                      .set("name", "automation-test-folder")
                                      .executeReturningDocument();
        assertNotNull(automationTestFolder);
    }

    @After
    public void tearDownTestFolder() throws IOException {
        session.newRequest(DeleteDocument.ID) //
               .setInput(automationTestFolder)
               .execute();
    }

    protected File newFile(String content) throws IOException {
        File file = Framework.createTempFile("automation-test-\u00e9\u00e1\u00f2-", ".xml");
        FileUtils.writeStringToFile(file, content, UTF_8);
        return file;
    }

    // ------ Tests comes here --------

    @Test
    public void testRemoteErrorHandling() throws Exception {
        // assert document removed
        session.newRequest(JsonStackToggleDisplayOperation.ID).execute();
        try {
            JsonNode node = session.newRequest(FetchDocument.ID) //
                                   .set("value", "/automation-test-folder/unexisting")
                                   .execute(SC_NOT_FOUND);
            assertEquals(ENTITY_TYPE_EXCEPTION, node.get(ENTITY_TYPE).asText());
            String cause = node.get("exception").get("className").asText();
            assertEquals(DocumentNotFoundException.class.getName(), cause);
        } finally {
            session.newRequest(JsonStackToggleDisplayOperation.ID).execute();
        }
    }

    @Test
    public void testErrorDueToInvalidJson() throws IOException {
        Function<HttpAutomationRequest, HttpEntity> entityCorruptor = request -> {
            try {
                String body;
                try (InputStream stream = request.getBodyEntity().getContent()) {
                    body = IOUtils.toString(stream, UTF_8);
                }
                // corrupt body
                body = "{ foo " + body.substring(1);
                return new StringEntity(body, APPLICATION_JSON);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        String res = session.newRequest(FetchDocument.ID) //
                            .set("value", "/")
                            .executeRaw(SC_BAD_REQUEST, entityCorruptor);
        JsonNode node = new ObjectMapper().readTree(res);
        assertEquals(ENTITY_TYPE_EXCEPTION, HttpAutomationRequest.getEntityType(node));
        String message = node.get("message").asText();
        assertTrue(message, message.contains("Unexpected character"));
    }

    @Test
    public void testGetCreateUpdateAndRemoveDocument() throws Exception {
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "myfolder")
                                 .set("properties", "dc:title=My Folder")
                                 .executeReturningDocument();

        assertNotNull(folder);
        assertEquals("/automation-test-folder/myfolder", getPath(folder));
        assertEquals("My Folder", getTitle(folder));

        // update folder properties
        folder = session.newRequest(UpdateDocument.ID)
                        .setHeader(DOCUMENT_PROPERTIES, "*")
                        .setInput(folder)
                        .set("properties", "dc:title=My Folder2\ndc:description=test")
                        .executeReturningDocument();

        assertNotNull(folder);
        assertEquals("/automation-test-folder/myfolder", getPath(folder));
        assertEquals("My Folder2", getTitle(folder));
        assertEquals("test", getDescription(folder));

        // remove folder
        session.newRequest(DeleteDocument.ID)//
               .setInput(folder)
               .execute();

        JsonNode folder1 = session.newRequest(CreateDocument.ID)
                                  .setInput(automationTestFolder)
                                  .set("type", "Folder")
                                  .set("name", "myfolder")
                                  .set("properties", "dc:title=My Folder")
                                  .executeReturningDocument();

        // remove folders
        session.newRequest(DeleteDocument.ID)//
               .setInput(List.of(folder1))
               .execute();

        // assert document removed
        String error = session.newRequest(FetchDocument.ID) //
                              .set("value", "/automation-test-folder/myfolder")
                              .executeReturningExceptionEntity(SC_NOT_FOUND);
        assertEquals("Failed to invoke operation: Repository.GetDocument, /automation-test-folder/myfolder", error);
    }

    /**
     * Test documents input / output
     */
    @Test
    public void testUpdateDocuments() throws Exception {
        // create a folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "docsInput")
                                 .set("properties", "dc:title=Query Test")
                                 .executeReturningDocument();
        // create 2 files
        JsonNode note1 = session.newRequest(CreateDocument.ID)
                                .setInput(folder)
                                .set("type", "Note")
                                .set("name", "note1")
                                .set("properties", "dc:title=Note1")
                                .executeReturningDocument();
        JsonNode note2 = session.newRequest(CreateDocument.ID)
                                .setInput(folder)
                                .set("type", "Note")
                                .set("name", "note2")
                                .set("properties", "dc:title=Note2")
                                .executeReturningDocument();

        List<JsonNode> docs = session.newRequest(UpdateDocument.ID)
                                     .setHeader(DOCUMENT_PROPERTIES, "*")
                                     .setInput(List.of(note1, note2))
                                     .set("properties", "dc:description=updated")
                                     .executeReturningDocuments();
        assertEquals(2, docs.size());
        // returned docs doesn't contains all properties.
        // TODO should we return all schemas?

        JsonNode doc = session.newRequest(FetchDocument.ID)
                              .setHeader(DOCUMENT_PROPERTIES, "*")
                              .set("value", getPath(note1))
                              .executeReturningDocument();
        assertEquals("updated", getDescription(doc));

        doc = session.newRequest(FetchDocument.ID)
                     .setHeader(DOCUMENT_PROPERTIES, "*")
                     .set("value", getPath(note2))
                     .executeReturningDocument();
        assertEquals("updated", getDescription(doc));
    }

    @Test
    public void testUpdateDocumentWithChangeToken() throws Exception {
        // create a doc
        JsonNode doc = session.newRequest(CreateDocument.ID)
                              .setInput(automationTestFolder)
                              .set("type", "Note")
                              .set("name", "note1")
                              .set("properties", "dc:title=Note1")
                              .executeReturningDocument();

        // we have a change token
        String changeToken = doc.get("changeToken").asText();
        assertNotNull(changeToken);

        // update with previous change token
        doc = session.newRequest(UpdateDocument.ID)
                     .setHeader(DOCUMENT_PROPERTIES, "*")
                     .setInput(doc)
                     .set("changeToken", changeToken)
                     .set("properties", "dc:title=Update 1")
                     .executeReturningDocument();
        assertEquals("Update 1", getTitle(doc));

        // update by simulating a system change in between (depends on change token internals)
        changeToken = doc.get("changeToken").asText();
        String[] parts = changeToken.split("-");
        String newChangeToken = (Long.parseLong(parts[0]) - 1) + "-" + parts[1];
        doc = session.newRequest(UpdateDocument.ID)
                     .setHeader(DOCUMENT_PROPERTIES, "*")
                     .setInput(doc)
                     .set("changeToken", newChangeToken)
                     .set("properties", "dc:title=Update 2")
                     .executeReturningDocument();
        assertEquals("Update 2", getTitle(doc));

        // failing update by passing an old/invalid change token
        String error = session.newRequest(UpdateDocument.ID)
                              .setHeader(DOCUMENT_PROPERTIES, "*")
                              .setInput(doc)
                              .set("changeToken", "9999-1234") // old/invalid change token
                              .set("properties", "dc:title=Update 3")
                              .executeReturningExceptionEntity(SC_CONFLICT);
        String expectedError = String.format(
                "Failed to invoke operation: Document.Update, Failed to invoke operation Document.Update, %s, Invalid change token",
                getId(doc));
        assertEquals(expectedError, error);

        // re-fetch
        doc = session.newRequest(FetchDocument.ID) //
                     .set("value", getPath(doc))
                     .execute();

        // check that the doc was not updated due to invalid change token
        assertEquals("Update 2", getTitle(doc));
    }

    @Test
    public void testNullProperties() throws Exception {
        JsonNode note = session.newRequest(CreateDocument.ID)
                               .setInput(automationTestFolder)
                               .set("type", "Note")
                               .set("name", "note1")
                               .set("properties", "dc:title=Note1")
                               .executeReturningDocument();
        note = session.newRequest(FetchDocument.ID)
                      .setHeader(DOCUMENT_PROPERTIES, "*")
                      .set("value", getPath(note))
                      .executeReturningDocument();

        JsonNode props = getProperties(note);
        assertTrue(props.get("dc:source").isNull());
    }

    /**
     * Test documents output - query and get children
     */
    @Test
    public void testQuery() throws Exception {
        // create a folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "queryTest")
                                 .set("properties", "dc:title=Query Test")
                                 .executeReturningDocument();
        // create 2 files
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note1")
               .set("properties", "dc:title=Note1")
               .executeReturningDocument();
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note2")
               .set("properties", "dc:title=Note2")
               .executeReturningDocument();

        // now query the two files
        List<JsonNode> docs = session.newRequest(DocumentPaginatedQuery.ID)
                                     .set("query",
                                             "SELECT * FROM Note WHERE ecm:path STARTSWITH '/automation-test-folder/queryTest' ")
                                     .executeReturningDocuments();
        assertEquals(2, docs.size());
        String title1 = getTitle(docs.get(0));
        String title2 = getTitle(docs.get(1));
        assertTrue(
                title1.equals("Note1") && title2.equals("Note2") || title1.equals("Note2") && title2.equals("Note1"));

        // now get children of /testQuery
        docs = session.newRequest(GetDocumentChildren.ID) //
                      .setInput(folder)
                      .executeReturningDocuments();
        assertEquals(2, docs.size());

        title1 = getTitle(docs.get(0));
        title2 = getTitle(docs.get(1));
        assertTrue(
                title1.equals("Note1") && title2.equals("Note2") || title1.equals("Note2") && title2.equals("Note1"));
    }

    @Test
    public void testQueryAndFetch() throws Exception {
        // create a folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "queryTest")
                                 .set("properties", "dc:title=Query Test")
                                 .executeReturningDocument();
        // create 2 files
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note1")
               .set("properties", "dc:title=Note1\ndc:description=Desc1")
               .executeReturningDocument();
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note2")
               .set("properties", "dc:title=Note2\ndc:description=Desc2")
               .executeReturningDocument();

        // now query the two files
        JsonNode node = session.newRequest(ResultSetPaginatedQuery.ID)
                               .set("query",
                                       "SELECT dc:title, ecm:uuid, dc:description FROM Note WHERE ecm:path STARTSWITH '/automation-test-folder/queryTest' order by dc:title ")
                               .execute();

        assertEquals("recordSet", node.get(ENTITY_TYPE).asText());
        JsonNode entries = node.get("entries");
        List<JsonNode> result = IteratorUtils.toList(entries.iterator());

        assertEquals(2, result.size());
        assertEquals("Note1", result.get(0).get("dc:title").asText());
        assertEquals("Note2", result.get(1).get("dc:title").asText());
        assertEquals("Desc1", result.get(0).get("dc:description").asText());
        assertEquals("Desc2", result.get(1).get("dc:description").asText());
    }

    /**
     * Tests blob input / output.
     */
    @Test
    public void testAttachAndGetFile() throws Exception {
        String filename = "test.xml";
        String mimeType = "text/xml";
        Blob fb = Blobs.createBlob("<doc>mydoc</doc>", mimeType, null, filename);
        // create a file
        session.newRequest(CreateDocument.ID)
               .setInput(automationTestFolder)
               .set("type", "File")
               .set("name", "myfile")
               .set("properties", "dc:title=My File")
               .execute();

        JsonNode res = session.newRequest("Blob.Attach")
                              .setHeader(VOID_OPERATION, "true")
                              .setInput(fb)
                              .set("document", "/automation-test-folder/myfile")
                              .execute();
        // test that output was avoided using X-NXVoidOperation
        assertNull(res);

        // get the file where blob was attached
        JsonNode doc = session.newRequest(FetchDocument.ID)
                              .setHeader(DOCUMENT_PROPERTIES, "*")
                              .set("value", "/automation-test-folder/myfile")
                              .execute();

        JsonNode map = doc.get("properties").get("file:content");
        assertEquals(filename, map.get("name").asText());
        assertEquals(mimeType, map.get("mime-type").asText());

        // get the data URL
        String url = map.get("data").asText();
        Blob blob = session.newRequest().getFile(url);
        assertNotNull(blob);
        assertEquals(filename, blob.getFilename());
        assertEquals(mimeType, blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", IOUtils.toString(blob.getStream(), UTF_8));

        // now test the GetBlob operation on the same blob
        blob = session.newRequest(GetDocumentBlob.ID) //
                      .setInput(doc)
                      .set("xpath", "file:content")
                      .executeReturningBlob();
        assertNotNull(blob);
        assertEquals(filename, blob.getFilename());
        assertEquals(mimeType, blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
    }

    /**
     * Test blobs input / output
     */
    @Test
    @Ignore("NXP-22652")
    public void testGetBlobs() throws Exception {
        // create a note
        session.newRequest(CreateDocument.ID)
               .setInput(automationTestFolder)
               .set("type", "Note")
               .set("name", "blobs")
               .set("properties", "dc:title=Blobs Test")
               .executeReturningDocument();
        // attach 2 files to that note
        Blob fb1 = Blobs.createBlob("<doc>mydoc1</doc>", "text/xml", null, "doc1.xml");
        Blob fb2 = Blobs.createBlob("<doc>mydoc2</doc>", "text/xml", null, "doc2.xml");
        // TODO attachblob cannot set multiple blobs at once.
        Blob blob = session.newRequest(AttachBlob.ID)
                           .setHeader(VOID_OPERATION, "true")
                           .setInput(fb1)
                           .set("document", "/automation-test-folder/blobs")
                           .set("xpath", "files:files")
                           .executeReturningBlob();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // attach second blob
        blob = session.newRequest(AttachBlob.ID)
                      .setHeader(VOID_OPERATION, "true")
                      .setInput(fb2)
                      .set("document", "/automation-test-folder/blobs")
                      .set("xpath", "files:files")
                      .executeReturningBlob();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // now retrieve the note with full schemas
        JsonNode note = session.newRequest(FetchDocument.ID)
                               .setHeader(DOCUMENT_PROPERTIES, "*")
                               .set("value", "/automation-test-folder/blobs")
                               .executeReturningDocument();

        JsonNode list = getProperties(note).get("files:files");
        assertEquals(2, list.size());

        JsonNode map = list.get(0).get("file");
        assertEquals("doc1.xml", map.get("name").asText());
        assertEquals("text/xml", map.get("mime-type").asText());

        // get the data URL
        String path = map.get("data").asText();
        blob = session.newRequest().getFile(path);
        assertNotNull(blob);
        assertEquals("doc1.xml", blob.getFilename());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc1</doc>", IOUtils.toString(blob.getStream(), "utf-8"));

        // the same for the second file
        map = list.get(1).get("file");
        assertEquals("doc2.xml", map.get("name").asText());
        assertEquals("text/xml", map.get("mime-type").asText());

        // get the data URL
        path = map.get("data").asText();
        blob = session.newRequest().getFile(path);
        assertNotNull(blob);
        assertEquals("doc2.xml", blob.getFilename());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc2</doc>", IOUtils.toString(blob.getStream(), "utf-8"));

        // now test the GetDocumentBlobs operation on the note document
        List<Blob> blobs = session.newRequest(GetDocumentBlobs.ID) //
                                  .setInput(note)
                                  .set("xpath", "files:files")
                                  .executeReturningBlobs();
        assertNotNull(blobs);
        assertEquals(2, blobs.size());

        // test first blob
        blob = blobs.get(0);
        assertEquals("doc1.xml", blob.getFilename());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc1</doc>", IOUtils.toString(blob.getStream(), "utf-8"));

        // test the second one
        blob = blobs.get(1);
        assertEquals("doc2.xml", blob.getFilename());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc2</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
    }

    /**
     * Upload blobs to create a zip and download it.
     */
    @Test
    public void testUploadBlobs() throws Exception {
        Blob blob1 = Blobs.createBlob("<doc>mydoc1</doc>", "text/xml", null, "doc1.xml");
        Blob blob2 = Blobs.createBlob("<doc>mydoc2</doc>", "text/xml", null, "doc2.xml");

        Blob zip = session.newRequest(CreateZip.ID)
                          .set("filename", "test.zip")
                          .setInput(List.of(blob1, blob2))
                          .executeReturningBlob();
        assertNotNull(zip);

        try (ZipFile zf = new ZipFile(zip.getFile())) {
            ZipEntry entry1 = zf.getEntry("doc1.xml");
            assertNotNull(entry1);
            ZipEntry entry2 = zf.getEntry("doc2.xml");
            assertNotNull(entry2);
        }
    }

    @Test
    public void queriesArePaginable() throws IOException {
        // create 1 folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "docsInput")
                                 .set("properties", "dc:title=Query Test")
                                 .executeReturningDocument();
        JsonNode[] notes = new JsonNode[15];
        // create 15 notes
        for (int i = 0; i < 14; i++) {
            notes[i] = session.newRequest(CreateDocument.ID)
                              .setInput(folder)
                              .set("type", "Note")
                              .set("name", "note" + i)
                              .set("properties", "dc:title=Note" + i)
                              .executeReturningDocument();
        }

        List<JsonNode> docs = session.newRequest(DocumentPaginatedQuery.ID)
                                     .set("query",
                                             "SELECT * from Document WHERE ecm:path STARTSWITH '/automation-test-folder/'")
                                     .executeReturningDocuments();

        JsonNode cursor = session.newRequest(DocumentPaginatedQuery.ID)
                                 .set("query",
                                         "SELECT * from Document WHERE ecm:path STARTSWITH '/automation-test-folder/'")
                                 .set("pageSize", 2)
                                 .execute();
        int pageSize = cursor.get("pageSize").asInt();
        int pageCount = cursor.get("numberOfPages").asInt();
        int totalSize = cursor.get("resultsCount").asInt();
        assertEquals(2, cursor.get("entries").size());
        int size = docs.size();
        assertEquals(size, totalSize);
        assertEquals(2, pageSize);
        assertEquals(size / 2 + size % 2, pageCount);
    }

    @Test
    public void testSetArrayProperty() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("dc:title", "My Test Folder");
        props.put("dc:description", "test");
        props.put("dc:subjects", "art,sciences,biology");
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setHeader(DOCUMENT_PROPERTIES, "*")
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "myfolder2")
                                 .set("properties", props)
                                 .executeReturningDocument();

        assertEquals("My Test Folder", getTitle(folder));
        assertEquals("test", getDescription(folder));
        JsonNode ar = getProperties(folder).get("dc:subjects");
        assertEquals(3, ar.size());
        assertEquals("art", ar.get(0).asText());
        assertEquals("sciences", ar.get(1).asText());
        assertEquals("biology", ar.get(2).asText());
    }

    @Test
    public void testBadAccess() throws Exception {
        String error = session.newRequest(FetchDocument.ID) //
                              .set("value", "/foo")
                              .executeReturningExceptionEntity(SC_NOT_FOUND);
        assertEquals("Failed to invoke operation: Repository.GetDocument, /foo", error);
    }

    @Test
    public void testLock() throws Exception {
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "myfolder")
                                 .set("properties", "dc:title=My Folder")
                                 .execute();

        // Getting the document
        JsonNode doc = session.newRequest(FetchDocument.ID)
                              .setHeader(DOCUMENT_PROPERTIES, "*")
                              .set("value", getPath(folder))
                              .execute();

        assertNull(doc.get("lockOwner"));

        session.newRequest(LockDocument.ID)//
               .setHeader(VOID_OPERATION, "*")
               .setInput(doc)
               .execute();

        doc = session.newRequest(FetchDocument.ID)
                     .setHeader(DOCUMENT_PROPERTIES, "*")
                     .setHeader("X-NXfetch.document", "lock")
                     .set("value", getPath(doc))
                     .execute();

        assertEquals("Administrator", doc.get("lockOwner").asText());
        assertNotNull(doc.get("lockCreated"));
    }

    @Test
    public void testEncoding() throws Exception {
        // Latin vowels with various French accents (avoid non-ascii literals in
        // java source code to avoid issues when working with developers who do
        // not configure there editor charset to UTF-8).
        String title = "\u00e9\u00e8\u00ea\u00eb\u00e0\u00e0\u00e4\u00ec\u00ee\u00ef\u00f9\u00fb\u00f9";
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput(automationTestFolder)
                                 .set("type", "Folder")
                                 .set("name", "myfolder")
                                 .set("properties", "dc:title=" + title)
                                 .executeReturningDocument();

        folder = session.newRequest(FetchDocument.ID)
                        .setHeader(DOCUMENT_PROPERTIES, "*")
                        .set("value", getPath(folder))
                        .execute();

        assertEquals(title, getTitle(folder));
    }

    /*
     * NXP-19835
     */
    @Test
    public void testGetEmptyBlobsList() throws Exception {
        // create a file
        JsonNode file = session.newRequest(CreateDocument.ID)
                               .setInput(automationTestFolder)
                               .set("type", "File")
                               .set("name", "blobs")
                               .set("properties", "dc:title=Blobs Test")
                               .executeReturningDocument();

        // Get blobs
        JsonNode node = session.newRequest(GetDocumentBlobs.ID) //
                               .setInput(file)
                               .execute(SC_NO_CONTENT);
        assertNull(node);
    }

}
