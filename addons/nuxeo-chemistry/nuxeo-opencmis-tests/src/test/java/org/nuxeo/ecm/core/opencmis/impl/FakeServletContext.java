/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;

public class FakeServletContext implements InvocationHandler {

    public static final String BASE_RESOURCE = CmisFeatureSessionHttp.BASE_RESOURCE;

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
