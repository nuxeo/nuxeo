/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationServiceFeature.class)
public class TestTokenAuthenticationService {

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testGetToken() throws ClientException {

        // Test omitting required parameters
        try {
            tokenAuthenticationService.getToken("joe", "myFavoriteApp", null,
                    null);
            fail("Getting token should have failed since required parameters are missing.");
        } catch (TokenAuthenticationException e) {
            assertEquals(
                    "All parameters are mandatory to get an authentication token: userName, applicationName, deviceName, permission.",
                    e.getMessage());
        }

        // Test token generation
        String token = tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits", "rw");
        assertNotNull(token);

        // Test token binding persistence
        Session directorySession = null;
        try {
            directorySession = directoryService.open("authTokens");
            DocumentModel tokenModel = directorySession.getEntry(token);
            assertNotNull(tokenModel);
            assertEquals(token, tokenModel.getPropertyValue("authtoken:token"));
            assertEquals("joe",
                    tokenModel.getPropertyValue("authtoken:userName"));
            assertEquals("myFavoriteApp",
                    tokenModel.getPropertyValue("authtoken:applicationName"));
            assertEquals("Ubuntu box 64 bits",
                    tokenModel.getPropertyValue("authtoken:deviceName"));
            assertEquals("rw",
                    tokenModel.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenModel.getPropertyValue("authtoken:creationDate"));
        } finally {
            if (directorySession != null) {
                directorySession.close();
            }
        }

        // Test existing token retrieval
        String sameToken = tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits", "rw");
        assertEquals(token, sameToken);

        // Test token uniqueness
        String otherToken = tokenAuthenticationService.getToken("jack",
                "myFavoriteApp", "Ubuntu box 64 bits", "rw");
        assertTrue(!otherToken.equals(token));
    }

    @Test
    public void testGetUserName() throws TokenAuthenticationException {

        // Test invalid token
        String token = "invalidToken";
        String userName = tokenAuthenticationService.getUserName(token);
        assertNull(userName);

        // Test valid token
        token = tokenAuthenticationService.getToken("joe", "myFavoriteApp",
                "Ubuntu box 64 bits", "rw");
        userName = tokenAuthenticationService.getUserName(token);
        assertEquals("joe", userName);
    }

    @Test
    public void testRevokeToken() throws TokenAuthenticationException {

        // Test revoking an unexisting token, should not fail
        tokenAuthenticationService.revokeToken("unexistingToken");

        // Test revoking an existing token
        String token = tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits", "rw");
        assertEquals("joe", tokenAuthenticationService.getUserName(token));

        tokenAuthenticationService.revokeToken(token);
        assertNull(tokenAuthenticationService.getUserName(token));
    }

    @Test
    public void testGetTokenBindings() throws ClientException {

        // Test empty token bindings
        assertEquals(0,
                tokenAuthenticationService.getTokenBindings("john").size());

        // Test existing token bindings
        String token1 = tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits", "rw");
        String token2 = tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Windows box 32 bits", "rw");
        String token3 = tokenAuthenticationService.getToken("joe",
                "nuxeoDrive", "Mac OSX VM", "rw");

        DocumentModelList tokenBindings = tokenAuthenticationService.getTokenBindings("joe");
        assertEquals(3,
                tokenAuthenticationService.getTokenBindings("joe").size());

        // Bindings should be sorted by descendant creation date
        DocumentModel tokenBinding = tokenBindings.get(0);
        assertEquals(token3, tokenBinding.getPropertyValue("authtoken:token"));
        assertEquals("joe", tokenBinding.getPropertyValue("authtoken:userName"));
        assertEquals("nuxeoDrive",
                tokenBinding.getPropertyValue("authtoken:applicationName"));
        assertEquals("Mac OSX VM",
                tokenBinding.getPropertyValue("authtoken:deviceName"));
        assertEquals("rw",
                tokenBinding.getPropertyValue("authtoken:permission"));
        assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));

        tokenBinding = tokenBindings.get(1);
        assertEquals(token2, tokenBinding.getPropertyValue("authtoken:token"));
        assertEquals("joe", tokenBinding.getPropertyValue("authtoken:userName"));
        assertEquals("myFavoriteApp",
                tokenBinding.getPropertyValue("authtoken:applicationName"));
        assertEquals("Windows box 32 bits",
                tokenBinding.getPropertyValue("authtoken:deviceName"));
        assertEquals("rw",
                tokenBinding.getPropertyValue("authtoken:permission"));
        assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));

        tokenBinding = tokenBindings.get(2);
        assertEquals(token1, tokenBinding.getPropertyValue("authtoken:token"));
        assertEquals("joe", tokenBinding.getPropertyValue("authtoken:userName"));
        assertEquals("myFavoriteApp",
                tokenBinding.getPropertyValue("authtoken:applicationName"));
        assertEquals("Ubuntu box 64 bits",
                tokenBinding.getPropertyValue("authtoken:deviceName"));
        assertEquals("rw",
                tokenBinding.getPropertyValue("authtoken:permission"));
        assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));

    }

}
