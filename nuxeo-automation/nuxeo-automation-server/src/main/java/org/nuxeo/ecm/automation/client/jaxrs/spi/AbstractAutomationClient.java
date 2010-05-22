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


import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AuthenticationCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.Session;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractAutomationClient implements AutomationClient {

    protected int state; // 0 - initialized, 1 - starting, 2 - started
    protected String url;
    protected volatile OperationRegistry registry;


    protected AbstractAutomationClient() {
    }

    public String getBaseUrl() {
        return url;
    }

    public OperationRegistry getRegistry() {
        return registry;
    }

    public synchronized void connect(String url) throws Exception {
        if (this.url != null) {
            throw new IllegalStateException("Already connected to "+url);
        }
        this.url = url.endsWith("/") ? url : url+"/";
        registry = getOperationRegistry(url);
    }

    public synchronized void connect(String url, final AsyncCallback<AutomationClient> cb) {
        if (this.url != null) {
            throw new IllegalStateException("Already connected to "+url);
        }
        this.url = url.endsWith("/") ? url : url+"/";
         getOperationRegistry(url, new AsyncCallback<OperationRegistry>() {
            public void onError(Throwable e) {
                cb.onError(e);
            }
            public void onSuccess(OperationRegistry data) {
                registry = data;
                cb.onSuccess(AbstractAutomationClient.this);
            }
        });
    }

    public synchronized boolean isConnected() {
        return url != null;
    }

    public synchronized void disconnect() {
        url = null;
        registry = null;
    }


    public Session getSession(final String username, final String password) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot create an user session since client is not connected");
        }
        return getSession(new AuthenticationCallback() {
            public String[] getCredentials() {
                return new String[] {username, password};
            }
        });
    }

    public Session getSession(AuthenticationCallback cb) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot create an user session since client is not connected");
        }
        return createSession(cb);
    }

    protected Session createSession(AuthenticationCallback cb) {
        String[] c = cb.getCredentials();
        if (c == null) {
            return new DefaultSession(this, null, null);
        }
        return new DefaultSession(this, c[0], c[1]);
    }


    protected abstract OperationRegistry getOperationRegistry(String url) throws Exception;

    protected abstract void getOperationRegistry(String url, AsyncCallback<OperationRegistry> cb);

    protected abstract Object execute(OperationRequest req) throws Exception;

    protected abstract void execute(OperationRequest req, AsyncCallback<Object> cb);

}
