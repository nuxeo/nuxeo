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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @author matic
 */
public class URLClassLoaderCloser {

    protected List<?> loaders;

    protected final URLJarFileIntrospector introspector;

    protected final Map<?, ?> index;

    public URLClassLoaderCloser(URLJarFileIntrospector anIntrospector, Map<?, ?> anIndex, List<?> someLoaders) {
        introspector = anIntrospector;
        index = anIndex;
        loaders = someLoaders;
    }

    protected static String serializeURL(URL location) {
        StringBuilder localStringBuilder = new StringBuilder(128);
        String str1 = location.getProtocol();
        if (str1 != null) {
            str1 = str1.toLowerCase();
            localStringBuilder.append(str1);
            localStringBuilder.append("://");
        }
        String str2 = location.getHost();
        if (str2 != null) {
            str2 = str2.toLowerCase();
            localStringBuilder.append(str2);
            int i = location.getPort();
            if (i == -1) {
                i = location.getDefaultPort();
            }
            if (i != -1) {
                localStringBuilder.append(":").append(i);
            }
        }
        String str3 = location.getFile();
        if (str3 != null) {
            localStringBuilder.append(str3);
        }
        return localStringBuilder.toString();
    }

    public boolean close(URL location) throws IOException {
        if (index.isEmpty()) {
            return false;
        }
        Object firstKey = index.keySet().iterator().next();
        Object loader = firstKey instanceof URL ? index.remove(location) : index.remove(serializeURL(location));
        if (loader == null) {
            return false;
        }
        loaders.remove(loader);
        JarFile jar = null;
        try {
            jar = (JarFile) introspector.jarField.get(loader);
            introspector.jarField.set(loader, null);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Cannot use reflection on url class path", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot use reflection on url class path", e);
        }
        jar.close();
        return true;
    }

}
