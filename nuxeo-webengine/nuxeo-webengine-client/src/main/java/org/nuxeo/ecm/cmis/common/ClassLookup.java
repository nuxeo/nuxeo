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

package org.nuxeo.ecm.cmis.common;


/**
 * A Class keyed map sensible to class hierarchy.
 * This map provides an additional method {@link #lookup(Class)}
 * that can be used to lookup compatible to the given one 
 * depending on the class hierarchy.
 *  
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClassLookup {

    private static final long serialVersionUID = 1L;

    public static Object lookup(Class<?> key, ClassRegistry registry) {
        Object v = registry.get(key);
        if (v == null) {
            Class<?> sk = key.getSuperclass();
            if (sk != null) {
                v = registry.get(sk);
            }
            Class<?>[] itfs = null;
            if (v == null) { // try interfaces
                itfs = key.getInterfaces();
                for (Class<?> itf : itfs) {
                    v = registry.get(itf);
                    if (v != null) {
                        break;
                    }
                }
            }
            if (v == null) {
                if (sk != null) { // superclass
                    v = lookup(sk, registry);
                }
                if (v == null) { // interfaces
                    for (Class<?> itf : itfs) {
                        v = lookup(itf, registry);
                        if (v != null) {
                            break;
                        }
                    }
                }
            }
            if (v != null) {
                registry.put(key, v);
            }
        }
        return v;
    }

    public static Object lookup(Class<?> key, ClassNameRegistry registry) {
        Object v = registry.get(key.getName());
        if (v == null) {
            Class<?> sk = key.getSuperclass();
            if (sk != null) {
                v = registry.get(sk.getName());
            }
            Class<?>[] itfs = null;
            if (v == null) { // try interfaces
                itfs = key.getInterfaces();
                for (Class<?> itf : itfs) {
                    v = registry.get(itf.getName());
                    if (v != null) {
                        break;
                    }
                }
            }
            if (v == null) {
                if (sk != null) { // superclass
                    v = lookup(sk, registry);
                }
                if (v == null) { // interfaces
                    for (Class<?> itf : itfs) {
                        v = lookup(itf, registry);
                        if (v != null) {
                            break;
                        }
                    }
                }
            }
            if (v != null) {
                registry.put(key.getName(), v);
            }
        }
        return v;
    }

}
