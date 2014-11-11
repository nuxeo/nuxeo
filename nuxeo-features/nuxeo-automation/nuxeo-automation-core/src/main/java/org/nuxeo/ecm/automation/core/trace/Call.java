/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * @since 5.7.3
 */
public class Call {

    protected final String chainId;

    protected final OperationType type;

    protected final InvokableMethod method;

    protected final Map<String, Object> parameters;

    protected final Map<String, Object> variables;

    protected final List<Trace> nested = new LinkedList<Trace>();

    protected final Object input;

    public Call(OperationType chain, OperationContext context,
            OperationType type, InvokableMethod method,
            Map<String, Object> parms) {
        this.type = type;
        this.variables = (context != null) ? new HashMap<String, Object>(
                context) : null;
        this.method = method;
        this.input = (context != null) ? context.getInput() : null;
        this.parameters = parms;
        this.chainId = (chain != null) ? chain.getId() : "No bound to a chain";
    }

    public OperationType getType() {
        return type;
    }

    public InvokableMethod getMethod() {
        return method;
    }

    public Map<String, Object> getParmeters() {
        return parameters;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getInput() {
        return input;
    }

    public List<Trace> getNested() {
        return nested;
    }

    public String getChainId() {
        return chainId;
    }
}
