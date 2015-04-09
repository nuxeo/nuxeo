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
package org.nuxeo.ecm.core.storage.binary;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.model.Document;

/**
 * Adapter between the {@link BinaryManager} and a {@link BlobProvider} for the {@link BlobManager}.
 *
 * @since 7.2
 */
public class BinaryBlobProvider implements BlobProvider {

    protected BinaryManagerService binaryManagerService;

    public BinaryBlobProvider(BinaryManagerService binaryManagerService) {
        this.binaryManagerService = binaryManagerService;
    }

    @Override
    public BinaryBlob createManagedBlob(String repositoryName, BlobInfo blobInfo, Document doc) throws IOException {
        Binary binary = binaryManagerService.getBinaryManager(repositoryName).getBinary(blobInfo.key);
        if (binary == null) {
            throw new IOException("Unknown binary: " + blobInfo.key);
        }
        return new BinaryBlob(binary, blobInfo.filename, blobInfo.mimeType, blobInfo.encoding, blobInfo.digest,
                binary.getLength()); // use binary length, authoritative
    }

    @Override
    public BlobInfo getBlobInfo(String repositoryName, Blob blob, Document doc) throws IOException {
        Binary binary = binaryManagerService.getBinaryManager(repositoryName).getBinary(blob);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = binary.getDigest();
        blobInfo.length = Long.valueOf(binary.getLength()); // use binary length, authoritative
        blobInfo.mimeType = blob.getMimeType();
        blobInfo.encoding = blob.getEncoding();
        blobInfo.filename = blob.getFilename();
        blobInfo.digest = blob.getDigest();
        return blobInfo;
    }

    @Override
    public InputStream getStream(String blobKey, URI uri) throws IOException {
        return null;
    }

    @Override
    public URI getURI(ManagedBlob blob, ManagedBlob.UsageHint hint) throws IOException {
        return null;
    }

    @Override
    public Map<String, URI> getAvailableConversions(ManagedBlob blob, ManagedBlob.UsageHint hint) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public URI getThumbnail(ManagedBlob blob, ManagedBlob.UsageHint hint) throws IOException {
        return null;
    }

}
