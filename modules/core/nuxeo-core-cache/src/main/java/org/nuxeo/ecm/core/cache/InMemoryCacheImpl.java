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

import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_CONCURRENCY_LEVEL;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_MAX_SIZE;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * Default in memory implementation for cache management based on guava
 *
 * @since 6.0
 */
public class InMemoryCacheImpl extends AbstractCache {

    private static final Logger log = LogManager.getLogger(InMemoryCacheImpl.class);

    protected final Cache<String, Serializable> cache;

    public InMemoryCacheImpl(CacheDescriptor desc) {
        super(desc);
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder = builder.expireAfterWrite(desc.getTTL(), TimeUnit.MINUTES);
        Map<String, String> options = desc.options;
        if (options.containsKey(OPTION_CONCURRENCY_LEVEL)) {
            builder = builder.concurrencyLevel(Integer.parseInt(options.get(OPTION_CONCURRENCY_LEVEL)));
        }
        if (options.containsKey(OPTION_MAX_SIZE)) {
            builder = builder.maximumSize(Integer.parseInt(options.get(OPTION_MAX_SIZE)));
        }
        cache = builder.build();
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
        invalidateLocal(key);
    }

    @Override
    public void invalidateLocal(String key) {
        if (key != null) {
            cache.invalidate(key);
        } else {
            log.warn("Can't invalidate a null key for the cache: {}!", name);
        }
    }

    @Override
    public void invalidateAll() {
        invalidateLocalAll();
    }

    @Override
    public void invalidateLocalAll() {
        cache.invalidateAll();
    }

    @Override
    public void put(String key, Serializable value) {
        putLocal(key, value);
    }

    @Override
    public void putLocal(String key, Serializable value) {
        if (key != null && value != null) {
            cache.put(key, value);
        } else {
            log.warn("Can't put a null key nor a null value in the cache: {}!", name);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to {@link com.google.common.cache.Cache#get(Object, java.util.concurrent.Callable)}
     * which provides single-flight loading: if multiple threads request the same absent key concurrently, only one
     * invokes the supplier and the others wait for that result, avoiding a thundering herd.
     * </p>
     *
     * @since 2025.20
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends Serializable> V computeIfAbsent(String key, Supplier<V> supplier) {
        try {
            return (V) cache.get(key, supplier::get);
        } catch (InvalidCacheLoadException e) {
            // supplier returned null: this is expected (e.g. unknown user), not an error
            return null;
        } catch (UncheckedExecutionException e) {
            // supplier threw a RuntimeException, unwrap and rethrow as-is
            throw (RuntimeException) e.getCause();
        } catch (ExecutionError e) {
            // supplier threw an Error (e.g. OutOfMemoryError), rethrow as-is to avoid masking fatal JVM conditions
            throw (Error) e.getCause();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new NuxeoException("Error computing cache value for key: " + key, cause);
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
