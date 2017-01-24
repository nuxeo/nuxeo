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
 *     bstefanescu
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.ExitException;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.runtime.api.Framework;

/**
 * An operation invocation chain. The chain is immutable (cannot be modified after it was built). To create a new chain
 * from a description call the static method: {@link this#buildChain (AutomationService, Class, List)} This is a self
 * contained object - once built it can be used at any time to invoke the operations in the chain.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
class CompiledChainImpl implements CompiledChain {

    protected final OperationType op;

    protected static String opId;

    protected OperationContext context;

    protected AutomationService service;

    protected Map<String, Object> compileParameters; // argument references

    protected InvokableMethod method;

    protected CompiledChainImpl next;

    CompiledChainImpl(OperationType op, Map<String, Object> args) {
        this(null, op, args);
    }

    CompiledChainImpl(CompiledChainImpl parent, OperationType op, Map<String, Object> args) {
        if (parent != null) {
            parent.next = this;
        }
        this.op = op;
        compileParameters = args;
        opId = op != null ? op.getId() : "null";
    }

    public final InvokableMethod method() {
        return method;
    }

    public final Map<String, Object> args() {
        return compileParameters;
    }

    /**
     * Compute the best matching path to perform the chain of operations. The path is computed using a backtracking
     * algorithm.
     */
    public boolean initializePath(Class<?> in) {
        InvokableMethod[] methods = op.getMethodsMatchingInput(in);
        if (methods == null) {
            return false;
        }
        if (next == null) {
            method = methods[0];
            return true;
        }
        for (InvokableMethod m : methods) {
            Class<?> nextIn = m.getOutputType();
            if (nextIn == Void.TYPE || nextIn.equals(Object.class)) {
                nextIn = in; // preserve last input
            }
            if (next.initializePath(nextIn)) {
                method = m;
                return true;
            }
        }
        return false;
    }

    @OperationMethod
    public Object run() throws OperationException {
        return invoke(context);
    }

    @Override
    public Object invoke(OperationContext ctx) throws OperationException {
        try {
            return doInvoke(ctx);
        } catch (ExitException e) {
            if (e.isRollback()) {
                ctx.setRollback();
            }
            return ctx.getInput();
        }
    }

    protected Object doInvoke(OperationContext ctx) throws OperationException {
        // add debug info
        final OperationCallback callback = ctx.getChainCallback();
        callback.onOperation(ctx, op, method, compileParameters);
        // invoke method
        Object out = method.invoke(ctx, compileParameters);
        ctx.setInput(out);
        if (next != null) {
            return next.invoke(ctx);
        } else {
            return out;
        }
    }

    @Override
    public String toString() {
        return "CompiledChainImpl [op=" + op + "]";
    }

    public static CompiledChainImpl buildChain(Class<?> in, OperationParameters[] params)
            throws OperationNotFoundException, InvalidChainException {
        return buildChain(Framework.getLocalService(AutomationService.class), in, params);
    }

    public static CompiledChainImpl buildChain(AutomationService service, Class<?> in, OperationParameters[] operations)
            throws OperationNotFoundException, InvalidChainException {
        if (operations.length == 0) {
            throw new InvalidChainException("Null operation chain.");
        }
        OperationParameters params = operations[0];

        CompiledChainImpl invocation = new CompiledChainImpl(service.getOperation(params.id()), params.map());
        CompiledChainImpl last = invocation;
        for (int i = 1; i < operations.length; i++) {
            params = operations[i];
            last = new CompiledChainImpl(last, service.getOperation(params.id()), params.map());
        }
        // find the best matching path in the chain
        if (!invocation.initializePath(in)) {
            throw new InvalidChainException(
                    "Cannot find any valid path in operation chain - no method found for operation '" + opId
                            + "' and for first input type '" + in.getName() + "'");
        }
        return invocation;
    }

}
