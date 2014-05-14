/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A copy-on-write {@link Map}, with deep copy of values that are {@link CowMap}
 * or {@link CowList} themselves. Other values are assumed immutable.
 * <p>
 * A {@link #deepCopy} will return fast, and a copy will be done as soon as a
 * write is done.
 */
public class CowMap implements Map<String, Serializable>, Serializable {

    private static final long serialVersionUID = 1L;

    protected Map<String, Serializable> map;

    protected boolean shared;

    public CowMap() {
        map = new HashMap<String, Serializable>();
        shared = false;
    }

    public CowMap(Map<String, Serializable> other) {
        map = CopyHelper.deepCopy(other);
        shared = false;
    }

    protected CowMap(CowMap other) {
        map = other.map;
        shared = true;
    }

    public CowMap deepCopy() {
        shared = true;
        return new CowMap(this);
    }

    protected Map<String, Serializable> unshare() {
        if (shared == true) {
            shared = false;
            map = new HashMap<String, Serializable>(map);
            for (Entry<String, Serializable> en : map.entrySet()) {
                Serializable value = en.getValue();
                if (value instanceof CowMap) {
                    en.setValue(((CowMap) value).deepCopy());
                } else if (value instanceof CowList) {
                    en.setValue(((CowList) value).deepCopy());
                }
            }
        }
        return map;
    }

    protected Map<String, Serializable> readonly() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Serializable get(Object key) {
        return map.get(key);
    }

    @Override
    public Serializable put(String key, Serializable value) {
        return unshare().put(key, value);
    }

    @Override
    public Serializable remove(Object key) {
        return unshare().remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> t) {
        unshare().putAll(t);
    }

    @Override
    public void clear() {
        unshare().clear();
    }

    @Override
    public Set<String> keySet() {
        return readonly().keySet();
    }

    @Override
    public Collection<Serializable> values() {
        return readonly().values();
    }

    @Override
    public Set<Entry<String, Serializable>> entrySet() {
        return readonly().entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }

}
