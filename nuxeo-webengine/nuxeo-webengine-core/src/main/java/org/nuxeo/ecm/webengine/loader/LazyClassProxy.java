/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webengine.loader;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LazyClassProxy implements ClassProxy {

    protected final String className;

    protected final ClassLoader loader;

    protected Class<?> clazz;

    public LazyClassProxy(ClassLoader loader, String className) {
        this.loader = loader;
        this.className = className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    @Override
    public Class<?> get() {
        if (clazz == null) {
            try {
                clazz = loader.loadClass(className);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to load class " + className, e);
            }
        }
        return clazz;
    }

    public void reset() {
        clazz = null;
    }

    @Override
    public String toString() {
        return className;
    }

}
