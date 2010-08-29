/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.app.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry which inherits values from super keys.
 * <p>
 * The super key relation is defined by the derived classes by overriding {@link #getSuperKey(Object)}  
 * method.
 * The registry is thread safe and is optimized for lookups. 
 * A concurrent cache is dynamically updated when a value is retrieved from a super entry.
 * The cache is removed each time a modification is made on the registry using {@link #put(Object, Object)}
 * or {@link #remove(Object)} methods. Thus, for maximum performance you need to avoid modifying the registry 
 * after lookups were done: 
 * at application startup build the registry, at runtime perform lookups, at shutdown remove entries.
 *  
 * The root key is passed in the constructor and is used to stop looking in super entries.  
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SuperKeyedRegistry<K, V> {

    private static final Object NULL = new Object();
    
    protected K root;
    
    protected Map<K, V> registry;
    
    /**
     * the cache map used for lookups. Object is used for the value to be able to
     * insert NULL values.
     */
    protected volatile ConcurrentMap<K, Object> lookup;

    /**
     * the lock used to update the registry
     */
    private final Object lock = new Object();
    
    public SuperKeyedRegistry(K root) {
        this.root = root;
        registry = new HashMap<K, V>();
    }
    
    public void put(K key, V value) {
        synchronized (lock) {
            registry.put(key, value);
            lookup = null;
        }
    }

    public V remove(K key) {
        V value;
        synchronized (lock) {
            value = registry.remove(key);
            lookup = null;
        }
        return value;
    }

    protected boolean isRoot(K key) {
        return key == root;
    }
    
    protected abstract K getSuperKey(K key);

    @SuppressWarnings("unchecked")
    public V get(K key) {
        Map<K, Object> _lookup = lookup;
        if (_lookup == null) {
            synchronized (lock) {
                lookup = new ConcurrentHashMap<K, Object>(registry);
                _lookup = lookup;
            }
        }
        Object v = _lookup.get(key);
        if (v == null && !isRoot(key)) {
            K sk = getSuperKey(key);
            if (sk != null) {
                v = get(sk);
                if (v != null) { // add inherited binding
                    _lookup.put(key, v);
                } else {
                    _lookup.put(key, NULL);
                }
            }
        }
        return (V)(v == NULL ? null : v);
    }

}
