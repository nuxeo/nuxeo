/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.io.Serializable;
import java.util.Properties;

/**
 * A service locator is used to locate services given a service ID or descriptor.
 * <p>
 * Service locators may looks up for services over the net or in the current JVM.
 * How the lookup is done is implementation-dependent.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ServiceLocator extends Serializable {

    /**
     * Initializes the service locator.
     *
     * @param host the host where the lookup will be done. The host is optional
     *            and may be null if the services are looked up in the current
     *            JVM
     * @param port the port of the host to be used to lookup.
     * @param properties the properties are implementation dependent Properties
     *            may be null if no properties are required. The port is
     *            meaningful only if the host was specified
     */
    void initialize(String host, int port, Properties properties) throws Exception;

    /**
     * Disposes this locator. Free any allocated resources.
     */
    void dispose();

    /**
     * Lookup the service described by the given service descriptor.
     *
     * @param descriptor the service descriptor
     * @return the service instance or null if no such service was found
     * @throws Exception if any error occurs
     */
    Object lookup(ServiceDescriptor descriptor) throws Exception;

    /**
     * Lookup the service given its ID.
     *
     * @param serviceId the service ID
     * @return the service instance or null if no such service was found
     * @throws Exception if any error occurs
     */
    Object lookup(String serviceId) throws Exception;

}
