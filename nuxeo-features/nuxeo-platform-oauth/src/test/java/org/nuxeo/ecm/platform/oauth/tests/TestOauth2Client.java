package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.impl.ComponentDescriptorReader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */

@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestOauth2Client {

    @Inject
    ClientRegistry registry;

    @Test
    public void clientsManagement() throws ClientException {
        assertNotNull(registry);
        assertEquals(0, registry.listClients().size());

        String clientId = "myId";
        OAuth2Client client = new OAuth2Client("My App", clientId,
                "mySecretSecret");
        assertTrue(registry.registerClient(client));
        // Ensure that a same client registering is forbids
        assertFalse(registry.registerClient(client));

        assertEquals(1, registry.listClients().size());
        assertTrue(registry.isValidClient(client.getId(), client.getSecret()));
        assertFalse(registry.isValidClient(client.getId(), "falsePositive"));

        client = new OAuth2Client("My App", "myNdId", "");
        assertTrue(registry.registerClient(client));
        assertEquals(2, registry.listClients().size());
        assertTrue(registry.isValidClient(client.getId(), "dummySecret"));

        assertTrue(registry.deleteClient(client.getId()));
        assertEquals(1, registry.listClients().size());
        assertTrue(registry.deleteClient(clientId));
        assertEquals(0, registry.listClients().size());
    }

    @Test
    public void clientComponentRegistration() throws Exception {
        assertEquals(0, registry.listClients().size());
        localDeploy("/OSGI-INF/oauth2-client-config.xml");

        assertEquals(2, registry.listClients().size());
        assertTrue(registry.deleteClient("xxx-xxx"));
        assertTrue(registry.deleteClient("yyy-yyy"));
        assertEquals(0, registry.listClients().size());
    }

    protected void localDeploy(String filename) throws Exception {
        ComponentDescriptorReader reader = new ComponentDescriptorReader();
        File file = new File(this.getClass().getResource(filename).toURI());
        RegistrationInfo info = reader.read(Framework.getRuntime().getContext(), new FileInputStream(file));
        Framework.getRuntime().getComponentManager().register(info);
    }
}
