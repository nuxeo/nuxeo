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
     * INTERNAL - Registers a blob provider.
     *
     * @param prefix the blob provider prefix, or {@code null} for the default
     * @param blobProvider the blob provider
     */
    void registerBlobProvider(String prefix, BlobProvider blobProvider);

    /**
     * INTERNAL - Unregisters a blob provider.
     *
     * @param prefix the blob provider prefix, or {@code null} for the default
     */
    void unregisterBlobProvider(String prefix);

    /**
     * Gets the blob provider for a give managed blob key.
     *
     * @param key the managed blob key
     * @return the blob provider
     */
    BlobProvider getBlobProvider(String key);

    /**
     * Creates a blob from the given blob info.
     *
     * @param repositoryName the repository name
     * @param blobInfo the blob information
     * @param doc the document to which this blob belongs
     * @return a managed blob
     */
    ManagedBlob getBlob(String repositoryName, BlobInfo blobInfo, Document doc) throws IOException;

    /**
     * Gets the blob info from a blob.
     *
     * @param repositoryName the repository name
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @return the blob information
     */
    BlobInfo getBlobInfo(String repositoryName, Blob blob, Document doc) throws IOException;

}
