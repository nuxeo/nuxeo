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
package org.nuxeo.ecm.googleclient;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.ManagedBlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.model.Document;

/**
 * Provider for blobs getting information from Google Drive.
 *
 * @since 7.2
 */
public class GoogleDriveBlobProvider implements ManagedBlobProvider {

    @Override
    public SimpleManagedBlob createManagedBlob(String repositoryName, BlobInfo blobInfo, Document doc)
            throws IOException {
        return new SimpleManagedBlob(blobInfo, this);
    }

    @Override
    public BlobInfo getBlobInfo(String repositoryName, Blob blob, Document doc) {
        throw new UnsupportedOperationException("Storing a standard blob is not supported");
    }

    @Override
    public InputStream getStream(ManagedBlob blob) {
        String key = blob.getKey();

        throw new UnsupportedOperationException("TODO");
    }

}
