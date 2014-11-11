/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.osgi.util.jar;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author matic
 *
 */
public class JarFileFactoryCloser {

    Object factory;

    Method factoryGetMethod;

    Method factoryCloseMethod;

    public JarFileFactoryCloser() {
        try {
            introspectClasses();
        } catch (Exception e) {
            throw new Error("Cannot introspect jar file factory class", e);
        }
    }

    protected void introspectClasses() throws ClassNotFoundException,
            SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, NoSuchMethodException {
        Class<?> jarURLConnectionClass = loadClass("sun.net.www.protocol.jar.JarURLConnection");
        Field jarFileFactoryField = jarURLConnectionClass.getDeclaredField("factory");
        jarFileFactoryField.setAccessible(true);
        factory = jarFileFactoryField.get(null);
        Class<?> factoryClass = loadClass("sun.net.www.protocol.jar.JarFileFactory");
        factoryGetMethod = factoryClass.getMethod("get",
                new Class<?>[] { URL.class });
        factoryGetMethod.setAccessible(true);
        factoryCloseMethod = factoryClass.getMethod("close",
                new Class<?>[] { JarFile.class });
        factoryCloseMethod.setAccessible(true);
    }

    protected static Class<?> loadClass(String name)
            throws ClassNotFoundException {
        return URLClassLoaderCloser.class.getClassLoader().loadClass(name);
    }

    public void close(URL location) throws IOException  {
        JarFile jar = null;
        try {
            jar = (JarFile) factoryGetMethod.invoke(factory,
                    new Object[] { location });
            factoryCloseMethod.invoke(factory, jar);
        } catch (Exception e) {
            throw new Error("Cannot use reflection on jar file factory", e);
        }
        jar.close();
    }


}
