/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.osgi.util.jar;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author matic
 */
public class JarFileFactoryCloser {

    private static final Log log = LogFactory.getLog(JarFileFactoryCloser.class);

    protected boolean ok;

    protected Object factory;

    protected Method factoryGetMethod;

    protected Method factoryCloseMethod;

    public JarFileFactoryCloser() {
        try {
            introspectClasses();
        } catch (ClassNotFoundException e) {
            log.error("Cannot introspect jar file factory class", e);
        } catch (SecurityException e) {
            log.error("Cannot introspect jar file factory class", e);
        } catch (NoSuchFieldException e) {
            log.error("Cannot introspect jar file factory class", e);
        } catch (IllegalAccessException e) {
            log.error("Cannot introspect jar file factory class", e);
        } catch (NoSuchMethodException e) {
            log.error("Cannot introspect jar file factory class", e);
        }
    }

    protected void introspectClasses() throws ClassNotFoundException, SecurityException, NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException {
        Class<?> jarURLConnectionClass = loadClass("sun.net.www.protocol.jar.JarURLConnection");
        Field jarFileFactoryField = jarURLConnectionClass.getDeclaredField("factory");
        jarFileFactoryField.setAccessible(true);
        factory = jarFileFactoryField.get(null);
        Class<?> factoryClass = loadClass("sun.net.www.protocol.jar.JarFileFactory");
        factoryGetMethod = factoryClass.getMethod("get", new Class<?>[] { URL.class });
        factoryGetMethod.setAccessible(true);
        factoryCloseMethod = factoryClass.getMethod("close", new Class<?>[] { JarFile.class });
        factoryCloseMethod.setAccessible(true);
        ok = true;
    }

    protected static Class<?> loadClass(String name) throws ClassNotFoundException {
        return URLClassLoaderCloser.class.getClassLoader().loadClass(name);
    }

    public void close(URL location) throws IOException {
        if (!ok) {
            return;
        }
        JarFile jar = null;
        try {
            jar = (JarFile) factoryGetMethod.invoke(factory, new Object[] { location });
            factoryCloseMethod.invoke(factory, jar);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot use reflection on jar file factory", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot use reflection on jar file factory", e);
        }
        jar.close();
    }

}
