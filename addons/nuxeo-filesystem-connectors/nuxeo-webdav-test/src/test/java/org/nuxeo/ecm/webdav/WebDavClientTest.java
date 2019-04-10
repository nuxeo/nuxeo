/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.apache.jackrabbit.webdav.client.methods.HttpMove;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpUnlock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

    private static CloseableHttpClient client;

    private static HttpClientContext context;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @BeforeClass
    public static void setUpClass() {
        context = getBasicAuthHttpContext(USERNAME, PASSWORD);
        client = HttpClients.createDefault();
    }

    protected static HttpClientContext getBasicAuthHttpContext(String username, String password) {
        // credentials
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return getHttpContext(credentialsProvider);
    }

    protected static HttpClientContext getHttpContext(CredentialsProvider credentialsProvider) {
        // AuthCache instance for preemptive authentication
        AuthCache authCache = new BasicAuthCache();
        authCache.put(new HttpHost("localhost", WebDavServerFeature.PORT), new BasicScheme());

        // create context
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);

        return context;
    }

    @Test
    public void testNotFoundVirtualRoot() throws Exception {
        HttpPropfind request = new HttpPropfind(TEST_URI + "/nosuchpath", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_0);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testNotFoundRegularPath() throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI + "/nosuchpath", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_0);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testPropFindOnFolderDepthInfinity() throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            // Not quite nice, but for a example ok
            DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);
            for (DavPropertyName propName : props.getPropertyNames()) {
                // System.out.println(propName + " " + props.get(propName).getValue());
            }
        }
    }

    @Test
    public void testPropFindOnFolderDepthZero() throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            // Not quite nice, but for a example ok
            DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);
            for (DavPropertyName propName : props.getPropertyNames()) {
                // System.out.println(propName + " " + props.get(propName).getValue());
            }
        }
    }

    @Ignore("On 9.10 the servlet test setup redirects the root to /index.html ...")
    @Test
    public void testListVirtualFolderContentsHTML() throws Exception {
        HttpGet request = new HttpGet(TEST_URI + "/");
        try (CloseableHttpResponse response = client.execute(request, context)) {
            String body;
            try (InputStream in = response.getEntity().getContent()) {
                body = IOUtils.toString(in, "UTF-8");
            }
            assertTrue(body, body.contains("Folder listing for /</p>"));
            assertTrue(body, body.contains("<a href=\"workspace/\">workspace</a>"));
        }
    }

    @Test
    public void testListFolderContentsHTML() throws Exception {
        String folderName = "foo bar[1] caf\u00e9";
        DocumentModel folder = session.createDocumentModel("/workspaces/workspace", folderName, "Folder");
        session.createDocument(folder);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        HttpGet request = new HttpGet(ROOT_URI);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            String body;
            try (InputStream in = response.getEntity().getContent()) {
                body = IOUtils.toString(in, "UTF-8");
            }
            assertTrue(body, body.contains("Folder listing for /workspaces/workspace/</p>"));
            assertTrue(body, body.contains("<a href=\"quality.jpg\">quality.jpg</a>"));
            assertTrue(body, body.contains("<a href=\"test.html\">test.html</a>"));
            assertTrue(body, body.contains("<a href=\"test.txt\">test.txt</a>"));
            assertTrue(body, body.contains("<a href=\"foo%20bar%5B1%5D%20caf%C3%A9/\">foo bar[1] caf&eacute;</a>"));
        }
    }

    @Test
    public void testListFolderContents() throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            StringBuilder failmsg = new StringBuilder("Failed to get 4 responses, got: ");
            if (responses.length < 4) {
                for (MultiStatusResponse resp : responses) {
                    failmsg.append(resp.getHref());
                    failmsg.append("\n");
                }
            }
            assertTrue(failmsg.toString(), responses.length >= 4);
            // there may be more than 4 entries if other testCreate* tests ran before this method
            boolean found = false;
            for (MultiStatusResponse resp : responses) {
                if (resp.getHref().endsWith("quality.jpg")) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testListFolderContentsSpecialName() throws Exception {
        String folderName = "foo bar[1] caf\u00e9";
        DocumentModel folder = session.createDocumentModel("/workspaces/workspace", folderName, "Folder");
        session.createDocument(folder);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        HttpPropfind request = new HttpPropfind(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            StringBuilder failmsg = new StringBuilder("Got: ");
            boolean gotit = false;
            for (MultiStatusResponse resp : responses) {
                String href = resp.getHref();
                failmsg.append(href);
                failmsg.append("\n");
                if (href.endsWith("/workspace/foo%20bar%5B1%5D%20caf%C3%A9")) {
                    gotit = true;
                }
            }
            assertTrue(failmsg.toString(), gotit);
        }
    }

    @Test
    public void testGetDocProperties() throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI + "quality.jpg", DavConstants.PROPFIND_ALL_PROP,
                DavConstants.DEPTH_1);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            assertEquals(1L, responses.length);
            MultiStatusResponse resp = responses[0];
            assertEquals("123631", resp.getProperties(200).get("getcontentlength").getValue());
        }
    }

    @Test
    public void testCreateFolder() throws Exception {
        String name = "newfolder";

        HttpMkcol request = new HttpMkcol(ROOT_URI + name);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
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
        // The bin extension is not in the MimetypeRegistry, so the default mimetype is used
        String mimeType = MimetypeRegistry.DEFAULT_MIMETYPE;
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
        String mimeType = "text/plain";
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

        HttpMove request = new HttpMove(ROOT_URI + name, ROOT_URI + newName, false);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
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

        HttpPut request = new HttpPut(ROOT_URI + name);
        request.setEntity(new InputStreamEntity(is, bytes.length, ContentType.create(mimeType)));
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
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
        HttpMkcol mkcol = new HttpMkcol(ROOT_URI + name);
        int status;
        try (CloseableHttpResponse response = client.execute(mkcol, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_CREATED, status);

        // rename it
        String newName = "myfolderRenamed";
        HttpMove move = new HttpMove(ROOT_URI + name, ROOT_URI + newName, false);
        try (CloseableHttpResponse response = client.execute(move, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_CREATED, status);
    }

    @Test
    public void testDeleteFile() throws Exception {
        String name = "test.txt";

        HttpDelete request = new HttpDelete(ROOT_URI + name);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
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

        HttpDelete request = new HttpDelete(ROOT_URI + name);
        int status;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_NOT_FOUND, status);
    }

    @Test
    public void testGetFolderPropertiesAcceptTextXml() throws Exception {
        checkAccept("text/xml");
    }

    @Test
    public void testGetFolderPropertiesAcceptApplicationXml() throws Exception {
        checkAccept("application/xml");
    }

    @Test
    public void testGetFolderPropertiesAcceptTextMisc() throws Exception {
        checkAccept("text/html, image/jpeg;q=0.9, image/png;q=0.9, text/*;q=0.9, image/*;q=0.9, */*;q=0.8");
    }

    protected void checkAccept(String accept) throws Exception {
        HttpPropfind request = new HttpPropfind(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        request.setHeader("Accept", accept);
        try (CloseableHttpResponse response = client.execute(request, context)) {
            MultiStatus multiStatus = request.getResponseBodyAsMultiStatus(response);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            assertEquals(1, responses.length);
            MultiStatusResponse resp = responses[0];
            assertEquals("workspace", resp.getProperties(200).get(DavConstants.PROPERTY_DISPLAYNAME).getValue());
        }
    }

    @Test
    public void testPropFindOnLockedFile() throws Exception {
        String fileUri = ROOT_URI + "quality.jpg";

        HttpLock request = new HttpLock(fileUri, new LockInfo(Scope.EXCLUSIVE, Type.WRITE, USERNAME, 10000L, false));
        try (CloseableHttpResponse response = client.execute(request, context)) {
            request.checkSuccess(response);
        }

        HttpPropfind request2 = new HttpPropfind(fileUri, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        try (CloseableHttpResponse response = client.execute(request2, context)) {
            MultiStatus multiStatus = request2.getResponseBodyAsMultiStatus(response);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            assertEquals(1L, responses.length);

            MultiStatusResponse resp = responses[0];
            DavProperty<?> pLockDiscovery = resp.getProperties(200).get(DavConstants.PROPERTY_LOCKDISCOVERY);
            Element eLockDiscovery = (Element) ((Element) pLockDiscovery.getValue()).getParentNode();
            LockDiscovery lockDiscovery = LockDiscovery.createFromXml(eLockDiscovery);
            assertEquals(USERNAME, lockDiscovery.getValue().get(0).getOwner());
        }
    }

    @Test
    public void testLockUnlockAcceptTextXml() throws Exception {
        testLockUnlock("text/xml");
    }

    @Test
    public void testLockUnlockAcceptApplicationXml() throws Exception {
        testLockUnlock("application/xml");
    }

    @Test
    public void testLockUnlockAcceptStar() throws Exception {
        testLockUnlock("*/*");
    }

    @Test
    public void testLockUnlockNoAccept() throws Exception {
        testLockUnlock(null);
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
        HttpClientContext fooClientContext = getBasicAuthHttpContext("foo", "123456");

        String tempName = "foo.tmp";
        byte[] newBytesContent = "123456".getBytes(UTF_8);
        doTestPutFile(tempName, newBytesContent, octetStreamMimeType, expectedNuxeoType);

        // rename it to a temp file
        String origTempName = "bar.tmp";

        HttpMove request = new HttpMove(ROOT_URI + origName, ROOT_URI + origTempName, false);
        int status;
        try (CloseableHttpResponse response = client.execute(request, fooClientContext)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_CREATED, status);

        request = new HttpMove(ROOT_URI + tempName, ROOT_URI + origName, false);
        try (CloseableHttpResponse response = client.execute(request, fooClientContext)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_CREATED, status);

        HttpDelete deleteRequest = new HttpDelete(ROOT_URI + origTempName);
        try (CloseableHttpResponse response = client.execute(deleteRequest, fooClientContext)) {
            status = response.getStatusLine().getStatusCode();
        }
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

    protected void testLockUnlock(String accept) throws Exception {
        String fileUri = ROOT_URI + "quality.jpg";

        HttpLock request = new HttpLock(fileUri, new LockInfo(Scope.EXCLUSIVE, Type.WRITE, USERNAME, 10000L, false));
        if (accept != null) {
            request.setHeader("Accept", accept);
        }
        int status;
        String token;
        try (CloseableHttpResponse response = client.execute(request, context)) {
            request.checkSuccess(response);
            status = response.getStatusLine().getStatusCode();
            token = request.getLockToken(response);
        }
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals("urn:uuid:Administrator", token);

        HttpUnlock request2 = new HttpUnlock(fileUri, token);
        if (accept != null) {
            request2.setHeader("Accept", accept);
        }
        try (CloseableHttpResponse response = client.execute(request2, context)) {
            status = response.getStatusLine().getStatusCode();
        }
        assertEquals(HttpStatus.SC_NO_CONTENT, status);
    }

    protected DocumentModel createFolder(String parentRef, String name, String title) throws Exception {
        DocumentModel doc = session.createDocumentModel(parentRef, name, "Folder");
        doc.setPropertyValue("dc:title", title);
        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

}
