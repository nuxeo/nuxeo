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

import org.javasimon.Counter;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

import java.io.Serializable;

public class ClientMonitoringInterceptor extends AbstractMonitoring
        implements Interceptor, Serializable {

    private static final long serialVersionUID = 1L;

    public String getName() {
        return getClass().getCanonicalName();
    }

    public Object invoke(Invocation context) throws Throwable {
        MethodInvocation mCtx = (MethodInvocation)context;
        Stopwatch stopwatch  = getStopwatch(mCtx);
        Split split = stopwatch.start();
        try {
            return context.invokeNext();
        } finally {
            split.stop();
            Long duration = (Long) context.getResponseAttachment(RESPONSE_DURATION_TAG);
            Counter counter = getCounter(mCtx,"serverDuration");
            counter.set(duration == null ? 0 : duration.longValue());
        }
    }

}
