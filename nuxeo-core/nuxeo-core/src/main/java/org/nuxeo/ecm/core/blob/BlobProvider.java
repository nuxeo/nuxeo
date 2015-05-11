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
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;

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
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @return the blob key
     */
    String writeBlob(Blob blob, Document doc) throws IOException;

}
