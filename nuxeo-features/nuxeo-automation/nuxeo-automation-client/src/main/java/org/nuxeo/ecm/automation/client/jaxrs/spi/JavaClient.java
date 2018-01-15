/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.AdapterManager;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginCallback;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.Session;

/**
 * Abstract class for clients running on real JVMs.
 * <p>
 * When your implementation is designed for running in environment that supports limited Java API like GWT or portable
 * devices you may need to directly implement the {@link AutomationClient} interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class JavaClient implements AutomationClient {

    protected String url;

    protected AdapterManager adapters;

    protected ExecutorService async;

    protected JavaClient(String url) {
        this(url, null);
    }

    public JavaClient(String url, ExecutorService executor) {
        this.url = url.endsWith("/") ? url : url + "/";
        this.async = executor == null ? Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread("AutomationAsyncExecutor");
            }
        }) : executor;
    }

    /**
     * Validate the credentials. The login must occurs in first place before a session is created. A stateless session
     * may login each time using these credentials but you canot get a session without login-in first.
     *
     * @param username
     * @param password
     * @return
     */
    protected abstract LoginInfo login(String username, String password);

    /**
     * Create a valid session using the authenticated login. The session will download any required data like the
     * operation registry before being usable.
     *
     * @param login
     * @return
     */
    protected abstract JavaSession createSession(LoginInfo login);

    @Override
    public String getBaseUrl() {
        return url;
    }

    public AdapterManager getAdapterManager() {
        return adapters;
    }

    public void asyncExec(Runnable runnable) {
        async.execute(runnable);
    }

    @Override
    public <T> T getAdapter(Session session, Class<T> adapterType) {
        return adapters.getAdapter(session, adapterType);
    }

    @Override
    public void registerAdapter(AdapterFactory<?> factory) {
        adapters.registerAdapter(factory);
    }

    @Override
    public Session getSession(LoginCallback cb) {
        String[] login = cb.getLogin();
        return getSession(login[0], login[1]);
    }

    @Override
    public Session getSession(String username, String password) {
        LoginInfo login = login(username, password);
        if (login == null) {
            throw new RuntimeException("Failed to login as " + username);
        }
        return createSession(login);
    }

    @Override
    public Session getSession() {
        return getSession((String) null, (String) null);
    }

    @Override
    public void shutdown() {
        shutdown(500);
    }

    /**
     * TODO Move this in interface?
     *
     * @param timeout
     */
    public void shutdown(long timeout) {
        try {
            async.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // do nothing - TODO: log?
        } finally {
            async = null;
            url = null;
            adapters = null;
        }
    }

}
