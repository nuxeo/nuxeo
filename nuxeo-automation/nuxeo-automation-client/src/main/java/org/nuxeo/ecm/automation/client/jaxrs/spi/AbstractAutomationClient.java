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
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractAutomationClient implements AutomationClient {

    protected String url;

    protected volatile OperationRegistry registry;

    protected Map<Class<?>, List<AdapterFactory<?>>> adapters;

    protected AbstractAutomationClient(String url) {
        adapters = new HashMap<Class<?>, List<AdapterFactory<?>>>();
        this.url = url.endsWith("/") ? url : url + "/";
    }

    public String getBaseUrl() {
        return url;
    }

    protected OperationRegistry getRegistry() {
        return registry;
    }

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

    protected synchronized void connect(String username, String password)
            throws Exception {
        Request req = new Request(Request.GET, url);
        req.put("Accept", CTYPE_AUTOMATION);
        if (username != null) {
            String auth = "Basic " + Base64.encode(username + ":" + password);
            req.put("Authorization", auth);
        }
        // TODO handle authorization failure
        registry = (OperationRegistry) newConnector().execute(req);
    }

    public synchronized void shutdown() {
        url = null;
        registry = null;
        adapters = null;
    }

    public Session getSession(final String username, final String password)
            throws Exception {
        if (registry == null) { // not yet connected
            synchronized (this) {
                if (registry == null) {
                    connect(username, password);
                }
            }
        }
        if (username != null) {
            return login(username, password);
        } else {
            return createSession(newConnector(), LoginInfo.ANONYNMOUS);
        }
    }

    public void getSession(final String username, final String password,
            final AsyncCallback<Session> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    Session session = getSession(username, password);
                    // TODO handle failures
                    cb.onSuccess(session);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    public Session login(String username, String password) throws Exception {
        Request request = new Request(Request.POST, url
                + getRegistry().getPath("login"));
        String auth = "Basic " + Base64.encode(username + ":" + password);
        request.put("Authorization", auth);
        request.put("Accept", CTYPE_ENTITY);
        Connector connector = newConnector();
        connector.setBasicAuth(auth);
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
