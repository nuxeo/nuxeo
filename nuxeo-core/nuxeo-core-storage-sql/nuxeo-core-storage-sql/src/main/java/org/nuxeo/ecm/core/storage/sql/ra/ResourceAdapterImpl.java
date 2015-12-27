/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.ra;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the singleton representing the resource adapter, created by the application server.
 * <p>
 * It is the central point where all non-local state (network endpoints, etc.) is registered.
 *
 * @author Florent Guillaume
 */
public class ResourceAdapterImpl implements ResourceAdapter {

    private static final Log log = LogFactory.getLog(ResourceAdapterImpl.class);

    private String name;

    /*
     * ----- Java Bean-----
     */

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /*
     * ----- javax.resource.spi.ResourceAdapter -----
     */

    @Override
    public void start(BootstrapContext serverContext) {
        log.debug("----------- starting resource adapter");
    }

    @Override
    public void stop() {
        log.debug("----------- stopping resource adapter");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory factory, ActivationSpec spec) {
        throw new UnsupportedOperationException("Message endpoints not supported");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory factory, ActivationSpec spec) {
        throw new UnsupportedOperationException("Message endpoints not supported");
    }

    /*
     * Used during crash recovery.
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) {
        return new XAResource[0];
    }

}
