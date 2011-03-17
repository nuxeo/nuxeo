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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdaptableDescriptor {

    public static final Class<?>[] EMPTY_CLASSES = new Class[0];

    protected final Class<?> adaptable;
    protected final Map<Class<?>, AdapterFactory<?>> factories;
    protected Class<?>[] superTypes;

    public AdaptableDescriptor(Class<?> adaptable) {
        this.adaptable = adaptable;
        factories = new ConcurrentHashMap<Class<?>, AdapterFactory<?>>();
    }

    public Class<?> getAdaptable() {
        return adaptable;
    }

    public Class<?>[] getSuperTypes() {
        if (superTypes == null) {
            superTypes = collectSuperTypes(adaptable);
        }
        return superTypes;
    }

    public AdapterFactory<?> getAdapterFactory(Class<?> adapter) {
        return factories.get(adapter);
    }

    public void addAdapterFactory(Class<?> adapter, AdapterFactory<?> factory) {
        factories.put(adapter, factory);
    }

    public void removeAdapterFactory(Class<?> adapter, AdapterFactory<?> factory) {
        factories.remove(adapter);
    }

    public static Class<?>[] collectSuperTypes(Class<?> klass) {
        List<Class<?>> superClasses = new ArrayList<Class<?>>();
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        collectSuperTypes(klass, superClasses, interfaces);
        if (!interfaces.isEmpty()) {
            superClasses.addAll(interfaces);
        }
        if (superClasses.isEmpty()) {
            return EMPTY_CLASSES;
        }
        return superClasses.toArray(new Class<?>[superClasses.size()]);
    }

    public static void collectSuperTypes(Class<?> klass, List<Class<?>> superClasses, List<Class<?>> interfaces) {
        Class<?>[] itfs = klass.getInterfaces();
        if (itfs.length > 0) {
            interfaces.addAll(Arrays.asList(itfs));
        }
        Class<?> superClass = klass.getSuperclass();
        if (superClass != null) {
            superClasses.add(superClass);
            collectSuperTypes(superClass, superClasses, interfaces);
        }
    }

}
