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
package org.nuxeo.ecm.core.blob.binary;

import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.model.Document;

/**
 * Adapter between the {@link BinaryManager} and a {@link BlobProvider} for the {@link BlobManager}.
 * <p>
 * Can be used by legacy implementations of a {@link BinaryManager} to provide a {@link BlobProvider} implementation.
 *
 * @since 7.3
 */
public class BinaryBlobProvider implements BlobProvider {

    protected final BinaryManager binaryManager;

    protected boolean supportsUserUpdate;

    public BinaryBlobProvider(BinaryManager binaryManager) {
        this.binaryManager = binaryManager;
    }

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        binaryManager.initialize(blobProviderId, properties);
        supportsUserUpdate = supportsUserUpdateDefaultTrue(properties);
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdate;
    }

    protected boolean supportsUserUpdateDefaultTrue(Map<String, String> properties) {
        return !Boolean.parseBoolean(properties.get(PREVENT_USER_UPDATE));
    }

    /**
     * Closes the adapted {@link BinaryManager}.
     */
    @Override
    public void close() {
        binaryManager.close();
    }

    @Override
    public BinaryManager getBinaryManager() {
        return binaryManager;
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        String digest = blobInfo.key;
        // strip prefix
        int colon = digest.indexOf(':');
        if (colon >= 0) {
            digest = digest.substring(colon + 1);
        }
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new IOException("Unknown binary: " + digest);
        }
        // TODO disallow null length altogether
        long length = blobInfo.length == null ? binary.getLength() : blobInfo.length.longValue();
        return new BinaryBlob(binary, blobInfo.key, blobInfo.filename, blobInfo.mimeType, blobInfo.encoding,
                blobInfo.digest, length);
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        // writes the blob and return its digest
        return binaryManager.getBinary(blob).getDigest();
    }

}
