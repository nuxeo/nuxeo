/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.osgi.application;

import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @since 5.4.2
 */
public class MutableClassLoaderDelegate implements MutableClassLoader {

    protected final ClassLoader cl;

    protected Method addURL;

    public MutableClassLoaderDelegate(ClassLoader cl) throws IllegalArgumentException {
        this.cl = cl;
        Class<?> clazz = cl.getClass();
        do {
            try {
                addURL = clazz.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            } catch (SecurityException e) {
                throw new IllegalArgumentException("Failed to adapt class loader: " + cl.getClass(), e);
            }
        } while (addURL == null && clazz != null);
        if (addURL == null) {
            throw new IllegalArgumentException("Incompatible class loader: " + cl.getClass()
                    + ". ClassLoader must provide a method: addURL(URL url)");
        }
        addURL.setAccessible(true);
    }

    @Override
    public void addURL(URL url) {
        try {
            addURL.invoke(cl, url);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to add URL to class loader: " + url, e);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return cl;
    }

    @Override
    public Class<?> loadClass(String startupClass) throws ClassNotFoundException {
        return cl.loadClass(startupClass);
    }

}
