/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestOauth2Client {

    @Inject
    protected OAuth2ClientService clientService;

    @Test
    public void testValidRedirectURI() {
        assertFalse(OAuth2Client.isRedirectURIValid(""));
        assertFalse(OAuth2Client.isRedirectURIValid("http://redirect.uri"));
        assertFalse(OAuth2Client.isRedirectURIValid(" http://redirect.uri"));
        assertFalse(OAuth2Client.isRedirectURIValid("http://localhost.somecompany.com"));
        assertTrue(OAuth2Client.isRedirectURIValid("nuxeo://authorize"));
        assertTrue(OAuth2Client.isRedirectURIValid("http://localhost:8080/nuxeo"));
        assertTrue(OAuth2Client.isRedirectURIValid("https://redirect.uri"));
    }

    @Test
    public void testClientIDAndSecretMatchesClient() {
        OAuth2Client client = clientService.getClient("notEnabled");
        assertFalse(client.isValidWith("notEnabled", "testSecret"));

        client = clientService.getClient("enabled");
        assertFalse(client.isValidWith("wrongId", "testSecret"));
        assertFalse(client.isValidWith("enabled", "wrongSecret"));
        assertTrue(client.isValidWith("enabled", "testSecret"));

        client = clientService.getClient("noSecret");
        assertTrue(client.isValidWith("noSecret", "someSecret"));
    }

    @Test
    public void testClientService() {
        assertTrue(clientService.hasClient("testClient"));

        OAuth2Client client = clientService.getClient("testClient");
        assertEquals("Dummy", client.getName());
        assertEquals("testClient", client.getId());
        assertTrue(client.isEnabled());
        assertEquals(Arrays.asList("https://redirect.uri", "http://localhost:8080/nuxeo", "nuxeo://authorize"),
                client.getRedirectURIs());
        assertFalse(client.isAutoGrant());

        assertTrue(clientService.isValidClient("testClient", "testSecret"));
    }

    @Test
    public void testAutoGrant() {
        assertFalse(clientService.getClient("testClient").isAutoGrant());
        assertTrue(clientService.getClient("autoGrant").isAutoGrant());
    }

    @Test
    public void shouldNotAllow2ClientsWithSameId() {
        try {
            clientService.getClient("existing");
            fail();
        } catch (NuxeoException e) {
            assertEquals("More than one client registered for the 'existing' id", e.getMessage());
        }
    }
}
