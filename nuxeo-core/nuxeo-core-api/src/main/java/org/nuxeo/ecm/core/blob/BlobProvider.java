/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 *     Gabriel Barata
 */
package org.nuxeo.ecm.core.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;

/**
 * Interface for a provider of {@link Blob}s, which knows how to read and write them.
 *
 * @since 7.2
 */
public interface BlobProvider {

    /**
     * Initializes the blob provider.
     *
     * @param blobProviderId the blob provider id for this binary manager
     * @param properties initialization properties
     *
     * @since 7.3
     */
    void initialize(String blobProviderId, Map<String, String> properties) throws IOException;

    /**
     * Closes this blob provider and releases resources that may be held by it.
     *
     * @since 7.3
     */
    void close();

    /**
     * Checks whether this blob provider uses "record mode".
     * <p>
     * Record mode has the following characteristics:
     * <ul>
     * <li>transactional (blobs aren't actually written/deleted until the transaction commits, and transaction rollback
     * is possible),
     * <li>can replace or delete a document's blob.
     * </ul>
     *
     * @since 11.1
     */
    default boolean isRecordMode() {
        return false;
    }

    /**
     * Checks whether this blob provider is transactional.
     * <p>
     * A transactional blob provider only writes blobs to final storage at commit time.
     *
     * @since 11.1
     */
    default boolean isTransactional() {
        return false;
    }

    /**
     * Checks whether this blob provider is transient: blobs may disappear after a while, so a caller should not rely on
     * them being available forever.
     *
     * @since 10.1
     */
    default boolean isTransient() {
        return false;
    }

    /**
     * Checks whether this blob provider allows byte ranges in keys.
     *
     * @since 11.1
     */
    default boolean allowByteRange() {
        return false;
    }

    /**
     * Reads a {@link Blob} from storage.
     *
     * @param blobInfoContext the blob information context
     * @return the blob
     * @since 11.1
     */
    default Blob readBlob(BlobInfoContext blobInfoContext) throws IOException {
        return readBlob(blobInfoContext.blobInfo);
    }

    /**
     * Reads a {@link Blob} from storage.
     *
     * @param blobInfo the blob information
     * @return the blob
     */
    Blob readBlob(BlobInfo blobInfo) throws IOException;

    /**
     * Writes a {@link Blob} to storage and returns information about it.
     * <p>
     * Called to store a user-created blob.
     *
     * @param blobContext the blob context
     * @return the blob key
     * @since 11.1
     */
    default String writeBlob(BlobContext blobContext) throws IOException {
        return writeBlob(blobContext.blob);
    }

    /**
     * Writes a {@link Blob} to storage and returns information about it.
     * <p>
     * Called to store a user-created blob.
     *
     * @param blob the blob
     * @return the blob key
     * @since 9.2
     */
    String writeBlob(Blob blob) throws IOException;

    /**
     * Updates a blob's properties in storage.
     *
     * @param blobUpdateContext the blob update context
     * @since 11.1
     */
    default void updateBlob(BlobUpdateContext blobUpdateContext) throws IOException {
        // ignore properties updates by default
    }

    /**
     * Deletes a blob from storage. Only meaningful for a record blob provider.
     *
     * @param blobContext the blob context
     * @see #isRecordMode
     * @since 11.1
     */
    default void deleteBlob(BlobContext blobContext) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the status of a blob.
     *
     * @param blob the blob
     * @since 11.1
     */
    default BlobStatus getStatus(ManagedBlob blob) throws IOException {
        return new BlobStatus();
    }

    /**
     * Checks if user update is supported.
     * <p>
     * A user update refers to the fact that a blob from this provider may be overwritten with another blob, wherever
     * the original blob may occur (usually in a document property).
     *
     * @return {@code true} if user update is supported
     * @since 7.10
     */
    boolean supportsUserUpdate();

    /**
     * Checks if sync is supported.
     * <p>
     * Sync refers to the fact that a blob from this provider may be synced with a remote system (like Nuxeo Drive) or
     * with a process that updates things in the blob (like Binary Metadata, or WOPI).
     *
     * @return {@code true} if sync is supported
     * @since 11.1
     */
    default boolean supportsSync() {
        return supportsUserUpdate() && getBinaryManager() != null;
    }

    /**
     * Gets an {@link InputStream} for a byte range of a managed blob.
     *
     * @param blobKey the blob key
     * @param byteRange the byte range
     * @return the stream
     * @since 11.1
     */
    default InputStream getStream(String blobKey, ByteRange byteRange) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets an {@link InputStream} for the data of a managed blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @return the stream
     * @since 7.3
     */
    default InputStream getStream(ManagedBlob blob) throws IOException {
        return null;
    }

    /**
     * Gets a {@link File} (if one exists) for the data of a managed blob.
     *
     * @param blob the managed blob
     * @return the file, or {@code null} if no underlying file is available
     * @since 11.1
     */
    default File getFile(ManagedBlob blob) {
        return null;
    }

    /**
     * Gets an {@link InputStream} for a thumbnail of a managed blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @return the stream
     * @since 7.3
     */
    default InputStream getThumbnail(ManagedBlob blob) throws IOException {
        return null;
    }

    /**
     * Gets an {@link URI} for the content of a managed blob.
     *
     * @param blob the managed blob
     * @param hint {@link UsageHint}
     * @param servletRequest the servlet request, or {@code null}
     * @return the {@link URI}, or {@code null} if none available
     * @since 7.4
     */
    default URI getURI(ManagedBlob blob, UsageHint hint, HttpServletRequest servletRequest) throws IOException {
        return null;
    }

    /**
     * Gets a map of available MIME type conversions and corresponding {@link URI} for a managed blob.
     *
     * @param blob the managed blob
     * @param hint {@link UsageHint}
     * @return a map of MIME types and {@link URI}, which may be empty
     * @since 7.3
     */
    default Map<String, URI> getAvailableConversions(ManagedBlob blob, UsageHint hint) throws IOException {
        return Collections.emptyMap();
    }

    /**
     * Checks if the conversion to the given {@code mimeType} is supported by the {@code blob}.
     *
     * @param blob the managed blob
     * @param mimeType the destination mime type
     * @return {@code true} if this managed blob supports the conversion to the given mime type
     * @since 10.1
     */
    default boolean canConvert(ManagedBlob blob, String mimeType) {
        try {
            Map<String, URI> availableConversions = getAvailableConversions(blob, UsageHint.STREAM);
            return availableConversions.containsKey(mimeType);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Returns true if version of the blob is a version.
     * <p>
     *
     * @param blob the managed blob
     * @return true if the blob is a version or a revision
     * @since 7.3
     */
    default boolean isVersion(ManagedBlob blob) {
        return false;
    }

    /**
     * Returns a list of application links for the given blob.
     *
     * @since 7.3
     */
    default List<AppLink> getAppLinks(String user, ManagedBlob blob) throws IOException {
        return Collections.emptyList();
    }

    /**
     * Gets the associated binary manager, if any.
     *
     * @return the binary manager, or {@code null}
     * @since 7.4
     */
    default BinaryManager getBinaryManager() {
        return null;
    }

    /**
     * Gets the associated garbage collector, if any.
     *
     * @return the garbage collector, or {@code null}
     * @since 11.1
     */
    default BinaryGarbageCollector getBinaryGarbageCollector() {
        BinaryManager binaryManager = getBinaryManager();
        return binaryManager == null ? null : binaryManager.getGarbageCollector();
    }

    /**
     * Checks if the blob provider performs external access control checks.
     *
     * @param blobInfo the blob information to be read
     * @return {@code true} if the provider performs security checks before reading a blob, {@code false} otherwise
     * @since 8.4
     */
    default boolean performsExternalAccessControl(BlobInfo blobInfo) {
        return false;
    }

    /**
     * Returns the properties of the blob provider.
     *
     * @since 10.2
     */
    Map<String, String> getProperties();

    /**
     * Checks if current user has the rights to create blobs in the blob provider using a key.
     *
     * @since 10.2
     */
    default boolean hasCreateFromKeyPermission() {
        return false;
    }

}
