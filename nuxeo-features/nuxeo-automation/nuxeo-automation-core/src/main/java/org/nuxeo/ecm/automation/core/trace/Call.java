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

import static org.nuxeo.ecm.automation.core.Constants.LF;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
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
     * Black listing of mvel expressions which should not be evaluated by the Automation traces for debugging purpose
     *
     * @since 8.1
     */
    public static final String[] MVEL_BLACK_LIST_EXPR = new String[] { "getNextId" };

    protected final String chainId;

    protected final String aliases;

    protected final OperationType type;

    protected final List<Trace> nested = new LinkedList<>();

    protected final Details details;

    protected Call(OperationType chain, OperationType op, Details details) {
        type = op;
        chainId = chain.getId();
        aliases = Arrays.toString(chain.getAliases());
        this.details = details;
    }

    public Call(OperationType chain, OperationType op) {
        this(chain, op, new Details());
    }

    public Call(OperationType chain, OperationContext context, OperationType type, InvokableMethod method,
            Map<String, Object> parms) {
        this(chain, type, new Details(context, method, parms));
    }

    protected static class Details {

        protected final Map<String, Object> parameters = new HashMap<>();

        protected final Map<String, Object> variables = new HashMap<>();

        protected final InvokableMethod method;

        protected final Object input;

        protected Object output;

        protected Details() {
            method = null;
            input = null;
        }

        protected Details(OperationContext context, InvokableMethod method, Map<String, Object> parms) {
            this.method = method;
            input = context.getInput();
            variables.putAll(context);
            parms.forEach(new Evaluator(context)::inject);
        }

        protected class Evaluator {

            protected final OperationContext context;

            protected Evaluator(OperationContext context) {
                this.context = context;
            }

            protected void inject(String key, Object value) {
                if (!(value instanceof Expression)) {
                    parameters.put(key, value);
                    return;
                }
                Expression exp = (Expression) value;
                for (String mvelExpr : MVEL_BLACK_LIST_EXPR) {
                    if (exp.getExpr().contains(mvelExpr)) {
                        parameters.put(key, new ExpressionParameter(key,
                                String.format("Cannot be evaluated in traces when using '%s' expression", mvelExpr)));
                        return;
                    }
                }
                try {
                    parameters.put(key, new ExpressionParameter(key, exp.eval(context)));
                    return;
                } catch (RuntimeException e) {
                    log.warn("Cannot evaluate mvel expression for parameter: " + key, e);
                }
            }
        }

    }

    /**
     * @since 7.1
     */
    public static class ExpressionParameter {

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
        return details.method;
    }

    public Map<String, Object> getParameters() {
        return details.parameters;
    }

    public Map<String, Object> getVariables() {
        return details.variables;
    }

    public Object getInput() {
        return details.input;
    }

    public Object getOutput() {
        return details.output;
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

    /**
     * @since 9.3
     */
    public void print(BufferedWriter writer) throws IOException {
        try {
            writer.append(LF);
            writer.append(LF);
            writer.append("****** ");
            writer.append(getType().getId());
            writer.append(" ******");
            writer.append(LF);
            writer.append("Chain ID: ");
            writer.append(getChainId());
            if (getAliases() != null) {
                writer.append(LF);
                writer.append("Chain Aliases: ");
                writer.append(getAliases());
            }
            writer.append(LF);
            writer.append("Class: ");
            writer.append(getType().getType().getSimpleName());
            writer.append(LF);
            writer.append("Method: '");
            writer.append(getMethod().getMethod().getName());
            writer.append("' | Input Type: ");
            writer.append(getMethod().getConsume().getName());
            writer.append(" | Output Type: ");
            writer.append(getMethod().getProduce().getName());
            writer.append(LF);
            writer.append("Input: ");
            writer.append(getInput() == null ? "null" : getInput().toString());
            if (!getParameters().isEmpty()) {
                writer.append(LF);
                writer.append("Parameters ");
                for (String parameter : getParameters().keySet()) {
                    writer.append(" | ");
                    writer.append("Name: ");
                    writer.append(parameter);
                    writer.append(", Value: ");
                    Object value = getParameters().get(parameter);
                    if (value instanceof Call.ExpressionParameter) {
                        value = String.format("Expr:(id=%s | value=%s)",
                                ((Call.ExpressionParameter) getParameters().get(parameter)).getParameterId(),
                                ((Call.ExpressionParameter) getParameters().get(parameter)).getParameterValue());
                    }
                    writer.append(value.toString());
                }
            }
            if (!getVariables().isEmpty()) {
                writer.append(LF);
                writer.append("Context Variables");
                for (String keyVariable : getVariables().keySet()) {
                    writer.append(" | ");
                    writer.append("Key: ");
                    writer.append(keyVariable);
                    writer.append(", Value: ");
                    Object variable = getVariables().get(keyVariable);
                    if (variable instanceof Calendar) {
                        writer.append(((Calendar) variable).getTime().toString());
                    } else {
                        writer.append(variable == null ? "null" : variable.toString());
                    }
                }
            }
            if (!getNested().isEmpty()) {
                printHeading("start sub chain", writer);
                for (Trace trace : getNested()) {
                    writer.append(LF);
                    trace.print(writer);
                    writer.append(LF);
                }
                printHeading("end sub chain", writer);
            }
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    protected void printHeading(String heading, BufferedWriter writer) throws IOException {
        writer.append(LF).append(LF).append("****** ").append(heading).append(" ******");
    }
}
