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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * Abstract class for sessions running on real JVMs.
 * <p>
 * When your implementation is designed for running in environment that supports limited Java API like GWT or portable
 * devices you may need to directly implement the {@link Session} interface.
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

    protected OperationRequest createOperationRequest(JavaSession session, OperationDocumentation op,
            Map<String, Object> ctx) {
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

    public OperationRequest newRequest(String id) {
        return newRequest(id, new HashMap<String, Object>());
    }

    public OperationRequest newRequest(String id, Map<String, Object> ctx) {
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
