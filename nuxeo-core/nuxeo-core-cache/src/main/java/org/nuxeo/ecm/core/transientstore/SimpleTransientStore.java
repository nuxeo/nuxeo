/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.ecm.core.transientstore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheDescriptor;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.cache.CacheServiceImpl;
import org.nuxeo.ecm.core.cache.InMemoryCacheImpl;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation (i.e., not cluster aware) of the {@link TransientStore}. Uses {@link StorageEntry} as a
 * representation of an entry in the store.
 *
 * @since 7.2
 */
public class SimpleTransientStore extends AbstractTransientStore {

    protected Cache l1Cache;

    protected Cache l2Cache;

    protected CacheDescriptor l1cd;

    protected CacheDescriptor l2cd;

    protected AtomicLong storageSize = new AtomicLong(0);

    public SimpleTransientStore() {
    }

    @Override
    public void init(TransientStoreConfig config) {
        super.init(config);
        CacheService cs = Framework.getService(CacheService.class);
        if (cs == null) {
            throw new UnsupportedOperationException("Cache service is required");
        }
        // register the caches
        l1cd = getL1CacheConfig();
        l2cd = getL2CacheConfig();
        ((CacheServiceImpl) cs).registerCache(l1cd);
        ((CacheServiceImpl) cs).registerCache(l2cd);
        l1cd.start();
        l2cd.start();

        // get caches
        l1Cache = cs.getCache(l1cd.name);
        l2Cache = cs.getCache(l2cd.name);
    }

    @Override
    public void shutdown() {
        CacheService cs = Framework.getService(CacheService.class);
        if (cs != null) {
            if (l1cd != null) {
                ((CacheServiceImpl) cs).unregisterCache(l1cd);
            }
            if (l2cd != null) {
                ((CacheServiceImpl) cs).unregisterCache(l2cd);
            }
        }
    }

    @Override
    public boolean exists(String key) {
        return getL1Cache().hasEntry(key) || getL2Cache().hasEntry(key);
    }

    @Override
    public void putParameter(String key, String parameter, Serializable value) {
        synchronized (this) {
            StorageEntry entry = getStorageEntry(key);
            if (entry == null) {
                entry = new StorageEntry();
            }
            entry.putParam(parameter, value);
            putStorageEntry(key, entry);
        }
    }

    @Override
    public Serializable getParameter(String key, String parameter) {
        StorageEntry entry = getStorageEntry(key);
        if (entry == null) {
            return null;
        }
        return entry.getParam(parameter);
    }

    @Override
    public void putParameters(String key, Map<String, Serializable> parameters) {
        synchronized (this) {
            StorageEntry entry = getStorageEntry(key);
            if (entry == null) {
                entry = new StorageEntry();
            }
            entry.putParams(parameters);
            putStorageEntry(key, entry);
        }
    }

    @Override
    public Map<String, Serializable> getParameters(String key) {
        StorageEntry entry = getStorageEntry(key);
        if (entry == null) {
            return null;
        }
        return entry.getParams();
    }

    @Override
    public List<Blob> getBlobs(String key) {
        StorageEntry entry = getStorageEntry(key);
        if (entry == null) {
            return null;
        }
        // Get blob information from the store
        List<Map<String, String>> blobInfos = entry.getBlobInfos();
        if (blobInfos == null) {
            return new ArrayList<>();
        }
        // Load blobs from the file system
        return loadBlobs(blobInfos);
    }

    @Override
    public long getSize(String key) {
        StorageEntry entry = getStorageEntry(key);
        if (entry == null) {
            return -1;
        }
        return entry.getSize();
    }

    @Override
    public boolean isCompleted(String key) {
        StorageEntry entry = getStorageEntry(key);
        return entry != null && entry.isCompleted();
    }

    @Override
    public void setCompleted(String key, boolean completed) {
        synchronized (this) {
            StorageEntry entry = getStorageEntry(key);
            if (entry == null) {
                entry = new StorageEntry();
            }
            entry.setCompleted(completed);
            putStorageEntry(key, entry);
        }
    }

    @Override
    public void remove(String key) {
        synchronized (this) {
            StorageEntry entry = (StorageEntry) getL1Cache().get(key);
            if (entry == null) {
                entry = (StorageEntry) getL2Cache().get(key);
                getL2Cache().invalidate(key);
            } else {
                getL1Cache().invalidate(key);
            }
            if (entry != null) {
                decrementStorageSize(entry.getSize());
            }
        }
    }

    @Override
    public void release(String key) {
        StorageEntry entry = (StorageEntry) getL1Cache().get(key);
        if (entry != null) {
            getL1Cache().invalidate(key);
            if (getStorageSize() <= config.getTargetMaxSizeMB() * (1024 * 1024) || config.getTargetMaxSizeMB() < 0) {
                getL2Cache().put(key, entry);
            }
        }
    }

    @Override
    protected void persistBlobs(String key, long sizeOfBlobs, List<Map<String, String>> blobInfos) {
        synchronized (this) {
            StorageEntry entry = getStorageEntry(key);
            // Update storage size
            if (entry == null) {
                incrementStorageSize(sizeOfBlobs);
                entry = new StorageEntry();
            } else {
                incrementStorageSize(sizeOfBlobs - entry.getSize());
            }
            // Update entry size
            entry.setSize(sizeOfBlobs);
            // Set blob information
            entry.setBlobInfos(blobInfos);
            putStorageEntry(key, entry);
        }
    }

    @Override
    public long getStorageSize() {
        return (int) storageSize.get();
    }

    @Override
    protected void setStorageSize(long newSize) {
        storageSize.set(newSize);
    }

    @Override
    protected void incrementStorageSize(long size) {
        storageSize.addAndGet(size);
    }

    @Override
    protected void decrementStorageSize(long size) {
        storageSize.addAndGet(-size);
    }

    @Override
    protected void removeAllEntries() {
        getL1Cache().invalidateAll();
        getL2Cache().invalidateAll();
    }

    public Cache getL1Cache() {
        return l1Cache;
    }

    public Cache getL2Cache() {
        return l2Cache;
    }

    protected CacheDescriptor getL1CacheConfig() {
        return new TransientCacheConfig(config.getName() + "L1", config.getFistLevelTTL());
    }

    protected CacheDescriptor getL2CacheConfig() {
        return new TransientCacheConfig(config.getName() + "L2", config.getSecondLevelTTL());
    }

    protected class TransientCacheConfig extends CacheDescriptor {

        TransientCacheConfig(String name, int ttl) {
            super();
            super.name = name;
            super.implClass = getCacheImplClass();
            super.ttl = ttl;
        }
    }

    protected Class<? extends Cache> getCacheImplClass() {
        return InMemoryCacheImpl.class;
    }

    /**
     * Returns the {@link StorageEntry} representing the entry with the given {@code key} or {@code null} if it doesn't
     * exist.
     */
    protected StorageEntry getStorageEntry(String key) {
        StorageEntry entry = (StorageEntry) getL1Cache().get(key);
        if (entry == null) {
            entry = (StorageEntry) getL2Cache().get(key);
        }
        return entry;
    }

    /**
     * Stores the given {@code entry} with the given {@code key}.
     * <p>
     * If an entry exists with the given {@code key} it is overwritten.
     */
    protected void putStorageEntry(String key, StorageEntry entry) {
        getL1Cache().put(key, entry);
    }

}
