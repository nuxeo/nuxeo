/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
