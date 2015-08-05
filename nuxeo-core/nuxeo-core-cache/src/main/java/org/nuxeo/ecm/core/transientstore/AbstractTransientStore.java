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
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link TransientStore} implementation.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractTransientStore implements TransientStore {

    protected TransientStoreConfig config;

    protected final Log log = LogFactory.getLog(AbstractTransientStore.class);

    protected final File cachingDir;

    protected AbstractTransientStore(TransientStoreConfig config) {
        this.config = config;
        cachingDir = createCachingDirectory();
    }

    @Override
    public void init() {
        CacheService caches = Framework.getService(CacheService.class);
        caches.createCacheIfNotExist(config.l1name);
        caches.createCacheIfNotExist(config.l2name);
    }

    @Override
    public void shutdown() {

    }


    protected abstract void incrementStorageSize(long size);

    protected abstract void decrementStorageSize(long size);

    protected void incrementStorageSize(StorageEntry entry) {
        incrementStorageSize(entry.getSize());
    }

    protected void decrementStorageSize(StorageEntry entry) {
        decrementStorageSize(entry.getSize());
    }

    public abstract long getStorageSize();

    protected abstract void setStorageSize(long newSize);

    protected Cache getL1Cache() {
        return Framework.getService(CacheService.class).getCache(config.l1name);
    }

    protected Cache getL2Cache() {
        return Framework.getService(CacheService.class).getCache(config.l2name);
    }

    protected boolean isMaximumSizeReached() {
        return config.absoluteMaxSize >= 0 && getStorageSize() >= config.absoluteMaxSize;
    }

    protected boolean isTargetSizeReached() {
        return config.absoluteMaxSize >= 0 && getStorageSize() >= config.absoluteMaxSize;
    }

    @Override
    public void put(StorageEntry entry) throws IOException {
        if (isMaximumSizeReached()) {
            throw new MaximumTransientSpaceExceeded();
        }
        StorageEntry old = get(entry.getId());
        long size = old != null ? -old.getLastStorageSize() : 0L;
        try {
            entry = persistEntry(entry);
        } finally {
            incrementStorageSize(size+entry.getLastStorageSize());
        }
        getL1Cache().put(entry.getId(), entry);

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
        if (entry == null) {
            return;
        }
        getL1Cache().invalidate(key);
        if (!isTargetSizeReached()) {
            getL2Cache().put(key, entry);
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
        dirName = dirName.replaceAll("/", "_");
        return dirName;
    }

    protected String getKeyCachingDirName(String dir) {
        String key = dir.replaceAll("_", "/");
        return new String(Base64.decodeBase64(key));
    }

    public File getCachingDirectory(String key) {
        File dir = new File(cachingDir, getCachingDirName(key));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    protected File createCachingDirectory() {
        File data = new File(Environment.getDefault().getData(), config.name);
        if (data.exists()) {
            try {
                FileUtils.deleteDirectory(data);
            } catch (IOException cause) {
                throw new RuntimeException("Cannot create cache dir " + data, cause);
            }
        }
        data.mkdirs();
        return data.getAbsoluteFile();
    }

    @Override
    public void doGC() {
        long newSize = 0;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(cachingDir.getAbsolutePath()))) {
                for (Path entry : stream) {
                    String key = getKeyCachingDirName(entry.getFileName().toString());
                    try {
                        if (getL1Cache().hasEntry(key)) {
                            newSize += getSize(entry);
                            continue;
                        }
                        if (getL2Cache().hasEntry(key)) {
                            newSize += getSize(entry);
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
            size += file.length();
        }
        return size;
    }

    protected abstract String getCacheType();

}
