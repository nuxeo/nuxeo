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
package org.nuxeo.ecm.platform.ws;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.3
 */
public class WSEndpointManagerImpl extends DefaultComponent implements WSEndpointManager {

    public static final String ENDPOINT_EP = "endpoint";

    private static final Log log = LogFactory.getLog(WSEndpointManagerImpl.class);

    private static final int APPLICATION_STARTED_ORDER = 1500;

    protected WSEndpointRegistry regitry = new WSEndpointRegistry();

    protected Map<String, Endpoint> endpoints = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ENDPOINT_EP.equals(extensionPoint)) {
            regitry.addContribution((WSEndpointDescriptor) contribution);
        } else {
            log.info("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ENDPOINT_EP.equals(extensionPoint)) {
            WSEndpointDescriptor descriptor = (WSEndpointDescriptor) contribution;
            stopIfExists(descriptor.name);
            regitry.removeContribution(descriptor);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        if (Framework.isTestModeSet()) {
            return;
        }
        publishEndpoints();
    }

    @Override
    public void applicationStopped(ComponentContext context, Instant deadline) {
        for (Endpoint ep : endpoints.values()) {
            ep.stop();
        }
        endpoints.clear();
    }

    @Override
    public void publishEndpoints() {
        for (WSEndpointDescriptor desc : regitry.getContributions()) {
            try {
                stopIfExists(desc.name);

                Endpoint ep = desc.toEndpoint();

                ep.publish(desc.address);
                desc.configurePostPublishing(ep);

                if (ep.isPublished()) {
                    endpoints.put(desc.name, ep);
                } else {
                    log.warn("Endpoint publishing is failing: " + desc.name);
                }
            } catch (IOException | IllegalAccessException | InstantiationException e) {
                log.warn("Unable to register endpoint: " + desc.name, e);
            }
        }
    }

    @Override
    public Collection<WSEndpointDescriptor> getDescriptors() {
        return regitry.getContributions();
    }

    protected void stopIfExists(String name) {
        // Stop endpoint publishing
        Endpoint ep = endpoints.get(name);
        if (ep != null) {
            ep.stop();
            endpoints.remove(name);
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }
}
