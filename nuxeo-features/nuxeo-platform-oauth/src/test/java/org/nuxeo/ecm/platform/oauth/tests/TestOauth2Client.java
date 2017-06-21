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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestOauth2Client {

    @Inject
    protected ClientRegistry registry;

    @Test
    public void testValidRedirectURI() {
        assertFalse(OAuth2Client.isRedirectURIValid("http://redirect.uri"));
        assertFalse(OAuth2Client.isRedirectURIValid("http://localhost.somecompany.com"));
        assertTrue(OAuth2Client.isRedirectURIValid("nuxeo://authorize"));
        assertTrue(OAuth2Client.isRedirectURIValid("http://localhost:8080/nuxeo"));
        assertTrue(OAuth2Client.isRedirectURIValid("https://redirect.uri"));
    }

    @Test
    public void testClientIDAndSecretMatchesClient() {
        OAuth2Client client = new OAuth2Client("testClient", "testClient", "testSecret", "https://redirect.uri");

        client.setEnabled(false);
        assertFalse(client.isValidWith("testClient", "testSecret"));

        client.setEnabled(true);
        assertFalse(client.isValidWith("wrongId", "testSecret"));
        assertFalse(client.isValidWith("testClient", "wrongSecret"));
        assertTrue(client.isValidWith("testClient", "testSecret"));

        client.setSecret(null);
        assertTrue(client.isValidWith("testClient", "someSecret"));
    }

    @Test
    public void clientsManagement() {
        assertNotNull(registry);
        assertTrue(registry.listClients().isEmpty());

        OAuth2Client client = new OAuth2Client("My App", "myId", "mySecretSecret", "https://redirect.uri");
        assertTrue(registry.registerClient(client));
        // Ensure that registering a client with the same ID is forbidden
        assertFalse(registry.registerClient(client));

        assertEquals(1, registry.listClients().size());
        assertTrue(registry.isValidClient(client.getId(), client.getSecret()));
        assertFalse(registry.isValidClient(client.getId(), "falsePositive"));

        client = new OAuth2Client("My App", "myNdId", "", "https://redirect.uri");
        assertTrue(registry.registerClient(client));
        assertEquals(2, registry.listClients().size());
        assertTrue(registry.isValidClient(client.getId(), "dummySecret"));

        assertTrue(registry.deleteClient("myNdId"));
        assertEquals(1, registry.listClients().size());
        assertTrue(registry.deleteClient("myId"));
        assertTrue(registry.listClients().isEmpty());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/oauth2-client-config.xml")
    public void clientComponentRegistration() throws Exception {
        assertEquals(2, registry.listClients().size());

        assertTrue(registry.hasClient("xxx-xxx"));
        assertFalse(registry.hasClient("unknown"));

        OAuth2Client client = registry.getClient("xxx-xxx");
        assertEquals("my client", client.getName());
        assertEquals("aSecret", client.getSecret());
        assertEquals("https://redirect.uri", client.getRedirectURI());
        assertTrue(client.isEnabled());

        client = registry.getClient("yyy-yyy");
        assertEquals("another client", client.getName());
        assertNull(client.getSecret());
        assertEquals("https://redirect.uri", client.getRedirectURI());
        assertFalse(client.isEnabled());

        assertTrue(registry.deleteClient("xxx-xxx"));
        assertTrue(registry.deleteClient("yyy-yyy"));
        assertTrue(registry.listClients().isEmpty());
    }
}
