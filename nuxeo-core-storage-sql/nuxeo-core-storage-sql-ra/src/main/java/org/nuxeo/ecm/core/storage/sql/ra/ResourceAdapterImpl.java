/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * This is the singleton representing the resource adapter, created by the
 * application server.
 * <p>
 * It is the central point where all non-local state (network endpoints, etc.)
 * is registered.
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
    public void endpointActivation(MessageEndpointFactory factory,
            ActivationSpec spec) {
        throw new UnsupportedOperationException(
                "Message endpoints not supported");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory factory,
            ActivationSpec spec) {
        throw new UnsupportedOperationException(
                "Message endpoints not supported");
    }

    /*
     * Used during crash recovery.
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) {
        return new XAResource[0];
    }

}
