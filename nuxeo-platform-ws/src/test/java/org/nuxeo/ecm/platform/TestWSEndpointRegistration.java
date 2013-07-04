package org.nuxeo.ecm.platform;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.ecm.platform.ws.WSEndpointManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.ws")
public class TestWSEndpointRegistration {

    @Inject
    WSEndpointManager service;

    @Test
    public void testServiceRegistration() {
        assertNotNull(service);
        assertEquals(1, service.getEndpoints().size());

        WSEndpointDescriptor desc = (WSEndpointDescriptor) service.getEndpoints().toArray()[0];
        assertEquals("/nuxeoremoting", desc.address);
        assertEquals(NuxeoRemotingBean.class.getName(), desc.clazz.getName());
    }
}
