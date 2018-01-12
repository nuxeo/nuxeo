/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;

/**
 * Manager for the cache system of the {@link ConversionService}.
 *
 * @author tiry
 */
public class ConversionCacheHolder {

    protected static final Map<String, ConversionCacheEntry> cache = new HashMap<>();

    protected static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private static final Log log = LogFactory.getLog(ConversionCacheHolder.class);

    public static final int NB_SUB_PATH_PART = 5;

    public static final int SUB_PATH_PART_SIZE = 2;

    public static final AtomicLong CACHE_HITS = new AtomicLong();

    // Utility class.
    private ConversionCacheHolder() {
    }

    public static long getCacheHits() {
        return CACHE_HITS.get();
    }

    public static int getNbCacheEntries() {
        return cache.keySet().size();
    }

    protected static List<String> getSubPathFromKey(String key) {
        List<String> subPath = new ArrayList<>();

        String path = Base64.encodeBase64String(key.getBytes());

        path = path.replace("+", "X");
        path = path.replace("/", "Y");

        int idx = 0;

        for (int i = 0; i < NB_SUB_PATH_PART; i++) {
            String subPart = path.substring(idx, idx + SUB_PATH_PART_SIZE);
            subPath.add(subPart);
            idx += SUB_PATH_PART_SIZE;
            if (idx >= path.length()) {
                break;
            }
        }
        return subPath;
    }

    protected static String getCacheEntryPath(String key) {
        Path path = new Path(ConversionServiceImpl.getCacheBasePath());

        List<String> subPath = getSubPathFromKey(key);

        for (String subPart : subPath) {
            path = path.append(subPart);
            new File(path.toString()).mkdir();
        }

        // path = path.append(key);

        return path.toString();
    }

    public static void addToCache(String key, BlobHolder result) {
        Objects.requireNonNull(key);
        cacheLock.writeLock().lock();
        try {
            doAddToCache(key, result);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    protected static void doAddToCache(String key, BlobHolder result) {
        ConversionCacheEntry cce = new ConversionCacheEntry(result);
        boolean persisted = false;

        try {
            persisted = cce.persist(getCacheEntryPath(key));
        } catch (IOException e) {
            log.error("Error while trying to persist cache entry", e);
        }

        if (persisted) {
            cache.put(key, cce);
        }
    }

    public static void removeFromCache(String key) {
        cacheLock.writeLock().lock();
        try {
            doRemoveFromCache(key);
        } finally {
            cacheLock.writeLock().unlock();
        }

    }

    protected static void doRemoveFromCache(String key) {
        if (cache.containsKey(key)) {
            ConversionCacheEntry cce = cache.get(key);
            cce.remove();
            cache.remove(key);
        }
    }

    public static ConversionCacheEntry getCacheEntry(String key) {
        cacheLock.readLock().lock();
        try {
            return doGetCacheEntry(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    protected static ConversionCacheEntry doGetCacheEntry(String key) {
        return cache.get(key);
    }

    public static BlobHolder getFromCache(String key) {
        cacheLock.readLock().lock();
        try {
            return doGetFromCache(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    protected static BlobHolder doGetFromCache(String key) {
        ConversionCacheEntry cacheEntry = cache.get(key);
        if (cacheEntry != null) {
            if (CACHE_HITS.incrementAndGet() < 0) {
                // skip all negative values
                CACHE_HITS.addAndGet(Long.MIN_VALUE); // back to 0
            }
            return cacheEntry.restore();
        }
        return null;
    }

    public static Set<String> getCacheKeys() {
        cacheLock.readLock().lock();
        try {
            return new HashSet<>(cache.keySet());
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * @since 6.0
     */
    public static void deleteCache() {
        cacheLock.writeLock().lock();
        try {
            cache.clear();
            new File(ConversionServiceImpl.getCacheBasePath()).delete();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
