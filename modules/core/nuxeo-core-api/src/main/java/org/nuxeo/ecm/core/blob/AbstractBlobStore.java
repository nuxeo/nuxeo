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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.KeyStrategy.WriteObserver;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Basic helper implementations for a {@link BlobStore}.
 *
 * @since 11.1
 */
public abstract class AbstractBlobStore implements BlobStore {

    /**
     * Separator between key and byte range (start/end, specified at end of key).
     * <p>
     * Used when the blob provider is configured to allow byte ranges.
     */
    public static final char BYTE_RANGE_SEP = ';';

    private static final Logger log = LogManager.getLogger(AbstractBlobStore.class);

    protected final String blobProviderId;

    protected final String name;

    protected final KeyStrategy keyStrategy;

    public AbstractBlobStore(String name, KeyStrategy keyStrategy) {
        this(null, name, keyStrategy);
    }

    /** @since 11.5 */
    public AbstractBlobStore(String blobProviderId, String name, KeyStrategy keyStrategy) {
        this.blobProviderId = blobProviderId;
        this.name = name;
        this.keyStrategy = keyStrategy;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void logTrace(String arrow, String message) {
        logTrace(null, arrow, null, message);
    }

    protected void logTrace(String source, String arrow, String dest, String message) {
        if (source == null) {
            source = "Nuxeo";
        }
        if (dest == null) {
            dest = name;
        }
        logTrace(source + " " + arrow + " " + dest + ": " + message);
    }

    protected void logTrace(String message) {
        log.trace(message);
    }

    @Override
    public boolean hasVersioning() {
        return false;
    }

    @Override
    public KeyStrategy getKeyStrategy() {
        return keyStrategy;
    }

    @Override
    public BlobStore unwrap() {
        return this;
    }

    @Override
    public String writeBlob(BlobContext blobContext) throws IOException {
        BlobWriteContext blobWriteContext = keyStrategy.getBlobWriteContext(blobContext);
        return writeBlob(blobWriteContext);
    }

    @Override
    public String writeBlob(BlobWriteContext blobWriteContext) throws IOException {
        String returnedKey = writeBlobUsingOptimizedCopy(blobWriteContext);
        if (returnedKey != null) {
            return returnedKey;
        }
        return writeBlobGeneric(blobWriteContext);
    }

    /**
     * Writes the blob without using any store-to-store optimization.
     *
     * @since 11.5
     */
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        throw new UnsupportedOperationException("abstract method");
    }

    /**
     * Tries to do an optimize copy to write this blob. Returns {@code null} if that's not possible.
     *
     * @since 11.5
     */
    protected String writeBlobUsingOptimizedCopy(BlobWriteContext blobWriteContext) throws IOException {
        Blob blob = blobWriteContext.blobContext.blob;
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(managedBlob);
        if (!(blobProvider instanceof BlobStoreBlobProvider)) {
            return null;
        }
        BlobStore sourceStore = ((BlobStoreBlobProvider) blobProvider).store;
        if (!sourceStore.copyBlobIsOptimized(this)) {
            return null;
        }
        // optimized copy is possible
        // now check what the destination key should be
        String sourceKey = stripBlobKeyPrefix(managedBlob.getKey());
        sourceKey = getBlobKeyReplacement(sourceKey);
        String key = blobWriteContext.getKey();
        if (key == null) {
            if (!keyStrategy.useDeDuplication()) {
                throw new NuxeoException("Non-deduplication should have a key");
            }
            // key not known or not yet computed
            // check if the original blob key can give us a digest
            String digest;
            if (sourceStore == this) {
                // copy to self, nothing to do (includes temporary pseudo-digest for async)
                return sourceKey;
            } else if (sourceStore.getKeyStrategy().equals(keyStrategy)
                    && (digest = keyStrategy.getDigestFromKey(stripBlobKeyVersionSuffix(sourceKey))) != null) {
                key = digest;
            } else {
                if (!useAsyncDigest()) {
                    // must do normal write to compute a digest synchronously
                    // TODO instead of read + write we could do read + compute digest + optimized copy
                    return null;
                }
                key = null; // let the store compute the digest, or trigger async digest computation
            }
        }
        return copyOrMoveBlob(key, sourceStore, sourceKey, false);
    }

    /**
     * Whether this blob store is configured for async digest computation.
     *
     * @since 11.5
     */
    public boolean useAsyncDigest() {
        return false;
    }

    @Override
    public void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException {
        // ignore properties updates by default
    }

    @Override
    public void deleteBlob(BlobContext blobContext) {
        BlobWriteContext blobWriteContext = keyStrategy.getBlobWriteContext(blobContext);
        String key = blobWriteContext.getKey();
        if (key == null) {
            throw new NuxeoException("Cannot delete blob with " + getClass().getName());
        }
        deleteBlob(key);
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        BlobStore unwrapped = unwrap();
        if (unwrapped == this) {
            throw new UnsupportedOperationException(
                    "Class " + getClass().getName() + " must implement copyBlobIsOptimized");
        }
        return unwrapped.copyBlobIsOptimized(sourceStore);
    }

    protected String stripBlobKeyPrefix(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        return key;
    }

    protected String stripBlobKeyVersionSuffix(String key) {
        int seppos = key.indexOf(KeyStrategy.VER_SEP);
        if (seppos >= 0) {
            key = key.substring(0, seppos);
        }
        return key;
    }

    public static String setByteRangeInKey(String key, ByteRange byteRange) {
        return key + String.valueOf(BYTE_RANGE_SEP) + byteRange.getStart() + String.valueOf(BYTE_RANGE_SEP)
                + byteRange.getEnd();
    }

    public static ByteRange getByteRangeFromKey(MutableObject<String> keyHolder) {
        String key = keyHolder.getValue();
        int j = key.lastIndexOf(BYTE_RANGE_SEP);
        int i = key.lastIndexOf(BYTE_RANGE_SEP, j - 1);
        if (j > 0) {
            try {
                long start = Long.parseLong(key.substring(i + 1, j));
                long end = Long.parseLong(key.substring(j + 1));
                keyHolder.setValue(key.substring(0, i));
                return ByteRange.inclusive(start, end);
            } catch (NumberFormatException e) {
                log.debug("Cannot parse byte range in key: {}", key, e);
            }
        }
        return null;
    }

    /** Returns a random string suitable as a key. */
    protected String randomString() {
        StringBuilder sb = new StringBuilder(21);
        sb.append(randomLong());
        sb.append("-0"); // so that it cannot be confused with a digest
        return sb.toString();
    }

    /** Returns a random positive long. */
    protected long randomLong() {
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (value == Long.MIN_VALUE);
        if (value < 0) {
            value = -value;
        }
        return value;
    }

    /**
     * Transfers a blob to a file, notifying an observer while doing this.
     *
     * @param blobWriteContext the blob write context, to get the blob stream and write observer
     * @param dest the destination file
     */
    public void transfer(BlobWriteContext blobWriteContext, Path dest) throws IOException {
        // no need for BufferedOutputStream as we write a buffer already
        try (OutputStream out = Files.newOutputStream(dest)) {
            transfer(blobWriteContext, out);
        }
    }

    /**
     * Transfers a blob to an output stream, notifying an observer while doing this.
     *
     * @param blobWriteContext the blob write context, to get the blob stream and write observer
     * @param out the output stream
     */
    public void transfer(BlobWriteContext blobWriteContext, OutputStream out) throws IOException {
        try (InputStream in = blobWriteContext.getStream()) {
            transfer(in, out, blobWriteContext.writeObserver);
        }
    }

    /**
     * Copies bytes from an input stream to an output stream, notifying an observer while doing this.
     *
     * @param in the input stream
     * @param out the output stream
     * @param writeObserver the write observer
     */
    @SuppressWarnings("resource")
    public void transfer(InputStream in, OutputStream out, WriteObserver writeObserver) throws IOException {
        if (writeObserver != null) {
            out = writeObserver.wrap(out);
        }
        IOUtils.copy(in, out);
        if (writeObserver != null) {
            writeObserver.done();
        }
    }

    protected void notifyAsyncDigest(String key) {
        // due to dependency issues between nuxeo-core-api and nuxeo-core,
        // we have to use a "runtime" event instead of a core event for dependency inversion
        // to trigger code living in nuxeo-core (because it needs to access the repository)
        EventService eventService = Framework.getService(EventService.class);
        Map<String, String> data = new HashMap<>();
        data.put("blobProviderId", blobProviderId);
        data.put("key", key);
        Event event = new Event("asyncDigest", null, null, data);
        eventService.sendEvent(event);
    }

    /** A key may have been replaced by an async digest computation, use the new one. */
    protected String getBlobKeyReplacement(String key) {
        if (blobProviderId == null) {
            // old blob store not using new constructor
            return key;
        }
        return Framework.getService(BlobManager.class).getBlobKeyReplacement(blobProviderId, key);
    }

}
