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
public abstract class CacheWrapper implements Cache {

    public final Cache cache;

    protected CacheWrapper(Cache cache) {
        this.cache = cache;
    }

    public void stop() {
        if (cache instanceof CacheWrapper) {
            ((CacheWrapper) cache).stop();
        }
        onStop();
    }

    protected void onStop() {
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
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void put(String key, Serializable value) {
        cache.put(key, value);
    }

    @Override
    public boolean hasEntry(String key) {
        return cache.hasEntry(key);
    }

}
