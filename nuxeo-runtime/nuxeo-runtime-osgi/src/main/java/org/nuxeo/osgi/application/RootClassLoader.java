/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
class RootClassLoader extends ClassLoader {

    private final Class<?> loaderClass;

    private final String loaderName;

    RootClassLoader(ClassLoader parent, Class<?> loaderClass) {
        super(parent);
        this.loaderClass = loaderClass;
        this.loaderName = loaderClass.getName();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (loaderName.equals(name)) {
            return loaderClass;
        }
        throw new ClassNotFoundException(name);
    }

}
