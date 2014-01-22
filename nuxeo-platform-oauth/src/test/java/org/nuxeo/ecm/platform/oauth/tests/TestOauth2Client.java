package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.oauth" })
@LocalDeploy({ "org.nuxeo.ecm.platform.oauth:OSGI-INF/directory-test-config.xml" })
public class TestOauth2Client {

    @Inject
    ClientRegistry registry;

    @Test
    public void clientsManagement() throws ClientException {
        assertNotNull(registry);
        assertEquals(0, registry.listClients().size());

        OAuth2Client client = new OAuth2Client("My App", "myId", "mySecretSecret");
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
    }
}
