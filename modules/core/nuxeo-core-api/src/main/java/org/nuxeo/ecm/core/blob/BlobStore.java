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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;

/**
 * Interface for basic access to storage of a Blob (read/write/copy/delete).
 * <p>
 * A blob is identified by a key, and holds a stream of bytes. It may have some associated metadata (filename, content
 * type).
 * <p>
 * A blob store may have versioning. When this is the case, the write method will return a key that includes a version
 * number. This same complete key must subsequently be provided to the read or delete methods. With versioning, two
 * writes may be requested with the same key in the blob context, and both will succeed because they return keys that
 * include a version number to distinguish them.
 *
 * @since 11.1
 */
public interface BlobStore {

    /** Name used for debugging / tracing. */
    String getName();

    /**
     * Whether this blob store has versioning.
     * <p>
     * With versioning, two writes may use the same key. The returned keys will include a different version number to
     * distinguish the writes.
     */
    boolean hasVersioning();

    /**
     * Gets the key strategy used by the store.
     */
    KeyStrategy getKeyStrategy();

    /**
     * Writes a blob.
     *
     * @param blobContext the blob context
     * @return the blob key
     */
    String writeBlob(BlobContext blobContext) throws IOException;

    /**
     * Writes a blob.
     * <p>
     * Note that the returned key may be different than the one requested by the {@link BlobWriteContext}, if the blob
     * store needs additional version info to retrieve it.
     *
     * @param blobWriteContext the context of the blob write, including the blob
     * @return the key to use to read this blob in the future
     */
    String writeBlob(BlobWriteContext blobWriteContext) throws IOException;

    /**
     * Checks if blob copy/move from another blob store to this one can be done efficiently.
     *
     * @param sourceStore the source store
     * @return {@code true} if the copy/move can be done efficiently
     */
    boolean copyBlobIsOptimized(BlobStore sourceStore);

    /**
     * Writes a file based on a key, as a copy/move from a source in another blob store.
     * <p>
     * If the copy/move is requested to be atomic, then the destination file is created atomically. In case of atomic
     * move, in some stores the destination will be created atomically but the source will only be deleted afterwards.
     *
     * @param key the key
     * @param sourceStore the source store
     * @param sourceKey the source key
     * @param atomicMove {@code true} for an atomic move, {@code false} for a regular copy
     * @return {@code true} if the file was found in the source store, {@code false} if it was not found
     */
    boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove) throws IOException;

    /**
     * A class representing an unknown value, a missing value, or a present (non-null) value.
     */
    class OptionalOrUnknown<T> {

        private static final OptionalOrUnknown<?> UNKNOWN = new OptionalOrUnknown<>();

        private static final OptionalOrUnknown<?> MISSING = new OptionalOrUnknown<>(null);

        private Optional<T> value;

        // constructor for unknown value
        private OptionalOrUnknown() {
            this.value = null;
        }

        // constructor for missing or present value
        private OptionalOrUnknown(T value) {
            this.value = value == null ? Optional.empty() : Optional.of(value);
        }

        /**
         * Returns an unknown instance.
         */
        public static <T> OptionalOrUnknown<T> unknown() {
            @SuppressWarnings("unchecked")
            OptionalOrUnknown<T> t = (OptionalOrUnknown<T>) UNKNOWN;
            return t;
        }

        /**
         * Returns a missing instance.
         */
        public static <T> OptionalOrUnknown<T> missing() {
            @SuppressWarnings("unchecked")
            OptionalOrUnknown<T> t = (OptionalOrUnknown<T>) MISSING;
            return t;
        }

        /**
         * Returns an instance of a present, non-null value.
         */
        public static <T> OptionalOrUnknown<T> of(T value) {
            return new OptionalOrUnknown<>(Objects.requireNonNull(value));
        }

        /**
         * Returns {@code true} if the value is unknown, otherwise {@code false}.
         */
        public boolean isUnknown() {
            return value == null; // NOSONAR
        }

        /**
         * Returns {@code true} if the value is known (missing or present), otherwise {@code false}.
         */
        public boolean isKnown() {
            return value != null; // NOSONAR
        }

        /**
         * Returns {@code true} if the value is present, otherwise {@code false}.
         */
        public boolean isPresent() {
            return value != null && value.isPresent(); // NOSONAR
        }

        /**
         * Returns {@code true} if the value is missing, otherwise {@code false}.
         */
        public boolean isMissing() {
            return value != null && !value.isPresent(); // NOSONAR
        }

        /**
         * Returns the value if it is present, otherwise throws {@code NoSuchElementException}.
         *
         * @throws NoSuchElementException if there is no value present
         */
        @NotNull
        public T get() {
            if (value == null || !value.isPresent()) { // NOSONAR
                throw new NoSuchElementException("No value known and present");
            }
            return value.get();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OptionalOrUnknown)) {
                return false;
            }
            OptionalOrUnknown<?> other = (OptionalOrUnknown<?>) obj;
            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /**
     * Gets an already-existing file containing the blob for the given key, if present.
     * <p>
     * Note that this method is best-effort, it may return unknown even though the blob exists in the store, it's just
     * that it's not handily available locally in a file.
     *
     * @param key the blob key
     * @return the file containing the blob, or empty if the blob cannot be found, or unknown if no file is available
     *         locally
     */
    @NotNull
    OptionalOrUnknown<Path> getFile(String key);

    /**
     * Gets the stream of the blob for the given key, if present.
     * <p>
     * Note that this method is best-effort, it may return unknown even though the blob exists in the store, it's just
     * that it's not efficient to return it as a stream.
     *
     * @param key the blob key
     * @return the blob stream, or empty if the blob cannot be found, or unknown if no stream is efficiently available
     */
    @NotNull
    OptionalOrUnknown<InputStream> getStream(String key) throws IOException;

    /**
     * Reads a blob based on its key into the given file.
     *
     * @param key the blob key
     * @param dest the file to use to store the fetched data
     * @return {@code true} if the file was fetched, {@code false} if the file was not found
     */
    boolean readBlob(String key, Path dest) throws IOException;

    /**
     * Sets properties on a blob.
     *
     * @param blobUpdateContext the blob update context
     */
    void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException;

    /**
     * Deletes a blob.
     *
     * @param blobContext the blob context
     */
    void deleteBlob(BlobContext blobContext);

    /**
     * Deletes a blob based on a key. No error occurs if the blob does not exist.
     * <p>
     * This method does not throw {@link IOException}, but may log an error message.
     *
     * @param key the blob key
     */
    void deleteBlob(String key);

    /**
     * Returns the binary garbage collector (GC).
     * <p>
     * Several calls to this method will return the same GC, so that its status can be monitored using
     * {@link BinaryGarbageCollector#isInProgress}.
     *
     * @return the binary GC
     */
    BinaryGarbageCollector getBinaryGarbageCollector();

    /**
     * If this blob store wraps another one, returns it, otherwise returns this.
     *
     * @return the lowest-level blob store
     */
    BlobStore unwrap();

}
