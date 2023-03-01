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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;

/**
 * Blob storage as files on a local filesystem. The actual storage path chosen for a given key is decided based on a
 * {@link PathStrategy}.
 *
 * @since 11.1
 */
public class LocalBlobStore extends AbstractBlobStore {

    private static final Logger log = LogManager.getLogger(LocalBlobStore.class);

    protected final PathStrategy pathStrategy;

    protected final LocalBlobGarbageCollector gc;

    public LocalBlobStore(String name, KeyStrategy keyStrategy, PathStrategy pathStrategy) {
        super(name, keyStrategy);
        this.pathStrategy = pathStrategy;
        gc = new LocalBlobGarbageCollector();
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        Path tmp = pathStrategy.createTempFile();
        try {
            write(blobWriteContext, tmp);
            logTrace("->", "write " + Files.size(tmp) + " bytes");
            logTrace("hnote right: " + tmp.getFileName().toString());
            String key = blobWriteContext.getKey(); // may depend on WriteObserver, for example for digests
            Path dest = pathStrategy.getPathForKey(key);
            Files.createDirectories(dest.getParent());
            logTrace(name, "-->", name, "rename");
            logTrace("hnote right of " + name + ": " + key);
            PathStrategy.atomicMove(tmp, dest);
            return key;
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                log.warn(e, e);
            }
        }
    }

    // overridden for encrypted storage
    protected void write(BlobWriteContext blobWriteContext, Path file) throws IOException {
        transfer(blobWriteContext, file);
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return sourceStore.unwrap() instanceof LocalBlobStore;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof LocalBlobStore) {
            LocalBlobStore sourceLocalBlobStore = (LocalBlobStore) unwrappedSourceStore;
            return copyBlob(key, sourceLocalBlobStore, sourceKey, atomicMove);
        }
        return copyBlobGeneric(key, sourceStore, sourceKey, atomicMove);
    }

    /**
     * Optimized file-to-file copy/move.
     */
    protected String copyBlob(String key, LocalBlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        Path source = sourceStore.pathStrategy.getPathForKey(sourceKey);
        if (!Files.exists(source)) { // NOSONAR (squid:S3725)
            return null;
        }
        if (key == null) { // fast compute or trigger async digest computation
            // here we can do a fast compute
            String digestAlgorithm = ((KeyStrategyDigest) keyStrategy).digestAlgorithm;
            key = new DigestUtils(digestAlgorithm).digestAsHex(source.toFile());
        }
        Path dest = pathStrategy.getPathForKey(key);
        Files.createDirectories(dest.getParent());
        if (atomicMove) {
            if (sourceStore == this) {
                logTrace("hnote right of " + name + ": " + sourceKey);
                logTrace(name, "-->", name, "rename");
                logTrace("hnote right of " + name + ": " + key);
            } else {
                logTrace("hnote right of " + sourceStore.name + ": " + sourceKey);
                logTrace(sourceStore.name, "->", name, "move");
                logTrace("hnote right: " + key);
            }
            PathStrategy.atomicMove(source, dest);
        } else {
            logTrace("hnote right of " + sourceStore.name + ": " + sourceKey);
            logTrace(sourceStore.name, "->", name, "copy");
            logTrace("hnote right: " + key);
            PathStrategy.atomicCopy(source, dest);
        }
        return key;
    }

    /**
     * Generic copy/move to a local file.
     */
    protected String copyBlobGeneric(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        Path dest = pathStrategy.getPathForKey(key);
        Files.createDirectories(dest.getParent());
        Path tmp = null;
        try {
            Path readTo;
            if (atomicMove) {
                readTo = tmp = pathStrategy.createTempFile();
            } else {
                readTo = dest;
            }
            OptionalOrUnknown<Path> fileOpt = sourceStore.getFile(sourceKey);
            if (fileOpt.isPresent()) {
                PathStrategy.atomicCopy(fileOpt.get(), readTo);
            } else {
                boolean found = sourceStore.readBlob(sourceKey, readTo);
                if (!found) {
                    return null;
                }
            }
            if (atomicMove) {
                PathStrategy.atomicMove(readTo, dest);
                sourceStore.deleteBlob(sourceKey);
            }
            return key;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return getStoredFile(key);
    }

    protected OptionalOrUnknown<Path> getStoredFile(String key) {
        Path file = pathStrategy.getPathForKey(key);
        return Files.exists(file) ? OptionalOrUnknown.of(file) : OptionalOrUnknown.missing(); // NOSONAR (squid:S3725)
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        Path file = pathStrategy.getPathForKey(key);
        try {
            return OptionalOrUnknown.of(new BufferedInputStream(Files.newInputStream(file)));
        } catch (NoSuchFileException e) {
            return OptionalOrUnknown.missing();
        }
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        Path file = pathStrategy.getPathForKey(key);
        if (Files.exists(file)) { // NOSONAR (squid:S3725)
            logTrace("<-", "read " + Files.size(file) + " bytes");
            logTrace("hnote right: " + key);
            PathStrategy.atomicCopy(file, dest);
            return true;
        } else {
            logTrace("<--", "missing");
            logTrace("hnote right: " + key);
            return false;
        }
    }

    @Override
    public void deleteBlob(String key) {
        Path file = pathStrategy.getPathForKey(key);
        try {
            logTrace("->", "delete");
            logTrace("hnote right: " + key);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn(e, e);
        }
    }

    @Override
    public void clear() {
        try {
            FileUtils.cleanDirectory(pathStrategy.dir.toFile());
        } catch (IOException e) {
            log.warn(e, e);
        }
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    public class LocalBlobGarbageCollector implements BinaryGarbageCollector {

        /**
         * Windows FAT filesystems have a time resolution of 2s. Other common filesystems have 1s.
         */
        public static final long TIME_RESOLUTION = 2000;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        // for tests, to simulate behavior of other GCs that don't use on-disk marking
        public volatile boolean markInMemory;

        // for tests
        protected Set<String> toDelete;

        @Override
        public String getId() {
            return pathStrategy.dir.toUri().toString();
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return status;
        }

        @Override
        public boolean isInProgress() {
            // volatile as this is designed to be called from another thread
            return startTime != 0;
        }

        @Override
        public void start() {
            if (startTime != 0) {
                throw new NuxeoException("Already started");
            }
            startTime = System.currentTimeMillis();
            status = new BinaryManagerStatus();
            if (markInMemory) {
                toDelete = new HashSet<>();
                computeToDelete(pathStrategy.dir.toFile(), startTime - TIME_RESOLUTION);
            }
        }

        // for tests
        protected void computeToDelete(File file, long minTime) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    computeToDelete(f, minTime);
                }
            } else if (file.isFile() && file.canWrite()) {
                long lastModified = file.lastModified();
                if (lastModified == 0) {
                    log.warn("GC cannot read last modified for file: {}", file);
                } else if (lastModified < minTime) {
                    status.sizeBinaries += file.length();
                    status.numBinaries++;
                    toDelete.add(file.getName());
                }
            }
        }

        @Override
        public void mark(String key) {
            if (markInMemory) {
                toDelete.remove(key);
            } else {
                OptionalOrUnknown<Path> fileOpt = getStoredFile(key);
                if (!fileOpt.isPresent()) {
                    log.warn("Unknown blob for key: {}",  key);
                    return;
                }
                // mark the blob by touching the file
                touch(fileOpt.get().toFile());
            }
        }

        @Override
        public void stop(boolean delete) {
            if (startTime == 0) {
                throw new NuxeoException("Not started");
            }
            if (markInMemory) {
                removeUnmarkedBlobsAndUpdateStatus(delete);
            } else {
                deleteOld(pathStrategy.dir.toFile(), startTime - TIME_RESOLUTION, 0, delete);
            }
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
            toDelete = null;
        }

        @Override
        public void reset() {
            startTime = 0;
        }

        protected void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String key : toDelete) {
                OptionalOrUnknown<Path> fileOpt = getStoredFile(key);
                if (!fileOpt.isPresent()) {
                    // shouldn't happen except if blob concurrently removed
                    continue;
                }
                Path path = fileOpt.get();
                long length;
                try {
                    length = Files.size(path);
                } catch (IOException e) {
                    log.error(key, e);
                    continue;
                }
                status.sizeBinariesGC += length;
                status.numBinariesGC++;
                status.sizeBinaries -= length;
                status.numBinaries--;
                if (delete) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error(key, e);
                    }
                }
            }
        }

        protected void deleteOld(File file, long minTime, int depth, boolean delete) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteOld(f, minTime, depth + 1, delete);
                }
                // NXP-30465: do not delete empty directories
            } else if (file.isFile() && file.canWrite()) {
                long lastModified = file.lastModified();
                long length = file.length();
                if (lastModified == 0) {
                    log.warn("GC cannot read last modified for file: {}", file);
                } else if (lastModified < minTime) {
                    status.sizeBinariesGC += length;
                    status.numBinariesGC++;
                    if (delete) {
                        log.debug("GC deleting file: {}", file);
                        if (!file.delete()) { // NOSONAR
                            log.warn("GC failed to delete file: {}", file);
                        }
                    }
                } else {
                    status.sizeBinaries += length;
                    status.numBinaries++;
                }
            }
        }

        /** Sets the last modification date to now on a file. */
        protected void touch(File file) {
            long time = System.currentTimeMillis();
            if (file.setLastModified(time)) {
                // ok
                return;
            }
            if (!file.canWrite()) {
                // cannot write -> stop won't be able to delete anyway
                return;
            }
            try {
                // Windows: the file may be open for reading
                // workaround found by Thomas Mueller, see JCR-2872
                try (RandomAccessFile r = new RandomAccessFile(file, "rw")) {
                    r.setLength(r.length());
                }
            } catch (IOException e) {
                log.warn("Cannot set last modified for file: " + file, e);
            }
        }

    }

}
