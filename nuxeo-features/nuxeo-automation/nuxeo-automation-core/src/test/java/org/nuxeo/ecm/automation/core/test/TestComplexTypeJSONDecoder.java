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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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

    @Inject
    protected ClientLoginFeature loginFeature;

    @Inject
    protected UserManager userManager;

    @Before
    public void setUp() throws Exception {
        loginFeature.login("Administrator");
    }

    @After
    public void tearDown() throws Exception {
        loginFeature.logout();
    }

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
    public void testDecodeManagedBlobWithUnauthorizedAccess() throws Exception {
        String json = "{\"providerId\":\"testBlobProvider\", \"key\":\"testKey\"}";
        loginFeature.login("dummyName");
        try {
            ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            fail("The blob should not have been fetched");
        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, e.getStatusCode());
        } finally {
            loginFeature.logout();
        }
    }

    @Test
    public void testDecodeManagedBlobWithAuthorizedUsers() throws Exception {
        String testUser1 = "testUser1";
        String testUser2 = "testUser2";
        String testUser3 = "testUser3";
        String testUser4 = "testUser4";
        String testGroup = "testGroup";

        createUser(testUser1);
        createUser(testUser2);
        createUser(testUser3);
        createUser(testUser4);
        createGroup(testGroup);

        NuxeoPrincipal principal = userManager.getPrincipal(testUser3);
        principal.setGroups(Collections.singletonList(testGroup));
        userManager.updateUser(principal.getModel());

        String json = "{\"providerId\":\"testBlobProviderWithAuthorizedUsers\", \"key\":\"testKey\"}";
        loginFeature.login(testUser1);
        Blob blob;
        try {
            blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            assertNotNull(blob);

            loginFeature.logout();
            loginFeature.login(testUser2);
            blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            assertNotNull(blob);

            loginFeature.logout();
            loginFeature.login(testUser3);
            blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));
            assertNotNull(blob);

            loginFeature.logout();
            loginFeature.login(testUser4);
            ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) OBJECT_MAPPER.readTree(json));

        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, e.getStatusCode());

        } finally {
            loginFeature.logout();
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
