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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import static org.nuxeo.ecm.automation.client.Constants.CTYPE_AUTOMATION;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_ENTITY;

import java.io.IOException;
import java.util.function.Supplier;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.AdapterManager;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginCallback;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.TokenCallback;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.TokenAuthInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public abstract class AbstractAutomationClient implements AutomationClient {

    private static final Object SHARED_REGISTRY_SYNCHRONIZER = new Object();

    // Use an operation registry shared by the multiple client sessions for
    // performance reasons
    private static volatile OperationRegistry sharedRegistry;

    private static long sharedRegistryUpdateTimestamp = 0L;

    private static long sharedRegistryExpirationDelay = 60000L;

    protected final AdapterManager adapterManager = new AdapterManager();

    protected String url;

    protected Supplier<String> urlSupplier;

    protected volatile OperationRegistry registry;

    protected RequestInterceptor requestInterceptor;

    protected AbstractAutomationClient(String url) {
        this.url = url.endsWith("/") ? url : url + "/";
    }

    /** @since 10.10 */
    protected AbstractAutomationClient(Supplier<String> urlSupplier) {
        this.urlSupplier = urlSupplier;
    }

    /**
     * Gets access to this request interceptor
     */
    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    /**
     * Can be used for intercepting requests before they are being sent to the server.
     */
    @Override
    public void setRequestInterceptor(RequestInterceptor interceptor) {
        requestInterceptor = interceptor;
    }

    @Override
    public String getBaseUrl() {
        return urlSupplier == null ? url : urlSupplier.get();
    }

    public void setBasicAuth(String username, String password) {
        setRequestInterceptor(new BasicAuthInterceptor(username, password));
    }

    protected OperationRegistry getRegistry() {
        return registry;
    }

    @Override
    public void registerAdapter(AdapterFactory<?> factory) {
        adapterManager.registerAdapter(factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Session session, Class<T> adapterType) {
        return adapterManager.getAdapter(session, adapterType);
    }

    protected OperationRegistry connect(Connector connector) throws IOException {
        Request req = new Request(Request.GET, getBaseUrl());
        req.put("Accept", CTYPE_AUTOMATION);
        // TODO handle authorization failure
        return (OperationRegistry) connector.execute(req);
    }

    public synchronized void shutdown() {
        adapterManager.clear();
        url = null;
        urlSupplier = null;
        registry = null;
    }

    public Session getSession() throws IOException {
        Connector connector = newConnector();
        if (requestInterceptor != null) {
            connector = new ConnectorHandler(connector, requestInterceptor);
        }
        if (registry == null) { // not yet connected
            if (System.currentTimeMillis() - sharedRegistryUpdateTimestamp < sharedRegistryExpirationDelay) {
                registry = sharedRegistry;
            } else {
                synchronized (SHARED_REGISTRY_SYNCHRONIZER) {
                    // duplicate the test to avoid reentrance
                    if (System.currentTimeMillis() - sharedRegistryUpdateTimestamp < sharedRegistryExpirationDelay) {
                        registry = sharedRegistry;
                    } else {
                        // retrieve the registry
                        registry = connect(connector);
                        sharedRegistry = registry;
                        sharedRegistryUpdateTimestamp = System.currentTimeMillis();
                    }
                }
            }
        }
        return login(connector);
    }

    public Session getSession(final String username, final String password) throws IOException {
        return getSession(new BasicAuthInterceptor(username, password));
    }

    public Session getSession(final String token) throws IOException {
        return getSession(new TokenAuthInterceptor(token));
    }

    protected Session getSession(RequestInterceptor interceptor) throws IOException {
        setRequestInterceptor(interceptor);
        return getSession();
    }

    public Session getSession(TokenCallback cb) throws IOException {
        String token = cb.getLocalToken();
        if (token == null) {
            token = cb.getRemoteToken(cb.getTokenParams());
            cb.saveToken(token);
        }
        return getSession(token);
    }

    @Override
    public Session getSession(LoginCallback loginCb) {
        throw new UnsupportedOperationException();
    }

    protected Session login(Connector connector) throws IOException {
        Request request = new Request(Request.POST, getBaseUrl() + getRegistry().getPath("login"));
        request.put("Accept", CTYPE_ENTITY);
        LoginInfo login = (LoginInfo) connector.execute(request);
        return createSession(connector, login);
    }

    protected Session createSession(final Connector connector, final LoginInfo login) {
        return new DefaultSession(this, connector, login == null ? LoginInfo.ANONYNMOUS : login);
    }

    public void asyncExec(Runnable runnable) {
        throw new UnsupportedOperationException("Async execution not supported");
    }

    protected abstract Connector newConnector();

    /**
     * @since 5.7
     */
    public void setSharedRegistryExpirationDelay(long delay) {
        sharedRegistryExpirationDelay = delay;
    }

    @Override
    public void registerPojoMarshaller(Class clazz) {
        JsonMarshalling.addMarshaller(PojoMarshaller.forClass(clazz));
    }
}
