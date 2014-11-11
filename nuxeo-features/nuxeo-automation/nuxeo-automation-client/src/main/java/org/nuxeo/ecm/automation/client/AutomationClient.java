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
 *     ataillefer
 */
package org.nuxeo.ecm.automation.client;

/**
 * The connection to the automation service is done the first time you create a
 * session. To create a session you need to pass the authentication information.
 * If null is passed as the user name an anonymous session will be created. Note
 * that anonymous sessions are not always accepted by a Nuxeo Server (it depends
 * on the server configuration).
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
     * Creates a new session. If no interceptors configured connect anonymously.
     */
    Session getSession();

    /**
     * Creates a new session. If no interceptors configured connect anonymously.
     */
    void getSession(AsyncCallback<Session> cb);

    /**
     * Create a new session using the given login callback to gather login info.
     * The given callback will be notified after the session is created.
     */
    Session getSession(LoginCallback loginCb);

    /**
     * Create asynchronously a new session using the given login callback to
     * gather login info. The given callback will be notified after the session
     * is created.
     */
    void getSession(LoginCallback loginCb, AsyncCallback<Session> cb);

    /**
     * Creates a new session using the given login.
     */
    Session getSession(String username, String password);

    /**
     * Creates a new session using the given token.
     *
     * @since 5.7
     */
    Session getSession(String token);

    /**
     * Creates a new session using the given token callback by following these
     * steps:
     * <ul>
     * <li>Look for a token saved locally using
     * {@link TokenCallback#getLocalToken()}</li>
     * <li>If it doesn't exist, use
     * {@link TokenCallback#getRemoteToken(java.util.Map))} to acquire a token
     * remotely using the information gathered by
     * {@link TokenCallback#getTokenParams()}, and save the token locally using
     * {@link TokenCallback#saveToken(String)}</li>
     * <li>Get a session with the token using {@link #getSession(String)}</li>
     * </ul>
     *
     * @since 5.7
     */
    Session getSession(TokenCallback cb);

    /**
     * Creates asynchronously a new session using the given login. The given
     * callback will be notified after the session is created.
     */
    void getSession(String username, String password, AsyncCallback<Session> cb);

    /**
     * Adapts the given object to the given type. Return the adapter instance if
     * any, otherwise null.
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
     * Cleanup any resources held by this client. After a shutdown the client is
     * no more usable.
     */
    void shutdown();

}
