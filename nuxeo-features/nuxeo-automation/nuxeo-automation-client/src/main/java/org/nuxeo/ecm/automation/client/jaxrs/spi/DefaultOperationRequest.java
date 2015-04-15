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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.client.AsyncCallback;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.model.DateUtils;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.client.model.OperationInput;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultOperationRequest implements OperationRequest {

    protected final OperationDocumentation op;

    protected final DefaultSession session;

    protected final Map<String, Object> params;

    protected final Map<String, Object> ctx;

    protected final Map<String, String> headers;

    protected Object input;

    public DefaultOperationRequest(DefaultSession session,
            OperationDocumentation op) {
        this(session, op, new HashMap<String, Object>());
    }

    public DefaultOperationRequest(DefaultSession session,
            OperationDocumentation op, Map<String, Object> ctx) {
        this.session = session;
        this.op = op;
        params = new HashMap<String, Object>();
        headers = new HashMap<String, String>();
        this.ctx = ctx;
    }

    public DefaultSession getSession() {
        return session;
    }

    protected final boolean acceptInput(String type) {
        for (int i = 0, size = op.signature.length; i < size; i += 2) {
            if ("void".equals(op.signature[i])) {
                return true;
            }
            if (type.equals(op.signature[i])) {
                return true;
            }
        }
        return false;
    }

    protected final void checkInput(String type) {
        if (!acceptInput(type)) {
            throw new IllegalArgumentException("Input not supported: " + type
                    + " for the operation: " + op.id);
        }
    }

    public List<String> getParamNames() {
        List<String> result = new ArrayList<String>();
        for (Param param : op.params) {
            result.add(param.name);
        }
        return result;
    }

    public Param getParam(String key) {
        for (Param param : op.params) {
            if (key.equals(param.name)) {
                return param;
            }
        }
        return null;
    }

    public OperationRequest setInput(Object input) {
        if (input == null) {
            checkInput("void");
        } else if (input instanceof OperationInput) {
            checkInput(((OperationInput) input).getInputType());
        }
        this.input = input;
        return this;
    }

    public Object getInput() {
        return input;
    }

    public String getUrl() {
        return session.getClient().getBaseUrl() + op.url;
    }

    public OperationRequest set(String key, Object value) {
        Param param = getParam(key);
        if (param == null) {
            throw new IllegalArgumentException("No such parameter '" + key
                    + "' for operation " + op.id + ".\n\tAvailable params: "
                    + getParamNames());
        }
        if (value == null) {
            params.remove(key);
            return this;
        }
        // handle strings and primitive differently
        // TODO
        // if (!param.type.equals(value.getParamType())) {
        // throw new
        // IllegalArgumentException("Invalid parameter type:
        // "+value.getParamType());
        // }
        if (value.getClass() == Date.class) {
            params.put(key, DateUtils.formatDate((Date) value));
        } else if ("properties".equals(key) && value instanceof Document) {
            // Handle document parameter in case of properties - and bind it to
            // properties
            List<Param> parameters = op.getParams();
            for (Param parameter : parameters) {
                // Check if one of params has the Properties type
                if ("properties".equals(parameter.getType())) {
                    params.put("properties",
                            ((Document) value).getDirties().toString());
                }
            }
        } else {
            params.put(key, value);
        }
        return this;
    }

    public OperationRequest setContextProperty(String key, Object value) {
        ctx.put(key, value != null ? value : null);
        return this;
    }

    public Map<String, Object> getContextParameters() {
        return ctx;
    }

    public Map<String, Object> getParameters() {
        return params;
    }

    public Object execute() throws Exception {
        return session.execute(this);
    }

    public void execute(AsyncCallback<Object> cb) {
        session.execute(this, cb);
    }

    public OperationRequest setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public OperationDocumentation getOperation() {
        return op;
    }
}
