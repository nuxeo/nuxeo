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
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;

/**
 * Interface for a provider of {@link Blob}s, which knows how to create them and fetch their content and additional
 * information.
 *
 * @since 7.2
 */
public interface BlobProvider {

    /**
     * Creates a managed blob.
     * <p>
     * Called to retrieve a managed blob from storage.
     *
     * @param repositoryName the repository name
     * @param blobInfo the blob information
     * @param doc the document to which this blob belongs
     * @return the managed blob
     */
    ManagedBlob createManagedBlob(String repositoryName, BlobInfo blobInfo, Document doc) throws IOException;

    /**
     * Gets information about a {@link Blob}.
     * <p>
     * Called to store a user-created blob.
     *
     * @param repositoryName the repository name
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @return the blob information
     */
    BlobInfo getBlobInfo(String repositoryName, Blob blob, Document doc) throws IOException;

}
