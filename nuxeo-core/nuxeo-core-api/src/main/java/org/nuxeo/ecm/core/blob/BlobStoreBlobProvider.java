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

import static org.nuxeo.ecm.core.blob.DigestConfiguration.DIGEST_ALGORITHM_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobStore.OptionalOrUnknown;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A {@link BlobProvider} implemented on top of an underlying {@link BlobStore}.
 * <p>
 * This abstract class deals with
 */
public abstract class BlobStoreBlobProvider extends AbstractBlobProvider {

    /** @since 11.2 */
    public static final String KEY_STRATEGY_PROPERTY = "keyStrategy";

    /** @since 11.2 */
    public static final String MANAGED_KEY_STRATEGY = "managed";

    /** @since 11.2 */
    public static final String DIGEST_KEY_STRATEGY = "digest";

    public BlobStore store;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        store = getBlobStore(blobProviderId, properties);
    }

    protected abstract BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException;

    /** @since 11.2 */
    public KeyStrategy getKeyStrategy() {
        boolean hasDigest = properties.get(DIGEST_ALGORITHM_PROPERTY) != null;
        KeyStrategy keyStrategy;
        if (isRecordMode() && !hasDigest) {
            keyStrategy = KeyStrategyDocId.instance();
        } else {
            String strKeyStrategy = properties.getOrDefault(KEY_STRATEGY_PROPERTY, DIGEST_KEY_STRATEGY);
            keyStrategy = new KeyStrategyDigest(getDigestAlgorithm());
            if (MANAGED_KEY_STRATEGY.equals(strKeyStrategy)) {
                keyStrategy = new KeyStrategyManaged(keyStrategy);
            }
        }
        return keyStrategy;
    }

    /** The digest algorithm to use for the default key strategy. */
    protected abstract String getDigestAlgorithm();

    @Override
    public BinaryManager getBinaryManager() {
        return null;
    }

    @Override
    public boolean supportsSync() {
        return supportsUserUpdate();
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return store.getBinaryGarbageCollector();
    }

    protected String stripBlobKeyPrefix(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0 && key.substring(0, colon).equals(blobProviderId)) {
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

    @Override
    public String writeBlob(BlobContext blobContext) throws IOException {
        String key = store.writeBlob(blobContext);
        fixupDigest(blobContext.blob, key);
        return key;
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        if (isRecordMode()) {
            throw new UnsupportedOperationException("Cannot write blob directly without context in record mode");
        }
        return writeBlob(new BlobContext(blob));
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        String blobKey = blob.getKey();
        return getStream(blobKey, null);
    }

    @Override
    public InputStream getStream(String blobKey, ByteRange byteRange) throws IOException {
        String key = stripBlobKeyPrefix(blobKey);
        if (byteRange != null) {
            if (!allowByteRange()) {
                throw new UnsupportedOperationException("Cannot use byte ranges in keys");
            }
            key = AbstractBlobStore.setByteRangeInKey(key, byteRange);
        }
        OptionalOrUnknown<InputStream> streamOpt = store.getStream(key);
        if (streamOpt.isKnown()) {
            if (!streamOpt.isPresent()) {
                throw new IOException("Missing blob: " + key);
            }
            return streamOpt.get();
        } else {
            // underlying store is low-level and doesn't have a stream available
            // this should only happen in test situations, in real life there's a cache in front
            boolean returned = false;
            Path tmp = Framework.createTempFilePath("bin_", ".tmp");
            try {
                boolean found = store.readBlob(key, tmp);
                if (!found) {
                    throw new IOException("Missing blob: " + key);
                }
                AutoDeleteFileInputStream stream = new AutoDeleteFileInputStream(tmp);
                returned = true;
                return stream;
            } finally {
                if (!returned) {
                    Files.deleteIfExists(tmp);
                }
            }
        }
    }

    @Override
    public File getFile(ManagedBlob blob) {
        String key = stripBlobKeyPrefix(blob.getKey());
        OptionalOrUnknown<Path> fileOpt = store.getFile(key);
        return fileOpt.isPresent() ? fileOpt.get().toFile() : null;
    }

    /**
     * A {@link FileInputStream} that deletes its underlying file when it is closed.
     */
    public static class AutoDeleteFileInputStream extends FileInputStream {

        private static final Logger log = LogManager.getLogger(AutoDeleteFileInputStream.class);

        protected Path file;

        public AutoDeleteFileInputStream(Path file) throws IOException {
            super(file.toFile());
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (file != null) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        log.warn(e, e);
                    }
                    // attempt delete only once, even if close() is called several times
                    file = null;
                }
            }
        }
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        ManagedBlob blob = new SimpleManagedBlob(blobProviderId, blobInfo); // calls back to #getStream
        fixupDigest(blob, blob.getKey());
        return blob;
    }

    /**
     * Fixup of the blob's digest, if possible.
     *
     * @param blob the blob
     * @param key the key
     * @since 11.2
     */
    protected void fixupDigest(Blob blob, String key) {
        if (blob.getDigestAlgorithm() == null) {
            KeyStrategy keyStrategy = store.getKeyStrategy();
            if (keyStrategy instanceof KeyStrategyManaged) {
                keyStrategy = ((KeyStrategyManaged) keyStrategy).strategy;
            }
            if (keyStrategy instanceof KeyStrategyDigest) {
                KeyStrategyDigest ksd = (KeyStrategyDigest) keyStrategy;
                String currentDigest = blob.getDigest();
                if (currentDigest == null || currentDigest.contains("-")) {
                    // missing or temporary digest
                    String digest = stripBlobKeyVersionSuffix(stripBlobKeyPrefix(key));
                    blob.setDigest(digest);
                    currentDigest = digest;
                }
                if (blob.getDigestAlgorithm() == null && ksd.isValidDigest(currentDigest)) {
                    blob.setDigestAlgorithm(ksd.digestAlgorithm);
                }
            }
        }
    }

    @Override
    public void updateBlob(BlobUpdateContext blobUpdateContext) throws IOException {
        store.writeBlobProperties(blobUpdateContext);
    }

    @Override
    public void deleteBlob(BlobContext blobContext) {
        store.deleteBlob(blobContext);
    }

}
