/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

    /**
     * Black listing of mvel expressions which should not be evaluated by the Automation traces for debugging
     * purpose
     *
     * @since 8.1
     */
    public static final String[] MVEL_BLACK_LIST_EXPR = new String[] { "getNextId" };

    protected final String chainId;

    protected final String aliases;

    protected final OperationType type;

    protected final InvokableMethod method;

    protected final Map<String, Object> parameters;

    protected final Map<String, Object> variables;

    protected final List<Trace> nested = new LinkedList<Trace>();

    protected final Object input;

    public Call(OperationType chain, OperationContext context, OperationType type, InvokableMethod method,
            Map<String, Object> parms) {
        this.type = type;
        variables = (context != null) ? new HashMap<>(context) : null;
        this.method = method;
        input = (context != null) ? context.getInput() : null;
        parameters = new HashMap<>();
        if (parms != null) {
            for (String paramId : parms.keySet()) {
                Object paramValue = parms.get(paramId);
                if (paramValue instanceof Expression) {
                    try {
                        ExpressionParameter expressionParameter = null;
                        for (String mvelExpr : MVEL_BLACK_LIST_EXPR) {
                            if (((Expression) paramValue).getExpr().contains(mvelExpr)) {
                                expressionParameter = new ExpressionParameter(paramId, String.format(
                                        "Cannot be evaluated in traces when using '%s' expression", mvelExpr));
                                parameters.put(paramId, expressionParameter);
                                break;
                            }
                        }
                        if (parameters.get(paramId) == null) {
                            expressionParameter = new ExpressionParameter(paramId,
                                    ((Expression) paramValue).eval(context));
                            parameters.put(paramId, expressionParameter);

                        }
                    } catch (RuntimeException e) {
                        log.warn("Cannot evaluate mvel expression for parameter: " + paramId, e);
                    }
                } else {
                    parameters.put(paramId, paramValue);
                }
            }
        }
        chainId = (chain != null) ? chain.getId() : "Not bound to a chain";
        aliases = (chain != null) ? Arrays.toString(chain.getAliases()) : null;
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
