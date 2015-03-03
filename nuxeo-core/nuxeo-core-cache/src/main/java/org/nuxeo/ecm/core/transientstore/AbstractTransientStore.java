/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheDescriptor;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.cache.CacheServiceImpl;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link TransientStore} implementation.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractTransientStore implements TransientStore {

    protected TransientStoreConfig config;

    protected static final Log log = LogFactory.getLog(AbstractTransientStore.class);

    protected File cacheDir;

    protected Cache l1Cache;

    protected Cache l2Cache;

    protected CacheDescriptor l1cd;

    protected CacheDescriptor l2cd;

    public void init(TransientStoreConfig config) {
        this.config = config;
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

    public void shutdown() {

        CacheService cs = Framework.getService(CacheService.class);
        if (cs == null) {
            throw new UnsupportedOperationException("Cache service is required");
        }
        if (l1cd!=null) {
            ((CacheServiceImpl) cs).unregisterCache(l1cd);
        }
        if (l2cd!=null) {
            ((CacheServiceImpl) cs).unregisterCache(l2cd);
        }
    }

    protected abstract void incrementStorageSize(StorageEntry entry);

    protected abstract void decrementStorageSize(StorageEntry entry);

    protected abstract long getStorageSize();

    protected abstract void setStorageSize(long newSize);

    protected Cache getL1Cache() {
        return l1Cache;
    }

    protected Cache getL2Cache() {
        return l2Cache;
    }

    @Override
    public void put(StorageEntry entry) throws IOException {
        if (config.getAbsoluteMaxSizeMB() < 0 || getStorageSize() < config.getAbsoluteMaxSizeMB() * (1024 * 1024)) {
            incrementStorageSize(entry);
            entry = persistEntry(entry);
            getL1Cache().put(entry.getId(), entry);
        } else {
            throw new MaximumTransientSpaceExceeded();
        }
    }

    protected StorageEntry persistEntry(StorageEntry entry) throws IOException {
        entry.persist(getCachingDirectory(entry.getId()));
        return entry;
    }

    @Override
    public StorageEntry get(String key) throws IOException {
        StorageEntry entry = (StorageEntry) getL1Cache().get(key);
        if (entry == null) {
            entry = (StorageEntry) getL2Cache().get(key);
        }
        if (entry != null) {
            entry.load(getCachingDirectory(key));
        }
        return entry;
    }

    @Override
    public void remove(String key) throws IOException {
        StorageEntry entry = (StorageEntry) getL1Cache().get(key);
        if (entry == null) {
            entry = (StorageEntry) getL2Cache().get(key);
            getL2Cache().invalidate(key);
        } else {
            getL1Cache().invalidate(key);
        }
        if (entry != null) {
            decrementStorageSize(entry);
            entry.beforeRemove();
        }
    }

    @Override
    public void canDelete(String key) throws IOException {
        StorageEntry entry = (StorageEntry) getL1Cache().get(key);
        if (entry != null) {
            getL1Cache().invalidate(key);
            if (getStorageSize() <= config.getTargetMaxSizeMB() * (1024 * 1024) || config.getTargetMaxSizeMB() < 0) {
                getL2Cache().put(key, entry);
            }
        }
    }

    @Override
    public void removeAll() throws IOException {
        getL1Cache().invalidateAll();
        getL2Cache().invalidateAll();
        doGC();
    }

    @Override
    public TransientStoreConfig getConfig() {
        return config;
    }

    @Override
    public int getStorageSizeMB() {
        return (int) getStorageSize() / (1024 * 1024);
    }

    protected String getCachingDirName(String key) {
        String dirName = Base64.encodeBase64String(key.getBytes());
        dirName =  dirName.replaceAll("/", "_");
        return dirName;
    }

    protected String getKeyCachingDirName(String dir) {
        String key = dir.replaceAll("_", "/");
        return new String (Base64.decodeBase64(key));
    }

    public File getCachingDirectory(String key) {
        File cachingDir = new File(getCachingDirectory(), getCachingDirName(key));
        if (!cachingDir.exists()) {
            cachingDir.mkdir();
        }
        return cachingDir;
    }

    protected File getCachingDirectory() {
        if (cacheDir == null) {
            File data = new File(Environment.getDefault().getData(), config.getName());
            if (data.exists()) {
                try {
                    FileUtils.deleteDirectory(data);
                } catch (IOException cause) {
                    throw new RuntimeException("Cannot create cache dir " + data, cause);
                }
            }
            data.mkdirs();
            return cacheDir = data.getAbsoluteFile();
        }
        return cacheDir;
    }

    public void doGC() {
        File dir = getCachingDirectory();
        long newSize = 0;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir.getAbsolutePath()))) {
                for (Path entry : stream) {
                    String key = getKeyCachingDirName(entry.getFileName().toString());
                    try {
                        // XXX should not get entry since it can mess the LRU
                        if (getL1Cache().hasEntry(key)) {
                            newSize+= getSize(entry);
                            continue;
                        }
                        if (getL2Cache().hasEntry(key)) {
                            newSize+= getSize(entry);
                            continue;
                        }
                        FileUtils.deleteDirectory(entry.toFile());
                    } catch (IOException e) {
                        log.error("Error while performing GC", e);
                    }

                }
            }
        } catch (IOException e) {
            log.error("Error while performing GC", e);
        }
        setStorageSize(newSize);
    }

    protected long getSize(Path entry) {
        long size = 0;
        for (File file : entry.toFile().listFiles()) {
            size+=file.length();
        }
        return size;
    }

    public abstract Class<? extends Cache> getCacheImplClass();

    protected class TransientCacheConfig extends CacheDescriptor {

        TransientCacheConfig(String name, int ttl) {
            super();
            super.name = name;
            super.implClass = getCacheImplClass();
            super.ttl = ttl;
        }
    }

    protected CacheDescriptor getL1CacheConfig() {
        return new TransientCacheConfig(config.getName() + "L1", config.getFistLevelTTL());
    }

    protected CacheDescriptor getL2CacheConfig() {
        return new TransientCacheConfig(config.getName() + "L2", config.getSecondLevelTTL());
    }

}
