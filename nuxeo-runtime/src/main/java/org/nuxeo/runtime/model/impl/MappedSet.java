package org.nuxeo.runtime.model.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class MappedSet<K,V> {
    protected Map<K, Set<V>> map;

    public MappedSet() {
        map = new HashMap<K, Set<V>>();
    }

    public Set<V> get(K key) {
        if (!map.containsKey(key)) {
            return Collections.emptySet();
        }
        return map.get(key);
    }

    public Set<V> put(K key, V value) {
        Set<V> set = map.get(key);
        if (set == null) {
            set = new HashSet<V>();
            map.put(key, set);
        }
        set.add(value);
        return set;
    }

    public Set<V> remove(K key) {
        if (!map.containsKey(key)) {
            return Collections.emptySet();
        }
        return map.remove(key);
    }

    public Set<V> remove(K key, V value) {
        Set<V> set = map.get(key);
        if (set == null) {
            return Collections.emptySet();
        }
        set.remove(value);
        if (set.isEmpty()) {
                map.remove(key);
        }
        return set;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }
}