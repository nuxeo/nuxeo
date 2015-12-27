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
 *     ataillefer
 */
package org.nuxeo.ecm.automation.client;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;

/**
 * The connection to the automation service is done the first time you create a session. To create a session you need to
 * pass the authentication information. If null is passed as the user name an anonymous session will be created. Note
 * that anonymous sessions are not always accepted by a Nuxeo Server (it depends on the server configuration).
 * <p>
 * When you attempt to create a new session using the same authentication info as an already created session the session
 * will be reused (TODO this is optional for implementors?)
 * <p>
 * Note for implementors: the implementation should provide a constructor that initialize the base URL
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AutomationClient {

    /**
     * @since 6.0
     * @param interceptor
     */
    void setRequestInterceptor(RequestInterceptor interceptor);

    /**
     * Gets the automation service URL.
     */
    String getBaseUrl();

    /**
     * Creates a new session. If no interceptors configured connect anonymously.
     */
    Session getSession() throws IOException;

    /**
     * Create a new session using the given login callback to gather login info. The given callback will be notified
     * after the session is created.
     */
    Session getSession(LoginCallback loginCb) throws IOException;

    /**
     * Creates a new session using the given login.
     */
    Session getSession(String username, String password) throws IOException;

    /**
     * Creates a new session using the given token.
     *
     * @since 5.7
     */
    Session getSession(String token) throws IOException;

    /**
     * Creates a new session using the given token callback by following these steps:
     * <ul>
     * <li>Look for a token saved locally using {@link TokenCallback#getLocalToken()}</li>
     * <li>If it doesn't exist, use {@link TokenCallback#getRemoteToken(java.util.Map))} to acquire a token remotely
     * using the information gathered by {@link TokenCallback#getTokenParams()}, and save the token locally using
     * {@link TokenCallback#saveToken(String)}</li>
     * <li>Get a session with the token using {@link #getSession(String)}</li>
     * </ul>
     *
     * @since 5.7
     */
    Session getSession(TokenCallback cb) throws IOException;

    /**
     * Adapts the given object to the given type. Return the adapter instance if any, otherwise null.
     * <p>
     * Optional operation. Framework that doesn't supports reflection like GWT must throw
     * {@link UnsupportedOperationException}
     */
    <T> T getAdapter(Session session, Class<T> adapterType);

    /**
     * Register an adapter for a given type. Registration is not thread safe. You should register adapters at
     * initialization time. An adapter type can be bound to a single adaptable type.
     *
     * @param typeToAdapt
     * @param adapterType
     */
    // FIXME: this javadoc doesn't correspond to the method signature.
    void registerAdapter(AdapterFactory<?> factory);

    /**
     * Marshaller registration for pojo bean
     *
     * @since 5.7.2
     * @param clazz the pojo bean to add to Marshalling
     */
    void registerPojoMarshaller(Class clazz);

    /**
     * Cleanup any resources held by this client. After a shutdown the client is no more usable.
     */
    void shutdown();

}
