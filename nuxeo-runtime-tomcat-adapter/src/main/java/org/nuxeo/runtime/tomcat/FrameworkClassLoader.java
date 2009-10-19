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
package org.nuxeo.runtime.tomcat;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FrameworkClassLoader extends URLClassLoader implements MutableURLClassLoader {

    private static FrameworkClassLoader instance;
    
    public synchronized static FrameworkClassLoader getInstance(ClassLoader parent) {
        if (instance == null) {
            instance = new FrameworkClassLoader(parent);
        }
        return instance;
    }
    
    public FrameworkClassLoader() {
        super (new URL[0]);
    }

    public FrameworkClassLoader(ClassLoader parent) {
        super (new URL[0], parent);
    }

    public FrameworkClassLoader(URL[] urls, ClassLoader parent) {
        super (urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

}
