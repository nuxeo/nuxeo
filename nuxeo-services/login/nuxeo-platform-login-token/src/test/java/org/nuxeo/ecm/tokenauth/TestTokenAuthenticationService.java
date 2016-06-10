/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationServiceFeature.class)
public class TestTokenAuthenticationService {

    private static final Log log = LogFactory.getLog(TestTokenAuthenticationService.class);

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected CoreFeature coreFeature;

    @After
    public void cleanDirectories() throws Exception {
        try (Session tokenDirSession = directoryService.open("authTokens")) {
            DocumentModelList entries = tokenDirSession.getEntries();
            for (DocumentModel entry : entries) {
                tokenDirSession.deleteEntry(entry);
            }
        }
    }

    @Test
    public void testAcquireToken() {

        // Test omitting required parameters
        try {
            tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", null, null, null);
            fail("Getting token should have failed since required parameters are missing.");
        } catch (TokenAuthenticationException e) {
            assertEquals(
                    "The following parameters are mandatory to get an authentication token: userName, applicationName, deviceId.",
                    e.getMessage());
        }

        // Test token generation
        String token = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertNotNull(token);

        // Test token binding persistence
        try (Session directorySession = directoryService.open("authTokens")) {
            DocumentModel tokenModel = directorySession.getEntry(token);
            assertNotNull(tokenModel);
            assertEquals(token, tokenModel.getPropertyValue("authtoken:token"));
            assertEquals("joe", tokenModel.getPropertyValue("authtoken:userName"));
            assertEquals("myFavoriteApp", tokenModel.getPropertyValue("authtoken:applicationName"));
            assertEquals("Ubuntu box 64 bits", tokenModel.getPropertyValue("authtoken:deviceId"));
            assertEquals("This is my personal box", tokenModel.getPropertyValue("authtoken:deviceDescription"));
            assertEquals("rw", tokenModel.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenModel.getPropertyValue("authtoken:creationDate"));
        }

        // Test existing token acquisition
        String sameToken = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertEquals(token, sameToken);

        // Test token uniqueness
        String otherToken = tokenAuthenticationService.acquireToken("jack", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertTrue(!otherToken.equals(token));
    }

    @Test
    public void testGetToken() throws TokenAuthenticationException {

        // Test non existing token retrieval
        assertNull(tokenAuthenticationService.getToken("john", "myFavoriteApp", "Ubuntu box 64 bits"));

        // Test existing token retrieval
        tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertNotNull(tokenAuthenticationService.getToken("joe", "myFavoriteApp", "Ubuntu box 64 bits"));
    }

    @Test
    public void testGetUserName() throws TokenAuthenticationException {

        // Test invalid token
        String token = "invalidToken";
        String userName = tokenAuthenticationService.getUserName(token);
        assertNull(userName);

        // Test valid token
        token = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        userName = tokenAuthenticationService.getUserName(token);
        assertEquals("joe", userName);
    }

    @Test
    public void testRevokeToken() throws TokenAuthenticationException {

        // Test revoking an unexisting token, should not fail
        tokenAuthenticationService.revokeToken("unexistingToken");

        // Test revoking an existing token
        String token = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertEquals("joe", tokenAuthenticationService.getUserName(token));

        tokenAuthenticationService.revokeToken(token);
        assertNull(tokenAuthenticationService.getUserName(token));
    }

    @Test
    public void testGetTokenBindings() {

        // Test empty token bindings
        assertEquals(0, tokenAuthenticationService.getTokenBindings("john").size());

        // Test existing token bindings
        String token1 = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal Linux box", "rw");
        log.debug("token1 = " + token1);
        String token2 = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp", "Windows box 32 bits",
                "This is my personal Windows box", "rw");
        log.debug("token2 = " + token2);
        String token3 = tokenAuthenticationService.acquireToken("joe", "nuxeoDrive", "Mac OSX VM",
                "This is my personal Mac box", "rw");
        log.debug("token3 = " + token3);

        DocumentModelList tokenBindings = tokenAuthenticationService.getTokenBindings("joe");
        assertEquals(3, tokenBindings.size());
        Set<TokenBindingMock> expectedTokenBindings = new HashSet<TokenBindingMock>();
        expectedTokenBindings.add(new TokenBindingMock(token1, "joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal Linux box", "rw"));
        expectedTokenBindings.add(new TokenBindingMock(token2, "joe", "myFavoriteApp", "Windows box 32 bits",
                "This is my personal Windows box", "rw"));
        expectedTokenBindings.add(new TokenBindingMock(token3, "joe", "nuxeoDrive", "Mac OSX VM",
                "This is my personal Mac box", "rw"));
        assertTrue(CollectionUtils.isEqualCollection(expectedTokenBindings, toTokenBindingMocks(tokenBindings)));
        for (DocumentModel tokenBinding : tokenBindings) {
            assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));
        }
    }

    protected Set<TokenBindingMock> toTokenBindingMocks(DocumentModelList tokenBindings) {
        Set<TokenBindingMock> tokenBindingMocks = new HashSet<TokenBindingMock>();
        for (DocumentModel tokenBinding : tokenBindings) {
            tokenBindingMocks.add(toTokenBindingMock(tokenBinding));
        }
        return tokenBindingMocks;
    }

    protected TokenBindingMock toTokenBindingMock(DocumentModel tokenBinding) {
        return new TokenBindingMock((String) tokenBinding.getPropertyValue("authtoken:token"),
                (String) tokenBinding.getPropertyValue("authtoken:userName"),
                (String) tokenBinding.getPropertyValue("authtoken:applicationName"),
                (String) tokenBinding.getPropertyValue("authtoken:deviceId"),
                (String) tokenBinding.getPropertyValue("authtoken:deviceDescription"),
                (String) tokenBinding.getPropertyValue("authtoken:permission"));
    }

    protected final class TokenBindingMock {

        protected String token;

        protected String userName;

        protected String applicationName;

        protected String deviceId;

        protected String deviceDescription;

        protected String permission;

        public TokenBindingMock(String token, String userName, String applicationName, String deviceId,
                String deviceDescription, String permission) {
            this.token = token;
            this.userName = userName;
            this.applicationName = applicationName;
            this.deviceId = deviceId;
            this.deviceDescription = deviceDescription;
            this.permission = permission;
        }

        public String getToken() {
            return token;
        }

        public String getUserName() {
            return userName;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getDeviceDescription() {
            return deviceDescription;
        }

        public String getPermission() {
            return permission;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 37 + token.hashCode();
            hash = hash * 37 + userName.hashCode();
            hash = hash * 37 + applicationName.hashCode();
            hash = hash * 37 + deviceId.hashCode();
            hash = hash * 37 + deviceDescription.hashCode();
            return hash * 37 + permission.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TokenBindingMock)) {
                return false;
            }
            TokenBindingMock other = (TokenBindingMock) obj;
            return token.equals(other.getToken()) && userName.equals(other.getUserName())
                    && applicationName.equals(other.getApplicationName()) && deviceId.equals(other.getDeviceId())
                    && deviceDescription.equals(other.getDeviceDescription())
                    && permission.equals(other.getPermission());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(token);
            sb.append(", ");
            sb.append(userName);
            sb.append(", ");
            sb.append(applicationName);
            sb.append(", ");
            sb.append(deviceId);
            sb.append(", ");
            sb.append(deviceDescription);
            sb.append(", ");
            sb.append(permission);
            sb.append(")");
            return sb.toString();
        }

    }

}
