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

package org.nuxeo.runtime.jboss.interceptors;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TraceInterceptor implements org.jboss.aop.advice.Interceptor, java.io.Serializable {

    private static final long serialVersionUID = 1839232187157414668L;

    public Object invoke(Invocation invocation) throws Throwable {
        if (invocation instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation)invocation;
            mi.getActualMethod();
            StackTraceElement[] st = new Exception().getStackTrace();
            System.out.println("---------------------------------------------");
            System.out.print("# "+Thread.currentThread()+" : Invoking : "+mi.getActualMethod());
            double s = System.currentTimeMillis();
            try {
                return invocation.invokeNext();
            } finally {
                System.out.println(" ["+((System.currentTimeMillis()-s)/1000)+" sec.]");
                System.out.println("---------------------------------------------");
                for (int i=0; i<Math.min(st.length, 10); i++) {
                    System.out.println(">> "+st[i]);
                }
                System.out.println("---------------------------------------------");
            }
        } else {
            return invocation.invokeNext();
        }
    }

    public String getName() {
        return getClass().getName();
    }

}
