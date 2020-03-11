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
 */
package org.nuxeo.ecm.automation.core.impl;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.automation.TypeAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AdapterKeyedRegistry extends SuperKeyedRegistry<TypeAdapterKey, TypeAdapter> {

    protected final Set<Class<?>> blacklist;

    public AdapterKeyedRegistry() {
        blacklist = new HashSet<>();
        blacklist.add(Serializable.class);
        blacklist.add(Cloneable.class);
        blacklist.add(Comparable.class);
    }

    @Override
    protected boolean isRoot(TypeAdapterKey key) {
        return key.input == Object.class;
    }

    @Override
    protected List<TypeAdapterKey> getSuperKeys(TypeAdapterKey key) {
        List<TypeAdapterKey> result = new ArrayList<>();
        Class<?> cl = key.input.getSuperclass();
        if (cl != null) {
            result.add(new TypeAdapterKey(cl, key.output));
        }
        for (Class<?> itf : key.input.getInterfaces()) {
            if (!blacklist.contains(itf)) {
                result.add(new TypeAdapterKey(itf, key.output));
            }
        }
        return result;
    }

    @Override
    protected boolean isCachingEnabled(TypeAdapterKey key) {
        return !Proxy.isProxyClass(key.input);
    }

}
