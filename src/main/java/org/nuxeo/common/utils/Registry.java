/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */

package org.nuxeo.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generic registry implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class Registry<T> {

    private final String name;

    private final Map<String, T> registry;

    public Registry(String name) {
        this.name = name;
        registry = new HashMap<String, T>();
    }

    public String getName() {
        return name;
    }

    public void register(String name, T object) {
        if (!isRegistered(name) && !isRegistered(object)) {
            registry.put(name, object);
        }
    }

    public void unregister(String name) {
        if (isRegistered(name)) {
            registry.remove(name);
        }
    }

    public boolean isRegistered(T object) {
        return registry.containsValue(object);
    }

    public boolean isRegistered(String name) {
        return registry.containsKey(name);
    }

    public int size() {
        return registry.size();
    }

    public T getObjectByName(String name) {
        return registry.get(name);
    }

    public void clear() {
        registry.clear();
    }

    public Set<String> getKeys() {
        return registry.keySet();
    }

}
