/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

public class MetricInvocationHandler<T> implements InvocationHandler {

    protected final T proxied;

    protected MetricInvocationHandler(T proxied) {
        this.proxied = proxied;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newProxy(T proxied, Class<?>... classes) {
        MetricInvocationHandler<T> handler = new MetricInvocationHandler<>(proxied);
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), classes, handler);
    }

    protected String formatParms(Object... parms) {
        if (parms == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object parm : parms) {
            sb.append(".").append(parm);
        }
        return sb.toString();
    }

    protected String formatName(Method m, Object[] parms) {
        Class<?> declaringClass = m.getDeclaringClass();
        return String.format("%s.%s", declaringClass.getSimpleName(), m.getName());
    }

    protected String formatNote(Method m, Object[] parms) {
        return String.format("%s#%s(%s)", m.getDeclaringClass().getSimpleName(), m.getName(), formatParms(parms));
    }

    protected Stopwatch getStopwatch(Method m, Object[] parms) {
        String name = formatName(m, parms);
        Stopwatch stopwatch = SimonManager.getStopwatch(name);
        stopwatch.setNote(formatNote(m, parms));
        return stopwatch;
    }

    @Override
    public Object invoke(Object proxy, Method m, Object[] parms) throws Throwable {
        Split split = getStopwatch(m, parms).start();
        try {
            return m.invoke(proxied, parms);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            split.stop();
        }
    }

}
