/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
