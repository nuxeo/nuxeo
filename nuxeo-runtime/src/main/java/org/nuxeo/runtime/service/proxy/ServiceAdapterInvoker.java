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
 *
 * $Id$
 */

package org.nuxeo.runtime.service.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceAdapterInvoker implements InvocationHandler {

    protected static final ConcurrentHashMap<Method, Method> methods = new ConcurrentHashMap<Method, Method>();

    protected final AdaptableServiceInvoker<?> invoker;

    public ServiceAdapterInvoker(AdaptableServiceInvoker<?> remote) {
        invoker = remote;
    }

    public AdaptableServiceInvoker<?> getServiceInvoker() {
        return invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation invocation = new MethodInvocation(method);
        try {
            return invoker.getRemote().invokeAdapter(invocation, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                invoker.handleException(cause);
            } else {
                invoker.handleException(e);
            }
            throw e;
        } catch (Throwable t) {
            invoker.handleException(t);
            throw t;
        }
    }

}
