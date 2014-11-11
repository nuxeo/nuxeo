/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
    public static <T> T newProxy(T proxied, Class<?> ...classes) {
        MetricInvocationHandler<T> handler = new MetricInvocationHandler<T>(proxied);
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), classes, handler);
    }

    protected String formatParms(Object... parms) {
        if (parms == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Object parm : parms) {
            buffer.append(".").append(parm);
        }
        return buffer.toString();
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
