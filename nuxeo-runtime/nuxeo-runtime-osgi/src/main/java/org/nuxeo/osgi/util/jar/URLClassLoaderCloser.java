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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @author matic
 *
 */
public class URLClassLoaderCloser  {

    protected  List<?> loaders;

    protected final URLJarFileIntrospector introspector;

    protected  final Map<?, ?> index;

    public URLClassLoaderCloser(URLJarFileIntrospector anIntrospector, Map<?,?> anIndex, List<?> someLoaders) {
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
        Object loader = firstKey instanceof URL ? index.remove(location)
                : index.remove(serializeURL(location));
        if (loader == null) {
            return false;
        }
        loaders.remove(loader);
        JarFile jar = null;
        try {
            jar = (JarFile) introspector.jarField.get(loader);
            introspector.jarField.set(loader, null);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Cannot use reflection on url class path", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Cannot use reflection on url class path", e);
        }
        jar.close();
        return true;
    }

}
