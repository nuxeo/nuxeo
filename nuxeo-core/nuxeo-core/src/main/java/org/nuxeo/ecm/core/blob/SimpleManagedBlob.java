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

import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple managed blob implementation holding just a key and delegating to its provider for implementation.
 *
 * @since 7.2
 */
public class SimpleManagedBlob extends AbstractBlob implements ManagedBlob {

    private static final long serialVersionUID = 1L;

    public final String key;

    public Long length;

    public transient ManagedBlobProvider blobProvider;

    public SimpleManagedBlob(BlobInfo blobInfo, ManagedBlobProvider blobProvider) {
        this.key = blobInfo.key;
        this.blobProvider = blobProvider;
        setMimeType(blobInfo.mimeType);
        setEncoding(blobInfo.encoding);
        setFilename(blobInfo.filename);
        setDigest(blobInfo.digest);
        length = blobInfo.length;
    }

    @Override
    public String getKey() {
        return key;
    }

    protected ManagedBlobProvider getBlobProvider() {
        if (blobProvider == null) {
            // after deserialization the transient will be null
            blobProvider = (ManagedBlobProvider) Framework.getService(BlobManager.class).getBlobProvider(key);
        }
        return blobProvider;
    }

    @Override
    public InputStream getStream() throws IOException {
        return getBlobProvider().getStream(this);
    }

    @Override
    public long getLength() {
        return length == null ? -1 : length.longValue();
    }

}
