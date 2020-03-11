/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;
import java.util.Set;

/**
 * @since 9.1
 */
public class CacheWrapper implements CacheManagement {

    public final CacheManagement cache;

    protected CacheWrapper(CacheManagement cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public Serializable get(String key) {
        return cache.get(key);
    }

    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }

    @Override
    public void invalidateLocal(String key) {
        cache.invalidateLocal(key);
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateLocalAll() {
        cache.invalidateLocalAll();
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void putLocal(String key, Serializable value) {
        cache.putLocal(key, value);
    }

    @Override
    public void put(String key, Serializable value) {
        cache.put(key, value);
    }

    @Override
    public boolean hasEntry(String key) {
        return cache.hasEntry(key);
    }

    @Override
    public void start() {
        cache.start();
    }

    @Override
    public void stop() {
        cache.stop();
    }

    @Override
    public long getSize() {
        return cache.getSize();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + cache + ")";
    }

}
