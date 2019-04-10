/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public static final String BASE_RESOURCE = "jetty-test";

    public static ServletContext getServletContext() {
        FakeServletContext handler = new FakeServletContext();
        return (ServletContext) Proxy.newProxyInstance(FakeServletContext.class.getClassLoader(),
                new Class<?>[] { ServletContext.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("getResourceAsStream".equals(methodName)) {
            String name = (String) args[0];
            // look up resource locally
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = BASE_RESOURCE + name;
            ClassLoader cl = getClass().getClassLoader();
            InputStream res = cl.getResourceAsStream(name);
            return res;
        }
        return null;
    }

}
