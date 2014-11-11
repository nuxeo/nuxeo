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

package org.nuxeo.common.collections;

import java.util.HashMap;

/**
 * A Class keyed map sensitive to class hierarchy.
 * This map provides an additional method {@link #find(Class)}
 * that can be used to lookup a class compatible to the given one
 * depending on the class hierarchy.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ClassMap<T> extends HashMap<Class<?>, T>{

    private static final long serialVersionUID = 1L;

    public T find(Class<?> key) {
        T v = get(key);
        if (v == null) {
            Class<?> sk = key.getSuperclass();
            if (sk != null) {
                v = get(sk);
            }
            Class<?>[] itfs = null;
            if (v == null) { // try interfaces
                itfs = key.getInterfaces();
                for (Class<?> itf : itfs) {
                    v = get(itf);
                    if (v != null) {
                        break;
                    }
                }
            }
            if (v == null) {
                if (sk != null) { // superclass
                    v = find(sk);
                }
                if (v == null) { // interfaces
                    for (Class<?> itf : itfs) {
                        v = find(itf);
                        if (v != null) {
                            break;
                        }
                    }
                }
            }
            if (v != null) {
                put(key, v);
            }
        }
        return v;
    }

}
