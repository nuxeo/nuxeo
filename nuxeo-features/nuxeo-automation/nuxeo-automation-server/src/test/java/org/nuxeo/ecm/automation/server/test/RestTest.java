/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blobs;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRefs;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyList;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.core.operations.document.Query;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Jetty(port = 18080)
@Deploy( { "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server" })
@LocalDeploy("org.nuxeo.ecm.automation.server:test-bindings.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class RestTest {

    @Inject
    AutomationServer server;

    @Inject
    AutomationService service;

    static HttpAutomationClient client;

    static Session session;

    protected File newFile(String content) throws IOException {
        File file = File.createTempFile("automation-test-", ".xml");
        file.deleteOnExit();
        FileUtils.writeFile(file, content);
        return file;
    }

    // ------ Tests comes here --------

    @BeforeClass
    public static void connect() throws Exception {
        try {
            client = new HttpAutomationClient();
            client.connect("http://localhost:18080/automation");

            session = client.getSession("Administrator", "Administrator");
        } catch (RemoteException e) {
            System.out.println(e.getStatus() + "-" + e.getMessage() + "\n"
                    + e.getRemoteStackTrace());
            throw e;
        }
    }

    @AfterClass
    public static void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
            session = null;
        }
    }

    @Test
    public void testInvalidLogin() throws Exception {
        try {
            client.getSession("foo", "bar");
            Assert.fail("login is suposed to fail");
        } catch (RemoteException e) {
            Assert.assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void testGetCreateUpdateAndRemoveDocument() throws Exception {
        // HttpAutomationClient client = new HttpAutomationClient();
        // client.connect("http://localhost:18080/automation");
        // Session cs = client.getSession("Administrator", "Administrator");

        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();

        Assert.assertNotNull(root);
        Assert.assertEquals("/", root.getPath());

        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "myfolder").set(
                "properties", "dc:title=My Folder").execute();

        Assert.assertNotNull(folder);
        Assert.assertEquals("/myfolder", folder.getPath());
        Assert.assertEquals("My Folder", folder.getTitle());

        // update folder properties
        folder = (Document) session.newRequest(UpdateDocument.ID).setHeader(
                "X-NXDocumentProperties", "*").setInput(folder).set(
                "properties", "dc:title=My Folder2\ndc:description=test").execute();

        Assert.assertNotNull(folder);
        Assert.assertEquals("/myfolder", folder.getPath());
        Assert.assertEquals("My Folder2", folder.getTitle());
        Assert.assertEquals("test", folder.getProperties().getString(
                "dc:description"));

        // remove folder
        session.newRequest(DeleteDocument.ID).setInput(folder).execute();

        // assert document removed
        try {
            session.newRequest(FetchDocument.ID).set("value", "/myfolder").execute();
            Assert.fail("request is suposed to return 404");
        } catch (RemoteException e) {
            Assert.assertEquals(404, e.getStatus());
        }

    }

    /**
     * Test documents input / output
     */
    @Test
    public void testUpdateDocuments() throws Exception {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "docsInput").set(
                "properties", "dc:title=Query Test").execute();
        // create 2 files
        session.newRequest(CreateDocument.ID).setInput(folder).set("type",
                "Note").set("name", "note1").set("properties", "dc:title=Note1").execute();
        session.newRequest(CreateDocument.ID).setInput(folder).set("type",
                "Note").set("name", "note2").set("properties", "dc:title=Note2").execute();

        DocRefs refs = new DocRefs();
        refs.add(new DocRef("/docsInput/note1"));
        refs.add(new DocRef("/docsInput/note2"));
        Documents docs = (Documents) session.newRequest(UpdateDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(refs).set(
                "properties", "dc:description=updated").execute();
        Assert.assertEquals(2, docs.size());
        // returned docs doesn't contains all properties.
        // TODO should we return all schemas?

        Document doc = (Document) session.newRequest(FetchDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value",
                "/docsInput/note1").execute();
        Assert.assertEquals("updated", doc.getString("dc:description"));

        doc = (Document) session.newRequest(FetchDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value",
                "/docsInput/note2").execute();
        Assert.assertEquals("updated", doc.getString("dc:description"));

    }

    /**
     * Test documents output - query and get children
     */
    @Test
    public void testQuery() throws Exception {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "queryTest").set(
                "properties", "dc:title=Query Test").execute();
        // create 2 files
        session.newRequest(CreateDocument.ID).setInput(folder).set("type",
                "Note").set("name", "note1").set("properties", "dc:title=Note1").execute();
        session.newRequest(CreateDocument.ID).setInput(folder).set("type",
                "Note").set("name", "note2").set("properties", "dc:title=Note2").execute();

        // now query the two files
        Documents docs = (Documents) session.newRequest(Query.ID).set("query",
                "SELECT * FROM Note WHERE ecm:path STARTSWITH '/queryTest' ").execute();
        Assert.assertEquals(2, docs.size());
        String title1 = docs.get(0).getTitle();
        String title2 = docs.get(1).getTitle();
        Assert.assertTrue(title1.equals("Note1") && title2.equals("Note2")
                || title1.equals("Note2") && title2.equals("Note1"));

        // now get children of /testQuery
        docs = (Documents) session.newRequest(GetDocumentChildren.ID).setInput(
                folder).execute();
        Assert.assertEquals(2, docs.size());
        title1 = docs.get(0).getTitle();
        title2 = docs.get(1).getTitle();
        Assert.assertTrue(title1.equals("Note1") && title2.equals("Note2")
                || title1.equals("Note2") && title2.equals("Note1"));

    }

    /**
     * test blob input / output
     * 
     * @throws Exception
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
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a file
        session.newRequest(CreateDocument.ID).setInput(root).set("type", "File").set(
                "name", "myfile").set("properties", "dc:title=My File").execute();

        FileBlob blob = (FileBlob) session.newRequest(
                DocumentService.AttachBlob).setHeader(
                Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set(
                "document", "/myfile").execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        Assert.assertNull(blob);

        // get the file where blob was attached
        Document doc = (Document) session.newRequest(
                DocumentService.FetchDocument).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/myfile").execute();

        PropertyMap map = doc.getProperties().getMap("file:content");
        Assert.assertEquals(filename, map.getString("name"));
        Assert.assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // now test the GetBlob operation on the same blob
        blob = (FileBlob) session.newRequest(GetDocumentBlob.ID).setInput(doc).set(
                "xpath", "file:content").execute();
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();
    }

    /**
     * Test blobs input / output
     */
    @Test
    public void testGetBlobs() throws Exception {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a note
        Document note = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Note").set("name", "blobs").set(
                "properties", "dc:title=Blobs Test").execute();
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
        FileBlob blob = (FileBlob) session.newRequest(AttachBlob.ID).setHeader(
                Constants.HEADER_NX_VOIDOP, "true").setInput(fb1).set(
                "document", "/blobs").set("xpath", "files:files").execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        Assert.assertNull(blob);
        // attach second blob
        blob = (FileBlob) session.newRequest(AttachBlob.ID).setHeader(
                Constants.HEADER_NX_VOIDOP, "true").setInput(fb2).set(
                "document", "/blobs").set("xpath", "files:files").execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        Assert.assertNull(blob);

        // now retrieve the note with full schemas
        note = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/blobs").execute();

        PropertyList list = note.getProperties().getList("files:files");
        Assert.assertEquals(2, list.size());
        PropertyMap map = list.getMap(0).getMap("file");
        Assert.assertEquals(filename1, map.getString("name"));
        Assert.assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename1, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc1</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // the same for the second file
        map = list.getMap(1).getMap("file");
        Assert.assertEquals(filename2, map.getString("name"));
        Assert.assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename2, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc2</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // now test the GetDocumentBlobs operation on the note document
        blobs = (Blobs) session.newRequest(GetDocumentBlobs.ID).setInput(note).set(
                "xpath", "files:files").execute();
        Assert.assertNotNull(blob);
        Assert.assertEquals(2, blobs.size());

        // test first blob
        blob = (FileBlob) blobs.get(0);
        Assert.assertEquals(filename1, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc1</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // test the second one
        blob = (FileBlob) blobs.get(1);
        Assert.assertEquals(filename2, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc2</doc>",
                FileUtils.read(blob.getStream()));
        blob.getFile().delete();
    }

    /**
     * Upload blobs to create a zip and download it
     * 
     * @throws Exception
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

        FileBlob zip = (FileBlob) session.newRequest(CreateZip.ID).set(
                "filename", "test.zip").setInput(blobs).execute();

        Assert.assertNotNull(zip);
        ZipFile zf = new ZipFile(zip.getFile());
        ZipEntry entry1 = zf.getEntry(filename1);
        Assert.assertNotNull(entry1);
        ZipEntry entry2 = zf.getEntry(filename2);
        Assert.assertNotNull(entry2);
        zip.getFile().delete();
    }

    /**
     * test a chain invocation
     */
    @Test
    public void testChain() throws Exception {
        OperationDocumentation opd = session.getOperation("testchain");
        Assert.assertNotNull(opd);

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "chainTest").execute();

        Document doc = (Document) session.newRequest("testchain").setInput(
                folder).execute();
        Assert.assertEquals("/chainTest/chain.doc", doc.getPath());
        Assert.assertEquals("Note", doc.getType());

        // fetch again the note
        doc = (Document) session.newRequest(FetchDocument.ID).set("value", doc).execute();
        Assert.assertEquals("/chainTest/chain.doc", doc.getPath());
        Assert.assertEquals("Note", doc.getType());

    }

    /**
     * test security on a chain - only diable flag is tested - TODO more tests
     * to test each security filter
     */
    @Test
    public void testChainSecurity() throws Exception {
        OperationDocumentation opd = session.getOperation("principals");
        Assert.assertNotNull(opd);
        try {
            session.newRequest("principals").setInput(null).execute();
            Assert.fail("chains invocation is supposed to fail since it is disabled - should return 404");
        } catch (RemoteException e) {
            Assert.assertEquals(404, e.getStatus());
        }

    }

}
