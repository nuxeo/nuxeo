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
