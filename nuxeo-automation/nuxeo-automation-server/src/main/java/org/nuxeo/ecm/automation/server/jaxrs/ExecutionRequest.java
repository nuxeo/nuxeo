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
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExecutionRequest {

    protected Object input;

    protected RestOperationContext ctx;

    protected Map<String, Object> params;

    public ExecutionRequest(Object input) {
        ctx = new RestOperationContext();
        this.input = input;
        this.params = new HashMap<String, Object>();
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void setContextParam(String key, String value) {
        ctx.put(key, value);
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

    public OperationContext createContext(HttpServletRequest request,
            CoreSession session) throws Exception {
        ctx.addRequestCleanupHandler(request);
        ctx.setCoreSession(session);
        ctx.setInput(decodeInput(session, input));
        return ctx;
    }

    public OperationChain createChain(OperationType op) {
        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(op.getId(),
                params);
        chain.add(oparams);
        return chain;
    }

    public static Object decodeInput(CoreSession session, Object input)
            throws Exception {
        if (input == null) {
            return null;
        }
        if (input instanceof String) {
            String inputS = input.toString();
            if (inputS.startsWith("/")) {
                return session.getDocument(new PathRef(inputS));
            } else {
                return session.getDocument(new IdRef(inputS));
            }
            // TODO decode documents
        } else {
            return input;
        }
    }
}
