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
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.runtime.api.Framework;

/**
 * Blob storage in memory, mostly for unit tests.
 *
 * @since 11.1
 */
public class InMemoryBlobStore extends AbstractBlobStore {

    private static final Logger log = LogManager.getLogger(InMemoryBlobStore.class);

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected Map<String, byte[]> map = new ConcurrentHashMap<>();

    protected Map<String, Boolean> legalHold = new ConcurrentHashMap<>();

    protected final InMemoryBlobGarbageCollector gc = new InMemoryBlobGarbageCollector();

    // used by unit tests to emulate absence of stream, to test copy
    protected final boolean emulateNoStream;

    // used by unit tests to emulate presence of a local file, to test copy
    protected final boolean emulateLocalFile;

    // used by unit tests to emulate versioning
    protected final boolean emulateVersioning;

    protected final boolean allowByteRange;

    public InMemoryBlobStore(String name, KeyStrategy keyStrategy) {
        this(name, null, keyStrategy, false, false);
    }

    public InMemoryBlobStore(String name, PropertyBasedConfiguration config, KeyStrategy keyStrategy) {
        this(name, config, keyStrategy, false, false);
    }

    protected InMemoryBlobStore(String name, KeyStrategy keyStrategy, boolean emulateNoStream,
            boolean emulateLocalFile) {
        this(name, null, keyStrategy, emulateNoStream, emulateLocalFile);
    }

    protected InMemoryBlobStore(String name, PropertyBasedConfiguration config, KeyStrategy keyStrategy,
            boolean emulateNoStream, boolean emulateLocalFile) {
        super(name, keyStrategy);
        this.emulateNoStream = emulateNoStream;
        this.emulateLocalFile = emulateLocalFile;
        emulateVersioning = config != null && config.getBooleanProperty("emulateVersioning");
        allowByteRange = config != null && config.getBooleanProperty(ALLOW_BYTE_RANGE);
    }

    @Override
    public boolean hasVersioning() {
        return emulateVersioning;
    }

    @Override
    public String writeBlob(BlobWriteContext blobWriteContext) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transfer(blobWriteContext, baos);
        String key = blobWriteContext.getKey(); // may depend on WriteObserver, for example for digests
        if (hasVersioning()) {
            key += "@" + RANDOM.nextLong();
        }
        map.put(key, baos.toByteArray());
        return key;
    }

    @Override
    public void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException {
        String key = blobUpdateContext.key;
        if (blobUpdateContext.updateLegalHold != null) {
            boolean hold = blobUpdateContext.updateLegalHold.hold;
            legalHold.put(key, hold);
        }
        // other updates not implemented for in-memory blob store
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        // this allows us to test "optimized copy" code paths
        return sourceStore instanceof InMemoryBlobStore;
    }

    @Override
    public boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        boolean found = copyBlob(key, sourceStore, sourceKey);
        if (found && atomicMove) {
            sourceStore.deleteBlob(sourceKey);
        }
        return found;
    }

    protected boolean copyBlob(String key, BlobStore sourceStore, String sourceKey) throws IOException {
        // try with a stream
        OptionalOrUnknown<InputStream> streamOpt = sourceStore.getStream(sourceKey);
        if (streamOpt.isKnown()) {
            if (!streamOpt.isPresent()) {
                return false;
            }
            byte[] bytes;
            try (InputStream stream = streamOpt.get()) {
                bytes = IOUtils.toByteArray(stream);
            }
            map.put(key, bytes);
            return true;
        }
        // try with a local file
        OptionalOrUnknown<Path> fileOpt = sourceStore.getFile(sourceKey);
        if (fileOpt.isKnown()) {
            if (!fileOpt.isPresent()) {
                return false;
            }
            byte[] bytes = Files.readAllBytes(fileOpt.get());
            map.put(key, bytes);
            return true;
        }
        // else use readBlobTo
        Path tmp = Files.createTempFile("bin_", ".tmp");
        try {
            boolean found = sourceStore.readBlob(sourceKey, tmp);
            if (!found) {
                return false;
            }
            byte[] bytes = Files.readAllBytes(tmp);
            map.put(key, bytes);
            return true;
        } finally {
            try {
                Files.delete(tmp);
            } catch (IOException e) {
                log.warn(e, e);
            }
        }
    }

    protected ByteArrayInputStream getStreamInternal(String key) {
        ByteRange byteRange;
        if (allowByteRange) {
            MutableObject<String> keyHolder = new MutableObject<>(key);
            byteRange = getByteRangeFromKey(keyHolder);
            key = keyHolder.getValue();
        } else {
            byteRange = null;
        }
        byte[] bytes = map.get(key);
        if (bytes == null) {
            return null;
        } else if (byteRange == null) {
            return new ByteArrayInputStream(bytes);
        } else {
            return new ByteArrayInputStream(bytes, (int) byteRange.getStart(), (int) byteRange.getLength());
        }
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        if (!emulateLocalFile) {
            return OptionalOrUnknown.unknown();
        }
        InputStream stream = getStreamInternal(key);
        if (stream == null) {
            return OptionalOrUnknown.missing();
        }
        try {
            Path tmp = Files.createTempFile("tmp_", ".tmp");
            Framework.trackFile(tmp.toFile(), tmp);
            FileUtils.copyToFile(stream, tmp.toFile());
            return OptionalOrUnknown.of(tmp);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        if (emulateNoStream) {
            return OptionalOrUnknown.unknown();
        }
        InputStream stream = getStreamInternal(key);
        if (stream == null) {
            return OptionalOrUnknown.missing();
        }
        return OptionalOrUnknown.of(stream);
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        InputStream stream = getStreamInternal(key);
        if (stream == null) {
            return false;
        }
        Files.copy(stream, dest, REPLACE_EXISTING);
        return true;
    }

    @Override
    public void deleteBlob(String key) {
        map.remove(key);
        legalHold.remove(key);
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    public class InMemoryBlobGarbageCollector extends AbstractBlobGarbageCollector {

        @Override
        public String getId() {
            return toString();
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (Iterator<Entry<String, byte[]>> it = map.entrySet().iterator(); it.hasNext();) {
                Entry<String, byte[]> es = it.next();
                String key = es.getKey();
                byte[] bytes = es.getValue();
                if (marked.contains(key)) {
                    status.sizeBinaries += bytes.length;
                    status.numBinaries++;
                } else {
                    status.sizeBinariesGC += bytes.length;
                    status.numBinariesGC++;
                    if (delete) {
                        it.remove();
                        legalHold.remove(key);
                    }
                }
            }
        }
    }

}
