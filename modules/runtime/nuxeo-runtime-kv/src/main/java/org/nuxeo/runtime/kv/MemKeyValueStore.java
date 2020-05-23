/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.kv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;

import net.jodah.expiringmap.ExpiringMap;

/**
 * Memory-based implementation of a Key/Value store.
 *
 * @since 9.1
 */
public class MemKeyValueStore extends AbstractKeyValueStoreProvider {

    protected final ExpiringMap<String, byte[]> map;

    protected final Lock writeLock;

    public MemKeyValueStore() {
        map = ExpiringMap.builder().expiration(Integer.MAX_VALUE, TimeUnit.DAYS).variableExpiration().build();
        try {
            writeLock = (Lock) FieldUtils.readField(map, "writeLock", true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<String> keyStream() {
        // don't return a keySet stream directly as it may
        // throw ConcurrentModificationException if there are concurrent writes
        List<String> keys = new ArrayList<>(map.keySet());
        return keys.stream();
    }

    @Override
    public Stream<String> keyStream(String prefix) {
        // don't return a keySet stream directly as it may
        // throw ConcurrentModificationException if there are concurrent writes
        List<String> keys = new ArrayList<>();
        map.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(keys::add);
        return keys.stream();
    }

    @Override
    public void close() {
    }

    @Override
    public void clear() {
        map.clear();
    }

    protected static byte[] clone(byte[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public void put(String key, byte[] value, long ttl) {
        Objects.requireNonNull(key);
        value = clone(value);
        if (value == null) {
            map.remove(key);
        } else if (ttl == 0) {
            map.put(key, value);
        } else {
            map.put(key, value, ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public byte[] get(String key) {
        Objects.requireNonNull(key);
        byte[] value = map.get(key);
        return clone(value);
    }

    @Override
    public boolean setTTL(String key, long ttl) {
        Objects.requireNonNull(key);
        byte[] value = map.get(key);
        if (value == null) {
            return false;
        }
        doSetTTL(key, ttl);
        return true;
    }

    protected void doSetTTL(String key, long ttl) {
        if (ttl == 0) {
            map.setExpiration(key, Integer.MAX_VALUE, TimeUnit.DAYS);
        } else {
            map.setExpiration(key, ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value, long ttl) {
        Objects.requireNonNull(key);
        // clone is not needed if the comparison fails
        // but we are optimistic and prefer to do the clone outside the lock
        value = clone(value);
        // we don't use ExpiringMap.replace because it deals with null differently
        writeLock.lock();
        try {
            byte[] current = map.get(key);
            boolean equal = Arrays.equals(expected, current);
            if (equal) {
                if (value == null) {
                    map.remove(key);
                } else {
                    map.put(key, value);
                    doSetTTL(key, ttl);
                }
            }
            return equal;
        } finally {
            writeLock.unlock();
        }
    }

}
