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
 *     bstefanescu
 */
package org.nuxeo.osgi.application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DelegateLoader implements SharedClassLoader {

    protected final URLClassLoader loader;

    protected Method addUrl;

    public DelegateLoader(URLClassLoader loader) {
        this.loader = loader;
        try {
            addUrl = loader.getClass().getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException("Failed to create a shared delegate loader for classloader: " + loader, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to create a shared delegate loader for classloader: " + loader, e);
        }
    }

    @Override
    public void addURL(URL url) {
        try {
            addUrl.invoke(loader, url);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to add an URL to this loader: " + url, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to add an URL to this loader: " + url, e);
        }
    }

    @Override
    public URL[] getURLs() {
        return loader.getURLs();
    }

    @Override
    public ClassLoader getLoader() {
        return loader;
    }

}
