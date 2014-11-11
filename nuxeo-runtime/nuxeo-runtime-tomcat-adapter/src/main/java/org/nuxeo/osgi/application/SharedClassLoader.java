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
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is used to override the SharedClassLoader from nuxeo-osgi.
 *
 * It will be deployed in tomcat/lib and will be visible in root class loader.
 * The one in nuxeo-osgi should be replaced by this one.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SharedClassLoader extends URLClassLoader {

    public SharedClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public SharedClassLoader(URL[] urls) {
        this(urls, getSystemClassLoader());
    }

    public SharedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

}
