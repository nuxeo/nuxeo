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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * A registry of REST bindings. Provides methods for checking if a given operation is allowed to be invoked in a REST
 * call.
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
     * @deprecated since 10.3 unused
     */
    @Deprecated
    void addBinding(RestBinding binding);

    /**
     * Removes a binding for the given operation name.
     *
     * @param binding the binding to remove
     * @return the removed binding if any, otherwise null
     * @deprecated since 10.3 unused
     */
    @Deprecated
    RestBinding removeBinding(RestBinding binding);

    /**
     * Checks if the given operation name is allowed in a REST call.
     */
    boolean accept(String name, boolean isChain, HttpServletRequest req);

    /**
     * Returns all the registered writers
     *
     * @return
     * @since 5.8
     */
    List<Class<? extends MessageBodyWriter<?>>> getWriters();

    /**
     * @return all the registered readers
     * @since 5.8
     */
    List<Class<? extends MessageBodyReader<?>>> getReaders();

}
