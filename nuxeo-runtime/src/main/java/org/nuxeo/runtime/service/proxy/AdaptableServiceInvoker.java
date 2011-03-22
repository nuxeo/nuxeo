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
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.model.Adaptable;
import org.nuxeo.runtime.service.AdaptableService;
import org.nuxeo.runtime.service.AdapterManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdaptableServiceInvoker<T extends AdaptableService> implements ServiceProxy<T>, InvocationHandler, Adaptable {

    protected final T remote;

    //TODO this map should be managed by derived classes
    protected static final Map<Method, Method> methods = new ConcurrentHashMap<Method, Method>();


    public AdaptableServiceInvoker(T remote) {
        this.remote = remote;
    }

    @Override
    public T getRemote() {
        return remote;
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        A adapterInst = AdapterManager.getInstance().getAdapter(this, adapter);
        if (adapterInst != null) {
            return adapterInst;
        }
        if (remote.hasAdapter(adapter)) {
            return getAdapterProxy(adapter);
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    protected <A> A getAdapterProxy(Class<A> adapter) {
        return (A) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{adapter}, new ServiceAdapterInvoker(this));
    }

    protected static void handleException(Throwable t) throws Throwable {
        throw t;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methods.get(method);
        if (m == null) {
            try {
                m = getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                m = method;
            }
            methods.put(method, m);
        }
        try {
            return m.invoke(m == method ? remote : this, args) ;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                handleException(cause);
            } else {
                handleException(e);
            }
            throw e;
        } catch (Throwable t) {
            handleException(t);
            throw t;
        }
    }

}
