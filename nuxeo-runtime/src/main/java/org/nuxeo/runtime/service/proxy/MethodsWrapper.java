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
