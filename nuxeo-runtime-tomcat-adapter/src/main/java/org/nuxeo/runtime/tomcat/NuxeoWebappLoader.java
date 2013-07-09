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

import java.io.File;
import java.lang.reflect.Method;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;

/**
 * Shared attribute is experimental. Do not use it yet.
 * <p>
 * (Its purpose is to be able to deploy multiple WARs using the same nuxeo instance
 * but it is not working yet).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoWebappLoader extends WebappLoader {

    protected File baseDir; // the baseDir from the Context (which is private..)

    protected void overwriteWar() throws Exception {
        //File baseDir = getBaseDir();
        // remove all files
    }

    public File getBaseDir() throws Exception {
        if (baseDir == null) {
            Container container = getContainer();
            Method method = StandardContext.class.getDeclaredMethod("getBasePath");
            method.setAccessible(true);
            String path = (String)method.invoke(container);
            baseDir = new File(path);
        }
        return baseDir;
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }
}
