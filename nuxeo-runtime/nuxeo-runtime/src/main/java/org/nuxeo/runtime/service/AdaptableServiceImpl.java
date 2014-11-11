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

package org.nuxeo.runtime.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.service.proxy.MethodInvocation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdaptableServiceImpl implements AdaptableService {

    protected final AdapterManager adapterManager;
    protected final Map<Class<?>, Object> adapters;

    public AdaptableServiceImpl() {
        adapterManager = AdapterManager.getInstance();
        adapters = new ConcurrentHashMap<Class<?>, Object>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        Object obj = adapters.get(adapter);
        if (obj == null) {
            obj = adapterManager.getAdapter(this, adapter);
            if (obj != null) {
                adapters.put(adapter, obj);
            }
        }
        return (T)obj;
    }

    @Override
    public boolean hasAdapter(Class<?> adapter) {
        return getAdapter(adapter) != null;
    }

    @Override
    public Object invokeAdapter(MethodInvocation invocation, Object[] args)
            throws NoSuchAdapterException, InvocationTargetException, IllegalAccessException {
        Method m = invocation.getMethod();
        Class<?> adapterClass = m.getDeclaringClass();
        Object adapterInstance = getAdapter(adapterClass);
        if (adapterInstance == null) {
            throw new NoSuchAdapterException(adapterClass.getName());
        }
        return m.invoke(adapterInstance, args);
    }

}
