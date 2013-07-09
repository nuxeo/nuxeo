package org.nuxeo.runtime.tomcat.dev;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
 *     bstefanescu
 */

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalURLClassLoader extends URLClassLoader implements
        LocalClassLoader {

    public LocalURLClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public LocalURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls == null ? new URL[0] : urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadLocalClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // do not look into parent
        synchronized(getParent()) {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                clazz = findClass(name);
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }

    @Override
    public URL getLocalResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getLocalResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    public InputStream getLocalResourceAsStream(String name) throws IOException {
         URL location = getLocalResource(name);
         if (location == null) {
             return null;
         }
         return location.openStream();
    }
}
