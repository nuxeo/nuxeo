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
