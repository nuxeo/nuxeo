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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;

/**
 * Interface for an extended provider of {@link Blob}s, which knows how to fetch alternate content.
 *
 * @since 7.3
 */
public interface ExtendedBlobProvider extends BlobProvider {

    /**
     * Gets an {@link InputStream} for the data of a managed blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @return the stream
     */
    InputStream getStream(ManagedBlob blob) throws IOException;

    /**
     * Gets an {@link InputStream} for a thumbnail of a managed blob.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @return the stream
     */
    InputStream getThumbnail(ManagedBlob blob) throws IOException;

    /**
     * Gets an {@link URI} for the content of a managed blob.
     *
     * @param blob the managed blob
     * @param hint {@link UsageHint}
     * @return the {@link URI}, or {@code null} if none available
     */
    URI getURI(ManagedBlob blob, UsageHint hint) throws IOException;

    /**
     * Gets a map of available MIME type conversions and corresponding {@link URI} for a managed blob.
     *
     * @param blob the managed blob
     * @param hint {@link UsageHint}
     * @return a map of MIME types and {@link URI}, which may be empty
     */
    Map<String, URI> getAvailableConversions(ManagedBlob blob, UsageHint hint) throws IOException;

    /**
     * Gets an {@link InputStream} for a conversion to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @param mimeType the MIME type to convert to
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     */
    InputStream getConvertedStream(ManagedBlob blob, String mimeType) throws IOException;

    /**
     * Returns a new managed blob pointing to a fixed version of the original blob.
     * <p>
     *
     * @param blob the original managed blob
     * @return a managed blob with fixed version, or {@code null} if no change is needed
     */
    ManagedBlob freezeVersion(ManagedBlob blob) throws IOException;

    /**
     * Returns true if version of the blob is a version.
     * <p>
     *
     * @param blob the managed blob
     * @return true if the blob is a version or a revision
     */
    boolean isVersion(ManagedBlob blob);

}
