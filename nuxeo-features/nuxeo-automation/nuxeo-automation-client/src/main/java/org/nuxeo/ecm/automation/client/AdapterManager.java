/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AdapterManager {

    protected final Set<AdapterFactory<?>> factories = new HashSet<AdapterFactory<?>>();

    // put(BusinessObjectService.class,
    public <T> T getAdapter(Session session, Class<T> adapterType) {
        for (AdapterFactory<?> f : factories) {
            if (!factoryAccept(f, adapterType)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            AdapterFactory<T> tFactory = (AdapterFactory<T>) f;
            return adapterType.cast(tFactory.getAdapter(session, adapterType));
        }
        return null;
    }

    protected boolean factoryAccept(AdapterFactory<?> factory, Class<?> adapterType) {
        ParameterizedType itf = (ParameterizedType) factory.getClass().getGenericInterfaces()[0];
        Type type = itf.getActualTypeArguments()[0];
        Class<?> clazz;
        if (type instanceof Class) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw new UnsupportedOperationException("Don't know how to handle " + type.getClass());
        }
        return clazz.isAssignableFrom(adapterType);
    }

    public void registerAdapter(AdapterFactory<?> factory) {
        factories.add(factory);
    }

    public void clear() {
        factories.clear();
    }

}
