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
 *     Antoine Taillefer
 *     Thomas Roger
 */
package org.nuxeo.wopi.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_SUBJECT;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_PARAMETER;
import static org.nuxeo.wopi.Constants.HOST_EDIT_URL;
import static org.nuxeo.wopi.Constants.HOST_VIEW_URL;
import static org.nuxeo.wopi.Constants.NAME;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_ONLY;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_WRITE;
import static org.nuxeo.wopi.Constants.URL;
import static org.nuxeo.wopi.Constants.WOPI_BASE_URL_PROPERTY;
import static org.nuxeo.wopi.Headers.FILE_CONVERSION;
import static org.nuxeo.wopi.Headers.ITEM_VERSION;
import static org.nuxeo.wopi.Headers.LOCK;
import static org.nuxeo.wopi.Headers.MAX_EXPECTED_SIZE;
import static org.nuxeo.wopi.Headers.OLD_LOCK;
import static org.nuxeo.wopi.Headers.OVERRIDE;
import static org.nuxeo.wopi.Headers.RELATIVE_TARGET;
import static org.nuxeo.wopi.Headers.REQUESTED_NAME;
import static org.nuxeo.wopi.Headers.SUGGESTED_TARGET;
import static org.nuxeo.wopi.Headers.URL_TYPE;
import static org.nuxeo.wopi.JSONHelper.readFile;
import static org.nuxeo.wopi.TestConstants.CHANGE_TOKEN_VAR;
import static org.nuxeo.wopi.TestConstants.DOC_ID_VAR;
import static org.nuxeo.wopi.TestConstants.FILENAME_VAR;
import static org.nuxeo.wopi.TestConstants.FILES_FIRST_FILE_PROPERTY;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;
import static org.nuxeo.wopi.TestConstants.ITEM_VERSION_VAR;
import static org.nuxeo.wopi.TestConstants.REPOSITORY_VAR;
import static org.nuxeo.wopi.TestConstants.XPATH_VAR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.jwt.JWTService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.Operation;
import org.nuxeo.wopi.WOPIDiscoveryFeature;
import org.nuxeo.wopi.WOPIFeature;
import org.nuxeo.wopi.lock.LockHelper;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Tests the {@link FilesEndpoint} WOPI endpoint.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WOPIFeature.class, WOPIDiscoveryFeature.class, WebEngineFeature.class })
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-jwt-contrib.xml")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-webengine-servletcontainer-contrib.xml")
public class TestFilesEndpoint {

    public static final String WOPI_FILES = "site/wopi/files";

    public static final String CONTENTS_PATH = "contents";

    @Inject
    protected UserManager userManager;

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected JWTService jwtService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected Client client;

    protected String joeToken;

    protected String johnToken;

    protected DocumentModel blobDoc;

    protected String blobDocFileId;

    protected DocumentModel zeroLengthBlobDoc;

    protected String zeroLengthBlobDocFileId;

    protected DocumentModel hugeBlobDoc;

    protected String hugeBlobDocFileId;

    protected DocumentModel noBlobDoc;

    protected String noBlobDocFileId;

    protected DocumentModel multipleBlobsDoc;

    protected String multipleBlobsDocFileId;

    protected String multipleBlobsDocAttachementId;

    protected Blob expectedFileBlob;

    protected Blob expectedAttachementBlob;

    ObjectMapper mapper;

    @Before
    public void setUp() throws IOException {
        mapper = new ObjectMapper();

        createUsers();

        createDocuments();

        // initialize REST API clients
        joeToken = jwtService.newBuilder().withClaim(CLAIM_SUBJECT, "joe").build();
        johnToken = jwtService.newBuilder().withClaim(CLAIM_SUBJECT, "john").build();
        client = JerseyClientHelper.clientBuilder().build();

        // make sure everything is committed
        transactionalFeature.nextTransaction();

        Framework.getProperties().put(Environment.PRODUCT_NAME, "WOPI Test");
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port + "/";
    }

    protected void createUsers() {
        DocumentModel joe = userManager.getBareUserModel();
        joe.setPropertyValue("user:username", "joe");
        joe.setPropertyValue("user:password", "joe");
        joe.setPropertyValue("user:firstName", "Joe");
        joe.setPropertyValue("user:lastName", "Jackson");
        userManager.createUser(joe);

        DocumentModel john = userManager.getBareUserModel();
        john.setPropertyValue("user:username", "john");
        john.setPropertyValue("user:password", "john");
        john.setPropertyValue("user:firstName", "John");
        john.setPropertyValue("user:lastName", "Doe");
        userManager.createUser(john);
    }

    @SuppressWarnings("unchecked")
    protected void createDocuments() throws IOException {
        DocumentModel folder = session.createDocumentModel("/", "wopi", "Folder");
        folder = session.createDocument(folder);
        Map<String, String> userPermissions = new HashMap<>();
        userPermissions.put("john", READ_WRITE);
        userPermissions.put("joe", READ);
        setPermissions(folder, userPermissions);

        expectedFileBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
        expectedAttachementBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-attachment.txt"));

        try (CloseableCoreSession johnSession = coreFeature.openCoreSession("john")) {
            blobDoc = johnSession.createDocumentModel("/wopi", "blobDoc", "File");
            blobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) expectedFileBlob);
            blobDoc = johnSession.createDocument(blobDoc);
            // retrieve the blob to get an updated digest
            expectedFileBlob = (Blob) blobDoc.getPropertyValue(FILE_CONTENT_PROPERTY);
            blobDocFileId = FileInfo.computeFileId(blobDoc, FILE_CONTENT_PROPERTY);

            zeroLengthBlobDoc = johnSession.createDocumentModel("/wopi", "zeroLengthBlobDoc", "File");
            Blob zeroLengthBlob = Blobs.createBlob("");
            zeroLengthBlob.setFilename("zero-length-blob");
            zeroLengthBlobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) zeroLengthBlob);
            zeroLengthBlobDoc = johnSession.createDocument(zeroLengthBlobDoc);
            zeroLengthBlobDocFileId = FileInfo.computeFileId(zeroLengthBlobDoc, FILE_CONTENT_PROPERTY);

            hugeBlobDoc = johnSession.createDocumentModel("/wopi", "hugeBlobDoc", "File");
            Blob hugeBlob = mock(Blob.class, withSettings().serializable());
            Mockito.when(hugeBlob.getLength()).thenReturn(Long.MAX_VALUE);
            Mockito.when(hugeBlob.getStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
            Mockito.when(hugeBlob.getFilename()).thenReturn("hugeBlobFilename");
            hugeBlobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) hugeBlob);
            hugeBlobDoc = johnSession.createDocument(hugeBlobDoc);
            hugeBlobDocFileId = FileInfo.computeFileId(hugeBlobDoc, FILE_CONTENT_PROPERTY);

            noBlobDoc = johnSession.createDocumentModel("/wopi", "noBlobDoc", "File");
            noBlobDoc = johnSession.createDocument(noBlobDoc);
            noBlobDocFileId = FileInfo.computeFileId(noBlobDoc, FILE_CONTENT_PROPERTY);

            multipleBlobsDoc = johnSession.createDocumentModel("/wopi", "multipleBlobsDoc", "File");
            multipleBlobsDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) expectedFileBlob);
            List<Map<String, Serializable>> files = Collections.singletonList(
                    Collections.singletonMap("file", (Serializable) expectedAttachementBlob));
            multipleBlobsDoc.setPropertyValue("files:files", (Serializable) files);
            multipleBlobsDoc = johnSession.createDocument(multipleBlobsDoc);
            // retrieve the blob to get an updated digest
            files = (List<Map<String, Serializable>>) multipleBlobsDoc.getPropertyValue("files:files");
            expectedAttachementBlob = (Blob) files.get(0).get("file");

            multipleBlobsDocFileId = FileInfo.computeFileId(multipleBlobsDoc, FILE_CONTENT_PROPERTY);
            multipleBlobsDocAttachementId = FileInfo.computeFileId(multipleBlobsDoc, FILES_FIRST_FILE_PROPERTY);
        }
    }

    protected void setPermissions(DocumentModel doc, Map<String, String> userPermissions) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        userPermissions.forEach((user, permission) -> localACL.add(new ACE(user, permission, true)));
        doc.setACP(acp, true);
    }

    @After
    public void tearDown() {
        Stream.of("john", "joe").forEach(userManager::deleteUser);
        client.destroy();

        Framework.getProperties().remove(Environment.PRODUCT_NAME);
    }

    @Test
    public void testCheckFileInfo() throws IOException, JSONException {
        // fail - 404
        checkGetNotFound();

        // success - john has write access
        Map<String, String> toReplace = new HashMap<>();

        toReplace.put(REPOSITORY_VAR, blobDoc.getRepositoryName());
        toReplace.put(DOC_ID_VAR, blobDoc.getId());
        toReplace.put(XPATH_VAR, FILE_CONTENT_PROPERTY);
        toReplace.put(FILENAME_VAR, "test-file.txt");
        toReplace.put(CHANGE_TOKEN_VAR, "1-0");
        toReplace.put(ITEM_VERSION_VAR, "0");
        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            checkJSONResponse(response, "json/CheckFileInfo-john-write.json", toReplace);
        }

        // success - joe has read access
        try (CloseableClientResponse response = get(joeToken, blobDocFileId)) {
            checkJSONResponse(response, "json/CheckFileInfo-joe-read.json", toReplace);
        }

        // success - john has write access on a file that supports conversion
        DocumentModel convertibleBlobDoc = session.createDocumentModel("/wopi", "convertibleBlobDoc", "File");
        Blob convertibleBlob = Blobs.createBlob("binary content".getBytes());
        String convertibleFilename = "convertibleFile.xls";
        convertibleBlob.setFilename(convertibleFilename);
        convertibleBlobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) convertibleBlob);
        convertibleBlobDoc = session.createDocument(convertibleBlobDoc);
        // retrieve the blob to get an updated digest
        convertibleBlob = (Blob) convertibleBlobDoc.getPropertyValue(FILE_CONTENT_PROPERTY);
        String convertibleBlobDocFileId = FileInfo.computeFileId(convertibleBlobDoc, FILE_CONTENT_PROPERTY);
        transactionalFeature.nextTransaction();
        toReplace.put(DOC_ID_VAR, convertibleBlobDoc.getId());
        toReplace.put(FILENAME_VAR, convertibleFilename);
        toReplace.put(ITEM_VERSION_VAR, "0");
        try (CloseableClientResponse response = get(johnToken, convertibleBlobDocFileId)) {
            checkJSONResponse(response, "json/CheckFileInfo-john-convert.json", toReplace);
        }
    }

    @Test
    public void testGetFile() throws IOException {
        // fail - 404
        checkGetNotFound(CONTENTS_PATH);

        // fail - 412 - blob size exceeding Integer.MAX_VALUE
        try (CloseableClientResponse response = get(joeToken, hugeBlobDocFileId, CONTENTS_PATH)) {
            assertEquals(412, response.getStatus());
        }

        // fail - 412 - blob size exceeding X-WOPI-MaxExpectedSize header
        Map<String, String> headers = new HashMap<>();
        headers.put(MAX_EXPECTED_SIZE, "1");
        try (CloseableClientResponse response = get(joeToken, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(412, response.getStatus());
        }

        // success - bad header
        headers.put(MAX_EXPECTED_SIZE, "foo");
        String expectedFileBlobString = expectedFileBlob.getString();
        try (CloseableClientResponse response = get(joeToken, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedFileBlobString, actualBlob.getString());
        }

        // success - no header
        try (CloseableClientResponse response = get(joeToken, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedFileBlobString, actualBlob.getString());
        }
    }

    @Test
    public void testLock() {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());

        // fail - 404
        checkPostNotFound(headers);

        // fail - 400 - no X-WOPI-Lock header
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 400 - empty header
        headers.put(LOCK, "");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 409 - no write permission, cannot lock
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
        }

        // success - 200 - can lock
        assertLockResponseOKForJohn(headers);

        // success - 200 - refresh lock
        assertLockResponseOKForJohn(headers);

        // fail - 409 - locked by another client
        headers.put(LOCK, "bar");
        assertConflictResponseWithLock(headers);

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }
    }

    protected void assertLockResponseOKForJohn(Map<String, String> headers) {
        assertLockResponseOK(johnToken, headers);
    }

    protected void assertLockResponseOKForJoe(Map<String, String> headers) {
        assertLockResponseOK(joeToken, headers);
    }

    protected void assertLockResponseOK(String userToken, Map<String, String> headers) {
        try (CloseableClientResponse response = post(userToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }
    }

    protected void assertConflictResponseWithLock(CloseableClientResponse response) {
        assertEquals(409, response.getStatus());
        transactionalFeature.nextTransaction();
        assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        String lock = response.getHeaders().getFirst(LOCK);
        assertEquals("foo", lock);
    }

    protected void assertConflictResponseWithLock(Map<String, String> headers) {
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertConflictResponseWithLock(response);
        }
    }

    @Test
    public void testGetLock() {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.GET_LOCK.name());

        // fail - 404
        checkPostNotFound(headers);

        // success - 200 - document not locked
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("", lock);
        }

        // lock document from WOPI client
        headers.put(OVERRIDE, Operation.LOCK.name());
        String expectedLock = "foo";
        headers.put(LOCK, expectedLock);
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // success - 200 - return lock
        headers.remove(LOCK);
        headers.put(OVERRIDE, Operation.GET_LOCK.name());
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals(expectedLock, lock);
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }
    }

    @Test
    public void testUnlock() {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.UNLOCK.name());

        // fail - 404
        checkPostNotFound(headers);

        // fail - 400 - no X-WOPI-Lock header
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 400 - empty header
        headers.put(LOCK, "");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 409 - not locked
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("", lock);
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }

        // lock document from WOPI client
        headers.put(OVERRIDE, Operation.LOCK.name());
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - no write permission, cannot unlock
        headers.put(OVERRIDE, Operation.UNLOCK.name());
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - lock mismatch
        headers.put(LOCK, "bar");
        assertConflictResponseWithLock(headers);

        // success - 200 - can unlock
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertFalse(session.getDocument(blobDoc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }
    }

    @Test
    public void testRefresh() {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.REFRESH_LOCK.name());

        // fail - 404
        checkPostNotFound(headers);

        // fail - 400 - no X-WOPI-Lock header
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 400 - empty header
        headers.put(LOCK, "");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 409 - not locked
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("", lock);
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }

        // lock document from WOPI client
        headers.put(OVERRIDE, Operation.LOCK.name());
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - no write permission, cannot unlock
        headers.put(OVERRIDE, Operation.REFRESH_LOCK.name());
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - lock mismatch
        headers.put(LOCK, "bar");
        assertConflictResponseWithLock(headers);

        // success - 200 - can refresh lock
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }
    }

    @Test
    public void testUnlockAndRelock() {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());

        // fail - 404
        checkPostNotFound(headers);

        // fail - 400 - no X-WOPI-Lock header
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 400 - empty header
        headers.put(LOCK, "");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(400, response.getStatus());
        }

        // fail - 409 - cannot unlock and relock unlocked document
        headers.put(LOCK, "foo");
        headers.put(OLD_LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertFalse(session.getDocument(blobDoc.getRef()).isLocked());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("", lock);
        }

        // lock document from WOPI client
        headers.remove(OLD_LOCK);
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // success - 200 - lock and relock
        headers.put(LOCK, "bar");
        headers.put(OLD_LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
            String lock = LockHelper.getLock(blobDocFileId);
            assertEquals("bar", lock);
        }

        // fail - 409 - locked by another client
        headers.put(LOCK, "bar");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("bar", lock);
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }
    }

    @Test
    public void testRenameFile() throws IOException, JSONException {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.RENAME_FILE.name());

        checkPostNotFound(headers, CONTENTS_PATH);

        // fail - 409 - joe has no write permission
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
        }

        // success - 200 - blob renamed
        headers.put(REQUESTED_NAME, "renamed-test-file");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            checkJSONResponse(response, "json/RenameFile.json");
            transactionalFeature.nextTransaction();
            Blob renamedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(renamedBlob);
            assertEquals("renamed-test-file.txt", renamedBlob.getFilename());
        }

        // lock document from WOPI client
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - joe has no write permission
        headers.put(OVERRIDE, Operation.RENAME_FILE.name());
        headers.put(REQUESTED_NAME, "renamed-wopi-locked-test-file");
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
        }

        // success - 200 - blob renamed
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            checkJSONResponse(response, "json/RenameFile-wopiLocked.json");
            transactionalFeature.nextTransaction();
            Blob renamedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(renamedBlob);
            assertEquals("renamed-wopi-locked-test-file.txt", renamedBlob.getFilename());
        }

        // fail - 409 - locked by another client
        headers.put(LOCK, "bar");
        headers.put(REQUESTED_NAME, "renamed-wopi-locked-other-client-test-file");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(409, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("foo", lock);
            transactionalFeature.nextTransaction();
            DocumentModel doc = session.getDocument(blobDoc.getRef());
            assertTrue(doc.isLocked());
            Blob blob = (Blob) doc.getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(blob);
            assertEquals("renamed-wopi-locked-test-file.txt", blob.getFilename());
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        headers.remove(LOCK);
        headers.put(REQUESTED_NAME, "renamed-wopi-locked-nuxeo-test-file");
        try (CloseableClientResponse response = post(johnToken, headers, hugeBlobDocFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            DocumentModel doc = session.getDocument(hugeBlobDoc.getRef());
            assertTrue(doc.isLocked());
            Blob blob = (Blob) doc.getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(blob);
            assertEquals("hugeBlobFilename", blob.getFilename());
        }
    }

    @Test
    public void testPutFile() throws IOException {
        String data = "new content";
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.PUT.name());

        checkPostNotFound(headers, CONTENTS_PATH);

        // fail - 409 - joe has no write permission
        try (CloseableClientResponse response = post(joeToken, data, headers, zeroLengthBlobDocFileId, CONTENTS_PATH)) {
            assertEquals(409, response.getStatus());
        }

        // success - 200 - blob updated
        try (CloseableClientResponse response = post(johnToken, data, headers, zeroLengthBlobDocFileId,
                CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(zeroLengthBlobDoc.getRef())
                                             .getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("zero-length-blob", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("1", itemVersion);
        }

        // fail - 409 - not locked and blob present
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(409, response.getStatus());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("", lock);
        }

        // lock document from WOPI client
        headers.put(LOCK, "foo");
        headers.put(OVERRIDE, Operation.LOCK.name());
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // fail - 409 - joe has no write permission
        headers.put(OVERRIDE, Operation.PUT.name());
        try (CloseableClientResponse response = post(joeToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(409, response.getStatus());
        }

        // success - 200 - blob updated
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("test-file.txt", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("1", itemVersion);
        }

        // fail - 409 - locked by another client
        headers.put(LOCK, "bar");
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertConflictResponseWithLock(response);
        }

        // fail - 409 - locked by Nuxeo
        session.getDocument(hugeBlobDoc.getRef()).setLock();
        transactionalFeature.nextTransaction();
        try (CloseableClientResponse response = post(johnToken, data, headers, hugeBlobDocFileId, CONTENTS_PATH)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(hugeBlobDoc.getRef()).isLocked());
        }
    }

    @Test
    public void testMultiplePutFile() throws IOException {
        String data = "new content";
        Map<String, String> headers = new HashMap<>();

        // lock document from WOPI client
        headers.put(LOCK, "foo");
        headers.put(OVERRIDE, Operation.LOCK.name());
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(blobDoc.getRef()).isLocked());
        }

        // 1st PUT - success - 200 - blob updated
        headers.put(OVERRIDE, Operation.PUT.name());
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("test-file.txt", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("1", itemVersion);
        }

        // 2nd PUT - success - 200 - same blob
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("test-file.txt", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            // item version has changed
            assertEquals("2", itemVersion);
        }

        // 3rd PUT - success - 200 - same blob
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("test-file.txt", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            // item version has changed
            assertEquals("3", itemVersion);
        }
    }

    @Test
    public void testPutRelativeFile() {
        String data = "new content";
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.PUT_RELATIVE.name());

        checkPostNotFound(headers);

        // fail - 501 - no headers
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }

        // fail - 501 - both headers
        headers.put(SUGGESTED_TARGET, "new file.docx");
        headers.put(RELATIVE_TARGET, "new file.docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }

        // fail - 501 - file creation not supported
        headers.remove(SUGGESTED_TARGET);
        try (CloseableClientResponse response = post(joeToken, data, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }
    }

    @Test
    public void testFileConversion() throws IOException {
        String data = "Converted content.";
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.PUT_RELATIVE.name());
        headers.put(FILE_CONVERSION, "True");

        // success - 200 - conversion from relative target
        headers.put(RELATIVE_TARGET, "new file.docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            // a version is done before updating the document, thus 0.1+
            assertPutRelativeFileCreateVersionResponse(response, blobDoc.getRef(), FILE_CONTENT_PROPERTY,
                    "new file.docx", getBaseURL(), data, "0.1+");

        }

        // success - 200 - conversion from suggested extension
        headers.remove(RELATIVE_TARGET);
        headers.put(SUGGESTED_TARGET, ".docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, zeroLengthBlobDocFileId)) {
            // a version is done before updating the document, thus 0.1+
            assertPutRelativeFileCreateVersionResponse(response, zeroLengthBlobDoc.getRef(), FILE_CONTENT_PROPERTY,
                    "zero-length-blob.docx", getBaseURL(), data, "0.1+");
        }

        // success - 200 - conversion from suggested filename
        headers.put(SUGGESTED_TARGET, "foo.docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, hugeBlobDocFileId)) {
            // a version is done before updating the document, thus 0.1+
            assertPutRelativeFileCreateVersionResponse(response, hugeBlobDoc.getRef(), FILE_CONTENT_PROPERTY,
                    "foo.docx", getBaseURL(), data, "0.1+");
        }

        // success - 200 - conversion from suggested filename with a custom WOPI base URL
        headers.put(SUGGESTED_TARGET, "bar.docx");
        String customWOPIBaseURL = "http://foo";
        try {
            Framework.getProperties().setProperty(WOPI_BASE_URL_PROPERTY, customWOPIBaseURL);
            try (CloseableClientResponse response = post(johnToken, data, headers, hugeBlobDocFileId)) {
                // a version is done before updating the document, thus 0.2+
                assertPutRelativeFileCreateVersionResponse(response, hugeBlobDoc.getRef(), FILE_CONTENT_PROPERTY,
                        "bar.docx", customWOPIBaseURL, data, "0.2+");
            }
        } finally {
            Framework.getProperties().remove(WOPI_BASE_URL_PROPERTY);
        }
    }

    protected void assertPutRelativeFileCreateVersionResponse(CloseableClientResponse response, DocumentRef docRef,
            String xpath, String newName, String wopiBaseURL, String newContent, String newVersion) throws IOException {
        assertEquals(200, response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(newName, node.get(NAME).asText());
        JsonNode jsonNode = node.get(URL);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.asText().startsWith(wopiBaseURL));
        jsonNode = node.get(HOST_EDIT_URL);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.asText().startsWith(getBaseURL()));
        jsonNode = node.get(HOST_VIEW_URL);
        assertNotNull(jsonNode);
        String hostViewUrl = jsonNode.asText();
        assertTrue(hostViewUrl.startsWith(getBaseURL()));

        transactionalFeature.nextTransaction();
        DocumentModel doc = session.getDocument(docRef);

        // request made in the context of a conversion
        Blob blob = (Blob) doc.getPropertyValue(xpath);
        assertNotNull(blob);
        assertEquals(newName, blob.getFilename());
        assertEquals(newContent, blob.getString());
        assertEquals(newVersion, doc.getVersionLabel());
        List<DocumentModel> versions = session.getVersions(docRef);
        assertTrue(versions.size() >= 1);
    }

    @Test
    public void testGetShareUrl() throws IOException, JSONException {
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.GET_SHARE_URL.name());

        checkPostNotFound(headers);

        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }

        headers.put(URL_TYPE, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }

        headers.put(URL_TYPE, SHARE_URL_READ_ONLY);
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put(REPOSITORY_VAR, blobDoc.getRepositoryName());
        toReplace.put(DOC_ID_VAR, blobDoc.getId());
        toReplace.put(XPATH_VAR, FILE_CONTENT_PROPERTY);
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            checkJSONResponse(response, "json/GetShareUrl-read-only.json", toReplace);
        }

        headers.put(URL_TYPE, SHARE_URL_READ_WRITE);
        try (CloseableClientResponse response = post(johnToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            checkJSONResponse(response, "json/GetShareUrl-read-write.json", toReplace);
        }
    }

    @Test
    public void testOnAttachment() throws IOException, JSONException {
        DocumentRef multipleBlobsDocRef = multipleBlobsDoc.getRef();
        // CheckFileInfo
        try (CloseableClientResponse response = get(johnToken, multipleBlobsDocAttachementId)) {
            Map<String, String> toReplace = new HashMap<>();
            toReplace.put(REPOSITORY_VAR, multipleBlobsDoc.getRepositoryName());
            toReplace.put(DOC_ID_VAR, multipleBlobsDoc.getId());
            toReplace.put(XPATH_VAR, FILES_FIRST_FILE_PROPERTY);
            toReplace.put(FILENAME_VAR, "test-attachment.txt");
            toReplace.put(CHANGE_TOKEN_VAR, "1-0");
            toReplace.put(ITEM_VERSION_VAR, "0");
            checkJSONResponse(response, "json/CheckFileInfo-files-john-write.json", toReplace);
        }

        // GetFile
        try (CloseableClientResponse response = get(joeToken, multipleBlobsDocAttachementId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedAttachementBlob.getString(), actualBlob.getString());
        }

        // Lock
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }

        // PutFile
        String data = "new attachment";
        headers.put(OVERRIDE, Operation.PUT.name());
        try (CloseableClientResponse response = post(johnToken, data, headers, multipleBlobsDocAttachementId,
                CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(multipleBlobsDocRef)
                                             .getPropertyValue(FILES_FIRST_FILE_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new attachment", updatedBlob.getString());
            assertEquals("test-attachment.txt", updatedBlob.getFilename());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("1", itemVersion);
        }

        // PutRelativeFile - file conversion
        data = "converted attachment";
        headers.put(OVERRIDE, Operation.PUT_RELATIVE.name());
        headers.put(FILE_CONVERSION, "True");
        headers.put(SUGGESTED_TARGET, ".docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, multipleBlobsDocAttachementId)) {
            // a version is done before updating the document, thus 0.1+
            assertPutRelativeFileCreateVersionResponse(response, multipleBlobsDoc.getRef(), FILES_FIRST_FILE_PROPERTY,
                    "test-attachment.docx", getBaseURL(), data, "0.1+");
        }
    }

    @Test
    public void testLockOnMultipleBlobs() {
        DocumentRef multipleBlobsDocRef = multipleBlobsDoc.getRef();
        // Lock file:content
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }

        // Lock files:files/0/file
        headers.put(LOCK, "fooAttachment");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // doc is still locked
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }

        // Relock file:content
        // Such a call to Lock can happen when another user is asking to edit the file but it should behave the same way
        // if the call is made by the same user so we can use johnToken here
        headers.put(LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }

        // Unlock files:files/0/file
        headers.put(OVERRIDE, Operation.UNLOCK.name());
        // fail - 409 - lock mismatch
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("fooAttachment", lock);
        }

        // Unlock file:content
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // document is still locked, WOPI lock set on files:files/0/file
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }

        // Unlock files:files/0/file
        headers.put(LOCK, "fooAttachment");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // document is now unlocked, no more WOPI lock set
            assertFalse(session.getDocument(multipleBlobsDocRef).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }
    }

    @Test
    public void testUnlockAndRelockOnMultipleBlobs() {
        DocumentRef multipleBlobsDocRef = multipleBlobsDoc.getRef();
        // Lock file:content
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String lock = LockHelper.getLock(multipleBlobsDocFileId);
            assertEquals("fooContent", lock);
        }

        // Unlock and relock file:content
        headers.put(LOCK, "barContent");
        headers.put(OLD_LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String lock = LockHelper.getLock(multipleBlobsDocFileId);
            assertEquals("barContent", lock);
        }

        // Lock files:files/0/file
        headers.put(LOCK, "fooAttachment");
        headers.remove(OLD_LOCK);
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String lock = LockHelper.getLock(multipleBlobsDocAttachementId);
            assertEquals("fooAttachment", lock);
        }

        // Unlock and relock files:files/0/file
        headers.put(LOCK, "barAttachment");
        headers.put(OLD_LOCK, "fooAttachment");
        try (CloseableClientResponse response = post(johnToken, headers, multipleBlobsDocAttachementId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(multipleBlobsDocRef).isLocked());
            String lock = LockHelper.getLock(multipleBlobsDocAttachementId);
            assertEquals("barAttachment", lock);
        }
    }

    @Test
    public void testCanUnlockIfNotLastUser() {
        // grant write access to joe
        blobDoc = session.getDocument(blobDoc.getRef());
        setPermissions(blobDoc, Collections.singletonMap("joe", READ_WRITE));

        // lock file as john
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "foo");
        assertLockResponseOKForJohn(headers);

        // refresh lock as joe
        assertLockResponseOKForJoe(headers);

        // unlock file as joe
        headers.put(OVERRIDE, Operation.UNLOCK.name());
        try (CloseableClientResponse response = post(joeToken, headers, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertFalse(session.getDocument(blobDoc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0", itemVersion);
        }
    }

    // NXP-30585
    @Test
    public void testItemVersionWithBlobUpdate() throws IOException {
        // file:content
        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("0", node.get("Version").asText());
        }

        DocumentModel doc = session.getDocument(blobDoc.getRef());
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) Blobs.createBlob("foo"));
        session.saveDocument(doc);
        transactionalFeature.nextTransaction();

        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("1", node.get("Version").asText());
        }

        // files:files/0/file
        try (CloseableClientResponse response = get(johnToken, multipleBlobsDocAttachementId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("0", node.get("Version").asText());
        }

        doc = session.getDocument(multipleBlobsDoc.getRef());
        doc.setPropertyValue(FILES_FIRST_FILE_PROPERTY, (Serializable) Blobs.createBlob("foo"));
        session.saveDocument(doc);
        transactionalFeature.nextTransaction();

        try (CloseableClientResponse response = get(johnToken, multipleBlobsDocAttachementId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("1", node.get("Version").asText());
        }
    }

    protected void checkPostNotFound(Map<String, String> headers) {
        checkPostNotFound(headers, "");
    }

    protected void checkPostNotFound(Map<String, String> headers, String additionalPath) {
        // not found
        try (CloseableClientResponse response = get(johnToken, headers, "foo", additionalPath)) {
            assertEquals(404, response.getStatus());
        }

        // no blob
        try (CloseableClientResponse response = get(johnToken, headers, noBlobDocFileId, additionalPath)) {
            assertEquals(404, response.getStatus());
        }
    }

    protected void checkGetNotFound() {
        checkGetNotFound("");
    }

    protected void checkGetNotFound(String additionalPath) {
        // not found
        try (CloseableClientResponse response = get(johnToken, "foo", additionalPath)) {
            assertEquals(404, response.getStatus());
        }

        // no blob
        try (CloseableClientResponse response = get(johnToken, noBlobDocFileId, additionalPath)) {
            assertEquals(404, response.getStatus());
        }
    }

    protected void checkJSONResponse(ClientResponse response, String expectedJSONFile)
            throws IOException, JSONException {
        checkJSONResponse(response, expectedJSONFile, Collections.emptyMap());
    }

    protected void checkJSONResponse(ClientResponse response, String expectedJSONFile, Map<String, String> toReplace)
            throws IOException, JSONException {
        assertEquals(200, response.getStatus());
        toReplace = new HashMap<>(toReplace);
        toReplace.put("PORT", String.valueOf(servletContainerFeature.getPort()));
        String json = response.getEntity(String.class);
        File file = FileUtils.getResourceFileFromContext(expectedJSONFile);
        String expected = readFile(file, toReplace);
        JSONAssert.assertEquals(expected, json, true);
    }

    protected CloseableClientResponse get(String token, String... path) {
        return get(token, null, path);
    }

    protected CloseableClientResponse get(String token, Map<String, String> headers, String... path) {
        WebResource wr = client.resource(getBaseURL() + WOPI_FILES)
                               .path(String.join("/", path))
                               .queryParam(ACCESS_TOKEN_PARAMETER, token);
        WebResource.Builder builder = wr.getRequestBuilder();
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return CloseableClientResponse.of(builder.get(ClientResponse.class));
    }

    protected CloseableClientResponse post(String token, Map<String, String> headers, String... path) {
        return post(token, null, headers, path);
    }

    protected CloseableClientResponse post(String token, String data, Map<String, String> headers, String... path) {
        WebResource wr = client.resource(getBaseURL() + WOPI_FILES)
                               .path(String.join("/", path))
                               .queryParam(ACCESS_TOKEN_PARAMETER, token);
        WebResource.Builder builder = wr.getRequestBuilder();
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return CloseableClientResponse.of(
                data != null ? builder.post(ClientResponse.class, data) : builder.post(ClientResponse.class));
    }
}
