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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ClassKeyedRegistry<V> extends SuperKeyedRegistry<Class<?>, V> {

    @Override
    protected boolean isRoot(Class<?> key) {
        return key == Object.class;
    }

    @Override
    protected List<Class<?>> getSuperKeys(Class<?> key) {
        List<Class<?>> result = new ArrayList<>();
        Class<?> cl = key.getSuperclass();
        if (cl != null) {
            result.add(cl);
        }
        for (Class<?> itf : key.getInterfaces()) {
            result.add(itf);
        }
        return result;
    }

    @Override
    protected boolean isCachingEnabled(Class<?> key) {
        return !Proxy.isProxyClass(key);
    }

}
