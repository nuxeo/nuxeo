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
import java.nio.file.Path;

import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;

/**
 * Blob store that stores nothing, useful only when there's a cache in front of it; mostly for unit tests.
 *
 * @since 11.5
 */
public class EmptyBlobStore extends AbstractBlobStore {

    protected final EmptyBlobGarbageCollector gc = new EmptyBlobGarbageCollector();

    public EmptyBlobStore(String blobProviderId, String name, KeyStrategy keyStrategy) {
        super(blobProviderId, name, keyStrategy);
    }

    @Override
    protected String writeBlobGeneric(BlobWriteContext blobWriteContext) throws IOException {
        String key = blobWriteContext.getKey();
        return key == null ? randomString() : key;
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return false;
    }

    @Override
    public String copyOrMoveBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        if (atomicMove) {
            sourceStore.deleteBlob(sourceKey);
        }
        return key;
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.missing();
    }

    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        return OptionalOrUnknown.missing();
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        return false;
    }

    @Override
    public void deleteBlob(String key) {
        // nothing
    }

    @Override
    public void clear() {
        // nothing
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return gc;
    }

    public class EmptyBlobGarbageCollector extends AbstractBlobGarbageCollector {

        @Override
        public String getId() {
            return toString();
        }

        @Override
        public void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            // nothing
        }
    }

}
