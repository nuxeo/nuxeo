/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;
import java.util.Set;

/**
 * Class to implement mandatory check attributes before calling implementation of cache This enable to have the same
 * behavior for any use of cache for all implementation of cache
 *
 * @since 6.0
 */
public class CacheAttributesChecker extends AbstractCache {

    protected Cache cache;

    protected CacheAttributesChecker(CacheDescriptor desc) {
        super(desc);
    }

    void setCache(Cache cache) {
        this.cache = cache;
    }

    public Cache getCache() {
        return cache;
    }

    @Override
    public Serializable get(String key) {
        if (key == null) {
            return null;
        }
        return cache.get(key);
    }

    @Override public Set<String> keySet() {
        return cache.keySet();
    }

    @Override
    public void invalidate(String key) {
        if (key == null) {
            throw new IllegalArgumentException(String.format("Can't invalidate a null key for the cache '%s'!", name));
        }
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void put(String key, Serializable value) {
        if (key == null) {
            throw new IllegalArgumentException(String.format("Can't put a null key for the cache '%s'!", name));
        }
        cache.put(key, value);
    }

    @Override
    public boolean hasEntry(String key) {
        if (key == null) {
            return false;
        }
        return cache.hasEntry(key);
    }

}
