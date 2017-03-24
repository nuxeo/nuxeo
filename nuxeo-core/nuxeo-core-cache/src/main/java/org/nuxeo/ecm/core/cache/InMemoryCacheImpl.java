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
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Default in memory implementation for cache management based on guava
 *
 * @since 6.0
 */
public class InMemoryCacheImpl extends AbstractCache {

    public InMemoryCacheImpl(CacheDescriptor desc) {
        super(desc);
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder = builder.expireAfterWrite(desc.ttl, TimeUnit.MINUTES);
        if (desc.options.containsKey("concurrencyLevel")) {
            builder = builder.concurrencyLevel(Integer.valueOf(desc.options.get("concurrencyLevel")).intValue());
        }
        if (desc.options.containsKey("maxSize")) {
            builder = builder.maximumSize(Integer.valueOf(desc.options.get("maxSize")).intValue());
        }
        cache = builder.build();
    }

    protected static final Log log = LogFactory.getLog(InMemoryCacheImpl.class);

    protected final Cache<String, Serializable> cache;

    /**
     * Get the instance cache
     *
     * @return the Guava instance cache used in this nuxeo cache
     * @since 6.0
     */
    public Cache<String, Serializable> getGuavaCache() {
        return cache;
    }

    @Override
    public Serializable get(String key) {
        if (key == null) {
            return null;
        } else {
            return cache.getIfPresent(key);
        }
    }

    @Override
    public Set<String> keySet() {
        return cache.asMap().keySet();
    }

    @Override
    public void invalidate(String key) {
        if (key != null) {
            cache.invalidate(key);
        } else {
            log.warn(String.format("Can't invalidate a null key for the cache '%s'!", name));
        }
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void put(String key, Serializable value) {
        if (key != null && value != null) {
            cache.put(key, value);
        } else {
            log.warn(String.format("Can't put a null key nor a null value in the cache '%s'!", name));
        }
    }

    @Override
    public boolean hasEntry(String key) {
        return cache.asMap().containsKey(key);
    }

    @Override
    public long getSize() {
        return cache.size();
    }

}
