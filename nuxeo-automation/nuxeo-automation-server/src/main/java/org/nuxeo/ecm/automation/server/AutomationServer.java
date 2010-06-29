/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server;

import javax.servlet.http.HttpServletRequest;

/**
 * A registry of REST bindings. Provides methods for checking if a given
 * operation is allowed to be invoked in a REST call.
 * 
 * The binding registry is synchronized
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface AutomationServer {

    /**
     * Get a binding given an operation.
     * 
     * @param name the operation name.
     * @return
     */
    RestBinding getOperationBinding(String name);

    /**
     * Get a binding given a chain name
     * 
     * @param name
     * @return
     */
    RestBinding getChainBinding(String name);

    /**
     * Get an array of registered bindings.
     * 
     * @return
     */
    RestBinding[] getBindings();

    /**
     * Register a new operation binding
     * 
     * @param binding
     */
    void addBinding(RestBinding binding);

    /**
     * Remove a binding for the given operation name
     * 
     * @param name
     * @return the removed binding if any otherwise null
     */
    RestBinding removeBinding(RestBinding binding);

    /**
     * Check if the given operation name is allowed in a REST call.
     * 
     * @param name
     * @return
     */
    boolean accept(String name, boolean isChain, HttpServletRequest req);
}
