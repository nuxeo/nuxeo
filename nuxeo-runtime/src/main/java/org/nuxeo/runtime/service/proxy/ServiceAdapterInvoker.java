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
