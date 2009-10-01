package org.nuxeo.opensocial.container.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class PortalComponentTest {
    private TestRuntimeHarness harness;

    @Inject
    public PortalComponentTest(TestRuntimeHarness harness) throws Exception {
        this.harness = harness;
        harness.deployBundle("org.nuxeo.opensocial.container");
    }

    @Test
    public void testStaticConfigValue() {
        PortalComponent component = PortalComponent.getInstance();
        assertNotNull(component);
        PortalConfig config = component.getConfig();
        assertEquals("localhost", config.getDomain());
        assertEquals("default", config.getContainerName());
        assertEquals("fnQdpl5HYOFQsGNsSjPEDhQLAlZiZCu91RGLN93w7LU=", config
                .getKey());
    }

    @Test
    public void testMyConfigValue() throws Exception {
        harness.deployContrib("org.nuxeo.opensocial.container.test",
                "OSGI-INF/opensocial-config-my-contrib.xml");

        PortalComponent component = PortalComponent.getInstance();
        assertNotNull(component);
        PortalConfig config = component.getConfig();
        assertEquals("myDomain", config.getDomain());
        assertEquals("myContainer", config.getContainerName());
        assertEquals("myKey", config.getKey());
    }
}
