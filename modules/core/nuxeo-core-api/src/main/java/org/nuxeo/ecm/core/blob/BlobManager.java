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
 */
package org.nuxeo.ecm.core.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Service managing the storage and retrieval of {@link Blob}s, through internally-registered {@link BlobProvider}s.
 *
 * @since 7.2
 */
public interface BlobManager {

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
     * Gets the blob provider with the given id, or, if none has been registered, a namespaced version of the default
     * blob provider.
     *
     * @param id the blob provider id or namespace
     * @return the blob provider
     * @since 10.10
     * @deprecated since 11.1, use {@link #getBlobProviderWithNamespace(String, String)} instead
     */
    @Deprecated
    default BlobProvider getBlobProviderWithNamespace(String id) {
        return getBlobProviderWithNamespace(id, "default");
    }

    /**
     * Gets the blob provider with the given id, or, if none has been registered, a namespaced version of the blob
     * provider with the given default id.
     *
     * @param id the blob provider id or namespace
     * @param defaultId the blob provider to use as a fallback to create a namespaced version
     * @return the blob provider
     * @since 11.1
     */
    BlobProvider getBlobProviderWithNamespace(String id, String defaultId);

    /**
     * Gets the blob provider for the given blob.
     *
     * @return the blob provider
     * @since 7.4
     */
    BlobProvider getBlobProvider(Blob blob);

    /**
     * INTERNAL - Gets an {@link InputStream} for the data of a managed blob. Used by internal implementations, regular
     * callers should call {@link Blob#getStream}.
     *
     * @param blob the blob
     * @return the stream
     */
    InputStream getStream(Blob blob) throws IOException;

    /**
     * INTERNAL - Gets a {@link File} (if one exists) for the data of a managed blob. Used by internal implementations,
     * regular callers should call {@link Blob#getFile}.
     *
     * @param blob the blob
     * @return the file, or {@code null} if no underlying file is available
     * @since 11.1
     */
    File getFile(Blob blob);

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
     * Get the map of blob providers
     *
     * @return the list of blob providers
     * @since 7.3
     */
    Map<String, BlobProvider> getBlobProviders();

}
