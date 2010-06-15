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

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRefs;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.client.jaxrs.util.FileBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
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

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Jetty(port = 18080)
@Deploy( { "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server" })
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class RestTest {

    @Inject
    AutomationServer server;

    @Inject
    AutomationService service;

    static HttpAutomationClient client;

    static Session session;

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
     * test blob input / output
     * 
     * @throws Exception
     */
    @Test
    public void testAttachAndGetFile() throws Exception {
        File file = File.createTempFile("automation-test-", ".xml");
        FileUtils.writeFile(file, "<doc>mydoc</doc>");
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

        Blob blob = (Blob) session.newRequest(DocumentService.AttachBlob).setHeader(
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
        blob = (Blob) session.getFile(path);
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc</doc>",
                FileUtils.read(blob.getStream()));

        // now test the GetBlob operation on the same blob
        blob = (Blob) session.newRequest(GetDocumentBlob.ID).setInput(doc).set(
                "xpath", "file:content").execute();
        Assert.assertNotNull(blob);
        Assert.assertEquals(filename, blob.getFileName());
        Assert.assertEquals("text/xml", blob.getMimeType());
        Assert.assertEquals("<doc>mydoc</doc>",
                FileUtils.read(blob.getStream()));
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
     * Test documents input / output
     */
    @Ignore("document list input is not correctly read on server side")
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
        try {
            Documents docs = (Documents) session.newRequest(UpdateDocument.ID).setHeader(
                    Constants.HEADER_NX_SCHEMAS, "*").setInput(refs).set(
                    "properties", "dc:description=updated").execute();
            Assert.assertEquals(2, docs.size());
            Assert.assertEquals("updated", docs.get(0).getString(
                    "dc:description"));
            Assert.assertEquals("updated", docs.get(1).getString(
                    "dc:description"));

        } catch (RemoteException e) {
            System.out.println(e.getRemoteStackTrace());
        }
    }

    /**
     * Test blobs input / output
     */
    @Test
    public void testGetBlobs() throws Exception {
        // TODO
    }

    /**
     * test a chain invocation
     */
    @Test
    public void testChain() throws Exception {
        // TODO
    }

    /**
     * test security on a chain
     */
    @Test
    public void testChainSecurity() throws Exception {
        // TODO
    }

    @Test
    public void testExpressions() throws Exception {
        // TODO
    }
}
