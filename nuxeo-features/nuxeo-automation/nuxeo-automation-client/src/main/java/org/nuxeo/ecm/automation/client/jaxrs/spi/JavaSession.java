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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.AsyncCallback;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * Abstract class for sessions running on real JVMs.
 * <p>
 * When your implementation is designed for running in environment that supports
 * limited Java API like GWT or portable devices you may need to directly implement
 * the {@link Session} interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class JavaSession implements Session {

    protected final JavaClient client;

    protected final LoginInfo login;

    protected final OperationRegistry registry;


    public JavaSession(JavaClient client, LoginInfo login, OperationRegistry registry) {
        this.client = client;
        this.login = login;
        this.registry = registry;
    }

    protected OperationRequest createOperationRequest(JavaSession session,
            OperationDocumentation op, Map<String, Object> ctx) {
        return new JavaOperationRequest(session, op, ctx);
    }

    @Override
    public AutomationClient getClient() {
        return client;
    }

    @Override
    public LoginInfo getLogin() {
        return login;
    }

    @Override
    public <T> T getAdapter(Class<T> type) {
        return client.getAdapter(this, type);
    }

    public void execute(final OperationRequest request,
            final AsyncCallback<Object> cb) {
        client.asyncExec(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(execute(request));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }


    public void getFile(final String path, final AsyncCallback<Blob> cb)
            throws Exception {
        client.asyncExec(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(getFile(path));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    public void getFiles(final String path, final AsyncCallback<Blobs> cb)
            throws Exception {
        client.asyncExec(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(getFiles(path));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    public OperationRequest newRequest(String id) throws Exception {
        return newRequest(id, new HashMap<String, Object>());
    }

    public OperationRequest newRequest(String id, Map<String, Object> ctx)
            throws Exception {
        OperationDocumentation op = getOperation(id);
        if (op == null) {
            throw new IllegalArgumentException("No such operation: " + id);
        }
        return createOperationRequest(this, op, ctx);
    }

    protected OperationRegistry getRegistry() {
        return registry;
    }

    public OperationDocumentation getOperation(String id) {
        return registry.getOperation(id);
    }

    public Map<String, OperationDocumentation> getOperations() {
        return registry.getOperations();
    }


    @Override
    public void close() {
        // do nothing
    }

}
