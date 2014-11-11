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
 *     "Stephane Lacoin at Nuxeo (aka matic)"
 */
package org.nuxeo.ecm.core.management.guards;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Invoke proxied service only if service is available regarding administrative statuses.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public class GuardedServiceHandler<T> implements InvocationHandler {

    protected final T object;

    public static <T> T newProxy(T object, Class<T> itf) {
         InvocationHandler h = new GuardedServiceHandler<T>(object);
         return itf.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { itf }, h));
    }

    protected GuardedServiceHandler(T object) {
        this.object = object;
    }

    protected Guarded getAdministred(Method m) {
        if (m.isAnnotationPresent(Guarded.class)) {
            return m.getAnnotation(Guarded.class);
        }
        Class<?> declaringClass = m.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(Guarded.class)) {
            return declaringClass.getAnnotation(Guarded.class);
        }
        return null;
    }

    protected void checkIsAdministred(Method m) {
        Guarded administred = getAdministred(m);
        if (administred == null) {
            return;
        }
        GuardedServiceProvider.INSTANCE.checkIsActive(m, administred.id());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        checkIsAdministred(method);
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
