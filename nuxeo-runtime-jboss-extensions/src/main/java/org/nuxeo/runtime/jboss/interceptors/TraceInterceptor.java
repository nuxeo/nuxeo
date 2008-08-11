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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TraceInterceptor implements org.jboss.aop.advice.Interceptor, java.io.Serializable {

    private static final long serialVersionUID = 1839232187157414668L;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public Object invoke(Invocation invocation) throws Throwable {
        if (invocation instanceof MethodInvocation) {
            Date date = new Date();
            String d = sdf.format(date);
            MethodInvocation mi = (MethodInvocation)invocation;
            StackTraceElement[] st = new Exception().getStackTrace();
            Object[] ar = mi.getArguments();
            List<Object> args = ar == null ? new ArrayList<Object>() : Arrays.asList(ar);
            System.out.println("---------------------------------------------");
            System.out.println("# "+d+" : Invoking : "+mi.getActualMethod());
            System.out.println("---------------------------------------------");
            System.out.println("#Args: "+args);
            System.out.println("---------------------------------------------");
            double s = System.currentTimeMillis();
            try {
                return invocation.invokeNext();
            } finally {
                System.out.println(" ["+((System.currentTimeMillis()-s)/1000)+" sec.]");
                System.out.println("---------------------------------------------");
                for (int i=0; i<Math.min(st.length, 15); i++) {
                    System.out.println(">> "+st[i]);
                }
            }
        } else {
            return invocation.invokeNext();
        }
    }

    public String getName() {
        return getClass().getName();
    }

}
