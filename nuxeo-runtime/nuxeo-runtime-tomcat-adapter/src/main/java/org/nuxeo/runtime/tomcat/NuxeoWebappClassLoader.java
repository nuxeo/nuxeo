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

package org.nuxeo.runtime.tomcat;

import java.net.URL;

import org.apache.catalina.loader.WebappClassLoader;
import org.nuxeo.osgi.application.MutableClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoWebappClassLoader extends WebappClassLoader implements MutableClassLoader {

    public NuxeoWebappClassLoader() {
    }

    public NuxeoWebappClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public void setParentClassLoader(ClassLoader pcl) {
        super.setParentClassLoader(pcl);
    }

    public ClassLoader getParentClassLoader() {
        return parent;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this;
    }

    @Override
    // synchronized to avoid some race conditions
    // see https://issues.apache.org/bugzilla/show_bug.cgi?id=44041
    public synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

}
