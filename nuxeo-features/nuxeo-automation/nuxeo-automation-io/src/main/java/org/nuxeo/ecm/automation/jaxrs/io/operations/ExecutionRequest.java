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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public OperationContext createContext(HttpServletRequest request, HttpServletResponse response, CoreSession session) {
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
