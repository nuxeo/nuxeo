/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Given a location, close the corresponding jar files opened by URL class loaders and in jar file cache
 *
 * @since 5.6
 * @author matic
 */
public class URLJarFileCloser implements JarFileCloser {

    protected final URLClassLoaderCloser applicationCloser;

    protected final URLJarFileIntrospector introspector;

    public URLJarFileCloser(URLJarFileIntrospector anIntrospector, ClassLoader appCL)
            throws URLJarFileIntrospectionError {
        introspector = anIntrospector;
        applicationCloser = appCL instanceof URLClassLoader ? introspector.newURLClassLoaderCloser((URLClassLoader) appCL)
                : null;
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
            LogFactory.getLog(URLJarFileCloser.class).error("Cannot introspect shared resource loader", cause);

        }
        if (closed == false) {
            if (applicationCloser != null) {
                closed = applicationCloser.close(location);
            }
        }
        introspector.close(location);
    }
}
