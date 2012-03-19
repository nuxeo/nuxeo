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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;


/**
 *
 * Given a location, close the corresponding jar files opened by URL class loaders and in jar file cache
 *
 * @since 5.6
 * @author matic
 *
 */
public class JarFileCloser {

    protected URLClassLoaderCloser sharedResourcesCloser;

    protected URLClassLoaderCloser applicationCloser;

    protected Map<URLClassLoader,URLClassLoaderCloser> urlClassLoderClosers =
            new HashMap<URLClassLoader,URLClassLoaderCloser>();

    protected JarFileFactoryCloser factoryCloser = new JarFileFactoryCloser();


    public JarFileCloser(URLClassLoader resourcesCL, ClassLoader appCL)  {
        sharedResourcesCloser = new URLClassLoaderCloser(resourcesCL);
        if (appCL instanceof URLClassLoader) {
            applicationCloser = new URLClassLoaderCloser((URLClassLoader)appCL);
        }
        factoryCloser = new JarFileFactoryCloser();
    }


    public  void close(JarFile file) throws IOException {
       file.close();
       URL location = new File(file.getName()).toURI().toURL();
       if (sharedResourcesCloser.close(location) == false) {
           if (applicationCloser != null) {
               applicationCloser.close(location);
           }
       }
       factoryCloser.close(location);
    }

}
