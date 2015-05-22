/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
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
    }

    /**
     * Hints for returning {@link URI}s appropriate for the expected usage.
     *
     * @since 7.3
     */
    enum UsageHint {
        /** Obtaining an {@link InputStream}. */
        STREAM,
        /** Downloading. */
        DOWNLOAD,
        /** Viewing. */
        VIEW,
        /** Editing. */
        EDIT,
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
     */
    String writeBlob(Blob blob, Document doc) throws IOException;

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
     * @return the {@link URI}, or {@code null} if none available
     */
    URI getURI(Blob blob, UsageHint hint) throws IOException;

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
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     */
    InputStream getConvertedStream(Blob blob, String mimeType) throws IOException;

    /**
     * Get the map of blob providers
     *
     * @return the list of blob providers
     * @since 7.3
     */
    Map<String, BlobProvider> getBlobProviders();

}
