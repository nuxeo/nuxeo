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
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;
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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.jwt.JWTService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.Operation;
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
@Features({ WOPIFeature.class, RestServerFeature.class })
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.wopi:test-jwt-contrib.xml")
@ServletContainer(port = 18090)
public class TestFilesEndpoint {

    public static final String BASE_URL = "http://localhost:18090/wopi/files";

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

    protected void createDocuments() throws IOException {
        DocumentModel folder = session.createDocumentModel("/", "wopi", "Folder");
        folder = session.createDocument(folder);
        Map<String, String> userPermissions = new HashMap<>();
        userPermissions.put("john", READ_WRITE);
        userPermissions.put("joe", READ);
        setPermissions(folder, userPermissions);

        try (CloseableCoreSession johnSession = coreFeature.openCoreSession("john")) {
            blobDoc = johnSession.createDocumentModel("/wopi", "blobDoc", "File");
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
            blobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
            blobDoc = johnSession.createDocument(blobDoc);
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
        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            checkJSONResponse(response, "json/CheckFileInfo-john-write.json", toReplace);
        }

        // success - joe has read access
        try (CloseableClientResponse response = get(joeToken, blobDocFileId)) {
            checkJSONResponse(response, "json/CheckFileInfo-joe-read.json", toReplace);
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

        Blob expectedBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
        // success - bad header
        headers.put(MAX_EXPECTED_SIZE, "foo");
        try (CloseableClientResponse response = get(joeToken, headers, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedBlob.getString(), actualBlob.getString());
        }

        // success - no header
        try (CloseableClientResponse response = get(joeToken, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedBlob.getString(), actualBlob.getString());
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
            assertEquals("0.0", itemVersion);
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
            assertEquals("0.0", itemVersion);
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
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.1", itemVersion);
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(zeroLengthBlobDoc.getRef())
                                             .getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("zero-length-blob", updatedBlob.getFilename());
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
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.1", itemVersion);
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(blobDoc.getRef()).getPropertyValue(FILE_CONTENT_PROPERTY);
            assertNotNull(updatedBlob);
            assertEquals("new content", updatedBlob.getString());
            assertEquals("test-file.txt", updatedBlob.getFilename());
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
    public void testPutRelativeFile() throws IOException {
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

        // fail - 501 - no ADD_CHILDREN permission
        headers.remove(SUGGESTED_TARGET);
        try (CloseableClientResponse response = post(joeToken, data, headers, blobDocFileId)) {
            assertEquals(501, response.getStatus());
        }

        // success - 200 - new file from relative target
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            assertPutRelativeFileResponse(response, "new file.docx");
        }

        // success - 200 - new file from suggested extension
        headers.remove(RELATIVE_TARGET);
        headers.put(SUGGESTED_TARGET, ".docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            assertPutRelativeFileResponse(response, "test-file.docx");
        }

        // success - 200 - new file from suggested filename
        headers.put(SUGGESTED_TARGET, "foo.docx");
        try (CloseableClientResponse response = post(johnToken, data, headers, blobDocFileId)) {
            assertPutRelativeFileResponse(response, "foo.docx");
        }
    }

    protected void assertPutRelativeFileResponse(CloseableClientResponse response, String newName) throws IOException {
        assertEquals(200, response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(newName, node.get(NAME).asText());
        assertTrue(node.has(URL));
        assertTrue(node.has(HOST_VIEW_URL));
        assertTrue(node.has(HOST_EDIT_URL));
        String hostViewUrl = node.get(HOST_VIEW_URL).asText();
        String[] split = hostViewUrl.split("/");
        String docId = split[split.length - 2];

        transactionalFeature.nextTransaction();
        DocumentModel newDoc = session.getDocument(new IdRef(docId));
        assertEquals(blobDoc.getParentRef(), newDoc.getParentRef());
        assertEquals(newName, newDoc.getTitle());
        Blob blob = (Blob) newDoc.getPropertyValue(FILE_CONTENT_PROPERTY);
        assertNotNull(blob);
        assertEquals(newName, blob.getFilename());
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
        // create a document with an attachment
        DocumentModel doc;
        try (CloseableCoreSession johnSession = coreFeature.openCoreSession("john")) {
            doc = johnSession.createDocumentModel("/wopi", "doc", "File");
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-attachment.txt"));
            List<Map<String, Serializable>> files = Collections.singletonList(
                    Collections.singletonMap("file", (Serializable) blob));
            doc.setPropertyValue("files:files", (Serializable) files);
            doc = johnSession.createDocument(doc);
        }
        String fileId = FileInfo.computeFileId(doc, "files:files/0/file");
        transactionalFeature.nextTransaction();

        // CheckFileInfo
        try (CloseableClientResponse response = get(johnToken, fileId)) {
            Map<String, String> toReplace = new HashMap<>();
            toReplace.put(REPOSITORY_VAR, doc.getRepositoryName());
            toReplace.put(DOC_ID_VAR, doc.getId());
            toReplace.put(XPATH_VAR, "files:files/0/file");
            toReplace.put(FILENAME_VAR, "test-attachment.txt");
            toReplace.put(CHANGE_TOKEN_VAR, "1-0");
            checkJSONResponse(response, "json/CheckFileInfo-files-john-write.json", toReplace);
        }

        // GetFile
        Blob expectedBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-attachment.txt"));
        try (CloseableClientResponse response = get(joeToken, fileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            Blob actualBlob = Blobs.createBlob(response.getEntityInputStream());
            assertEquals(expectedBlob.getString(), actualBlob.getString());
        }

        // Lock
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "foo");
        try (CloseableClientResponse response = post(johnToken, headers, fileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(doc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.0", itemVersion);
        }

        // PutFile
        String data = "new attachment";
        headers.put(OVERRIDE, Operation.PUT.name());
        try (CloseableClientResponse response = post(johnToken, data, headers, fileId, CONTENTS_PATH)) {
            assertEquals(200, response.getStatus());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.1", itemVersion);
            transactionalFeature.nextTransaction();
            Blob updatedBlob = (Blob) session.getDocument(doc.getRef()).getPropertyValue("files:files/0/file");
            assertNotNull(updatedBlob);
            assertEquals("new attachment", updatedBlob.getString());
            assertEquals("test-attachment.txt", updatedBlob.getFilename());
        }
    }

    @Test
    public void testLockOnMultipleBlobs() throws IOException {
        // create a document with 2 blobs
        DocumentModel doc;
        try (CloseableCoreSession johnSession = coreFeature.openCoreSession("john")) {
            doc = johnSession.createDocumentModel("/wopi", "doc", "File");
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
            doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
            blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-attachment.txt"));
            List<Map<String, Serializable>> files = Collections.singletonList(
                    Collections.singletonMap("file", (Serializable) blob));
            doc.setPropertyValue("files:files", (Serializable) files);
            doc = johnSession.createDocument(doc);
        }
        String contentFileId = FileInfo.computeFileId(doc, FILE_CONTENT_PROPERTY);
        String attachmentFileId = FileInfo.computeFileId(doc, "files:files/0/file");
        transactionalFeature.nextTransaction();

        // Lock file:content
        Map<String, String> headers = new HashMap<>();
        headers.put(OVERRIDE, Operation.LOCK.name());
        headers.put(LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, contentFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(doc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.0", itemVersion);
        }

        // Lock files:files/0/file
        headers.put(LOCK, "fooAttachment");
        try (CloseableClientResponse response = post(johnToken, headers, attachmentFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // doc is still locked
            assertTrue(session.getDocument(doc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.0", itemVersion);
        }

        // Unlock
        headers.put(OVERRIDE, Operation.UNLOCK.name());
        // fail - 409 - lock mismatch
        try (CloseableClientResponse response = post(johnToken, headers, contentFileId)) {
            assertEquals(409, response.getStatus());
            transactionalFeature.nextTransaction();
            assertTrue(session.getDocument(doc.getRef()).isLocked());
            String lock = response.getHeaders().getFirst(LOCK);
            assertEquals("fooContent", lock);
        }

        // Unlock files:files/0/file
        try (CloseableClientResponse response = post(johnToken, headers, attachmentFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // document is still locked, WOPI lock set on file:content
            assertTrue(session.getDocument(doc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.0", itemVersion);
        }

        // Unlock file:content
        headers.put(LOCK, "fooContent");
        try (CloseableClientResponse response = post(johnToken, headers, contentFileId)) {
            assertEquals(200, response.getStatus());
            transactionalFeature.nextTransaction();
            // document is now unlocked, no more WOPI lock set
            assertFalse(session.getDocument(doc.getRef()).isLocked());
            String itemVersion = response.getHeaders().getFirst(ITEM_VERSION);
            assertEquals("0.0", itemVersion);
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
            assertEquals("0.0", itemVersion);
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
        String json = response.getEntity(String.class);
        File file = FileUtils.getResourceFileFromContext(expectedJSONFile);
        String expected = readFile(file, toReplace);
        JSONAssert.assertEquals(expected, json, true);
    }

    protected CloseableClientResponse get(String token, String... path) {
        return get(token, null, path);
    }

    protected CloseableClientResponse get(String token, Map<String, String> headers, String... path) {
        WebResource wr = client.resource(BASE_URL).path(String.join("/", path)).queryParam(ACCESS_TOKEN_PARAMETER,
                token);
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
        WebResource wr = client.resource(BASE_URL).path(String.join("/", path)).queryParam(ACCESS_TOKEN_PARAMETER,
                token);
        WebResource.Builder builder = wr.getRequestBuilder();
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return CloseableClientResponse.of(
                data != null ? builder.post(ClientResponse.class, data) : builder.post(ClientResponse.class));
    }
}
