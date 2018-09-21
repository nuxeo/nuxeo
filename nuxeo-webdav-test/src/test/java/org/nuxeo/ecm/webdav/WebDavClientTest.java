/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.w3c.dom.Element;

/**
 * Jackrabbit includes a WebDAV client library. Let's use it to test our server.
 */
public class WebDavClientTest extends AbstractServerTest {

    private static String USERNAME = "Administrator";

    private static String PASSWORD = "Administrator";

    private static HttpClient client;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @BeforeClass
    public static void setUpClass() {
        client = createClient(USERNAME, PASSWORD);
    }

    protected static HttpClient createClient(String username, String password) {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", WebDavServerFeature.PORT);

        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);

        HttpClient httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfig);

        Credentials creds = new UsernamePasswordCredentials(username, password);
        httpClient.getState().setCredentials(AuthScope.ANY, creds);
        httpClient.getParams().setAuthenticationPreemptive(true);
        return httpClient;
    }

    @Test
    public void testNotFoundVirtualRoot() throws Exception {
        DavMethod method = new PropFindMethod(TEST_URI + "/nosuchpath", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_0);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testNotFoundRegularPath() throws Exception {
        DavMethod method = new PropFindMethod(ROOT_URI + "/nosuchpath", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_0);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testPropFindOnFolderDepthInfinity() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();

        // Not quite nice, but for a example ok
        DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);

        for (DavPropertyName propName : props.getPropertyNames()) {
            // System.out.println(propName + " " + props.get(propName).getValue());
        }
    }

    @Test
    public void testPropFindOnFolderDepthZero() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();

        // Not quite nice, but for a example ok
        DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);

        for (DavPropertyName propName : props.getPropertyNames()) {
            // System.out.println(propName + " " + props.get(propName).getValue());
        }
    }

    @Test
    public void testListFolderContents() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        StringBuilder failmsg = new StringBuilder("Failed to get 4 responses, got: ");
        if (responses.length < 4) {
            for (MultiStatusResponse response : responses) {
                failmsg.append(response.getHref());
                failmsg.append("\n");
            }
        }
        assertTrue(failmsg.toString(), responses.length >= 4);
        // there may be more than 4 entries if other testCreate* tests
        // run before this method

        boolean found = false;
        for (MultiStatusResponse response : responses) {
            if (response.getHref().endsWith("quality.jpg")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testGetDocProperties() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI + "quality.jpg", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_1);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        assertEquals(1L, responses.length);

        MultiStatusResponse response = responses[0];
        assertEquals("123631", response.getProperties(200).get("getcontentlength").getValue());
    }

    @Test
    public void testCreateFolder() throws Exception {
        String name = "newfolder";

        DavMethod method = new MkColMethod(ROOT_URI + name);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_CREATED, status);

        // check using Nuxeo Core APIs
        session.save(); // process invalidations
        PathRef pathRef = new PathRef("/workspaces/workspace/" + name);
        assertTrue(session.exists(pathRef));
        DocumentModel doc = session.getDocument(pathRef);
        assertEquals("Folder", doc.getType());
        assertEquals(name, doc.getTitle());
    }

    @Test
    public void testCreateBinaryFile() throws Exception {
        String name = "newfile.bin";
        String mimeType = "application/binary";
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
        String expectedType = "File";
        doTestPutFile(name, bytes, mimeType, expectedType);
    }

    @Test
    public void testCreateTextFile() throws Exception {
        String name = "newfile.txt";
        String mimeType = "text/plain";
        byte[] bytes = "Hello, world!".getBytes("UTF-8");
        String expectedType = "Note";
        doTestPutFile(name, bytes, mimeType, expectedType);
    }

    @Test
    public void testCreateTextFileWithSemiColon() throws Exception {
        String name = "newfile;;;.txt"; // name with semicolons
        String mimeType = "text/plain";
        byte[] bytes = "Hello, world!".getBytes("UTF-8");
        String expectedType = "Note";
        doTestPutFile(name, bytes, mimeType, expectedType);
    }

    @Test
    // NXP-12735: disabled because failing under windows + pgsql
    public void testOverwriteExistingFile() throws Exception {
        String name = "test.txt"; // this file already exists
        String mimeType = "application/binary";
        PathRef pathRef = new PathRef("/workspaces/workspace/" + name);
        assertTrue(session.exists(pathRef));
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
        String expectedType = "File";
        doTestPutFile(name, bytes, mimeType, expectedType);
    }

    @Test
    public void testMoveWithRenaming() throws Exception {
        // create a fake bin tmp file which will finally be a docx file
        String name = "tmpfile.tmp";
        String mimeType = MimetypeRegistry.DEFAULT_MIMETYPE;
        byte[] bytes = "Fake BIN".getBytes("UTF-8");
        String expectedType = "File";
        doTestPutFile(name, bytes, mimeType, expectedType);

        PathRef pathRef = new PathRef("/workspaces/workspace/" + name);
        assertTrue(session.exists(pathRef));
        DocumentModel doc = session.getDocument(pathRef);
        assertEquals(name, doc.getTitle());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(name, blob.getFilename());
        assertEquals(MimetypeRegistry.DEFAULT_MIMETYPE, blob.getMimeType());

        // rename it to a docx file
        String newName = "sample.docx";
        HttpMethod method = new MoveMethod(ROOT_URI + name, ROOT_URI + newName, false);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_CREATED, status);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        pathRef = new PathRef("/workspaces/workspace/" + newName);
        doc = session.getDocument(pathRef);
        assertEquals(newName, doc.getName());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(newName, blob.getFilename());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", blob.getMimeType());
    }

    protected void doTestPutFile(String name, byte[] bytes, String mimeType, String expectedType) throws Exception {
        InputStream is = new ByteArrayInputStream(bytes);
        PutMethod method = new PutMethod(ROOT_URI + name);
        method.setRequestEntity(new InputStreamRequestEntity(is, bytes.length, mimeType));
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_CREATED, status);

        // check using Nuxeo Core APIs
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        PathRef pathRef = new PathRef("/workspaces/workspace/" + name);
        assertTrue(session.exists(pathRef));
        DocumentModel doc = session.getDocument(pathRef);
        assertEquals(expectedType, doc.getType());
        assertEquals(name, doc.getTitle());
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertEquals(bytes.length, blob.getLength());
        assertEquals(mimeType, blob.getMimeType());
        assertArrayEquals(bytes, blob.getByteArray());
    }

    @Test
    public void testMoveFolder() throws Exception {
        // create a folder
        String name = "myfolder";
        HttpMethod method = new MkColMethod(ROOT_URI + name);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_CREATED, status);

        // rename it
        String newName = "myfolderRenamed";
        method = new MoveMethod(ROOT_URI + name, ROOT_URI + newName, false);
        status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_CREATED, status);
    }

    @Test
    public void testDeleteFile() throws Exception {
        String name = "test.txt";

        HttpMethod method = new DeleteMethod(ROOT_URI + name);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_NO_CONTENT, status);

        // check using Nuxeo Core APIs
        session.save(); // process invalidations
        PathRef pathRef = new PathRef("/workspaces/workspace/" + name);
        assertFalse(session.exists(pathRef)); // in trash with different name

        // recreate it, for other tests using the same repo
        byte[] bytes = "Hello, world!".getBytes("UTF-8");
        doTestPutFile(name, bytes, "text/plain", "Note");
    }

    @Test
    public void testDeleteMissingFile() throws Exception {
        String name = "nosuchfile.txt";

        HttpMethod method = new DeleteMethod(ROOT_URI + name);
        int status = client.executeMethod(method);
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testGetFolderPropertiesAcceptTextXml() throws Exception {
        checkAccept("text/xml");
    }

    @Test
    public void testGetFolderPropertiesAcceptTextMisc() throws Exception {
        checkAccept("text/html, image/jpeg;q=0.9, image/png;q=0.9, text/*;q=0.9, image/*;q=0.9, */*;q=0.8");
    }

    protected void checkAccept(String accept) throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        pFind.setRequestHeader("Accept", accept);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        assertEquals(1, responses.length);

        MultiStatusResponse response = responses[0];
        assertEquals("workspace", response.getProperties(200).get(DavConstants.PROPERTY_DISPLAYNAME).getValue());
    }

    @Test
    public void testMSOfficeSaveFileFlow() throws Exception {
        // create a fake bin tmp file which will finally be a docx file
        String basePath = "/workspaces/workspace/";
        String rootUri = TEST_URI + basePath;
        String origName = "Document.docx";
        String octetStreamMimeType = MimetypeRegistry.DEFAULT_MIMETYPE;
        byte[] bytes = "Fake BIN".getBytes(UTF_8);
        String expectedNuxeoType = "File";
        String wordMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        doTestPutFile(origName, bytes, wordMimeType, expectedNuxeoType);

        PathRef origPathRef = new PathRef(basePath + origName);
        assertTrue(session.exists(origPathRef));
        DocumentModel doc = session.getDocument(origPathRef);
        assertEquals(origName, doc.getTitle());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(origName, blob.getFilename());
        assertEquals(wordMimeType, blob.getMimeType());

        DocumentModel bareUserModel = userManager.getBareUserModel();
        bareUserModel.setPropertyValue("user:username", "foo");
        bareUserModel.setPropertyValue("user:password", "123456");
        bareUserModel = userManager.createUser(bareUserModel);
        NuxeoPrincipal foo = userManager.getPrincipal("foo");
        foo.setPassword("123456");
        foo.setGroups(userManager.getAdministratorsGroups());
        userManager.updateUser(foo.getModel());

        DocumentModel workspace = session.getDocument(new PathRef("/workspaces/workspace/"));
        ACP acp = workspace.getACP();
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("foo", SecurityConstants.EVERYTHING).build());
        session.setACP(workspace.getRef(), acp, true);
        session.save();

        transactionalFeature.nextTransaction();
        HttpClient fooClient = createClient("foo", "123456");

        String tempName = "foo.tmp";
        byte[] newBytesContent = "123456".getBytes(UTF_8);
        doTestPutFile(tempName, newBytesContent, octetStreamMimeType, expectedNuxeoType);

        // rename it to a temp file
        String origTempName = "bar.tmp";

        HttpMethod request = new MoveMethod(ROOT_URI + origName, ROOT_URI + origTempName, false);
        int status = fooClient.executeMethod(request);
        assertEquals(HttpStatus.SC_CREATED, status);

        request = new MoveMethod(ROOT_URI + tempName, ROOT_URI + origName, false);
        status = fooClient.executeMethod(request);
        assertEquals(HttpStatus.SC_CREATED, status);

        request = new DeleteMethod(ROOT_URI + origTempName);
        status = fooClient.executeMethod(request);
        assertEquals(HttpStatus.SC_NO_CONTENT, status);

        PathRef pathRef = new PathRef(basePath + origName);

        transactionalFeature.nextTransaction();

        doc = session.getDocument(pathRef);
        assertEquals(origName, doc.getTitle());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(origName, blob.getFilename());
        assertArrayEquals(newBytesContent, blob.getByteArray());
        assertEquals(wordMimeType, blob.getMimeType());

        assertEquals("0.1+", doc.getVersionLabel());
    }

    @Test
    public void testPropFindOnLockedFile() throws Exception {
        String fileUri = ROOT_URI + "quality.jpg";
        DavMethod pLock = new LockMethod(fileUri, Scope.EXCLUSIVE, Type.WRITE, USERNAME, 10000l, false);
        client.executeMethod(pLock);
        pLock.checkSuccess();

        DavMethod pFind = new PropFindMethod(fileUri, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        assertEquals(1L, responses.length);

        MultiStatusResponse response = responses[0];
        DavProperty<?> pLockDiscovery = response.getProperties(200).get(DavConstants.PROPERTY_LOCKDISCOVERY);
        Element eLockDiscovery = (Element) ((Element) pLockDiscovery.getValue()).getParentNode();
        LockDiscovery lockDiscovery = LockDiscovery.createFromXml(eLockDiscovery);
        assertEquals(USERNAME, lockDiscovery.getValue().get(0).getOwner());
    }

    protected DocumentModel createFolder(String parentRef, String name, String title) throws Exception {
        DocumentModel doc = session.createDocumentModel(parentRef, name, "Folder");
        doc.setPropertyValue("dc:title", title);
        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

}
