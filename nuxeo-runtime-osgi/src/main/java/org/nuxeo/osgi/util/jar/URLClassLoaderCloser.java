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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarFile;

/**
 * @author matic
 *
 */
public class URLClassLoaderCloser  {


    protected  URLClassLoader loader;

    protected  ArrayList<?> loaders;

    protected  Field jarField;

    protected  Method getJarFileMethod;

    protected  HashMap<?, ?> lmap;



    public URLClassLoaderCloser(URLClassLoader loader) {
        try {
            introspectClassLoader(loader);
        } catch (Exception e) {
            throw new Error("Cannot introspect url class loader " + loader, e);
    }
    }

    protected void introspectClassLoader(URLClassLoader loader) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        this.loader = loader;
        Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
        ucpField.setAccessible(true);

        Object ucp = ucpField.get(loader);
        Class<?> ucpClass = ucp.getClass();
        Field lmapField = ucpClass.getDeclaredField("lmap");
        lmapField.setAccessible(true);
        lmap = (HashMap<?, ?>) lmapField.get(ucp);
        Field loadersField = ucpClass.getDeclaredField("loaders");
        loadersField.setAccessible(true);
        loaders = (ArrayList<?>) loadersField.get(ucp);
        Class<?> jarLoaderClass = getJarLoaderClass();
        jarField = jarLoaderClass.getDeclaredField("jar");
        jarField.setAccessible(true);
        getJarFileMethod = jarLoaderClass.getDeclaredMethod("getJarFile",
                new Class<?>[] { URL.class });
        getJarFileMethod.setAccessible(true);

    }

    protected static Class<?> getJarLoaderClass() throws ClassNotFoundException {
        return URLClassLoaderCloser.class.getClassLoader().loadClass("sun.misc.URLClassPath$JarLoader");
    }

    protected static String serializeURL(URL location) {
        StringBuilder localStringBuilder = new StringBuilder(128);
        String str1 = location.getProtocol();
        if (str1 != null) {
            str1 = str1.toLowerCase();
            localStringBuilder.append(str1);
            localStringBuilder.append("://");
        }
        String str2 = location.getHost();
        if (str2 != null) {
            str2 = str2.toLowerCase();
            localStringBuilder.append(str2);
            int i = location.getPort();
            if (i == -1)
                i = location.getDefaultPort();
            if (i != -1)
                localStringBuilder.append(":").append(i);
        }
        String str3 = location.getFile();
        if (str3 != null)
            localStringBuilder.append(str3);
        return localStringBuilder.toString();
    }

    Object getLoader(String name) throws IllegalArgumentException,
            IllegalAccessException {
        for (Object loader : loaders) {
            JarFile jar = (JarFile) jarField.get(loader);
            if (name.equals(jar.getName())) {
                return loader;
            }
        }
        throw new IllegalArgumentException("No such jar " + name);
    }

    public boolean close(URL location) throws IOException {
        if (lmap.isEmpty()) {
            return false;
        }
        Object firstKey = lmap.keySet().iterator().next();
        Object loader = firstKey instanceof URL ? lmap.remove(location)
                : lmap.remove(serializeURL(location));
        if (loader == null) {
            return false;
        }
        loaders.remove(loader);
        JarFile jar = null;
        try {
            jar = (JarFile) jarField.get(loader);
            jarField.set(loader, null);
        } catch (Exception e) {
            throw new Error("Cannot use reflection on url class path", e);
        }
        jar.close();
        return true;
    }

}
