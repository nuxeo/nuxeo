/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class InvokableMethod implements Comparable<InvokableMethod> {

    protected static final Log log = LogFactory.getLog(InvokableMethod.class);

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
            throw new IllegalArgumentException("Operation method must accept at most one argument: " + method);
        }
        // if produce is Void => a control operation
        // if (produce == Void.TYPE) {
        // throw new IllegalArgumentException("Operation method must return a
        // value: "+method);
        // }
        this.op = op;
        this.method = method;
        priority = anno.priority();
        if (priority > 0) {
            priority += USER_PRIORITY;
        }
        consume = p.length == 0 ? Void.TYPE : p[0];
    }

    public InvokableMethod(OperationType op, Method method) {
        produce = method.getReturnType();
        Class<?>[] p = method.getParameterTypes();
        if (p.length > 1) {
            throw new IllegalArgumentException("Operation method must accept at most one argument: " + method);
        }
        this.op = op;
        this.method = method;
        if (priority > 0) {
            priority += USER_PRIORITY;
        }
        String inputType = this.op.getInputType();
        if (inputType != null) {
            switch (inputType) {
            case "document":
                consume = DocumentModel.class;
                break;
            case "documents":
                consume = DocumentModelList.class;
                break;
            case "blob":
                consume = Blob.class;
                break;
            case "blobs":
                consume = Blobs.class;
                break;
            default:
                consume = Object.class;
                break;
            }
        } else {
            consume = p.length == 0 ? Void.TYPE : p[0];
        }    }

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

    OperationServiceImpl service;

    protected Object doInvoke(OperationContext ctx, Map<String, Object> args) throws OperationException,
            ReflectiveOperationException {
        Object target = op.newInstance(ctx, args);
        Object input = ctx.getInput();
        if (consume == Void.TYPE) {
            // preserve last output for void methods
            Object out = method.invoke(target);
            return produce == Void.TYPE ? input : out;
        }
        if (input == null || !consume.isAssignableFrom(input.getClass())) {
            // try to adapt
            input = op.getService().getAdaptedValue(ctx, input, consume);
        }
        return method.invoke(target, input);
    }

    public Object invoke(OperationContext ctx, Map<String, Object> args) throws OperationException {
        try {
            return doInvoke(ctx, args);
        } catch (OperationException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof OperationException) {
                throw (OperationException) t;
            } else {
                String exceptionMessage = "Failed to invoke operation " + op.getId();
                if (op.getAliases() != null && op.getAliases().length > 0) {
                    exceptionMessage += " with aliases " + Arrays.toString(op.getAliases());
                }
                throw new OperationException(exceptionMessage, t);
            }
        } catch (ReflectiveOperationException e) {
            String exceptionMessage = "Failed to invoke operation " + op.getId();
            if (op.getAliases() != null && op.getAliases().length > 0) {
                exceptionMessage += " with aliases " + Arrays.toString(op.getAliases());
            }
            throw new OperationException(exceptionMessage, e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + method + ", " + priority + ")";
    }

    @Override
    // used for methods of the same class, so ignore the class
    public int compareTo(InvokableMethod o) {
        // compare on name
        int cmp = method.getName().compareTo(o.method.getName());
        if (cmp != 0) {
            return cmp;
        }
        // same name, compare on parameter types
        Class<?>[] pt = method.getParameterTypes();
        Class<?>[] opt = o.method.getParameterTypes();
        // smaller length first
        cmp = pt.length - opt.length;
        if (cmp != 0) {
            return cmp;
        }
        // compare parameter classes lexicographically
        for (int i = 0; i < pt.length; i++) {
            cmp = pt[i].getName().compareTo(opt[i].getName());
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getProduce() {
        return produce;
    }

    public Class<?> getConsume() {
        return consume;
    }
}
