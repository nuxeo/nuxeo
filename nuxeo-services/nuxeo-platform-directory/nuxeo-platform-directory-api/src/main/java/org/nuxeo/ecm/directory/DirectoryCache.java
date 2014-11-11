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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

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

    protected class NullCache implements Cache {
        @Override
        public String getName() {
            return "none";
        }

        @Override
        public Serializable get(String key) throws IOException {
            return null;
        }

        @Override
        public void invalidate(String key) throws IOException {

        }

        @Override
        public void invalidateAll() throws IOException {

        }

        @Override
        public void put(String key, Serializable value) throws IOException {

        }
    }

    protected final String name;

    protected Cache entryCache;

    protected Cache entryCacheWithoutReferences;

    protected final MetricRegistry metrics = SharedMetricRegistries
        .getOrCreate(MetricsService.class.getName());

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
        entryCache = new NullCache();
        entryCacheWithoutReferences = new NullCache();
    }

    public DocumentModel getEntry(String entryId, EntrySource source)
            throws DirectoryException {
        return getEntry(entryId, source, true);
    }

    public DocumentModel getEntry(String entryId, EntrySource source,
            boolean fetchReferences) throws DirectoryException {
        try {
            DocumentModel dm = null;
            if (fetchReferences) {
                dm = (DocumentModel) entryCache.get(entryId);
                if (dm != null) {
                    hitsCounter.inc();
                    return dm;
                }
                // fetch the entry from the backend and cache it for later
                // reuse
                dm = source.getEntryFromSource(entryId, fetchReferences);
                dm = readonly(dm);
                if (dm != null) {
                    entryCache.put(entryId, dm);
                } else {
                    entryCache.invalidate(entryId);
                }
                return dm;

            }

            dm = (DocumentModel) entryCacheWithoutReferences.get(entryId);
            if (dm != null) {
                hitsCounter.inc();
                return dm;
            }

            dm = source.getEntryFromSource(entryId, fetchReferences);
            dm = readonly(dm);
            entryCacheWithoutReferences.put(entryId, dm);
            if (dm != null) {
                entryCacheWithoutReferences.put(entryId, dm);
            } else {
                entryCache.invalidate(entryId);
            }
            return dm;

        } catch (IOException e) {
            throw new DirectoryException(e);
        }
    }

    protected DocumentModel readonly(DocumentModel dm) {
        if (dm == null) {
            return null;
        }
        try {
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
        synchronized (this) {
            try {
                for (String entryId : entryIds) {
                    entryCache.invalidate(entryId);
                    entryCacheWithoutReferences.invalidate(entryId);
                    sizeCounter.dec();
                    invalidationsCounter.inc();
                }
            } catch (IOException e) {
                throw new DirectoryException(e);
            }
        }
    }

    public void invalidate(String... entryIds) {
        invalidate(Arrays.asList(entryIds));
    }

    public void invalidateAll() {
        synchronized (this) {
            try {
                long count = sizeCounter.getCount();
                sizeCounter.dec(count);
                invalidationsCounter.inc(count);
                entryCache.invalidateAll();
                entryCacheWithoutReferences.invalidateAll();
            } catch (IOException e) {
                throw new DirectoryException(e);
            }
        }
    }

    public void initialize(String name, String refname) {
        entryCache = Framework.getLocalService(CacheService.class).getCache(
                name);
        entryCacheWithoutReferences = Framework.getLocalService(
                CacheService.class).getCache(refname);
    }

    public Cache getEntryCache() {
        return entryCache;
    }

    public Cache getEntryCacheWithoutReferences() {
        return entryCacheWithoutReferences;
    }

}
