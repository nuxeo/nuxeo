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

import org.javasimon.Split;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

public class ServerMonitoringInterceptor extends AbstractMonitoring implements Interceptor {

    protected final ThreadLocal<Split> reentrancy = new ThreadLocal<Split>();

    public String getName() {
        return getClass().getCanonicalName();
    }

    public Object invoke(Invocation context) throws Throwable {
        Split split = reentrancy.get();
        if (!(context instanceof MethodInvocation) || split != null) {
            return context.invokeNext();
        }
        MethodInvocation mContext = (MethodInvocation)context;
        split = getStopwatch(mContext).start();
        reentrancy.set(split);
        try {
            return context.invokeNext();
        } finally {
            context.addResponseAttachment(RESPONSE_DURATION_TAG, Long.valueOf(split.stop()));
            reentrancy.remove();
        }
    }

}
