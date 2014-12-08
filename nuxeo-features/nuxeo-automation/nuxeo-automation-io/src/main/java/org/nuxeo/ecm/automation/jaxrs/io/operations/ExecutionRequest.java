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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExecutionRequest {

    protected Object input;

    protected RestOperationContext restOperationContext;

    protected Map<String, Object> params;

    public ExecutionRequest() {
        this(null);
    }

    public ExecutionRequest(Object input) {
        restOperationContext = new RestOperationContext();
        this.input = input;
        params = new HashMap<String, Object>();
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void setContextParam(String key, Object value) {
        restOperationContext.put(key, value);
    }

    public void setContextParam(String key, String value) {
        restOperationContext.put(key, value);
    }

    public void setParam(String key, Object jsonObject) {
        params.put(key, jsonObject);
    }

    public void setParam(String key, String value) {
        if (value.startsWith("expr:")) {
            value = value.substring(5).trim();
            if (value.contains("@{")) {
                params.put(key, Scripting.newTemplate(value));
            } else {
                params.put(key, Scripting.newExpression(value));
            }
        } else {
            params.put(key, value);
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public OperationContext createContext(HttpServletRequest request, CoreSession session) {
        restOperationContext.addRequestCleanupHandler(request);
        restOperationContext.setCoreSession(session);
        restOperationContext.setInput(input);
        restOperationContext.put("request", request);
        return restOperationContext;
    }

    /**
     * @since 7.1
     */
    public RestOperationContext getRestOperationContext() {
        return restOperationContext;
    }
}
