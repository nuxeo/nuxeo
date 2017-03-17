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
package org.nuxeo.ecm.core.storage.kv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Memory-based implementation of a Key/Value store.
 *
 * @since 9.1
 */
public class MemKeyValueStore implements KeyValueStoreProvider {

    protected final Map<String, byte[]> map;

    protected final Lock readLock;

    protected final Lock writeLock;

    public MemKeyValueStore() {
        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
        // We can't use ConcurrentHashMap because compareAndSet needs to compare by value
        // and ConcurrentHashMap doesn't know how to do that. Instead we use full locking;
        // this is ok as this class isn't expected to be used in a high write rate scenario.
        map = new HashMap<>();
    }

    @Override
    public void initialize(Map<String, String> properties) {
    }

    @Override
    public void close() {
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            map.clear();
        } finally {
            writeLock.unlock();
        }
    }

    protected static byte[] clone(byte[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public void put(String key, byte[] value) {
        Objects.requireNonNull(key);
        value = clone(value);
        writeLock.lock();
        try {
            map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public byte[] get(String key) {
        Objects.requireNonNull(key);
        byte[] value;
        readLock.lock();
        try {
            value = map.get(key);
        } finally {
            readLock.unlock();
        }
        return clone(value);
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value) {
        Objects.requireNonNull(key);
        // clone is not needed if the comparison fails
        // but we are optimistic and prefer to do the clone outside the lock
        value = clone(value);
        writeLock.lock();
        try {
            byte[] current = map.get(key);
            boolean equal = Arrays.equals(expected, current);
            if (equal) {
                map.put(key, value);
            }
            return equal;
        } finally {
            writeLock.unlock();
        }
    }

}
