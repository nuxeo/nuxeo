/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.hamcrest.number.IsCloseTo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.BeanInput;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blobs;
import org.nuxeo.ecm.automation.client.jaxrs.model.DateInput;
import org.nuxeo.ecm.automation.client.jaxrs.model.DateUtils;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRefs;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.jaxrs.model.PrimitiveInput;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyList;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
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
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Jetty(port = 18080)
@Deploy({ "org.nuxeo.runtime.jtajca", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.platform.query.api" })
@LocalDeploy({ "org.nuxeo.ecm.automation.server:test-bindings.xml",
        "org.nuxeo.ecm.automation.server:test-marshalling.xml",
        "org.nuxeo.ecm.automation.server:test-mvalues.xml" })
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

    @BeforeClass
    public static void setupClientMarshalling() {
        JsonMarshalling.addMarshaller(new MyObjectClientMarshaller());
    }
    
    // ------ Tests comes here --------

    @BeforeClass
    public static void connect() {
        try {
            client = new HttpAutomationClient(
                    "http://localhost:18080/automation");

            session = client.getSession("Administrator", "Administrator");
        } catch (RemoteException e) {
            System.out.println(e.getStatus() + "-" + e.getMessage() + "\n"
                    + e.getRemoteStackTrace());
            throw e;
        }
    }

    @AfterClass
    public static void shutdown() {
        if (client != null) {
            client.shutdown();
            client = null;
            session = null;
        }
    }

    @Test
    public void testInvalidLogin() {
        try {
            client.getSession("foo", "bar");
            fail("login is supposed to fail");
        } catch (RemoteException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void testMultiValued() throws Exception {
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();

        Document note = (Document) session.newRequest(CreateDocument.ID).setHeader(
                "X-NXDocumentProperties", "*").setInput(root).set("type", "MV").set(
                "name", "pfff").set("properties",
                "mv:sl=s1,s2\nmv:ss=s1,s2\nmv:bl=true,false\nmv:b=true\n").execute();
        checkHasCorrectMultiValues(note);

        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                DocumentPageProviderOperation.ID).setHeader(
                "X-NXDocumentProperties", "*").set("query", "SELECT * from MV").set(
                "pageSize", 2).execute();

        assertThat(docs, notNullValue());
        assertThat(docs.size(), is(1));
        checkHasCorrectMultiValues(docs.get(0));
    }

    private void checkHasCorrectMultiValues(Document note) {
        assertThat(note, notNullValue());
        PropertyMap properties = note.getProperties();
        assertThat(properties, notNullValue());

        PropertyList sl = properties.getList("mv:sl");
        assertThat(sl, notNullValue());
        List<Object> slValues = sl.list();
        assertThat(slValues, hasItem((Object) "s1"));
        assertThat(slValues, hasItem((Object) "s2"));

        PropertyList ss = properties.getList("mv:ss");
        assertThat(ss, notNullValue());
        List<Object> ssValues = ss.list();
        assertThat(ssValues, hasItem((Object) "s1"));
        assertThat(ssValues, hasItem((Object) "s2"));

        Boolean b = properties.getBoolean("mv:b");
        assertThat(b, is(true));

        PropertyList bl = properties.getList("mv:bl");
        assertThat(bl, notNullValue());
        List<Object> blValues = bl.list();
        assertThat(blValues, hasItem((Object) "true"));
        assertThat(blValues, hasItem((Object) "false"));
        assertThat(bl.getBoolean(0), is(Boolean.TRUE));
        assertThat(bl.getBoolean(1), is(Boolean.FALSE));
    }

    @Test
    public void testGetCreateUpdateAndRemoveDocument() throws Exception {
        // HttpAutomationClient client = new HttpAutomationClient();
        // client.connect("http://localhost:18080/automation");
        // Session cs = client.getSession("Administrator", "Administrator");

        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();

        assertNotNull(root);
        assertEquals("/", root.getPath());

        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "myfolder").set(
                "properties", "dc:title=My Folder").execute();

        assertNotNull(folder);
        assertEquals("/myfolder", folder.getPath());
        assertEquals("My Folder", folder.getTitle());

        // update folder properties
        folder = (Document) session.newRequest(UpdateDocument.ID).setHeader(
                "X-NXDocumentProperties", "*").setInput(folder).set(
                "properties", "dc:title=My Folder2\ndc:description=test").execute();

        assertNotNull(folder);
        assertEquals("/myfolder", folder.getPath());
        assertEquals("My Folder2", folder.getTitle());
        assertEquals("test", folder.getProperties().getString("dc:description"));

        // remove folder
        session.newRequest(DeleteDocument.ID).setInput(folder).execute();

        // assert document removed
        try {
            session.newRequest(FetchDocument.ID).set("value", "/myfolder").execute();
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
        assertEquals(2, docs.size());
        // returned docs doesn't contains all properties.
        // TODO should we return all schemas?

        Document doc = (Document) session.newRequest(FetchDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value",
                "/docsInput/note1").execute();
        assertEquals("updated", doc.getString("dc:description"));

        doc = (Document) session.newRequest(FetchDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value",
                "/docsInput/note2").execute();
        assertEquals("updated", doc.getString("dc:description"));
       
        String now = DateUtils.formatDate(new Date());
        doc = (Document)session.newRequest(UpdateDocument.ID).setHeader(Constants.HEADER_NX_SCHEMAS, "*").setInput(new DocRef("/docsInput/note1")).set("properties", "dc:valid=".concat(now)).execute();
        assertThat(doc.getDate("dc:valid"), is(DateUtils.parseDate(now)));
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
        assertEquals(2, docs.size());
        String title1 = docs.get(0).getTitle();
        String title2 = docs.get(1).getTitle();
        assertTrue(title1.equals("Note1") && title2.equals("Note2")
                || title1.equals("Note2") && title2.equals("Note1"));

        // now get children of /testQuery
        docs = (Documents) session.newRequest(GetDocumentChildren.ID).setInput(
                folder).execute();
        assertEquals(2, docs.size());

        title1 = docs.get(0).getTitle();
        title2 = docs.get(1).getTitle();
        assertTrue(title1.equals("Note1") && title2.equals("Note2")
                || title1.equals("Note2") && title2.equals("Note1"));

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
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a file
        session.newRequest(CreateDocument.ID).setInput(root).set("type", "File").set(
                "name", "myfile").set("properties", "dc:title=My File").execute();

        FileBlob blob = (FileBlob) session.newRequest("Blob.Attach").setHeader(
                Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set(
                "document", "/myfile").execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // get the file where blob was attached
        Document doc = (Document) session.newRequest(
                DocumentService.FetchDocument).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/myfile").execute();

        PropertyMap map = doc.getProperties().getMap("file:content");
        assertEquals(filename, map.getString("name"));
        assertEquals("text/xml", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (FileBlob) session.getFile(path);
        assertNotNull(blob);
        assertEquals(filename, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // now test the GetBlob operation on the same blob
        blob = (FileBlob) session.newRequest(GetDocumentBlob.ID).setInput(doc).set(
                "xpath", "file:content").execute();
        assertNotNull(blob);
        assertEquals(filename, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", FileUtils.read(blob.getStream()));
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
        assertNull(blob);

        // attach second blob
        blob = (FileBlob) session.newRequest(AttachBlob.ID).setHeader(
                Constants.HEADER_NX_VOIDOP, "true").setInput(fb2).set(
                "document", "/blobs").set("xpath", "files:files").execute();
        // test that output was avoided using Constants.HEADER_NX_VOIDOP
        assertNull(blob);

        // now retrieve the note with full schemas
        note = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/blobs").execute();

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
        assertEquals("<doc>mydoc1</doc>", FileUtils.read(blob.getStream()));
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
        assertEquals("<doc>mydoc2</doc>", FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // now test the GetDocumentBlobs operation on the note document
        blobs = (Blobs) session.newRequest(GetDocumentBlobs.ID).setInput(note).set(
                "xpath", "files:files").execute();
        assertNotNull(blob);
        assertEquals(2, blobs.size());

        // test first blob
        blob = (FileBlob) blobs.get(0);
        assertEquals(filename1, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc1</doc>", FileUtils.read(blob.getStream()));
        blob.getFile().delete();

        // test the second one
        blob = (FileBlob) blobs.get(1);
        assertEquals(filename2, blob.getFileName());
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc2</doc>", FileUtils.read(blob.getStream()));
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

        FileBlob zip = (FileBlob) session.newRequest(CreateZip.ID).set(
                "filename", "test.zip").setInput(blobs).execute();
        assertNotNull(zip);

        ZipFile zf = new ZipFile(zip.getFile());
        ZipEntry entry1 = zf.getEntry(filename1);
        assertNotNull(entry1);

        ZipEntry entry2 = zf.getEntry(filename2);
        assertNotNull(entry2);
        zip.getFile().delete();
    }

    /**
     * test a chain invocation
     */
    @Test
    public void testChain() throws Exception {
        OperationDocumentation opd = session.getOperation("testchain");
        assertNotNull(opd);

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "chainTest").execute();

        Document doc = (Document) session.newRequest("testchain").setInput(
                folder).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());

        // fetch again the note
        doc = (Document) session.newRequest(FetchDocument.ID).set("value", doc).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());
    }

    /**
     * test security on a chain - only disable flag is tested - TODO more tests
     * to test each security filter
     */
    @Test
    public void testChainSecurity() throws Exception {
        OperationDocumentation opd = session.getOperation("principals");
        assertNotNull(opd);
        try {
            session.newRequest("principals").setInput(null).execute();
            fail("chains invocation is supposed to fail since it is disabled - should return 404");
        } catch (RemoteException e) {
            assertEquals(404, e.getStatus());
        }
    }

    /**
     * test a chain rollback
     */
    @Test
    public void testChainRollback() throws Exception {

        NuxeoContainer.install();

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // 1. create a note and exit gracefully
        Document doc = (Document) session.newRequest("exitNoRollback").setInput(
                root).execute();
        assertEquals("/test-exit1", doc.getPath());
        Document note = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/test-exit1").execute();
        assertEquals(doc.getPath(), note.getPath());

        // 2. create a note and exit with rollback
        doc = (Document) session.newRequest("exitRollback").setInput(root).execute();
        assertEquals("/test-exit2", doc.getPath());
        try {
            note = (Document) session.newRequest(FetchDocument.ID).set("value",
                    "/test-exit2").execute();
            fail("document should not exist");
        } catch (RemoteException e) {
            // do nothing
        }

        // 3. create a note and exit with error (+rollback)
        try {
            doc = (Document) session.newRequest("exitError").setInput(root).execute();
            Assert.fail("expected error");
        } catch (RemoteException t) {
            assertTrue(t.getRemoteStackTrace().contains("termination error"));
        }
        // test the note was not created
        try {
            note = (Document) session.newRequest(FetchDocument.ID).set("value",
                    "/test-exit3").execute();
            fail("document should not exist");
        } catch (RemoteException e) {
            // do nothing
        }

    }

    @Test
    public void queriesArePaginable() throws Exception {
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                DocumentPageProviderOperation.ID).set("query",
                "SELECT * from Document").set("pageSize", 2).execute();
        final int pageSize = docs.getPageSize();
        final int pageCount = docs.getPageCount();
        assertThat(docs.size(), is(2));
        assertThat(pageSize, is(2));
        assertThat(pageCount, greaterThanOrEqualTo(8));
        assertThat(docs.getTotalSize(), greaterThanOrEqualTo((pageCount - 1)
                * pageSize));
    }

    @Test
    public void testSetArrayProperty() throws Exception {
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();

        assertNotNull(root);
        assertEquals("/", root.getPath());

        PropertyMap props = new PropertyMap();
        props.set("dc:title", "My Test Folder");
        props.set("dc:description", "test");
        props.set("dc:subjects", "a,b,c\\,d");
        Document folder = (Document) session.newRequest(CreateDocument.ID).setHeader(
                "X-NXDocumentProperties", "*").setInput(root).set("type",
                "Folder").set("name", "myfolder2").set("properties", props).execute();

        assertEquals("My Test Folder", folder.getString("dc:title"));
        assertEquals("test", folder.getString("dc:description"));
        PropertyList ar = (PropertyList) folder.getProperties().get(
                "dc:subjects");
        assertEquals(3, ar.size());
        assertEquals("a", ar.getString(0));
        assertEquals("b", ar.getString(1));
        assertEquals("c,d", ar.getString(2));
    }

    @Test
    public void testReturnValues() throws Exception {
        Object r;
        r = session.newRequest(ReturnOperation.ID).setInput(
                new PrimitiveInput<Boolean>(Boolean.TRUE)).execute();
        assertThat((Boolean) r, is(Boolean.TRUE));
        r = session.newRequest(ReturnOperation.ID).setInput(
                new PrimitiveInput<String>("hello")).execute();
        assertThat((String) r, is("hello"));
        r = session.newRequest(ReturnOperation.ID).setInput(
                new PrimitiveInput<Integer>(1)).execute();
        assertThat((Integer) r, is(1));
        r = session.newRequest(ReturnOperation.ID).setInput(
                new PrimitiveInput<Long>(1000000000000000000L)).execute();
        assertThat((Long) r, is(1000000000000000000L));
        r = session.newRequest(ReturnOperation.ID).setInput(
                new PrimitiveInput<Double>(1.1d)).execute();
        assertThat((Double) r, IsCloseTo.closeTo(1.1d, 0.1));
        Date now = DateUtils.parseDate(DateUtils.formatDate(new Date(0)));
        r = session.newRequest(ReturnOperation.ID).setInput(new DateInput(now)).execute();
        assertThat((Date) r, is(now));
    }

    @Test
    public void testMyObject() throws Exception {
        MyObject mo = new MyObject();
        Object r;
        r = session.newRequest(ReturnOperation.ID).setInput(
                new BeanInput<MyObject>(MyObject.class, mo)).execute();
        assertThat((MyObject) r, is(mo));
    }

    @Test
    public void testBadAccess() throws Exception {
        try {
            session.newRequest(FetchDocument.ID).set("value", "/foo").execute();
        } catch (RemoteException e) {
            return;
        }
        fail("no exception caught");
    }

}
