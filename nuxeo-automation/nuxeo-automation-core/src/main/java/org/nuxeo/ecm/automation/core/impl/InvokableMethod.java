/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

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

    public final static int VOID_PRIORITY = 1;

    public final static int ADAPTABLE_PRIORITY = 2;

    public final static int ISTANCE_OF_PRIORITY = 3;

    public final static int EXACT_MATCH_PRIORITY = 4;

    public final static int USER_PRIORITY = 9; // priorities from 1 to 10 are

    // reserved for internal use.

    protected OperationType op;

    protected Method method;

    protected Class<?> produce;

    protected Class<?> consume;

    protected int priority;

    public InvokableMethod(OperationType op, Method method) {
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
        this.priority = method.getAnnotation(OperationMethod.class).priority();
        if (priority > 0) {
            priority += USER_PRIORITY;
        }
        consume = p.length == 0 ? Void.TYPE : p[0];
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
     * Return 0 for no match
     *
     * @param in
     * @param consume
     * @return
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

    /**
     * Return 0 for no match
     *
     * @param in
     * @param consume
     * @return
     */
    public int outputMatch(Class<?> out) {
        if (produce == out) {
            return priority > 0 ? priority : EXACT_MATCH_PRIORITY;
        }
        if (out.isAssignableFrom(produce)) {
            return priority > 0 ? priority : ISTANCE_OF_PRIORITY;
        }
        if (op.getService().isTypeAdaptable(produce, out)) {
            return priority > 0 ? priority : ADAPTABLE_PRIORITY;
        }
        return 0;
    }

    public Object invoke(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        try {
            Object input = ctx.getInput();
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
        } catch (Exception e) {
            // be more explicit about the operation that failed
            throw new OperationException("Failed to invoke operation "
                    + getOperation().getId(), e);
        }
    }

}
