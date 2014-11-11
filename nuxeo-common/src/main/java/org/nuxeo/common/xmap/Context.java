/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Benjamin JALON
 *
 */

package org.nuxeo.common.xmap;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Context extends ArrayList<Object> {

    private static final long serialVersionUID = 1L;

    private final HashMap<String, Object> properties = new HashMap<String, Object>();


    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (className.startsWith("[")) {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public Object getObject() {
        int size = size();
        if (size > 0) {
            return get(size - 1);
        }
        return null;
    }

    public Object getParent() {
        int size = size();
        if (size > 1) {
            return get(size - 2);
        }
        return null;
    }

    public void push(Object object) {
        add(object);
    }

    public Object pop() {
        int size = size();
        if (size > 0) {
            return remove(size - 1);
        }
        return null;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

}
