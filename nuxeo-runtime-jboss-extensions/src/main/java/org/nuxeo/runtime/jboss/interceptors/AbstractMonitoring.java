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

package org.nuxeo.runtime.jboss.interceptors;

import java.lang.reflect.Method;

import org.javasimon.Counter;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.jboss.aop.joinpoint.MethodInvocation;

public abstract class AbstractMonitoring {

    public static final String RESPONSE_DURATION_TAG = "nx:duration";

    protected Stopwatch getStopwatch(MethodInvocation ctx, String... names) {
        String name = formatName(ctx, names);
        Stopwatch stopwatch = SimonManager.getStopwatch(name);
        stopwatch.setNote(formatNote(ctx));
        return stopwatch;
    }

    protected Counter getCounter(MethodInvocation ctx, String... names) {
        String name = formatName(ctx, names);
        return SimonManager.getCounter(name);
    }

    protected String formatParams(Object... params) {
        if (params == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Object param : params) {
            buffer.append(".").append(param);
        }
        return buffer.toString();
    }

    protected String formatName(MethodInvocation context, String... params) {
        Method m = context.getActualMethod();
        Class<?> declaringClass = m.getDeclaringClass();
        return String.format("%s.%s%s", declaringClass.getSimpleName(), m.getName(), formatParams(params));
    }

    protected String formatNote(MethodInvocation context) {
        Method m = context.getActualMethod();
        return String.format("%s#%s(%s)", m.getDeclaringClass().getSimpleName(), m.getName(), formatParams(context.getArguments()));
    }

}
