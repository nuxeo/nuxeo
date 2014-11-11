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
