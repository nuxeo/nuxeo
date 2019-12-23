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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.runtime.trackers.files.FileEventTracker;

/**
 * Blob store wrapper that caches blobs locally because fetching them may be expensive.
 *
 * @since 11.1
 */
public class CachingBlobStore extends AbstractBlobStore {

    protected final BlobStore store;

    // public for tests
    public final Path cacheDir;

    protected final FileCache fileCache;

    protected final PathStrategyFlat tmpPathStrategy;

    protected final BlobStore tmpStore;

    protected final BinaryGarbageCollector gc;

    public CachingBlobStore(String name, BlobStore store, CachingConfiguration config) {
        super(name, store.getKeyStrategy());
        this.store = store;
        cacheDir = config.dir;
        fileCache = new LRUFileCache(cacheDir.toFile(), config.maxSize, config.maxCount, config.minAge);
        // be sure FileTracker won't steal our files !
        FileEventTracker.registerProtectedPath(cacheDir.toAbsolutePath().toString());
        tmpPathStrategy = new PathStrategyFlat(cacheDir);
        tmpStore = new LocalBlobStore(name, store.getKeyStrategy(), tmpPathStrategy); // view of the LRUFileCache tmp dir
        gc = new CachingBinaryGarbageCollector(store.getBinaryGarbageCollector());
    }

    @Override
    public boolean hasVersioning() {
        return store.hasVersioning();
    }

    @Override
    public BlobStore unwrap() {
        return store.unwrap();
    }

    @Override
    public String writeBlob(BlobWriteContext blobWriteContext)
            throws IOException {
        // write the blob to a temporary file
        String tmpKey = tmpStore.writeBlob(blobWriteContext.copyWithKey(randomString()));
        // get the final key
        String key = blobWriteContext.getKey(); // may depend on write observer, for example for digests

        // when using deduplication, check if it's in the cache already
        if (blobWriteContext.useDeDuplication()) {
            if (fileCache.getFile(key) != null) {
                logTrace("<--", "exists");
                logTrace("hnote right: " + key);
                // delete tmp file, not needed anymore
                tmpStore.deleteBlob(tmpKey);
                return key;
            } else {
                logTrace("<--", "missing");
                logTrace("hnote right: " + key);
                // fall through
            }
        }

        // we now have a file for this blob
        Path tmp = tmpPathStrategy.getPathForKey(tmpKey);
        blobWriteContext.setFile(tmp);
        // send the file to storage
        String returnedKey = store.writeBlob(blobWriteContext.copyWithNoWriteObserverAndKey(key));
        // register the file in the file cache using its actual key
        logTrace(name, "-->", name, "rename");
        logTrace("hnote right of " + name + ": " + returnedKey);
        fileCache.putFile(returnedKey, tmp.toFile());
        return returnedKey;
    }

    @Override
    public boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        CachingBlobStore cachingSourceStore = sourceStore instanceof CachingBlobStore ? (CachingBlobStore) sourceStore
                : null;
        if ((!atomicMove || copyBlobIsOptimized(sourceStore)) && cachingSourceStore != null) {
            // if it's a copy and the original cached file won't be touched
            // else optimized move won't need the cache, so we can move the cache ahead of time
            tmpStore.copyBlob(key, cachingSourceStore.tmpStore, sourceKey, atomicMove);
        }
        boolean found = store.copyBlob(key, sourceStore, sourceKey, atomicMove);
        if (found && atomicMove && cachingSourceStore != null) {
            // clear source cache
            cachingSourceStore.tmpStore.deleteBlob(sourceKey);
        }
        return found;
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        File cachedFile = fileCache.getFile(key);
        if (cachedFile == null) {
            logTrace("<--", "missing");
            logTrace("hnote right: " + key);
            return OptionalOrUnknown.missing();
        } else {
            logTrace("<-", "read " + cachedFile.length() + " bytes");
            logTrace("hnote right: " + key);
            return OptionalOrUnknown.of(cachedFile.toPath());
        }
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        File cachedFile = fileCache.getFile(key);
        if (cachedFile == null) {
            logTrace("<--", "missing");
            logTrace("hnote right: " + key);
            // fetch file from storage into the cache
            // go through a tmp file for atomicity
            String tmpKey = randomString();
            boolean found = tmpStore.copyBlob(tmpKey, store, key, false);
            if (!found) {
                return OptionalOrUnknown.missing();
            }
            File tmp = tmpPathStrategy.getPathForKey(tmpKey).toFile();
            logTrace("->", "write " + tmp.length() + " bytes");
            logTrace("hnote right: " + key);
            cachedFile = fileCache.putFile(key, tmp);
        } else {
            logTrace("<-", "read " + cachedFile.length() + " bytes");
            logTrace("hnote right: " + key);
        }
        return OptionalOrUnknown.of(new FileInputStream(cachedFile));
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
        tmpStore.deleteBlob(key); // TODO add API to FileCache to do this cleanly
        store.deleteBlob(key);
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

        public CachingBinaryGarbageCollector(BinaryGarbageCollector delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public void start() {
            delegate.start();
        }

        @Override
        public void mark(String key) {
            delegate.mark(key);
        }

        @Override
        public void stop(boolean delete) {
            delegate.stop(delete);
            if (delete) {
                logTrace("->", "clear");
                fileCache.clear();
            }
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return delegate.getStatus();
        }

        @Override
        public boolean isInProgress() {
            return delegate.isInProgress();
        }
    }
}
