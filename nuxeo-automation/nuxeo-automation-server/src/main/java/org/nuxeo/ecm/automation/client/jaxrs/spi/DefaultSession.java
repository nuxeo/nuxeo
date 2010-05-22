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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultSession implements Session {

    protected AbstractAutomationClient client;
    protected String auth;
    protected String username;
    protected String password;

    public DefaultSession(AbstractAutomationClient client, String username, String password) {
        this.client = client;
        this.username = username;
        this.password = password;
        if (username != null) {
            auth = "Basic "+Base64.encode(username+":"+password);
        }
    }

    public AutomationClient getClient() {
        return client;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAuth() {
        return auth;
    }

    public OperationDocumentation getOperation(String id) {
        return client.getRegistry().getOperation(id);
    }

    public Map<String, OperationDocumentation> getOperations() {
        return client.getRegistry().getOperations();
    }

    public OperationRequest newRequest(String id) throws Exception {
        return newRequest(id, new HashMap<String,String>());
    }

    public OperationRequest newRequest(String id, Map<String, String> ctx)
            throws Exception {
        OperationDocumentation op = getOperation(id);
        if (op == null) {
            throw new IllegalArgumentException("No such operation: "+id);
        }
        DefaultOperationRequest req = new DefaultOperationRequest(this, op, ctx);
        return req;
    }

    public Object execute(OperationRequest request) throws Exception {
        return client.execute(request);
    }

    public void execute(OperationRequest request, AsyncCallback<Object> cb) {
        client.execute(request, cb);
    }

    public <T> T getAdapter(Class<T> type) {
        return client.getAdapter(this, type);
    }

}
