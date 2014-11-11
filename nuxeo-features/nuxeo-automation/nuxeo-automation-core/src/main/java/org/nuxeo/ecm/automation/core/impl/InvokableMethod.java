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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class InvokableMethod {

    public static final int VOID_PRIORITY = 1;

    public static final int ADAPTABLE_PRIORITY = 2;

    public static final int ISTANCE_OF_PRIORITY = 3;

    public static final int EXACT_MATCH_PRIORITY = 4;

    // priorities from 1 to 16 are reserved for internal use.
    public static final int USER_PRIORITY = 16;

    protected OperationType op;

    protected Method method;

    protected Class<?> produce;

    protected Class<?> consume;

    protected int priority;


    public InvokableMethod(OperationType op, Method method, OperationMethod anno) {
        produce = method.getReturnType();
        Class<?>[] p = method.getParameterTypes();
        if (p.length > 1) {
            throw new IllegalArgumentException(
                    "Operation method must accept at most one argument: "
                            + method);
        }
        // if produce is Void => a control operation
        // if (produce == Void.TYPE) {
        // throw new IllegalArgumentException("Operation method must return a
        // value: "+method);
        // }
        this.op = op;
        this.method = method;
        this.priority = anno.priority();
        if (priority > 0) {
            priority += USER_PRIORITY;
        }
        consume = p.length == 0 ? Void.TYPE : p[0];
    }

    public boolean isIterable() {
        return false;
    }

    public int getPriority() {
        return priority;
    }

    public OperationType getOperation() {
        return op;
    }

    public final Class<?> getOutputType() {
        return produce;
    }

    public final Class<?> getInputType() {
        return consume;
    }

    /**
     * Return 0 for no match.
     */
    public int inputMatch(Class<?> in) {
        if (consume == in) {
            return priority > 0 ? priority : EXACT_MATCH_PRIORITY;
        }
        if (consume.isAssignableFrom(in)) {
            return priority > 0 ? priority : ISTANCE_OF_PRIORITY;
        }
        if (op.getService().isTypeAdaptable(in, consume)) {
            return priority > 0 ? priority : ADAPTABLE_PRIORITY;
        }
        if (consume == Void.TYPE) {
            return priority > 0 ? priority : VOID_PRIORITY;
        }
        return 0;
    }


    protected Object doInvoke(OperationContext ctx, Map<String, Object> args, Object input)
            throws Exception {
        Object target = op.newInstance(ctx, args);
        if (consume == Void.TYPE) {
            // preserve last output for void methods
            Object out = method.invoke(target);
            return produce == Void.TYPE ? input : out;
        } else {
            if (input != null
                    && !consume.isAssignableFrom(input.getClass())) {
                // try to adapt
                input = op.getService().getAdaptedValue(ctx, input, consume);
            }
            return method.invoke(target, input);
        }
    }

    public Object invoke(OperationContext ctx, Map<String, Object> args)
            throws OperationException {
        try {
            return doInvoke(ctx, args, ctx.getInput());
        } catch (OperationException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof OperationException) {
                throw (OperationException)t;
            } else {
                throw new OperationException("Failed to invoke operation "
                    + op.getId(), t);
            }
        } catch (Throwable t) {
            throw new OperationException("Failed to invoke operation "
                    + op.getId(), t);
        }
    }

}
