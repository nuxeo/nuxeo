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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import static org.nuxeo.ecm.automation.client.jaxrs.Constants.CTYPE_AUTOMATION;
import static org.nuxeo.ecm.automation.client.jaxrs.Constants.CTYPE_ENTITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.AdapterFactory;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.LoginInfo;
import org.nuxeo.ecm.automation.client.jaxrs.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;

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

    @Override
    public void setRequestInterceptor(RequestInterceptor interceptor) {
        requestInterceptor = interceptor;
    }

    @Override
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
        setRequestInterceptor(new BasicAuthInterceptor(username, password));
        return getSession();
    }

    public void getSession(final String username, final String password,
            final AsyncCallback<Session> cb) {
        setBasicAuth(username, password);
        getSession(cb);
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

    protected Session login(Connector connector)  {
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
