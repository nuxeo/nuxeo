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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebClassLoader extends URLClassLoader {

    public WebClassLoader() {
        super(new URL[0], Thread.currentThread().getContextClassLoader());
    }

    public WebClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public WebClassLoader(URL[] urls) {
        super(urls, Thread.currentThread().getContextClassLoader());
    }

    public WebClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addFile(File file) {
        try {
            super.addURL(file.getCanonicalFile().toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();// should never happens
        }
    }

}
