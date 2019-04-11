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

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.nuxeo.ecm.tokenauth.io.AuthenticationToken;
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
        Set<AuthenticationToken> expectedTokenBindings = new HashSet<>();
        expectedTokenBindings.add(new AuthenticationToken(token1, "joe", "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal Linux box", "rw"));
        expectedTokenBindings.add(new AuthenticationToken(token2, "joe", "myFavoriteApp", "Windows box 32 bits",
                "This is my personal Windows box", "rw"));
        expectedTokenBindings.add(new AuthenticationToken(token3, "joe", "nuxeoDrive", "Mac OSX VM",
                "This is my personal Mac box", "rw"));
        assertTrue(CollectionUtils.isEqualCollection(expectedTokenBindings, asAuthenticationTokens(tokenBindings)));
        for (DocumentModel tokenBinding : tokenBindings) {
            assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));
        }
    }

    private List<AuthenticationToken> asAuthenticationTokens(DocumentModelList entries) {
        return entries.stream().map(this::asAuthenticationToken).collect(Collectors.toList());
    }

    private AuthenticationToken asAuthenticationToken(DocumentModel entry) {
        Map<String, Object> props = entry.getProperties("authtoken");
        AuthenticationToken token = new AuthenticationToken(
                (String) props.get("token"),
                (String) props.get("userName"),
                (String) props.get("applicationName"),
                (String) props.get("deviceId"),
                (String) props.get("deviceDescription"),
                (String) props.get("permission"));
        token.setCreationDate((Calendar) props.get("creationDate"));
        return token;
    }
}
