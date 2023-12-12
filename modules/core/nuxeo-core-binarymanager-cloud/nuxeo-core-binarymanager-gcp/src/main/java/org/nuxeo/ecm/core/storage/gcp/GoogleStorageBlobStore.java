/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.gcp;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.AbstractBlobGarbageCollector;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageException;
import com.google.common.io.ByteStreams;

/**
 * @since 2023.5
 */
public class GoogleStorageBlobStore extends AbstractBlobStore {

    protected static final Logger log = LogManager.getLogger(GoogleStorageBlobStore.class);

    protected final String bucketName;

    protected final String bucketPrefix;

    protected final Bucket bucket;

    protected final Storage storage;

    protected final int chunkSize;

    protected final boolean allowByteRange;

    protected final GoogleStorageBlobStoreConfiguration config;

    protected final BinaryGarbageCollector gc;

    public GoogleStorageBlobStore(String blobProviderId, String name, GoogleStorageBlobStoreConfiguration config,
            KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
        this.config = config;
        this.bucket = config.bucket;
        this.storage = config.storage;
        this.bucketName = config.bucketName;
        this.bucketPrefix = config.bucketPrefix;
        this.allowByteRange = config.getBooleanProperty(ALLOW_BYTE_RANGE);
        this.chunkSize = config.chunkSize;
        this.gc = new GoogleStorageBlobGarbageCollector();
    }

    @Override
    public void clear() {
        for (Blob blob : bucket.list().iterateAll()) {
            blob.delete();
        }
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return sourceStore.unwrap() instanceof GoogleStorageBlobStore;
    }

    protected Blob getBlob(String key) {
        return bucket.get(bucketPrefix + key);
    }

    @Override
    public boolean hasVersioning() {
        // Maybe later
        return false;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean move) throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof GoogleStorageBlobStore sourceGSBlobStore) {
            // attempt direct GS-level copy
            String sourceBucketName = sourceGSBlobStore.bucketName;
            String sourceBucketKey = sourceGSBlobStore.bucketPrefix + sourceKey;
            String bucketKey = bucketPrefix + key;
            BlobId source = BlobId.of(sourceGSBlobStore.bucketName, sourceGSBlobStore.bucketPrefix + sourceKey);
            BlobId target = BlobId.of(bucketName, bucketKey);
            Storage.BlobTargetOption precondition;
            if (storage.get(bucketName, bucketKey) == null) {
                // For a target object that does not yet exist, set the DoesNotExist precondition.
                // This will cause the request to fail if the object is created before the request runs.
                precondition = Storage.BlobTargetOption.doesNotExist();
            } else {
                // If the destination already exists in your bucket, instead set a generation-match
                // precondition. This will cause the request to fail if the existing object's generation
                // changes before the request runs.
                precondition = Storage.BlobTargetOption.generationMatch(
                        storage.get(bucketName, bucketKey).getGeneration());
            }
            String resultKey = null;
            try {
                CopyWriter writer = storage.copy(
                        Storage.CopyRequest.newBuilder().setSource(source).setTarget(target, precondition).build());
                Blob blob = writer.getResult();
                if (blob != null) {
                    resultKey = blob.getBlobId().getName();
                }
            } catch (StorageException e) {
                String message = "Direct copy failed from gs://{}/{} to gs://{}/{}, falling back to slow copy: {}";
                log.warn(message, () -> sourceBucketName, () -> sourceBucketKey, () -> bucketName, () -> bucketKey,
                        e::getMessage);
                log.debug(message, () -> sourceBucketName, () -> sourceBucketKey, () -> bucketName, () -> bucketKey,
                        () -> e);
                if (!isMissingKey(e)) {
                    throw new IOException(e);
                }
            }
            if (resultKey != null) {
                if (move) {
                    storage.delete(source);
                }
                return key;
            }
            // fall through if not copied
        }
        return copyOrMoveBlobGeneric(key, sourceStore, sourceKey, move);
    }

    protected String copyOrMoveBlobGeneric(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        Path tmp = null;
        try {
            OptionalOrUnknown<Path> fileOpt = sourceStore.getFile(sourceKey);
            Path file;
            if (fileOpt.isPresent()) {
                file = fileOpt.get();
            } else {
                // no local file available, read from source
                tmp = Files.createTempFile("bin_", ".tmp");
                boolean found = sourceStore.readBlob(sourceKey, tmp);
                if (!found) {
                    return null;
                }
                file = tmp;
            }
            // if the digest is not already known then save to GCS
            String bucketKey = bucketPrefix + key;
            Blob blob = bucket.get(bucketKey);
            if (blob == null) {
                try (var is = new BufferedInputStream(new FileInputStream(file.toFile()));
                        var writer = storage.writer(BlobInfo.newBuilder(bucketName, bucketKey).build())) {
                    int bufferLength;
                    byte[] buffer = new byte[chunkSize];
                    writer.setChunkSize(chunkSize);
                    while ((bufferLength = IOUtils.read(is, buffer)) > 0) {
                        writer.write(ByteBuffer.wrap(buffer, 0, bufferLength));
                    }
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
            if (atomicMove) {
                sourceStore.deleteBlob(sourceKey);
            }
            return key;
        } finally {
            if (tmp != null) {
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }
    }

    @Override
    public void deleteBlob(String key) {
        storage.delete(BlobId.of(bucketName, bucketPrefix + key));
    }

    @Override
    public boolean exists(String key) {
        return bucket.get(bucketPrefix + key) != null;
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        Blob blob = bucket.get(bucketPrefix + key);
        if (blob == null) {
            return OptionalOrUnknown.missing();
        }
        return OptionalOrUnknown.of(Channels.newInputStream(blob.reader()));
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        ByteRange byteRange;
        if (allowByteRange) {
            MutableObject<String> keyHolder = new MutableObject<>(key);
            byteRange = getByteRangeFromKey(keyHolder);
            key = keyHolder.getValue();
        } else {
            byteRange = null;
        }
        String bucketKey = bucketPrefix + key;
        Blob blob = bucket.get(bucketKey);
        if (blob == null) {
            return false;
        }
        if (byteRange != null) {
            try (ReadChannel from = storage.reader(blob.getBlobId());
                    FileChannel to = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                from.seek(byteRange.getStart());
                // limit method is available from gcp client >= 2.5.0 see
                // https://github.com/googleapis/java-storage/pull/1180
                // Weirdly gcp client limit api takes the maximum number of bytes to be read, not an offset hence
                // byteRange.getEnd() + 1
                from.limit(byteRange.getEnd() + 1);
                ByteStreams.copy(from, to);
            }
        } else {
            blob.downloadTo(dest);
        }
        return true;
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        Path file;
        Path tmp = null;
        try {
            BlobContext blobContext = blobWriteContext.blobContext;
            Path blobWriteContextFile = blobWriteContext.getFile();
            if (blobWriteContextFile != null) { // we have a file, assume that the caller already observed the write
                file = blobWriteContextFile;
            } else {
                // no transfer to a file was done yet (no caching)
                // we may be able to use the blob's underlying file, if not pure streaming
                File blobFile = blobContext.blob.getFile();
                if (blobFile != null) { // otherwise use blob file directly
                    if (blobWriteContext.writeObserver != null) { // but we must still run the writes through the write
                                                                  // observer
                        transfer(blobWriteContext, NULL_OUTPUT_STREAM);
                    }
                    file = blobFile.toPath();
                } else {
                    // we must transfer the blob stream to a tmp file
                    tmp = Files.createTempFile("bin_", ".tmp");
                    transfer(blobWriteContext, tmp);
                    file = tmp;
                }
            }
            String key = blobWriteContext.getKey(); // may depend on write observer, for example for digests
            if (key == null) {
                // should never happen unless an invalid WriteObserver is used in new code
                throw new NuxeoException("Missing key");
            }
            // if the digest is not already known then save to GCS
            long t0 = System.currentTimeMillis();
            log.debug("Storing blob with digest: {} to GCS", key);
            String bucketKey = bucketPrefix + key;
            if (bucket.get(bucketKey) == null) {
                try (var is = new BufferedInputStream(new FileInputStream(file.toFile()));
                        var writer = storage.writer(BlobInfo.newBuilder(bucketName, bucketKey).build())) {
                    int bufferLength;
                    byte[] buffer = new byte[chunkSize];
                    writer.setChunkSize(chunkSize);
                    while ((bufferLength = IOUtils.read(is, buffer)) > 0) {
                        writer.write(ByteBuffer.wrap(buffer, 0, bufferLength));
                    }
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
                log.debug("Stored blob with digest: {} to GCS in {}ms", key, System.currentTimeMillis() - t0);
            } else {
                log.debug("Blob with digest: {} is already in GCS", key);
            }
            return key;
        } finally {
            if (tmp != null) {
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.warn(e, e);
                }
            }
        }

    }

    protected static boolean isMissingKey(StorageException e) {
        return e.getCode() == 404;
    }

    public class GoogleStorageBlobGarbageCollector extends AbstractBlobGarbageCollector {

        @Override
        public void computeToDelete() {
            int prefixLength = bucketPrefix.length();
            toDelete = new HashSet<>();
            Page<Blob> blobs = bucket.list(BlobListOption.fields(BlobField.ID, BlobField.SIZE),
                    BlobListOption.prefix(bucketPrefix));
            do {
                for (Blob blob : blobs.iterateAll()) {
                    String digest = blob.getName().substring(prefixLength);
                    status.sizeBinaries += blob.getSize();
                    status.numBinaries++;
                    toDelete.add(digest);
                }
                blobs = blobs.getNextPage();
            } while (blobs != null);
        }

        @Override
        public String getId() {
            return "gcp:" + bucketName + "/" + bucketPrefix;
        }

        @Override
        public void mark(String key) {
            toDelete.remove(key);
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String key : toDelete) {
                String prefixedKey = bucketPrefix + key;
                Blob blob = bucket.get(prefixedKey);
                if (bucket.get(prefixedKey) != null) {
                    long length = blob.getSize();
                    status.sizeBinariesGC += length;
                    status.numBinariesGC++;
                    status.sizeBinaries -= length;
                    status.numBinaries--;
                    if (delete) {
                        deleteBlob(key);
                    }
                }
            }
        }
    }

}
