/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;

/**
 * Blob store wrapper that caches blobs locally because fetching them may be expensive.
 *
 * @since 11.1
 */
public class CachingBlobStore extends AbstractBlobStore {

    private static final Logger log = LogManager.getLogger(CachingBlobStore.class);

    // static because we want all caches to share the same locks
    protected static final Set<Path> LOCKED_FILES = ConcurrentHashMap.newKeySet();

    protected final BlobStore store;

    protected final CachingConfiguration cacheConfig;

    // public for tests
    public final LocalBlobStore cacheStore;

    protected final BinaryGarbageCollector gc;

    // lock to avoid doing redundant work in parallel, and protect access to clearOldBlobsLastTime
    protected final Lock clearOldBlobsLock = new ReentrantLock();

    protected long clearOldBlobsLastTime;

    // not a constant for tests
    protected long clearOldBlobsInterval = Duration.ofMinutes(1).toMillis();

    // not a constant for tests
    protected Clock clock = Clock.systemUTC();

    /** @deprecated since 11.5 */
    @Deprecated
    public CachingBlobStore(String name, BlobStore store, CachingConfiguration config) {
        this(null, name, store, config);
    }

    /** @since 11.5 */
    public CachingBlobStore(String blobProviderId, String name, BlobStore store, CachingConfiguration config) {
        super(blobProviderId, name, store.getKeyStrategy());
        this.store = store;
        this.cacheConfig = config;
        cacheStore = new LocalBlobStore(name, store.getKeyStrategy(), new PathStrategyShortened(config.dir));
        gc = new CachingBinaryGarbageCollector();
    }

    @Override
    public boolean hasVersioning() {
        return store.hasVersioning();
    }

    @Override
    public BlobStore unwrap() {
        return store.unwrap();
    }

    protected Path renameCachedBlob(String tmpKey, String key) throws IOException {
        return copyOrMoveCachedBlob(key, cacheStore, tmpKey, true);
    }

    protected Path copyOrMoveCachedBlob(String destKey, LocalBlobStore sourceStore, String sourceKey,
            boolean atomicMove) throws IOException {
        recordBlobAccess(sourceStore, sourceKey);
        String returnedKey = cacheStore.copyBlob(destKey, sourceStore, sourceKey, atomicMove);
        if (returnedKey == null) {
            // source key didn't exist
            return null;
        }
        if (!returnedKey.equals(destKey)) {
            throw new IllegalStateException("Expected " + destKey + " but was " + returnedKey);
        }
        OptionalOrUnknown<Path> fileOpt = cacheStore.getFile(destKey);
        if (!fileOpt.isPresent()) {
            throw new IllegalStateException("File disappeared after copy/move: " + destKey);
        }
        Path path = fileOpt.get();
        recordBlobAccess(path);
        clearOldBlobs();
        return path;
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        // write the blob to a temporary file
        String tmpKey = cacheStore.writeBlob(blobWriteContext.copyWithKey(randomString()));
        // get the final key
        String key = blobWriteContext.getKey(); // may depend on write observer, for example for digests

        // when using deduplication, check if it's in the cache already
        if (blobWriteContext.useDeDuplication() && getFileFromCache(key, true).isPresent()) {
            // delete tmp file, not needed anymore
            cacheStore.deleteBlob(tmpKey);
            return key;
        }

        // we now have a file for this blob
        OptionalOrUnknown<Path> fileOpt = cacheStore.getFile(tmpKey);
        if (!fileOpt.isPresent()) {
            throw new IllegalStateException("File disappeared after write: " + tmpKey);
        }
        blobWriteContext.setFile(fileOpt.get());
        // send the file to storage
        String returnedKey = store.writeBlob(blobWriteContext.copyWithNoWriteObserverAndKey(key));
        // renamed the cached file to the actual key
        renameCachedBlob(tmpKey, returnedKey);
        return returnedKey;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        LocalBlobStore sourceCacheStore = sourceStore instanceof CachingBlobStore
                ? ((CachingBlobStore) sourceStore).cacheStore
                : null;
        if ((!atomicMove || copyBlobIsOptimized(sourceStore)) && sourceCacheStore != null && key != null) {
            // if it's a copy and the original cached file won't be touched
            // else optimized move won't need the cache, so we can move the cache ahead of time
            copyOrMoveCachedBlob(key, sourceCacheStore, sourceKey, atomicMove);
        }
        String returnedKey = store.copyOrMoveBlob(key, sourceStore, sourceKey, atomicMove);
        if (returnedKey != null && atomicMove && sourceCacheStore != null) {
            // clear source cache
            sourceCacheStore.deleteBlob(sourceKey);
        }
        return returnedKey;
    }

    @Override
    public boolean useAsyncDigest() {
        return ((AbstractBlobStore) store).useAsyncDigest();
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        // best effort: if it's not in the cache then return unknown
        // because if the underlying store was able to return files efficiently
        // it wouldn't need a caching layer
        OptionalOrUnknown<Path> fileOpt = getFileFromCache(key, false);
        return fileOpt.isPresent() ? fileOpt : OptionalOrUnknown.unknown();
    }

    protected OptionalOrUnknown<Path> getFileFromCache(String key, boolean exists) {
        recordBlobAccess(cacheStore, key);
        OptionalOrUnknown<Path> fileOpt = cacheStore.getFile(key);
        if (fileOpt.isPresent()) {
            Path path = fileOpt.get();
            long len = path.toFile().length();
            if (exists) {
                logTrace("<--", "exists (" + len + " bytes)");
            } else { // read
                logTrace("<-", "read " + len + " bytes");
            }
            logTrace("hnote right: " + key);
        } else {
            logTrace("<--", "missing");
            logTrace("hnote right: " + key);
        }
        return fileOpt;
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        Path path;
        OptionalOrUnknown<Path> fileOpt = getFile(key);
        if (fileOpt.isPresent()) {
            path = fileOpt.get();
        } else {
            // fetch file from storage into the cache
            // go through a tmp file for atomicity
            String tmpKey = cacheStore.copyOrMoveBlob(randomString(), store, key, false);
            if (tmpKey == null) {
                return OptionalOrUnknown.missing();
            }
            path = renameCachedBlob(tmpKey, key);
        }
        return OptionalOrUnknown.of(Files.newInputStream(path));
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        OptionalOrUnknown<InputStream> streamOpt = getStream(key);
        if (!streamOpt.isPresent()) {
            return false;
        }
        try (InputStream stream = streamOpt.get()) {
            Files.copy(stream, dest, REPLACE_EXISTING);
            return true;
        }
    }

    @Override
    public void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException {
        store.writeBlobProperties(blobUpdateContext);
    }

    @Override
    public void deleteBlob(String key) {
        cacheStore.deleteBlob(key);
        store.deleteBlob(key);
    }

    @Override
    public void clear() {
        cacheStore.clear();
        store.clear();
    }

    /**
     * Clear old blobs from the cache, but not too often (as doing directory listings has a cost).
     *
     * @since 11.5
     */
    protected void clearOldBlobs() {
        if (clearOldBlobsLock.tryLock()) {
            try {
                if (clock.millis() > clearOldBlobsLastTime + clearOldBlobsInterval) {
                    clearOldBlobsNow();
                    clearOldBlobsLastTime = clock.millis();
                }
            } finally {
                clearOldBlobsLock.unlock();
            }
        }
        // else don't do anything, another thread is already doing it
    }

    /**
     * Clear old blobs from the cache.
     * <p>
     * A blob is deleted if it has not been recently created or accessed (minimum age), and if in addition it would be
     * too big for the maximum cache size in bytes, or if the cache would contain too many blobs.
     *
     * @since 11.5
     */
    protected void clearOldBlobsNow() {
        long maxSize = cacheConfig.maxSize;
        long maxCount = cacheConfig.maxCount;
        long minAgeMillis = cacheConfig.minAge * 1000;
        long threshold = clock.millis() - minAgeMillis;
        log.debug("clearOldBlobs starting, dir={} maxSize={}, maxCount={}, minAge={}s, threshold={}", cacheConfig.dir,
                maxSize, maxCount, cacheConfig.minAge, threshold);

        List<PathInfo> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheConfig.dir)) {
            for (Path path : ds) {
                PathInfo pi;
                try {
                    pi = new PathInfo(path);
                } catch (NoSuchFileException e) {
                    log.trace("clearOldBlobs ignoring missing file: {}", path);
                    continue;
                } catch (IOException e) {
                    log.warn(e.getMessage());
                    continue;
                }
                if (cacheStore.pathStrategy.isTempFile(path)) {
                    log.trace("clearOldBlobs ignoring temporary file: {} (timestamp {})", path, pi.time);
                    continue;
                }
                files.add(pi);
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
        Collections.sort(files); // sort by most recent first

        log.debug("clearOldBlobs {} files to check", files.size());
        long size = 0;
        long count = 0;
        long recentCount = 0;
        for (PathInfo pi : files) {
            size += pi.size;
            count++;
            // are there too many files, or do they occupy too much space?
            if (count > maxCount || size > maxSize) {
                // is the file old enough to be a candidate for deletion?
                if (pi.time < threshold) {
                    if (tryLock(pi.path)) {
                        try {
                            // re-check file age under lock
                            long time = Files.getLastModifiedTime(pi.path).toMillis();
                            if (time < threshold) {
                                // delete the file
                                log.trace(
                                        "clearOldBlobs DELETING file: {} (timestamp {}, cumulative count {}, cumulative size {})",
                                        pi.path, time, count, size);
                                Files.delete(pi.path);
                                size -= pi.size;
                                count--;
                            } else {
                                recentCount++;
                                log.trace("clearOldBlobs keeping file: {} because it's recent (timestamp {})", pi.path,
                                        time);
                            }
                        } catch (IOException e) {
                            log.warn(e.getMessage());
                        } finally {
                            unlock(pi.path);
                        }
                    } else {
                        log.trace("clearOldBlobs skipping file: {} because it's already locked", pi.path);
                    }
                } else {
                    recentCount++;
                    log.trace("clearOldBlobs keeping file: {} because it's recent (timestamp {})", pi.path, pi.time);
                }
            } else {
                log.trace("clearOldBlobs keeping file: {}", pi.path);
            }
        }
        if (log.isDebugEnabled()) {
            if (maxSize == 0) {
                maxSize = 1; // shouldn't happen, but don't divide by zero
            }
            log.debug(String.format(
                    "clearOldBlobs done (keeping %d files out of %d (including %d recent), cache fill ratio now %.1f%%)",
                    count, files.size(), recentCount, 100d * size / maxSize));
        }
    }

    protected static class PathInfo implements Comparable<PathInfo> {

        protected final Path path;

        protected final long time;

        protected final long size;

        public PathInfo(Path path) throws IOException {
            this.path = path;
            this.time = Files.getLastModifiedTime(path).toMillis();
            this.size = Files.size(path);
        }

        @Override
        public int compareTo(PathInfo other) { // NOSONAR no need for equals
            // compare in reverse order (most recent first)
            return Long.compare(other.time, time);
        }
    }

    protected void recordBlobAccess(LocalBlobStore localBlobStore, String key) {
        recordBlobAccess(localBlobStore.pathStrategy.getPathForKey(key));
    }

    /**
     * Records access to a file by changing its modification time.
     * <p>
     * Recording access is also a form of locking against concurrent deletion by the clearing mechanism.
     *
     * @since 11.5
     */
    protected void recordBlobAccess(Path path) {
        if (tryLock(path)) {
            try {
                // note that the filesystem may round the time
                Files.setLastModifiedTime(path, FileTime.fromMillis(clock.millis()));
            } catch (NoSuchFileException e) {
                // ignore
            } catch (IOException e) {
                log.error(e, e);
            } finally {
                unlock(path);
            }
        }
    }

    // try to lock with exponential backoff
    protected static boolean tryLock(Path path) {
        long millis = 1;
        for (int i = 0; i < 20; i++) {
            if (LOCKED_FILES.add(path)) {
                return true;
            }
            if (i >= 10) {
                millis *= 2; // exponential backoff after a 10ms
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e); // NOSONAR
            }
        }
        return false;
    }

    protected static void unlock(Path path) {
        LOCKED_FILES.remove(path);
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    /**
     * Garbage collector that delegates to the underlying one, but purges the cache after an actual GC is done.
     */
    public class CachingBinaryGarbageCollector implements BinaryGarbageCollector {

        protected final BinaryGarbageCollector delegate;

        protected final BinaryGarbageCollector cacheDelegate;

        public CachingBinaryGarbageCollector() {
            delegate = store.getBinaryGarbageCollector();
            cacheDelegate = cacheStore.getBinaryGarbageCollector();
        }

        /** @deprecated since 11.5 */
        @Deprecated
        public CachingBinaryGarbageCollector(BinaryGarbageCollector delegate) {
            this();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public void start() {
            delegate.start();
            cacheDelegate.start();
        }

        @Override
        public void mark(String key) {
            delegate.mark(key);
            cacheDelegate.mark(key);
        }

        @Override
        public void stop(boolean delete) {
            delegate.stop(delete);
            cacheDelegate.stop(delete);
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return delegate.getStatus();
        }

        @Override
        public boolean isInProgress() {
            return delegate.isInProgress();
        }

        @Override
        public void reset() {
            delegate.reset();
        }
    }
}
