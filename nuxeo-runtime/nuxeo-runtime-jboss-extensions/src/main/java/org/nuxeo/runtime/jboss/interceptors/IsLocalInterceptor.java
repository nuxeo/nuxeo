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
import org.jboss.aspects.remoting.InvokeRemoteInterceptor;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IsLocalInterceptor extends org.jboss.ejb3.remoting.IsLocalInterceptor  {

    private static final long serialVersionUID = 8262438857183770747L;

    private static String bindAddr;

    public static String getBindAddr() {
        if (bindAddr == null) {
            bindAddr = System.getProperty("jboss.bind.address", "");
            if (bindAddr.equals("0.0.0.0")) {
                InvokerLocator localLocator = new InvokerLocator("socket", "0.0.0.0", 3873, "", null);
                bindAddr = localLocator.getHost();
            }
        }
        return bindAddr;
    }

    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    @Override
    public Object invoke(Invocation invocation) throws Throwable {
        InvokerLocator locator = (InvokerLocator)invocation.getMetaData(InvokeRemoteInterceptor.REMOTING,
                InvokeRemoteInterceptor.INVOKER_LOCATOR); // "REMOTING", "INVOKER_LOCATOR"
        if (locator != null) {
            //System.out.println("######## TARGET: "+locator.getHost()+"; BIND_ADDR: "+getBindAddr());
            String host = locator.getHost();
            if (!host.equals(getBindAddr())) { // avoid doing default isLocalInterceptor check
                return invocation.invokeNext();
            }
        }
        return super.invoke(invocation);
    }

}
