/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Default in memory implementation for cache management based on guava
 *
 * @since 5.9.6
 */
public class InMemoryCacheImpl extends AbstractCache {

    public InMemoryCacheImpl(CacheDescriptor desc) {
        super(desc);
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder = builder.expireAfterWrite(desc.ttl, TimeUnit.MINUTES);
        if (desc.options.containsKey("concurrencyLevel")) {
            builder = builder.concurrencyLevel(Integer.valueOf(
                    desc.options.get("concurrencyLevel")).intValue());
        }
        if (desc.options.containsKey("maxSize")) {
            builder = builder.maximumSize(Integer.valueOf(
                    desc.options.get("maxSize")).intValue());
        }
        cache = builder.build();
    }

    protected static final Log log = LogFactory.getLog(InMemoryCacheImpl.class);

    protected final Cache<String, Serializable> cache;

    /**
     * Get the instance cache
     *
     * @return the Guava instance cache used in this nuxeo cache
     * @since 5.9.6
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
    public void invalidate(String key) {
        if (key != null) {
            cache.invalidate(key);
        } else {
            log.warn(String.format(
                    "Can't invalidate a null key for the cache '%s'!", name));
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
            log.warn(String.format(
                    "Can't put a null key nor a null value in the cache '%s'!",
                    name));
        }
    }

}
