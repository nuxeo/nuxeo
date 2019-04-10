/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;

public class FakeServletContext implements InvocationHandler {

    public static ServletContext getServletContext() {
        FakeServletContext handler = new FakeServletContext();
        return (ServletContext) Proxy.newProxyInstance(
                FakeServletContext.class.getClassLoader(),
                new Class<?>[] { ServletContext.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        if ("getResourceAsStream".equals(methodName)) {
            String name = (String) args[0];
            // look up resource locally
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = TestNuxeoSessionLocal.BASE_RESOURCE + name;
            ClassLoader cl = getClass().getClassLoader();
            InputStream res = cl.getResourceAsStream(name);
            return res;
        }
        return null;
    }

}
