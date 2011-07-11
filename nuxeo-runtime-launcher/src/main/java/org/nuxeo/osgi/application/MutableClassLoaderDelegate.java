/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.osgi.application;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated Use {@link org.nuxeo.launcher.commons.MutableClassLoaderDelegate}
 */
public class MutableClassLoaderDelegate implements MutableClassLoader {

    protected ClassLoader cl;

    protected Method addURL;

    public MutableClassLoaderDelegate(ClassLoader delegate) {
        addURL = urlSetter(delegate);
        if (addURL == null) {
            delegate = new URLClassLoader(new URL[0], delegate);
            addURL = urlSetter(delegate);
        }
        addURL.setAccessible(true);
        cl = delegate;
    }

    private Method urlSetter(ClassLoader cl) {
        Class<?> clazz = cl.getClass();
        do {
            try {
                return clazz.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to adapt class loader: " + cl.getClass(), e);
            }
        } while (clazz != null);
        return null;
    }

    @Override
    public void addURL(URL url) {
        try {
            addURL.invoke(cl, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add URL to class loader: "
                    + url, e);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return cl;
    }

}
