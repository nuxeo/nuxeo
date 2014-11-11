/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.osgi.application;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DelegateLoader implements SharedClassLoader {
    
    protected URLClassLoader loader;
    protected Method addUrl;
    
    public DelegateLoader(URLClassLoader loader) {
        this.loader = loader; 
        try {
            addUrl = loader.getClass().getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
        } catch (Exception e) {
            throw new Error("Failed to create a shared delegate loader for classloader: "+loader, e);
        }
    }

    public void addURL(URL url) {
        try {
            addUrl.invoke(loader, new Object[] {url});
        } catch (Throwable e) {
            throw new Error("Failed to add an URL to this loader: "+url, e);
        }
    }

    public URL[] getURLs() {
        return loader.getURLs();
    }

    public ClassLoader getLoader() {
        return loader;
    }

}
