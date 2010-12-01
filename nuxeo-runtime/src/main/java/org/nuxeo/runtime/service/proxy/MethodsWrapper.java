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
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Define wrappers for some methods on the target class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class MethodsWrapper implements InvocationHandler {

    private static final Log log = LogFactory.getLog(MethodsWrapper.class);

    protected Map<Method, Method> methods;
    protected Class<?> wrapee;

    public MethodsWrapper(Class<?> klass) {
        methods = new ConcurrentHashMap<Method, Method>();
    }

    protected void loadMethods() {
        methods = new ConcurrentHashMap<Method, Method>();
        Method[] methods = getClass().getMethods();
        for (Method method : methods) {
            if (method.getAnnotation(MethodWrapper.class) != null) {
                try {
                    Method m = wrapee.getMethod(method.getName(), method.getParameterTypes());
                    this.methods.put(m , method);
                } catch (NoSuchMethodException e) {
                    // malformed wrapper
                    log.error(e, e);
                }
            }
        }
    }

    public Method getWrappedMethod(Method method) {
        if (methods == null) {
            synchronized (this) {
                if (methods == null) {
                    loadMethods();
                }
            }
        }
        return methods.get(method);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Method m = methods.get(method);
        if (m == null) {
            return method.invoke(proxy, args);
        } else {
            return method.invoke(this, args);
        }
    }

}
