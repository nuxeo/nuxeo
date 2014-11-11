/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */

package org.nuxeo.runtime.annotated;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

/**
 * Start a user transaction if method or class is annotated for and if there is
 * no transaction active
 * 
 * @author matic
 * 
 * @param <T>
 */
public class DummyInstanceHandler<T> implements InvocationHandler {

    protected final T object;

    public static <T> T newProxy(T object, Class<T> itf) {
        InvocationHandler h = new DummyInstanceHandler<T>(object);
        return itf.cast(Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { itf }, h));
    }

    protected DummyInstanceHandler(T object) {
        this.object = object;
    }

    protected boolean requireDummy(Method m) {
//        Stopwatch sw = SimonManager.getStopwatch("annotated.introspection");
//        Split s = sw.start();
//        try {
            if (!m.isAnnotationPresent(Dummy.class)
                    && !m.getClass().isAnnotationPresent(Dummy.class)) {
                return false;
            }
            return true;
//        } finally {
//            s.stop();
//        }
    }

    protected Object doInvoke(Method m, Object[] args) throws Throwable {
        try {
            return m.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (!requireDummy(method)) {
            return doInvoke(method, args);
        }
        // before invoke
        try {
            return doInvoke(method, args);
        } finally {
            // after invoke
            ;
        }

    }

}
