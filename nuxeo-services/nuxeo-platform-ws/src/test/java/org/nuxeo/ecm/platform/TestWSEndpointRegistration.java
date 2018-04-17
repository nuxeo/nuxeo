/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.ws.Endpoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.ecm.platform.ws.WSEndpointManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.ws")
@Deploy("org.nuxeo.ecm.platform.ws:ws-contrib.xml")
public class TestWSEndpointRegistration {

    @Inject
    WSEndpointManager service;

    Map<String, WSEndpointDescriptor> contribs = new HashMap<>();

    @Before
    public void indexRegistry() {
        for (WSEndpointDescriptor desc : service.getDescriptors()) {
            contribs.put(desc.name, desc);
        }
        assertEquals(2, contribs.size());
    }

    @Test
    public void testServiceRegistration() {
        WSEndpointDescriptor desc = contribs.get("nuxeoremoting");
        assertEquals("/nuxeoremoting", desc.address);
        assertEquals(NuxeoRemotingBean.class.getName(), desc.clazz.getName());
        assertEquals(0, desc.handlers.length);
    }

    @Test
    public void testCompleteWebService() throws Exception {
        WSEndpointDescriptor desc = contribs.get("testWS");

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
