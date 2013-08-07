package org.nuxeo.ecm.platform;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import javax.xml.ws.Endpoint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.ecm.platform.ws.WSEndpointManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.ws")
@LocalDeploy("org.nuxeo.ecm.platform.ws:ws-contrib.xml")
public class TestWSEndpointRegistration {

    @Inject
    WSEndpointManager service;

    @Test
    public void testServiceRegistration() {
        assertNotNull(service);
        assertEquals(2, service.getDescriptors().size());

        WSEndpointDescriptor desc = (WSEndpointDescriptor) service.getDescriptors().toArray()[0];
        assertEquals("/nuxeoremoting", desc.address);
        assertEquals(NuxeoRemotingBean.class.getName(), desc.clazz.getName());
        assertEquals(0, desc.handlers.length);
    }

    @Test
    public void testCompleteWebService() throws Exception {
        WSEndpointDescriptor desc = (WSEndpointDescriptor) service.getDescriptors().toArray()[1];
        assertEquals("testWS", desc.name);
        assertEquals("/nuxeoremoting", desc.address);
        assertEquals(NuxeoRemotingBean.class.getName(), desc.clazz.getName());
        assertEquals(3, desc.handlers.length);
        assertEquals("serviceName", desc.service);
        assertEquals("portName", desc.port);
        assertEquals("http://nuxeo.example.org", desc.namespace);

        Endpoint ep = desc.toEndpoint();
        assertNotNull(ep);
    }
}
