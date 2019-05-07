/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.Blobs.createBlob;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-blobprovider.xml")
public class TestComplexTypeJSONDecoder {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final String OBJECT_BLOB_JSON = "{ \"data\": \"%s\" }";

    protected static final String BASE_URL = "http://localhost:8080/nuxeo/";

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected DownloadService downloadService;

    @Test
    public void testDecodeManagedBlob() throws Exception {
        String json = "{\"providerId\":\"testBlobProvider\", \"key\":\"testKey\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
        assertTrue(blob instanceof ManagedBlob);
        ManagedBlob managedBlob = (ManagedBlob) blob;
        assertEquals("testBlobProvider", managedBlob.getProviderId());
        assertEquals("testBlobProvider:testKey", managedBlob.getKey());
    }

    @Test
    public void testDecodeManagedBlobEmptyKey() throws Exception {
        String emptyKeyJson = "{\"providerId\":\"testBlobProvider\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(emptyKeyJson));
        assertNull(blob);
    }

    @Test
    public void testDecodeManagedBlobUnknownProvider() throws Exception {
        String unknownProviderJson = "{\"providerId\":\"fakeBlobProvider\", \"key\":\"testKey\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(unknownProviderJson));
        assertNull(blob);
    }

    @Test
    @WithUser("dummyName")
    public void testDecodeManagedBlobWithUnauthorizedAccess() throws Exception {
        String json = "{\"providerId\":\"testBlobProvider\", \"key\":\"testKey\"}";
        try {
            ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            fail("The blob should not have been fetched");
        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-blobprovider-authorized-users-property.xml")
    @WithUser("testUser1")
    public void testDecodeManagedBlobWithAuthorizedUserFromPropertyUser1() throws Exception {
        String json = "{\"providerId\":\"testBlobProviderWithAuthorizedUsers\", \"key\":\"testKey\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
        assertNotNull(blob);
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-blobprovider-authorized-users-property.xml")
    @WithUser("testUser2")
    public void testDecodeManagedBlobWithAuthorizedUserFromPropertyUser2() throws Exception {
        String json = "{\"providerId\":\"testBlobProviderWithAuthorizedUsers\", \"key\":\"testKey\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
        assertNotNull(blob);
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-blobprovider-authorized-users-property.xml")
    @WithUser("testUser3")
    public void testDecodeManagedBlobWithAuthorizedUserFromPropertyGroup() throws Exception {
        // add testUser3 to testGroup group
        NuxeoPrincipalImpl user3 = (NuxeoPrincipalImpl) ClientLoginModule.getCurrentPrincipal();
        user3.setVirtualGroups(List.of("testGroup"), true);
        // test
        String json = "{\"providerId\":\"testBlobProviderWithAuthorizedUsers\", \"key\":\"testKey\"}";
        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
        assertNotNull(blob);
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-blobprovider-authorized-users-property.xml")
    @WithUser("testUser4")
    public void testDecodeManagedBlobWithUnauthorizedUserFromProperty() throws Exception {
        try {
            String json = "{\"providerId\":\"testBlobProviderWithAuthorizedUsers\", \"key\":\"testKey\"}";
            Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            assertNotNull(blob);
        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testDecodeBlobObject() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "File");
        Blob blob = createBlob("foo");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getScheme()).thenReturn("http");
        new RequestContext(request, null); // set RequestContext.getActiveContext()

        String downloadURL = BASE_URL + downloadService.getDownloadUrl(doc, "file:content", null);
        String json = String.format(OBJECT_BLOB_JSON, downloadURL);
        Blob resolvedBlob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
        assertEquals("foo", resolvedBlob.getString());
    }

    @Test
    public void testDecodeUnknownBlobObject() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getScheme()).thenReturn("http");
        new RequestContext(request, null); // set RequestContext.getActiveContext()

        // fake url
        String fakeURL = "http://fakeurl.com/nuxeo/foo/bar";
        String json = String.format(OBJECT_BLOB_JSON, fakeURL);
        assertDecodeBlobObjectFail(json);

        // unknown document
        String downloadURL = BASE_URL + downloadService.getDownloadUrl("test", "1", "file:content", "foo.txt");
        json = String.format(OBJECT_BLOB_JSON, downloadURL);
        assertDecodeBlobObjectFail(json);

        // document with no blob
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "File");
        doc = session.createDocument(doc);
        downloadURL = BASE_URL + downloadService.getDownloadUrl(doc, "file:content", null);
        json = String.format(OBJECT_BLOB_JSON, downloadURL);
        assertDecodeBlobObjectFail(json);
    }

    protected void assertDecodeBlobObjectFail(String json) throws IOException {
        try {
            ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            fail();
        } catch (NuxeoException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    protected void createUser(String userId) {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", userId);
        userModel.setProperty("user", "password", userId);
        userManager.createUser(userModel);
    }

    protected void createGroup(String groupId) {
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupId);
        userManager.createGroup(groupModel);
    }

}
