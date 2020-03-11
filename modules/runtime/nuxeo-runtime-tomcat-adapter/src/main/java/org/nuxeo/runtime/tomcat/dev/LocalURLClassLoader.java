/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LocalURLClassLoader extends URLClassLoader implements LocalClassLoader {

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
    public Class<?> loadLocalClass(String name, boolean resolve) throws ClassNotFoundException {
        // do not look into parent
        synchronized (getParent()) {
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
