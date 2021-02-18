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
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;

import java.io.File;

/**
 * Simple managed blob implementation holding just a key and delegating to its provider for implementation.
 *
 * @since 7.2
 */
public class SimpleManagedBlob extends AbstractBlob implements ManagedBlob {

    private static final long serialVersionUID = 1L;

    public final String blobProviderId;

    public final String key;

    public Long length;

    public SimpleManagedBlob(String blobProviderId, BlobInfo blobInfo) {
        this.blobProviderId = blobProviderId;
        key = blobInfo.key;
        setMimeType(blobInfo.mimeType);
        setEncoding(blobInfo.encoding);
        setFilename(blobInfo.filename);
        setDigest(blobInfo.digest);
        length = blobInfo.length;
    }

    public SimpleManagedBlob(BlobInfo blobInfo) {
        this(blobProviderIdFromKey(blobInfo.key), blobInfo);
    }

    /**
     * Returns a copy of this blob with a new key and digest.
     *
     * @since 11.5
     */
    public SimpleManagedBlob withKeyAndDigest(String newKey, String newDigest) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = newKey;
        blobInfo.mimeType = mimeType;
        blobInfo.encoding = encoding;
        blobInfo.filename = filename;
        blobInfo.length = length;
        blobInfo.digest = newDigest;
        return new SimpleManagedBlob(blobProviderId, blobInfo);
    }

    @Override
    public String getProviderId() {
        return blobProviderId;
    }

    @Override
    public String getKey() {
        return key;
    }

    protected static String blobProviderIdFromKey(String key) {
        int colon = key.indexOf(':');
        if (colon < 0) {
            // no prefix
            throw new IllegalArgumentException("Invalid managed blob key: " + key);
        }
        return key.substring(0, colon);
    }

    @Override
    public File getFile() {
        return ManagedBlob.super.getFile();
    }

    @Override
    public long getLength() {
        return length == null ? -1 : length.longValue();
    }

}
