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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

/**
 * A method proxy which accept as input only iterable inputs. At invocation time
 * it iterates over the input elements and invoke the real method using the
 * current input element as the input.
 * <p>
 * The result is collected into a {@link OutputCollector} as specified by the
 * {@link OperationMethod} annotation.
 * <p>
 * This proxy is used (instead of the default {@link InvokableMethod}) when an
 * operation is defining an output collector in the {@link OperationMethod}
 * annotation so that iterable inputs are automatically handled by the chain
 * executor.
 * <p>
 * This specialized implementation is declaring the same consume type as its
 * non-iterable counterpart. But at runtime it consumes any Iterable of the
 * cosume type.
 * <p>
 * To correctly generate the operation documentation the
 * {@link OperationTypeImpl} is checking if the method is iterable or not
 * through {@link #isIterable()} to declare the correct consume type.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class InvokableIteratorMethod extends InvokableMethod {

    @SuppressWarnings("rawtypes")
    protected Class<? extends OutputCollector> collector;

    public InvokableIteratorMethod(OperationType op, Method method,
            OperationMethod anno) {
        super(op, method, anno);
        collector = anno.collector();
        if (collector == OutputCollector.class) {
            throw new IllegalArgumentException("Not an iterable method");
        }
        // check the collector match the method signature - to early detect invalid
        // operation definitions.
        if (consume == Void.TYPE) {
            throw new IllegalArgumentException(
                    "An iterable method must have an argument");
        }
        Type[] ctypes = IterableInputHelper.findCollectorTypes(collector);
        if (!((Class<?>)ctypes[0]).isAssignableFrom(produce)) {
            throw new IllegalArgumentException(
            "The collector used on "+method+" doesn't match the method return type");
        }
        // must modify the produced type to fit the real produced type.
        try {
            produce = (Class<?>)ctypes[1];
        } catch (Exception e) {
            throw new IllegalStateException("Invalid output collector: "
                    + collector + ". No getOutput method found.");
        }
        // the consumed type is not used in chain compilation so we let it as is
        // for now.
    }

    @Override
    public boolean isIterable() {
        return true;
    }

    @Override
    public int inputMatch(Class<?> in) {
        Class<?> iterableIn = IterableInputHelper.getIterableType(in);
        if (iterableIn != null) {
            return super.inputMatch(iterableIn);
        }
        return 0;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Object doInvoke(OperationContext ctx, Map<String, Object> args,
            Object input) throws Exception {
        if (input instanceof Iterable == false) {
            throw new IllegalStateException(
                    "An iterable method was called in a non iterable context");
        }
        OutputCollector list = collector.newInstance();
        Iterable<?> iterable = (Iterable<?>) input;
        Iterator<?> it = iterable.iterator();
        while (it.hasNext()) {
            Object in = it.next();
            // update context to use as input the current entry
            ctx.setInput(in);
            list.collect(ctx, super.doInvoke(ctx, args, in));
        }
        return list.getOutput();
    }

}
