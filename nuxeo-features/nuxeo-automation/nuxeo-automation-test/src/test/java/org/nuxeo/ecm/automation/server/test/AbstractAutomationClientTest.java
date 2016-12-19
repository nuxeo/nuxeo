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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.RemoteThrowable;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.DateUtils;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.DocRefs;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.client.model.RecordSet;
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
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.ResultSetPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.server.test.UploadFileSupport.DigestMockInputStream;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractAutomationClientTest {

    protected Document automationTestFolder;

    @Inject
    Session session;

    @Inject
    HttpAutomationClient client;

    @Before
    public void setupTestFolder() throws Exception {
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        assertNotNull(root);
        assertEquals("/", root.getPath());
        automationTestFolder = (Document) session.newRequest(CreateDocument.ID)
                                                 .setInput(root)
                                                 .set("type", "Folder")
                                                 .set("name", "automation-test-folder")
                                                 .execute();
        assertNotNull(automationTestFolder);
    }

    @After
    public void tearDownTestFolder() throws Exception {
        session.newRequest(DeleteDocument.ID).setInput(automationTestFolder).execute();
    }

    protected File newFile(String content) throws IOException {
        File file = Framework.createTempFile("automation-test-\u00e9\u00e1\u00f2-", ".xml");
        FileUtils.writeFile(file, content);
        return file;
    }

    // ------ Tests comes here --------

    @Test
    public void testInvalidLogin() throws Exception {
        try {
            client.getSession("foo", "bar");
            fail("login is supposed to fail");
        } catch (RemoteException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void testRemoteErrorHandling() throws Exception {
        // assert document removed
        try {
            session.newRequest(FetchDocument.ID).set("value", "/automation-test-folder/unexisting").execute();
            fail("request is supposed to return 404");
        } catch (RemoteException e) {
            Throwable remoteCause = e.getRemoteCause();
            assertEquals(404, e.getStatus());
            assertThat(remoteCause, is(notNullValue()));
            final StackTraceElement[] remoteStack = remoteCause.getStackTrace();
            assertThat(remoteStack, is(notNullValue()));
            Boolean rollback = ((RemoteThrowable) remoteCause).getOtherNodes().get("rollback").getBooleanValue();
            assertThat(rollback, is(Boolean.TRUE));
            while (remoteCause.getCause() != remoteCause && remoteCause.getCause() != null) {
                remoteCause = remoteCause.getCause();
            }
            String className = ((RemoteThrowable) remoteCause).getOtherNodes().get("className").getTextValue();
            assertThat(className, is(DocumentNotFoundException.class.getName()));
        }
    }

    @Test
    public void testGetCreateUpdateAndRemoveDocument() throws Exception {
        // HttpAutomationClient client = new HttpAutomationClient();
        // client.connect("http://localhost:18080/automation");
        // Session cs = client.getSession("Administrator", "Administrator");

        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "myfolder")
                                            .set("properties", "dc:title=My Folder")
                                            .execute();

        assertNotNull(folder);
        assertEquals("/automation-test-folder/myfolder", folder.getPath());
        assertEquals("My Folder", folder.getTitle());

        // update folder properties
        folder = (Document) session.newRequest(UpdateDocument.ID)
                                   .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                   .setInput(folder)
                                   .set("properties", "dc:title=My Folder2\ndc:description=test")
                                   .execute();

        assertNotNull(folder);
        assertEquals("/automation-test-folder/myfolder", folder.getPath());
        assertEquals("My Folder2", folder.getTitle());
        assertEquals("test", folder.getProperties().getString("dc:description"));

        // remove folder
        session.newRequest(DeleteDocument.ID).setInput(folder).execute();

        Document folder1 = (Document) session.newRequest(CreateDocument.ID)
                                             .setInput(automationTestFolder)
                                             .set("type", "Folder")
                                             .set("name", "myfolder")
                                             .set("properties", "dc:title=My Folder")
                                             .execute();
        Documents folders = new Documents();
        folders.add(folder1);

        // remove folders
        session.newRequest(DeleteDocument.ID).setInput(folders).execute();

        // assert document removed
        try {
            session.newRequest(FetchDocument.ID).set("value", "/automation-test-folder/myfolder").execute();
            fail("request is suposed to return 404");
        } catch (RemoteException e) {
            assertEquals(404, e.getStatus());
        }
    }

    /**
     * Test documents input / output
     */
    @Test
    public void testUpdateDocuments() throws Exception {
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "docsInput")
                                            .set("properties", "dc:title=Query Test")
                                            .execute();
        // create 2 files
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note1")
               .set("properties", "dc:title=Note1")
               .execute();
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note2")
               .set("properties", "dc:title=Note2")
               .execute();

        DocRefs refs = new DocRefs();
        refs.add(new DocRef("/automation-test-folder/docsInput/note1"));
        refs.add(new DocRef("/automation-test-folder/docsInput/note2"));
        Documents docs = (Documents) session.newRequest(UpdateDocument.ID)
                                            .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                            .setInput(refs)
                                            .set("properties", "dc:description=updated")
                                            .execute();
        assertEquals(2, docs.size());
        // returned docs doesn't contains all properties.
        // TODO should we return all schemas?

        Document doc = (Document) session.newRequest(FetchDocument.ID)
                                         .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                         .set("value", "/automation-test-folder/docsInput/note1")
                                         .execute();
        assertEquals("updated", doc.getString("dc:description"));

        doc = (Document) session.newRequest(FetchDocument.ID)
                                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                .set("value", "/automation-test-folder/docsInput/note2")
                                .execute();
        assertEquals("updated", doc.getString("dc:description"));

        String now = DateUtils.formatDate(new Date());
        doc = (Document) session.newRequest(UpdateDocument.ID)
                                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                .setInput(new DocRef("/automation-test-folder/docsInput/note1"))
                                .set("properties", "dc:valid=" + now)
                                .execute();
        // TODO this test will not work if the client date writer and the server
        // date writer
        // are encoding differently the date (for instance the client add the
        // milliseconds field but the server not)
        // should instead compare date objects up to the second field.
        assertThat(doc.getDate("dc:valid"), is(DateUtils.parseDate(now)));
    }

    @Test
    public void testNullProperties() throws Exception {
        Document note = (Document) session.newRequest(CreateDocument.ID)
                                          .setInput(automationTestFolder)
                                          .set("type", "Note")
                                          .set("name", "note1")
                                          .set("properties", "dc:title=Note1")
                                          .execute();
        note = (Document) session.newRequest(FetchDocument.ID)
                                 .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                 .set("value", note.getPath())
                                 .execute();

        PropertyMap props = note.getProperties();
        assertTrue(props.getKeys().contains("dc:source"));
        assertNull(note.getString("dc:source"));
    }

    /**
     * Test documents output - query and get children
     */
    @Test
    public void testQuery() throws Exception {
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "queryTest")
                                            .set("properties", "dc:title=Query Test")
                                            .execute();
        // create 2 files
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note1")
               .set("properties", "dc:title=Note1")
               .execute();
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note2")
               .set("properties", "dc:title=Note2")
               .execute();

        // now query the two files
        Documents docs = (Documents) session.newRequest(DocumentPaginatedQuery.ID)
                                            .set("query",
                                                    "SELECT * FROM Note WHERE ecm:path STARTSWITH '/automation-test-folder/queryTest' ")
                                            .execute();
        assertEquals(2, docs.size());
        String title1 = docs.get(0).getTitle();
        String title2 = docs.get(1).getTitle();
        assertTrue(
                title1.equals("Note1") && title2.equals("Note2") || title1.equals("Note2") && title2.equals("Note1"));

        // now get children of /testQuery
        docs = (Documents) session.newRequest(GetDocumentChildren.ID).setInput(folder).execute();
        assertEquals(2, docs.size());

        title1 = docs.get(0).getTitle();
        title2 = docs.get(1).getTitle();
        assertTrue(
                title1.equals("Note1") && title2.equals("Note2") || title1.equals("Note2") && title2.equals("Note1"));

    }

    @Test
    public void testQueryAndFetch() throws Exception {
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "queryTest")
                                            .set("properties", "dc:title=Query Test")
                                            .execute();
        // create 2 files
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note1")
               .set("properties", "dc:title=Note1\ndc:description=Desc1")
               .execute();
        session.newRequest(CreateDocument.ID)
               .setInput(folder)
               .set("type", "Note")
               .set("name", "note2")
               .set("properties", "dc:title=Note2\ndc:description=Desc2")
               .execute();

        // now query the two files
        RecordSet result = (RecordSet) session.newRequest(ResultSetPageProviderOperation.ID)
                                              .set("query",
                                                      "SELECT dc:title, ecm:uuid, dc:description FROM Note WHERE ecm:path STARTSWITH '/automation-test-folder/queryTest' order by dc:title ")
                                              .execute();

        assertEquals(2, result.size());
        String title1 = (String) result.get(0).get("dc:title");
        String title2 = (String) result.get(1).get("dc:title");

        assertTrue(title1.equals("Note1") && title2.equals("Note2"));

        String desc1 = (String) result.get(0).get("dc:description");
        String desc2 = (String) result.get(1).get("dc:description");

        assertTrue(desc1.equals("Desc1") && desc2.equals("Desc2"));

    }

    /**
     * Tests blob input / output.
     */
    @Test
    public void testAttachAndGetFile() throws Exception {
        File file = newFile("<doc>mydoc</doc>");
        String filename = file.getName();
        FileBlob fb = new FileBlob(file);
        // TODO the next line is not working as expected - the file.getName()
        // will be used instead
        // fb.setFileName("test.xml");
        fb.setMimeType("text/xml");
        // create a file
        session.newRequest(CreateDocument.ID)
               .setInput(automationTestFolder)
               .set("type", "File")
               .set("name", "myfile")
               .set("properties", "dc:title=My File")
               .execute();

        FileBlob blob = (FileBlob) session.newRequest("Blob.Attach")
                                          .setHeader(Constants.HEADER_NX_VOIDOP, "true")
                                          .setInput(fb)
                                          .set("document", "/automation-test-folder/myfile")
                                          .execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // get the file where blob was attached
        Document doc = (Document) session.newRequest(DocumentService.FetchDocument)
                                         .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                         .set("value", "/automation-test-folder/myfile")
                                         .execute();

        PropertyMap map = doc.getProperties().getMap("file:content");
        assertEquals(filename, map.getString("name"));
        assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        assertNotNull(blob);
        assertEquals(filename, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();

        // now test the GetBlob operation on the same blob
        blob = (FileBlob) session.newRequest(GetDocumentBlob.ID).setInput(doc).set("xpath", "file:content").execute();
        assertNotNull(blob);
        assertEquals(filename, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();
    }

    /**
     * Test blobs input / output
     */
    @Test
    public void testGetBlobs() throws Exception {
        // create a note
        Document note = (Document) session.newRequest(CreateDocument.ID)
                                          .setInput(automationTestFolder)
                                          .set("type", "Note")
                                          .set("name", "blobs")
                                          .set("properties", "dc:title=Blobs Test")
                                          .execute();
        // attach 2 files to that note
        File file1 = newFile("<doc>mydoc1</doc>");
        File file2 = newFile("<doc>mydoc2</doc>");
        String filename1 = file1.getName();
        String filename2 = file2.getName();
        FileBlob fb1 = new FileBlob(file1);
        fb1.setMimeType("text/xml");
        FileBlob fb2 = new FileBlob(file2);
        fb2.setMimeType("text/xml");
        // TODO attachblob cannot set multiple blobs at once.
        Blobs blobs = new Blobs();
        // blobs.add(fb1);
        // blobs.add(fb2);
        FileBlob blob = (FileBlob) session.newRequest(AttachBlob.ID)
                                          .setHeader(Constants.HEADER_NX_VOIDOP, "true")
                                          .setInput(fb1)
                                          .set("document", "/automation-test-folder/blobs")
                                          .set("xpath", "files:files")
                                          .execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // attach second blob
        blob = (FileBlob) session.newRequest(AttachBlob.ID)
                                 .setHeader(Constants.HEADER_NX_VOIDOP, "true")
                                 .setInput(fb2)
                                 .set("document", "/automation-test-folder/blobs")
                                 .set("xpath", "files:files")
                                 .execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // now retrieve the note with full schemas
        note = (Document) session.newRequest(DocumentService.FetchDocument)
                                 .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                 .set("value", "/automation-test-folder/blobs")
                                 .execute();

        PropertyList list = note.getProperties().getList("files:files");
        assertEquals(2, list.size());

        PropertyMap map = list.getMap(0).getMap("file");
        assertEquals(filename1, map.getString("name"));
        assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        assertNotNull(blob);
        assertEquals(filename1, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc1</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();

        // the same for the second file
        map = list.getMap(1).getMap("file");
        assertEquals(filename2, map.getString("name"));
        assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        assertNotNull(blob);
        assertEquals(filename2, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc2</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();

        // now test the GetDocumentBlobs operation on the note document
        blobs = (Blobs) session.newRequest(GetDocumentBlobs.ID).setInput(note).set("xpath", "files:files").execute();
        assertNotNull(blob);
        assertEquals(2, blobs.size());

        // test first blob
        blob = (FileBlob) blobs.get(0);
        assertEquals(filename1, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc1</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();

        // test the second one
        blob = (FileBlob) blobs.get(1);
        assertEquals(filename2, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc2</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
        blob.getFile().delete();
    }

    /**
     * Upload blobs to create a zip and download it.
     */
    @Test
    public void testUploadBlobs() throws Exception {
        File file1 = newFile("<doc>mydoc1</doc>");
        File file2 = newFile("<doc>mydoc2</doc>");
        String filename1 = file1.getName();
        String filename2 = file2.getName();
        FileBlob fb1 = new FileBlob(file1);
        fb1.setMimeType("text/xml");
        FileBlob fb2 = new FileBlob(file2);
        fb2.setMimeType("text/xml");
        Blobs blobs = new Blobs();
        blobs.add(fb1);
        blobs.add(fb2);

        FileBlob zip = (FileBlob) session.newRequest(CreateZip.ID)
                                         .set("filename", "test.zip")
                                         .setInput(blobs)
                                         .execute();
        assertNotNull(zip);

        try (ZipFile zf = new ZipFile(zip.getFile())) {
            ZipEntry entry1 = zf.getEntry(filename1);
            assertNotNull(entry1);
            ZipEntry entry2 = zf.getEntry(filename2);
            assertNotNull(entry2);
            zip.getFile().delete();
        }
    }

    @Test
    public void testUploadSmallFile() throws Exception {
        DigestMockInputStream source = new DigestMockInputStream(100);
        FileInputStream in = new UploadFileSupport(session, automationTestFolder.getPath()).testUploadFile(source);
        byte[] sentSum = source.digest.digest();
        while (in.available() > 0) {
            source.digest.update((byte) in.read());
        }
        byte[] receivedSum = source.digest.digest();
        assertTrue("Expected (sent) bytes array: " + Arrays.toString(sentSum) + " - Actual (received) bytes array: "
                + Arrays.toString(receivedSum), MessageDigest.isEqual(sentSum, receivedSum));
    }

    @Test
    public void queriesArePaginable() throws Exception {
        // craete 1 folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "docsInput")
                                            .set("properties", "dc:title=Query Test")
                                            .execute();
        Document[] notes = new Document[15];
        // create 15 notes
        for (int i = 0; i < 14; i++) {
            notes[i] = (Document) session.newRequest(CreateDocument.ID)
                                         .setInput(folder)
                                         .set("type", "Note")
                                         .set("name", "note" + i)
                                         .set("properties", "dc:title=Note" + i)
                                         .execute();
        }

        Documents docs = (Documents) session.newRequest(DocumentPaginatedQuery.ID)
                                            .set("query",
                                                    "SELECT * from Document WHERE ecm:path STARTSWITH '/automation-test-folder/'")
                                            .execute();

        PaginableDocuments cursor = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                                .set("query",
                                                                        "SELECT * from Document WHERE ecm:path STARTSWITH '/automation-test-folder/'")
                                                                .set("pageSize", 2)
                                                                .execute();
        final int pageSize = cursor.getPageSize();
        final int pageCount = cursor.getNumberOfPages();
        final int totalSize = cursor.getResultsCount();
        assertThat(cursor.size(), is(2));
        int size = docs.size();
        assertThat(totalSize, is(size));
        assertThat(pageSize, is(2));
        assertThat(pageCount, is(size / 2 + size % 2));
        assertThat(cursor.getResultsCount(), greaterThanOrEqualTo((pageCount - 1) * pageSize));
    }

    @Test
    public void testSetArrayProperty() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("dc:title", "My Test Folder");
        props.put("dc:description", "test");
        props.put("dc:subjects", "art,sciences,biology");
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "myfolder2")
                                            .set("properties", props)
                                            .execute();

        assertEquals("My Test Folder", folder.getString("dc:title"));
        assertEquals("test", folder.getString("dc:description"));
        PropertyList ar = (PropertyList) folder.getProperties().get("dc:subjects");
        assertEquals(3, ar.size());
        assertEquals("art", ar.getString(0));
        assertEquals("sciences", ar.getString(1));
        assertEquals("biology", ar.getString(2));
    }

    @Test
    public void testSchemaSelection() throws Exception {

        try {
            Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
            // only title and modified
            assertEquals(1, root.getProperties().size());

            // lot of properties ...
            session.setDefaultSchemas("common,dublincore,file");
            root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
            assertTrue(root.getProperties().size() > 15);

            // set at request level with only common schema + dc:title
            root = (Document) session.newRequest(FetchDocument.ID)
                                     .set("value", "/")
                                     .setHeader(Constants.HEADER_NX_SCHEMAS, "common")
                                     .execute();
            assertEquals(3, root.getProperties().size());

            // reset
            session.setDefaultSchemas(null);
            root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
            assertEquals(1, root.getProperties().size());

        } finally {
            session.setDefaultSchemas(null);
        }
    }

    @Test
    public void testBadAccess() throws Exception {
        try {
            session.newRequest(FetchDocument.ID).set("value", "/foo").execute();
            fail("no exception caught");
        } catch (RemoteException e) {
            // expected
        }
    }

    @Test
    public void testLock() throws Exception {
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "myfolder")
                                            .set("properties", "dc:title=My Folder")
                                            .execute();

        // Getting the document
        Document doc = (Document) session.newRequest(FetchDocument.ID)
                                         .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                         .set("value", folder.getPath())
                                         .execute();

        assertNull(doc.getLock());

        session.newRequest(LockDocument.ID).setHeader(Constants.HEADER_NX_VOIDOP, "*").setInput(doc).execute();

        doc = (Document) session.newRequest(FetchDocument.ID)
                                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                .setHeader("X-NXfetch.document", "lock")
                                .set("value", doc.getPath())
                                .execute();

        assertNotNull(doc.getLock());
        assertEquals("Administrator", doc.getLockOwner());
        assertNotNull(doc.getLockCreated());
    }

    @Test
    public void testEncoding() throws Exception {
        // Latin vowels with various French accents (avoid non-ascii literals in
        // java source code to avoid issues when working with developers who do
        // not configure there editor charset to UTF-8).
        String title = "\u00e9\u00e8\u00ea\u00eb\u00e0\u00e0\u00e4\u00ec\u00ee\u00ef\u00f9\u00fb\u00f9";
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(automationTestFolder)
                                            .set("type", "Folder")
                                            .set("name", "myfolder")
                                            .set("properties", "dc:title=" + title)
                                            .execute();

        folder = (Document) session.newRequest(FetchDocument.ID)
                                   .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                   .set("value", folder.getPath())
                                   .execute();

        assertEquals(folder.getTitle(), title);
    }

    /*
     * NXP-19835
     */
    @Test
    public void testGetEmptyBlobsList() throws Exception {
        // create a file
        Document file = (Document) session.newRequest(CreateDocument.ID)
                                          .setInput(automationTestFolder)
                                          .set("type", "File")
                                          .set("name", "blobs")
                                          .set("properties", "dc:title=Blobs Test")
                                          .execute();

        // Get blobs
        Blobs blobs = (Blobs) session.newRequest(GetDocumentBlobs.ID)
                                     .setInput(file)
                                     .execute();
        assertNotNull(blobs);
        assertTrue(blobs.isEmpty());
    }

}
