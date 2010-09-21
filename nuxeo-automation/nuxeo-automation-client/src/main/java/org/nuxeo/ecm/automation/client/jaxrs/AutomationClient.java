/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.client.jaxrs;

/**
 * The connection to the automation service is done the first time you create a
 * session. To create a session you need to pass the authentication
 * information. If null is passed as the user name an anonymous session will be
 * created. Note that anonymous sessions are not always accepted by a Nuxeo
 * Server (it depends on the server configuration).
 * <p>
 * When you attempt to create a new session using the same authentication info
 * as an already created session the session will be reused (TODO this is
 * optional for implementors?)
 * <p>
 * Note for implementors: the implementation should provide a constructor that
 * initialize the base URL
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AutomationClient {

    /**
     * Gets the automation service URL.
     */
    String getBaseUrl();

    /**
     * Creates a new session using the given login.
     */
    Session getSession(String username, String password) throws Exception;

    /**
     * Creates asynchronously a new session using the given login. The given
     * callback will be notified after the session is created.
     */
    void getSession(String username, String password, AsyncCallback<Session> cb);

    /**
     * Adapts the given object to the given type. Return the adapter instance
     * if any, otherwise null.
     * <p>
     * Optional operation. Framework that doesn't supports reflection like GWT
     * must throw {@link UnsupportedOperationException}
     */
    <T> T getAdapter(Object objToAdapt, Class<T> adapterType);

    /**
     * Register an adapter for a given type. Registration is not thread safe.
     * You should register adapters at initialization time. An adapter type can
     * be bound to a single adaptable type.
     *
     * @param typeToAdapt
     * @param adapterType
     */
    // FIXME: this javadoc doesn't correspond to the method signature.
    void registerAdapter(AdapterFactory<?> factory);

    /**
     * Cleanup any resources held by this client. After a shutdown the client
     * is no more usable.
     */
    void shutdown();

}
