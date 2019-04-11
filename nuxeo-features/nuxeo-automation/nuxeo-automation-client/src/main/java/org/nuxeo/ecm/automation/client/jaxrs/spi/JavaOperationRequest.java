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
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.model.DateUtils;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.client.model.OperationInput;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JavaOperationRequest implements OperationRequest {

    protected final OperationDocumentation op;

    protected final JavaSession session;

    protected final Map<String, Object> params;

    protected final Map<String, Object> ctx;

    protected final Map<String, String> headers;

    protected Object input;

    public JavaOperationRequest(JavaSession session, OperationDocumentation op) {
        this(session, op, new HashMap<String, Object>());
    }

    public JavaOperationRequest(JavaSession session, OperationDocumentation op, Map<String, Object> ctx) {
        this.session = session;
        this.op = op;
        params = new HashMap<>();
        headers = new HashMap<>();
        this.ctx = ctx;
    }

    public JavaSession getSession() {
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
            throw new IllegalArgumentException("Input not supported: " + type);
        }
    }

    public List<String> getParamNames() {
        List<String> result = new ArrayList<>();
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
            throw new IllegalArgumentException("No such parameter '" + key + "' for operation " + op.id
                    + ".\n\tAvailable params: " + getParamNames());
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
        } else {
            params.put(key, value.toString());
        }
        return this;
    }

    public OperationRequest setContextProperty(String key, Object value) {
        ctx.put(key, value != null ? value.toString() : null);
        return this;
    }

    public Map<String, Object> getContextParameters() {
        return ctx;
    }

    public Map<String, Object> getParameters() {
        return params;
    }

    public Object execute() throws IOException {
        return session.execute(this);
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
