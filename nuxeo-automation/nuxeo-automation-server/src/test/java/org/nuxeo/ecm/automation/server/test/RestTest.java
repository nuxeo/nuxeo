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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.client.jaxrs.util.FileBlob;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
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

    @Test
    public void testAttachAndGetFile() throws Exception {
        FileBlob fb = new FileBlob(new File("/Users/bstefanescu/test.jpg"));
        fb.setMimeType("image/jpeg");
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
        Assert.assertEquals("test.jpg", map.getString("name"));
        Assert.assertEquals("image/jpeg", map.getString("mime-type"));

        // get the data URL
        String path = map.getString("data");
        blob = (Blob) session.getFile(path);
        Assert.assertNotNull(blob);
        Assert.assertEquals("test.jpg", blob.getFileName());
        Assert.assertEquals("image/jpeg", blob.getMimeType());

        File file2 = ((FileBlob) blob).getFile();

    }

}
