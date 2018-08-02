/*
 * (C) Copyright 2007-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheManagement;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Very simple cache system to cache directory entry lookups (not search queries) on top of nuxeo cache
 * <p>
 * Beware that this cache is not transaction aware (which is not a problem for LDAP directories anyway).
 */
public class DirectoryCache {

    private static final Serializable CACHE_MISS = Boolean.FALSE;

    protected final String name;

    protected Cache entryCache;

    protected String entryCacheName = null;

    protected Cache entryCacheWithoutReferences;

    protected String entryCacheWithoutReferencesName = null;

    protected boolean negativeCaching;

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter hitsCounter;

    protected final Counter negativeHitsCounter;

    protected final Counter missesCounter;

    protected final Counter invalidationsCounter;

    protected final Counter sizeCounter;

    private final static Log log = LogFactory.getLog(DirectoryCache.class);

    protected DirectoryCache(String name) {
        this.name = name;
        hitsCounter = metrics.counter(MetricRegistry.name("nuxeo", "directories", name, "cache", "hits"));
        negativeHitsCounter = metrics.counter(MetricRegistry.name("nuxeo", "directories", name, "cache", "neghits"));
        missesCounter = metrics.counter(MetricRegistry.name("nuxeo", "directories", name, "cache", "misses"));
        invalidationsCounter = metrics.counter(MetricRegistry.name("nuxeo", "directories", name, "cache",
                "invalidations"));
        sizeCounter = metrics.counter(MetricRegistry.name("nuxeo", "directories", name, "cache", "size"));
    }

    protected boolean isCacheEnabled() {
        return (entryCacheName != null && entryCacheWithoutReferencesName != null);
    }

    public DocumentModel getEntry(String entryId, EntrySource source) {
        return getEntry(entryId, source, true);
    }

    public DocumentModel getEntry(String entryId, EntrySource source, boolean fetchReferences) {
        if (!isCacheEnabled()) {
            return source.getEntryFromSource(entryId, fetchReferences);
        } else if (isCacheEnabled() && (getEntryCache() == null || getEntryCacheWithoutReferences() == null)) {
            if (log.isDebugEnabled()) {
                if (getEntryCache() == null) {
                    log.debug(String.format(
                            "The cache '%s' is undefined for directory '%s', it will be created with the default cache configuration",
                            entryCacheName, name));
                }
                if (getEntryCacheWithoutReferences() == null) {
                    log.debug(String.format(
                            "The cache '%s' is undefined for directory '%s', it will be created with the default cache configuration",
                            entryCacheWithoutReferencesName, name));
                }
            }
            return source.getEntryFromSource(entryId, fetchReferences);
        }

        Cache cache = fetchReferences ? getEntryCache() : getEntryCacheWithoutReferences();
        Serializable entry = cache.get(entryId);
        if (CACHE_MISS.equals(entry)) {
            negativeHitsCounter.inc();
            return null;
        }
        DocumentModel dm = (DocumentModel) entry;
        if (dm == null) {
            // fetch the entry from the backend and cache it for later reuse
            dm = source.getEntryFromSource(entryId, fetchReferences);
            if (dm != null) {
                // DocumentModelImpl is not thread-safe and when we fetch and clone it when returning
                // a value from the cache there may be concurrency.
                // So we avoid thread-safety issues by exercising once the code paths that may do
                // concurrent accesses to ComplexProperty (NXP-23458).
                try {
                    dm.clone();
                } catch (CloneNotSupportedException e) {
                    // ignore, no concurrency issues if not a DocumentModelImpl
                }
                ((CacheManagement) cache).putLocal(entryId, dm);
                if (fetchReferences) {
                    sizeCounter.inc();
                }
            } else if (negativeCaching) {
                ((CacheManagement) cache).putLocal(entryId, CACHE_MISS);
            }
            missesCounter.inc();
        } else {
            hitsCounter.inc();
        }
        try {
            if (dm == null) {
                return null;
            }
            // this is the clone() that needs to be careful (see above) when there's concurrency
            DocumentModel clone = dm.clone();
            // DocumentModelImpl#clone does not copy context data, hence
            // propagate the read-only flag manually
            if (BaseSession.isReadOnlyEntry(dm)) {
                BaseSession.setReadOnlyEntry(clone);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            // will never happen as long a DocumentModelImpl is used
            return dm;
        }
    }

    public void invalidate(List<String> entryIds) {
        if (isCacheEnabled()) {
            synchronized (this) {
                for (String entryId : entryIds) {
                    sizeCounter.dec();
                    invalidationsCounter.inc();
                    // caches may be null if we're called for invalidation during a hot-reload
                    Cache cache = getEntryCache();
                    if (cache != null) {
                        cache.invalidate(entryId);
                    }
                    cache = getEntryCacheWithoutReferences();
                    if (cache != null) {
                        cache.invalidate(entryId);
                    }
                }
            }
        }
    }

    public void invalidate(String... entryIds) {
        invalidate(Arrays.asList(entryIds));
    }

    public void invalidateAll() {
        if (isCacheEnabled()) {
            synchronized (this) {
                long count = sizeCounter.getCount();
                sizeCounter.dec(count);
                invalidationsCounter.inc(count);
                // caches may be null if we're called for invalidation during a hot-reload
                Cache cache = getEntryCache();
                if (cache != null) {
                    cache.invalidateAll();
                }
                cache = getEntryCacheWithoutReferences();
                if (cache != null) {
                    cache.invalidateAll();
                }
            }
        }
    }

    public void setEntryCacheName(String entryCacheName) {
        this.entryCacheName = entryCacheName;
    }

    public void setEntryCacheWithoutReferencesName(String entryCacheWithoutReferencesName) {
        this.entryCacheWithoutReferencesName = entryCacheWithoutReferencesName;
    }

    public void setNegativeCaching(Boolean negativeCaching) {
        this.negativeCaching = Boolean.TRUE.equals(negativeCaching);
    }

    public Cache getEntryCache() {
        if (entryCache == null) {
            entryCache = getCacheService().getCache(entryCacheName);
        }
        return entryCache;
    }

    public Cache getEntryCacheWithoutReferences() {

        if (entryCacheWithoutReferences == null) {
            entryCacheWithoutReferences = getCacheService().getCache(
                    entryCacheWithoutReferencesName);
        }
        return entryCacheWithoutReferences;
    }

    protected CacheService getCacheService() {
        CacheService cacheService = Framework.getService(CacheService.class);
        if (cacheService == null) {
            throw new NuxeoException("Missing CacheService");
        }
        return cacheService;
    }

}
