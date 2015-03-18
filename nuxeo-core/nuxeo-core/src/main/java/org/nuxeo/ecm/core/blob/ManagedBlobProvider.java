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
import java.util.List;

/**
 * Blob provider that knows how to get the stream from its {@link ManagedBlob}'s key, and do further blob-related
 * functions.
 *
 * @since 7.2
 */
public interface ManagedBlobProvider extends BlobProvider {

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
     * Gets an {@link InputStream} for a conversion of a managed blob to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @param mimeType the MIME type to convert to
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     */
    InputStream getConvertedStream(ManagedBlob blob, String mimeType) throws IOException;

    /**
     * Gets the list of available MIME type conversions for a managed blob.
     *
     * @return a list of MIME types, which may be empty
     */
    List<String> getAvailableConversions(ManagedBlob blob) throws IOException;

}
