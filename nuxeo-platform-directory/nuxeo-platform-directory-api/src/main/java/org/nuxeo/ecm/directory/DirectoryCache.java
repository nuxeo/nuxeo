/*
 * (C) Copyright 2007-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.directory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.mortbay.log.Log;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Very simple cache system to cache directory entry lookups (not search
 * queries) on top of nuxeo cache
 * <p>
 * Beware that this cache is not transaction aware (which is not a problem for
 * LDAP directories anyway).
 *
 */
public class DirectoryCache {

    protected final String name;

    protected Cache entryCache;

    protected String entryCacheName = null;

    protected Cache entryCacheWithoutReferences;

    protected String entryCacheWithoutReferencesName = null;

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter hitsCounter;

    protected final Counter invalidationsCounter;

    protected final Counter maxCounter;

    protected final Counter sizeCounter;

    protected DirectoryCache(String name) {
        this.name = name;
        hitsCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", name, "cache", "hits"));
        invalidationsCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", name, "cache", "invalidations"));
        sizeCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", name, "cache", "size"));
        maxCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", name, "cache", "max"));
    }

    protected boolean isCacheEnabled() {
        return (entryCacheName != null && entryCacheWithoutReferencesName != null);
    }

    public DocumentModel getEntry(String entryId, EntrySource source)
            throws DirectoryException {
        return getEntry(entryId, source, true);
    }

    public DocumentModel getEntry(String entryId, EntrySource source,
            boolean fetchReferences) throws DirectoryException {
        if (!isCacheEnabled()) {
            return source.getEntryFromSource(entryId, fetchReferences);
        } else if (isCacheEnabled()
                && (getEntryCache() == null || getEntryCacheWithoutReferences() == null)) {

            Log.warn("Your directory configuration for cache is wrong, directory cache will not be used.");
            if (getEntryCache() == null) {
                Log.warn(String.format(
                        "The cache for entry '%s' has not been found, please check the cache name or make sure you have deployed it",
                        entryCacheName));
            }
            if (getEntryCacheWithoutReferences() == null) {
                Log.warn(String.format(
                        "The cache for entry without references '%s' has not been found, please check the cache name or make sure you have deployed it",
                        entryCacheWithoutReferencesName));
            }

            return source.getEntryFromSource(entryId, fetchReferences);
        }
        try {
            DocumentModel dm = null;
            if (fetchReferences) {
                dm = (DocumentModel) getEntryCache().get(entryId);
                if (dm == null) {
                    // fetch the entry from the backend and cache it for later
                    // reuse
                    dm = source.getEntryFromSource(entryId, fetchReferences);
                    if (dm != null) {
                        getEntryCache().put(entryId, dm);
                    }
                } else {
                    hitsCounter.inc();
                }
            } else {
                dm = (DocumentModel) getEntryCacheWithoutReferences().get(
                        entryId);
                if (dm == null) {
                    // fetch the entry from the backend and cache it for later
                    // reuse
                    dm = source.getEntryFromSource(entryId, fetchReferences);
                    if (dm != null) {
                        getEntryCacheWithoutReferences().put(entryId, dm);
                    }
                } else {
                    hitsCounter.inc();
                }
            }
            try {
                if (dm == null) {
                    return null;
                }
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
        } catch (IOException e) {
            throw new DirectoryException(e);
        }
    }

    public void invalidate(List<String> entryIds) {
        if (isCacheEnabled()) {
            synchronized (this) {
                try {
                    for (String entryId : entryIds) {
                        getEntryCache().invalidate(entryId);
                        getEntryCacheWithoutReferences().invalidate(entryId);
                        sizeCounter.dec();
                        invalidationsCounter.inc();
                    }
                } catch (IOException e) {
                    throw new DirectoryException(e);
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
                try {
                    long count = sizeCounter.getCount();
                    sizeCounter.dec(count);
                    invalidationsCounter.inc(count);
                    getEntryCache().invalidateAll();
                    getEntryCacheWithoutReferences().invalidateAll();
                } catch (IOException e) {
                    throw new DirectoryException(e);
                }
            }
        }
    }

    public void setEntryCacheName(String entryCacheName) {
        this.entryCacheName = entryCacheName;
    }

    public void setEntryCacheWithoutReferencesName(
            String entryCacheWithoutReferencesName) {
        this.entryCacheWithoutReferencesName = entryCacheWithoutReferencesName;
    }

    public Cache getEntryCache() {
        if (entryCache == null) {
            entryCache = Framework.getService(CacheService.class).getCache(
                    entryCacheName);
        }
        return entryCache;
    }

    public Cache getEntryCacheWithoutReferences() {

        if (entryCacheWithoutReferences == null) {
            entryCacheWithoutReferences = Framework.getService(
                    CacheService.class).getCache(
                    entryCacheWithoutReferencesName);
        }
        return entryCacheWithoutReferences;
    }

}
