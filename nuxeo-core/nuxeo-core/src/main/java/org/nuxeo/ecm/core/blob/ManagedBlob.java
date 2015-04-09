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
 *     Nelson Silva
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.api.Blob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Interface for {@link Blob}s created and managed by the {@link BlobManager}.
 *
 * @since 7.2
 */
public interface ManagedBlob extends Blob {

    /**
     * Hints for returning {@link URI}s appropriate for the expected usage.
     *
     * @since 7.3
     */
    enum UsageHint {
        /** obtaining an {@link InputStream} */
        STREAM,
        /** downloading */
        DOWNLOAD,
        /** viewing */
        VIEW,
        /** editing */
        EDIT,
        /** embedding / previewing */
        EMBED
    }

    /**
     * Gets the stored representation of this blob.
     *
     * @return the stored representation
     */
    String getKey();

    /**
     * Gets an {@link java.io.InputStream} for the data of the blob.
     * <p>
     * Like all {@link java.io.InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @return the stream
     */
    InputStream getStream() throws IOException;

    /**
     * Gets an {@link java.net.URI} for the content of a the blob.
     *
     * @return the {@link java.net.URI
     */
    URI getURI(UsageHint hint) throws IOException;

    /**
     * Gets an {@link InputStream} for a conversion to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param mimeType the MIME type to convert to
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     */
    InputStream getConvertedStream(String mimeType) throws IOException;

    /**
     * Gets a map of available MIME type conversions and corresponding {@link URI}.
     *
     * @return a map of MIME types and {@link URI}, which may be empty
     */
    Map<String, URI> getAvailableConversions(UsageHint hint) throws IOException;

    /**
     * Gets an {@link InputStream} for a thumbnail of the blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @return the stream
     */
    InputStream getThumbnail() throws IOException;
}
