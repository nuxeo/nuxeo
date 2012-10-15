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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import static org.nuxeo.ecm.automation.client.Constants.CTYPE_AUTOMATION;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_ENTITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.AsyncCallback;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginCallback;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.TokenCallback;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.TokenAuthInterceptor;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractAutomationClient implements AutomationClient {

    protected String url;

    protected volatile OperationRegistry registry;

    protected Map<Class<?>, List<AdapterFactory<?>>> adapters;

    protected RequestInterceptor requestInterceptor;

    protected AbstractAutomationClient(String url) {
        this.adapters = new HashMap<Class<?>, List<AdapterFactory<?>>>();
        this.url = url.endsWith("/") ? url : url + "/";
    }

    /**
     * Can be used for intercepting requests before they are being sent to the
     * server.
     */
    public void setRequestInterceptor(RequestInterceptor interceptor) {
        requestInterceptor = interceptor;
    }

    /**
     * Gets access to this request interceptor
     */
    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    @Override
    public String getBaseUrl() {
        return url;
    }

    public void setBasicAuth(String username, String password) {
        setRequestInterceptor(new BasicAuthInterceptor(username, password));
    }

    protected OperationRegistry getRegistry() {
        return registry;
    }

    @Override
    public void registerAdapter(AdapterFactory<?> factory) {
        Class<?> adapter = factory.getAdapterType();
        List<AdapterFactory<?>> factories = adapters.get(adapter);
        if (factories == null) {
            factories = new ArrayList<AdapterFactory<?>>();
            adapters.put(adapter, factories);
        }
        factories.add(factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Object objToAdapt, Class<T> adapterType) {
        Class<?> cls = objToAdapt.getClass();
        List<AdapterFactory<?>> factories = adapters.get(adapterType);
        if (factories != null) {
            for (AdapterFactory<?> f : factories) {
                if (f.getAcceptType().isAssignableFrom(cls)) {
                    return (T) f.getAdapter(objToAdapt);
                }
            }
        }
        return null;
    }

    protected OperationRegistry connect(Connector connector) {
        Request req = new Request(Request.GET, url);
        req.put("Accept", CTYPE_AUTOMATION);
        // TODO handle authorization failure
        return (OperationRegistry) connector.execute(req);
    }

    public synchronized void shutdown() {
        url = null;
        registry = null;
        adapters = null;
    }

    public Session getSession() {
        Connector connector = newConnector();
        if (requestInterceptor != null) {
            connector = new ConnectorHandler(connector, requestInterceptor);
        }
        if (registry == null) { // not yet connected
            synchronized (this) {
                if (registry == null) {
                    registry = connect(connector);
                }
            }
        }
        return login(connector);
    }

    public Session getSession(final String username, final String password) {
        return getSession(new BasicAuthInterceptor(username, password));
    }

    public Session getSession(final String token) {
        return getSession(new TokenAuthInterceptor(token));
    }

    protected Session getSession(RequestInterceptor interceptor) {
        setRequestInterceptor(interceptor);
        return getSession();
    }

    public Session getSession(TokenCallback cb) {
        String token = cb.getLocalToken();
        if (token == null) {
            token = cb.getRemoteToken(cb.getTokenParams());
            cb.saveToken(token);
        }
        return getSession(token);
    }

    public void getSession(final String username, final String password,
            final AsyncCallback<Session> cb) {
        setBasicAuth(username, password);
        getSession(cb);
    }

    @Override
    public Session getSession(LoginCallback loginCb) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getSession(LoginCallback loginCb, AsyncCallback<Session> cb) {
        throw new UnsupportedOperationException();
    }

    public void getSession(final AsyncCallback<Session> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    Session session = getSession();
                    // TODO handle failures
                    cb.onSuccess(session);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    protected Session login(Connector connector) {
        Request request = new Request(Request.POST, url
                + getRegistry().getPath("login"));
        request.put("Accept", CTYPE_ENTITY);
        LoginInfo login = (LoginInfo) connector.execute(request);
        return createSession(connector, login);
    }

    protected Session createSession(final Connector connector,
            final LoginInfo login) {
        return new DefaultSession(this, connector,
                login == null ? LoginInfo.ANONYNMOUS : login);
    }

    public void asyncExec(Runnable runnable) {
        throw new UnsupportedOperationException("Async execution not supported");
    }

    protected abstract Connector newConnector();

}
