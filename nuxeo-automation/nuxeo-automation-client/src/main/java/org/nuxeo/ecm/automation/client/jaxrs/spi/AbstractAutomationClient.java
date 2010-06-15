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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.AdapterFactory;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.LoginInfo;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class AbstractAutomationClient implements AutomationClient,
        Constants {

    protected int state; // 0 - initialized, 1 - starting, 2 - started

    protected String url;

    protected volatile OperationRegistry registry;

    protected Map<Class<?>, List<AdapterFactory<?>>> adapters;

    protected AbstractAutomationClient() {
        adapters = new HashMap<Class<?>, List<AdapterFactory<?>>>();
    }

    public String getBaseUrl() {
        return url;
    }

    public OperationRegistry getRegistry() {
        return registry;
    }

    /**
     * Register and adapter for a given type. Registration is not thread safe.
     * You should register adapters at initialization time. An adapter type can
     * be bound to a single adaptable type.
     * 
     * @param typeToAdapt
     * @param adapterType
     */
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

    public OperationDocumentation getOperation(String id) {
        if (registry == null) {
            throw new IllegalStateException("Client not connected");
        }
        return registry.getOperation(id);
    }

    public Map<String, OperationDocumentation> getOperations() {
        if (registry == null) {
            throw new IllegalStateException("Client not connected");
        }
        return registry.getOperations();
    }

    public synchronized void connect(String url) throws Exception {
        if (this.url != null) {
            throw new IllegalStateException("Already connected to " + url);
        }
        this.url = url.endsWith("/") ? url : url + "/";
        Request req = new Request(Request.GET, url);
        req.put("Accept", CTYPE_AUTOMATION);
        registry = (OperationRegistry) newConnector().execute(req);
    }

    public synchronized void connect(final String url,
            final AsyncCallback<AutomationClient> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    connect(url);
                    cb.onSuccess(AbstractAutomationClient.this);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    public synchronized boolean isConnected() {
        return url != null;
    }

    public synchronized void disconnect() {
        url = null;
        registry = null;
        adapters = null;
    }

    public Session getSession(final String username, final String password)
            throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Cannot create an user session since client is not connected");
        }
        if (username != null) {
            return login(username, password);
        } else {
            return createSession(newConnector(), LoginInfo.ANONYNMOUS);
        }
    }

    public void getSession(final String username, final String password,
            final AsyncCallback<Session> cb) {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Cannot create an user session since client is not connected");
        }
        asyncExec(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(getSession(username, password));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    public Session login(String username, String password) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Cannot login since client is not connected");
        }
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
