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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.KeyStrategy.WriteObserver;

/**
 * Basic helper implementations for a {@link BlobStore}.
 *
 * @since 11.1
 */
public abstract class AbstractBlobStore implements BlobStore {

    private static final Logger log = LogManager.getLogger(AbstractBlobStore.class);

    protected final String name;

    protected final KeyStrategy keyStrategy;

    public AbstractBlobStore(String name, KeyStrategy keyStrategy) {
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
        return unwrapped.copyBlobIsOptimized(sourceStore.unwrap());
    }

    protected String stripBlobKeyPrefix(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        return key;
    }

    /** Returns a random string suitable as a key. */
    protected String randomString() {
        return String.valueOf(randomLong());
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

}
