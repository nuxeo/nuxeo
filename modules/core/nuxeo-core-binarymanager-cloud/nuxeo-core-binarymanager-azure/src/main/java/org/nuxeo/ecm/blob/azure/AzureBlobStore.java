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
package org.nuxeo.ecm.blob.azure;

import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import org.apache.commons.io.output.NullOutputStream;
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
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;

/**
 * @since 2023.6
 */
public class AzureBlobStore extends AbstractBlobStore {

    protected static final Logger log = LogManager.getLogger(AzureBlobStore.class);

    protected final AzureBlobStoreConfiguration config;

    protected final BinaryGarbageCollector gc;

    protected BlobContainerClient client;

    protected String prefix;

    protected boolean allowByteRange;

    public AzureBlobStore(String blobProviderId, String name, AzureBlobStoreConfiguration config,
            KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
        this.config = config;
        this.client = config.client;
        this.prefix = config.prefix;
        this.allowByteRange = config.getBooleanProperty(ALLOW_BYTE_RANGE);
        this.gc = new AzureBlobGarbageCollector();
    }

    public class AzureBlobGarbageCollector extends AbstractBlobGarbageCollector {

        @Override
        public void computeToDelete() {
            toDelete = new HashSet<>();
            client.listBlobsByHierarchy(prefix).forEach(blob -> {
                if (blob.isPrefix()) {
                    // ignore sub directories
                    return;
                }
                String name = blob.getName();
                String digest = name.substring(prefix.length());
                if (!((KeyStrategyDigest) keyStrategy).isValidDigest(digest)) {
                    // ignore blobs that cannot be digests, for safety
                    return;
                }
                long length = blob.getProperties().getContentLength();
                status.sizeBinaries += length;
                status.numBinaries++;
                toDelete.add(digest);
            });
        }

        @Override
        public String getId() {
            return "azure:" + config.containerName + "/" + prefix;
        }

        @Override
        public void mark(String key) {
            toDelete.remove(key);
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String digest : toDelete) {
                BlobClient blobClient = client.getBlobClient(prefix + digest);
                if (!blobClient.exists()) {
                    // shouldn't happen except if blob concurrently removed
                    continue;
                }
                long length = blobClient.getProperties().getBlobSize();
                status.sizeBinariesGC += length;
                status.numBinariesGC++;
                status.sizeBinaries -= length;
                status.numBinaries--;
                if (delete) {
                    deleteBlob(digest);
                }
            }
        }
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return sourceStore.unwrap() instanceof AzureBlobStore;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean move) throws IOException {
        BlobStore unwrappedSourceStore = sourceStore.unwrap();
        if (unwrappedSourceStore instanceof AzureBlobStore sourceAzureBlobStore) {
            BlobClient sourceBlobClient = sourceAzureBlobStore.client.getBlobClient(
                    sourceAzureBlobStore.prefix + sourceKey);
            if (!sourceBlobClient.exists()) {
                return null;
            }
            String sourceBlobSasURL = AzureBlobProvider.generateSASUrl(sourceBlobClient, null, null,
                    60L * 60L /* 1 hour */);
            BlobClient destBlobClient = client.getBlobClient(prefix + key);
            // if the digest is not already known then save to Azure
            if (!destBlobClient.exists()) {
                destBlobClient.getBlockBlobClient().uploadFromUrl(sourceBlobSasURL);
            }
            if (move) {
                sourceStore.deleteBlob(sourceKey);
            }
            return key;
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
            writeFile(key, file);
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

    protected void writeFile(String key, Path file) {
        BlobClient blobClient = client.getBlobClient(prefix + key);
        if (blobClient.exists()) {
            return;
        }
        ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
        parallelTransferOptions.setBlockSizeLong(config.blockSize)
                               .setMaxConcurrency(config.maxConcurrency)
                               .setMaxSingleUploadSizeLong(config.maxSingleUploadSize);
        BlobUploadFromFileOptions options = new BlobUploadFromFileOptions(file.toString());
        options.setParallelTransferOptions(parallelTransferOptions);
        try {
            blobClient.uploadFromFileWithResponse(options, config.uploadTimeout, null);
        } catch (UncheckedIOException e) {
            throw new NuxeoException("Failed to write blob: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        BlobClient blobClient = client.getBlobClient(prefix + key);
        return blobClient.exists();
    }

    @Override
    public boolean hasVersioning() {
        // Maybe later
        return false;
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        BlobClient blobClient = client.getBlobClient(prefix + key);
        if (!blobClient.exists()) {
            return OptionalOrUnknown.missing();
        }
        return OptionalOrUnknown.of(blobClient.openInputStream());
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
        log.debug("fetching blob: {} from Azure", key);
        BlobClient blobClient = client.getBlobClient(prefix + key);
        if (!blobClient.exists()) {
            return false;
        }
        try (OutputStream os = new FileOutputStream(dest.toFile())) {
            if (byteRange != null) {
                blobClient.downloadStreamWithResponse(os, new BlobRange(byteRange.getStart(), byteRange.getLength()),
                        null, null, false, null, Context.NONE);
            } else {
                blobClient.downloadStream(os);
            }
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
                        transfer(blobWriteContext, NullOutputStream.INSTANCE);
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
            // if the digest is not already known then save to Azure
            log.debug("Storing blob with digest: {} to Azure", key);
            writeFile(key, file);
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
        client.getBlobClient(prefix + key).deleteIfExists();
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    @Override
    public void clear() {
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
        client.listBlobs(options, null).iterator().forEachRemaining(item -> {
            client.getBlobClient(item.getName()).delete();
        });
    }

}
