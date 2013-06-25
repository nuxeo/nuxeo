/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.AdapterManager;
import org.nuxeo.ecm.automation.client.AsyncCallback;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginCallback;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.Session;

/**
 * Abstract class for clients running on real JVMs.
 * <p>
 * When your implementation is designed for running in environment that supports
 * limited Java API like GWT or portable devices you may need to directly implement
 * the {@link AutomationClient} interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JavaClient implements AutomationClient {

    protected String url;
    protected AdapterManager adapters;
    protected ExecutorService async;

    protected JavaClient(String url) {
        this(url, null);
    }

    public JavaClient(String url, ExecutorService executor) {
        this.url = url.endsWith("/") ? url : url+"/";
        this.async = executor == null ?
                Executors.newCachedThreadPool(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread("AutomationAsyncExecutor");
                    }
                })
                : executor;
    }

    /**
     * Validate the credentials. The login must occurs in first place
     * before a session is created. A stateless session may login each time
     * using these credentials but you canot get a session without login-in first.
     * @param username
     * @param password
     * @return
     */
    protected abstract LoginInfo login(String username, String password);

    /**
     * Create a valid session using the authenticated login.
     * The session will download any required data like the operation registry before being usable.
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
    public void getSession(
            final LoginCallback loginCb,
            final AsyncCallback<Session> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    Session session = getSession(loginCb);
                    cb.onSuccess(session);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    @Override
    public void getSession(final AsyncCallback<Session> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    Session session = getSession();
                    cb.onSuccess(session);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    @Override
    public void getSession(final String username, final String password,
            final AsyncCallback<Session> cb) {
        asyncExec(new Runnable() {
            public void run() {
                try {
                    Session session = getSession(username, password);
                    cb.onSuccess(session);
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
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
            throw new RuntimeException("Failed to login as "+username);
        }
        return createSession(login);
    }

    @Override
    public Session getSession() {
        return getSession((String)null, (String)null);
    }

    @Override
    public void shutdown() {
        shutdown(500);
    }

    /**
     * TODO Move this in interface?
     * @param timeout
     */
    public void shutdown(long timeout) {
        try {
            async.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // do nothing - TODO: log?
        } finally {
            async = null;
            url = null;
            adapters = null;
        }
    }

}
