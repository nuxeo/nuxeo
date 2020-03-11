/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Helper class to acess a resource by name within the current classloader
 *
 * @since 10.2
 */
public class ResourceHelper {

    public static URL getResource(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String callerName = Thread.currentThread().getStackTrace()[2].getClassName();
        String relativePath = callerName.replace('.', '/').concat(".class");
        String fullPath = loader.getResource(relativePath).getPath();
        String basePath = fullPath.substring(0, fullPath.indexOf(relativePath));
        try {
            Enumeration<URL> resources = loader.getResources(name);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getPath().startsWith(basePath)) {
                    return resource;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return loader.getResource(name);
    }

}
