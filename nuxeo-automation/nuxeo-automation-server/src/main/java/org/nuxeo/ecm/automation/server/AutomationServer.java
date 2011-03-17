/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server;

import javax.servlet.http.HttpServletRequest;

/**
 * A registry of REST bindings. Provides methods for checking if a given
 * operation is allowed to be invoked in a REST call.
 * <p>
 * The binding registry is synchronized.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AutomationServer {

    /**
     * Gets a binding given an operation.
     *
     * @param name the operation name.
     */
    RestBinding getOperationBinding(String name);

    /**
     * Gets a binding given a chain name.
     *
     * @param name the chain name
     */
    RestBinding getChainBinding(String name);

    /**
     * Gets an array of registered bindings.
     */
    RestBinding[] getBindings();

    /**
     * Registers a new operation binding.
     *
     * @param binding the new binding to register
     */
    void addBinding(RestBinding binding);

    /**
     * Removes a binding for the given operation name.
     *
     * @param binding the binding to remove
     * @return the removed binding if any, otherwise null
     */
    RestBinding removeBinding(RestBinding binding);

    /**
     * Checks if the given operation name is allowed in a REST call.
     */
    boolean accept(String name, boolean isChain, HttpServletRequest req);

}
