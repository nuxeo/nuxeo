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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.scripting.Expression;

/**
 * @since 5.7.3
 */
public class Call {

    private static final Log log = LogFactory.getLog(Call.class);

    protected final String chainId;

    protected final String aliases;

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
        this.variables = (context != null) ? new HashMap<>(
                context) : null;
        this.method = method;
        this.input = (context != null) ? context.getInput() : null;
        this.parameters = new HashMap<>();
        if (parms != null) {
            for (String paramId : parms.keySet()) {
                Object paramValue = parms.get(paramId);
                if (paramValue instanceof Expression) {
                    try {
                        ExpressionParameter expressionParameter = new
                                ExpressionParameter(paramId,
                                ((Expression) paramValue).eval(context));
                        this.parameters.put(paramId, expressionParameter);
                    } catch (RuntimeException e) {
                        log.warn(
                                "Cannot evaluate mvel expression for parameter: "
                                        + paramId, e);
                    }
                } else {
                    this.parameters.put(paramId, paramValue);
                }
            }
        }
        this.chainId = (chain != null) ? chain.getId() : "Not bound to a chain";
        this.aliases = (chain != null) ? Arrays.toString(chain.getAliases()) : null;
    }

    /**
     * @since 7.1
     */
    public class ExpressionParameter {

        protected final String parameterId;

        protected final Object parameterValue;

        public ExpressionParameter(String parameterId, Object parameterValue) {
            this.parameterId = parameterId;
            this.parameterValue = parameterValue;
        }

        public Object getParameterValue() {
            return parameterValue;
        }

        public String getParameterId() {

            return parameterId;
        }
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

    public String getAliases() {
        return aliases;
    }

}
