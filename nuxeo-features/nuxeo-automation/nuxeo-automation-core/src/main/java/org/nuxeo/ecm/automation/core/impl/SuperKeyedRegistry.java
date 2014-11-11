/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry which is inheriting values from super keys. The super key
 * relation is defined by the derived classes by overriding
 * {@link #getSuperKeys(Object)} method. The registry is thread safe and is
 * optimized for lookups. A concurrent cache is dynamically updated when a
 * value is retrieved from a super entry. The cache is removed each time a
 * modification is made on the registry using {@link #put(Object, Object)} or
 * {@link #remove(Object)} methods. Thus, for maximum performance you need to
 * avoid modifying the registry after lookups were done: at application startup
 * build the registry, at runtime perform lookups, at shutdown remove entries.
 * The root key is passed in the constructor and is used to stop looking in
 * super entries.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class SuperKeyedRegistry<K, V> {

    private static final Object NULL = new Object();

    protected Map<K, V> registry;

    /**
     * the cache map used for lookups. Object is used for the value to be able
     * to insert NULL values.
     */
    protected volatile ConcurrentMap<K, Object> lookup;

    /**
     * the lock used to update the registry
     */
    private final Object lock = new Object();

    public SuperKeyedRegistry() {
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

    public void flushCache() {
        synchronized (lock) {
            lookup = null;
        }
    }

    protected abstract boolean isRoot(K key);

    protected abstract List<K> getSuperKeys(K key);

    /**
     * Override this in order to disable caching some specific keys. For
     * example when using java classes as keys you may want to avoid caching
     * proxy classes. The default is to return true. (cache is enabled)
     */
    protected boolean isCachingEnabled(K key) {
        return true;
    }

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
            // System.out.println("cache missed: "+key);
            for (K sk : getSuperKeys(key)) {
                v = get(sk);
                if (v != null && v != NULL) {
                    // we found what we need so abort scanning interfaces /
                    // subclasses
                    if (isCachingEnabled(sk)) {
                        _lookup.put(key, v); // update cache
                        return (V) v;
                    }
                } else {
                    if (isCachingEnabled(sk)) {
                        if (v != null) { // add inherited binding
                            _lookup.put(key, v);
                        } else {
                            _lookup.put(key, NULL);
                        }
                    }
                }
            }
        }
        return (V) (v == NULL ? null : v);
    }

}
