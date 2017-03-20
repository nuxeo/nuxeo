/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.model.Document;

/**
 * Service managing the storage and retrieval of {@link Blob}s, through internally-registered {@link BlobProvider}s.
 *
 * @since 7.2
 */
public interface BlobManager {

    /**
     * Class describing information from a {@link Blob}, suitable for serialization and storage.
     *
     * @since 7.2
     */
    class BlobInfo {
        public String key;

        public String mimeType;

        public String encoding;

        public String filename;

        public Long length;

        public String digest;

        /** Empty constructor. */
        public BlobInfo() {
        }

        /**
         * Copy constructor.
         *
         * @since 7.10
         */
        public BlobInfo(BlobInfo other) {
            key = other.key;
            mimeType = other.mimeType;
            encoding = other.encoding;
            filename = other.filename;
            length = other.length;
            digest = other.digest;
        }
    }

    /**
     * Hints for returning {@link URI}s appropriate for the expected usage.
     *
     * @since 7.3
     */
    enum UsageHint {
        /** Obtaining an {@link InputStream}. */
        STREAM, //
        /** Downloading. */
        DOWNLOAD, //
        /** Viewing. */
        VIEW, //
        /** Editing. */
        EDIT, //
        /** Embedding / previewing. */
        EMBED
    }

    /**
     * Gets the blob provider with the given id.
     *
     * @param id the blob provider id
     * @return the blob provider
     */
    BlobProvider getBlobProvider(String id);

    /**
     * Gets the blob provider for the given blob.
     *
     * @return the blob provider
     * @since 7.4
     */
    BlobProvider getBlobProvider(Blob blob);

    /**
     * Reads a {@link Blob} from storage.
     *
     * @param blobInfo the blob information
     * @param repositoryName the repository to which this blob belongs
     * @return a managed blob
     */
    Blob readBlob(BlobInfo blobInfo, String repositoryName) throws IOException;

    /**
     * Writes a {@link Blob} to storage and returns its key.
     *
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @return the blob key
     * @deprecated since 9.1, use {@link #writeBlob(Blob, Document, String)} instead
     */
    @Deprecated
    default String writeBlob(Blob blob, Document doc) throws IOException {
        return writeBlob(blob, doc, null);
    }

    /**
     * Writes a {@link Blob} to storage and returns its key.
     *
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @param xpath the xpath of blob in doc
     * @return the blob key
     * @since 9.1
     */
    String writeBlob(Blob blob, Document doc, String xpath) throws IOException;

    /**
     * INTERNAL - Gets an {@link InputStream} for the data of a managed blob. Used by internal implementations, regular
     * callers should call {@link Blob#getStream}.
     *
     * @param blob the blob
     * @return the stream
     */
    InputStream getStream(Blob blob) throws IOException;

    /**
     * Gets an {@link InputStream} for a thumbnail of a blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the blob
     * @return the thumbnail stream
     */
    InputStream getThumbnail(Blob blob) throws IOException;

    /**
     * Gets an {@link URI} for the content of a blob.
     *
     * @param blob the blob
     * @param hint {@link UsageHint}
     * @param servletRequest the servlet request, or {@code null}
     * @return the {@link URI}, or {@code null} if none available
     */
    URI getURI(Blob blob, UsageHint hint, HttpServletRequest servletRequest) throws IOException;

    /**
     * Gets a map of available MIME type conversions and corresponding {@link URI} for a blob.
     *
     * @return a map of MIME types and {@link URI}, which may be empty
     */
    Map<String, URI> getAvailableConversions(Blob blob, UsageHint hint) throws IOException;

    /**
     * Gets an {@link InputStream} for a conversion to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the blob
     * @param mimeType the MIME type to convert to
     * @param doc the document that holds the blob
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     */
    InputStream getConvertedStream(Blob blob, String mimeType, DocumentModel doc) throws IOException;

    /**
     * Get the map of blob providers
     *
     * @return the list of blob providers
     * @since 7.3
     */
    Map<String, BlobProvider> getBlobProviders();

    /**
     * Freezes the blobs' versions on a document version when it is created via a check in.
     *
     * @param doc the new document version
     * @since 7.3
     */
    void freezeVersion(Document doc);

    /**
     * Notifies the blob manager that a set of xpaths have changed on a document.
     *
     * @param doc the document
     * @param xpaths the set of changed xpaths
     * @since 7.3
     */
    void notifyChanges(Document doc, Set<String> xpaths);

    /**
     * Garbage collect the unused binaries.
     *
     * @param delete if {@code false} don't actually delete the garbage collected binaries (but still return statistics
     *            about them), if {@code true} delete them
     * @return a status about the number of garbage collected binaries
     * @since 7.4
     */
    BinaryManagerStatus garbageCollectBinaries(boolean delete);

    /**
     * Checks if a garbage collection of the binaries in progress.
     *
     * @return {@code true} if a garbage collection of the binaries is in progress
     * @since 7.4
     */
    boolean isBinariesGarbageCollectionInProgress();

    /**
     * INTERNAL. Marks a binary as referenced during garbage collection. Called back by repository implementations
     * during {@link #garbageCollectBinaries}.
     *
     * @param key the binary key
     * @param repositoryName the repository name
     * @since 7.4
     */
    void markReferencedBinary(String key, String repositoryName);

}
