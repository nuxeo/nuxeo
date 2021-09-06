/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob.binary;

import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.TRANSIENT;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.DownloadBlobGuard;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;

/**
 * Adapter between the {@link BinaryManager} and a {@link BlobProvider} for the {@link BlobManager}.
 * <p>
 * Can be used by legacy implementations of a {@link BinaryManager} to provide a {@link BlobProvider} implementation.
 *
 * @since 7.3
 */
public class BinaryBlobProvider implements BlobProvider {

    private static final Log log = LogFactory.getLog(BinaryBlobProvider.class);

    protected final BinaryManager binaryManager;

    protected boolean supportsUserUpdate;

    protected boolean transientFlag;

    public BinaryBlobProvider(BinaryManager binaryManager) {
        this.binaryManager = binaryManager;
    }

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        binaryManager.initialize(blobProviderId, properties);
        supportsUserUpdate = supportsUserUpdateDefaultTrue(properties);
        transientFlag = Boolean.parseBoolean(properties.get(TRANSIENT));
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdate;
    }

    protected boolean supportsUserUpdateDefaultTrue(Map<String, String> properties) {
        return !Boolean.parseBoolean(properties.get(PREVENT_USER_UPDATE));
    }

    @Override
    public boolean isTransient() {
        return transientFlag;
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

    protected String stripBlobKeyPrefix(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        return key;
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        String digest = stripBlobKeyPrefix(blobInfo.key);
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new IOException("Unknown binary: " + digest);
        }
        long length;
        if (blobInfo.length != null) {
            length = blobInfo.length;
        } else if (DownloadBlobGuard.isEnable()) {
            length = -1;
        } else {
            log.info("Missing content length for blob key: " + blobInfo.key
                    + ", get the length from the binary's file (may be costly)");
            File file = binary.getFile();
            length = file == null ? -1 : file.length();
        }
        BinaryBlob blob = new BinaryBlob(binary, blobInfo.key, blobInfo.filename, blobInfo.mimeType, blobInfo.encoding,
                null, blobInfo.digest, length);
        fixupDigest(blob, digest);
        return blob;
    }

    @Override
    public File getFile(ManagedBlob blob) {
        String key = stripBlobKeyPrefix(blob.getKey());
        Binary binary = binaryManager.getBinary(key);
        if (binary == null || binary instanceof LazyBinary) {
            return null;
        }
        return binary.getFile();
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        // writes the blob and return its digest
        String digest = binaryManager.getBinary(blob).getDigest();
        fixupDigest(blob, digest);
        return digest;
    }

    /**
     * Fixup of the blob's digest, if possible.
     *
     * @param blob the blob
     * @param digest the digest
     * @since 11.5
     */
    protected void fixupDigest(Blob blob, String digest) {
        if (binaryManager instanceof AbstractBinaryManager) {
            AbstractBinaryManager bm = (AbstractBinaryManager) binaryManager;
            String currentDigest = blob.getDigest();
            if (currentDigest == null || currentDigest.contains("-")) {
                // missing or temporary digest
                blob.setDigest(digest);
                currentDigest = digest;
            }
            if (blob.getDigestAlgorithm() == null && bm.isValidDigest(currentDigest)) {
                blob.setDigestAlgorithm(bm.getDigestAlgorithm());
            }
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

}
