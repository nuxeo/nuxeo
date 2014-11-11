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
package org.nuxeo.ecm.automation.core.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.ExitException;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.runtime.api.Framework;

/**
 * An operation invocation chain. The chain is immutable (cannot be modified
 * after it was built). To create a new chain from a description call the
 * static method: {@link #buildChain(AutomationService, Class, List)} This is a
 * self contained object - once built it can be used at any time to invoke the
 * operations in the chain.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
class CompiledChainImpl implements CompiledChain {

    protected AutomationService service;

    protected final OperationTypeImpl op;

    protected final Map<String, Object> args; // argument references

    protected InvokableMethod method;

    protected CompiledChainImpl next;

    CompiledChainImpl(OperationTypeImpl op, Map<String, Object> args) {
        this(null, op, args);
    }

    CompiledChainImpl(CompiledChainImpl parent, OperationTypeImpl op,
            Map<String, Object> args) {
        if (parent != null) {
            parent.next = this;
        }
        this.op = op;
        this.args = args;
    }

    public final InvokableMethod method() {
        return method;
    }

    public final Map<String, Object> args() {
        return args;
    }

    /**
     * Compute the best matching path to perform the chain of operations. The
     * path is computed using a backtracking algorithm.
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
            if (nextIn == Void.TYPE) { // a control operation
                nextIn = in; // preserve last input
            }
            if (next.initializePath(nextIn)) {
                method = m;
                return true;
            }
        }
        return false;
    }

    public Object invoke(OperationContext ctx) throws OperationException {
        try {
            return doInvoke(ctx);
        } catch (ExitException e) {
            if (e.isRollback()) {
                ctx.setRollback();
            }
            return ctx.getInput();
        } catch (OperationException e) {
            if (e.isRollback()) {
                ctx.setRollback();
            }
            throw e;
        }
    }

    protected Object doInvoke(OperationContext ctx) throws OperationException {
        // add debug info
        ctx.addTrace(method.op.getId() + ":" + method.method.getName());
        // invoke method
        Object out = method.invoke(ctx, args);
        ctx.setInput(out);
        if (next != null) {
            return next.invoke(ctx);
        } else {
            return out;
        }
    }

    public static CompiledChainImpl buildChain(Class<?> in,
            OperationParameters[] params) throws Exception {
        return buildChain(Framework.getLocalService(AutomationService.class),
                in, params);
    }

    public static CompiledChainImpl buildChain(AutomationService service,
            Class<?> in, OperationParameters[] chainParams) throws Exception {
        if (chainParams.length == 0) {
            throw new InvalidChainException("Null operation chain.");
        }
        OperationParameters params = chainParams[0];
        CompiledChainImpl invocation = new CompiledChainImpl(
                (OperationTypeImpl) service.getOperation(params.id()),
                params.map());
        CompiledChainImpl last = invocation;
        for (int i = 1; i < chainParams.length; i++) {
            params = chainParams[i];
            last = new CompiledChainImpl(last,
                    (OperationTypeImpl) service.getOperation(params.id()),
                    params.map());
        }
        // find the best matching path in the chain
        if (!invocation.initializePath(in)) {
            throw new InvalidChainException(
                    "Cannot find any valid path in operation chain");
        }
        return invocation;
    }

}
