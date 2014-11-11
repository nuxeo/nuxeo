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
import java.util.jar.JarFile;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.SharedResourceLoader;

/**
 *
 * Given a location, close the corresponding jar files opened by URL class
 * loaders and in jar file cache
 *
 * @since 5.6
 * @author matic
 *
 */
public class URLJarFileCloser implements JarFileCloser {

    protected final URLClassLoaderCloser applicationCloser;

    protected final URLJarFileIntrospector introspector;


    public URLJarFileCloser(URLJarFileIntrospector anIntrospector, ClassLoader appCL) throws URLJarFileIntrospectionError {
        introspector = anIntrospector;
        applicationCloser = appCL instanceof URLClassLoader ? introspector
            .newURLClassLoaderCloser((URLClassLoader) appCL) : null;
    }

    @Override
    public void close(JarFile file) throws IOException {
        file.close();
        URL location = new File(file.getName()).toURI().toURL();
        boolean closed = false;
        try {
            final SharedResourceLoader loader = Framework.getResourceLoader();
            if (loader != null) {
               closed = introspector.newURLClassLoaderCloser(loader).close(location);
            }
        } catch (URLJarFileIntrospectionError cause) {
            LogFactory.getLog(URLJarFileCloser.class).error(
                    "Cannot introspect shared resource loader", cause);

        }
        if (closed == false) {
            if (applicationCloser != null) {
                closed = applicationCloser.close(location);
            }
        }
        introspector.close(location);
    }
}
