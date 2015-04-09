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
import java.net.URI;
import java.util.Map;

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

    public transient BlobProvider blobProvider;

    public SimpleManagedBlob(BlobInfo blobInfo, BlobProvider blobProvider) {
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

    @Override
    public InputStream getStream() throws IOException {
        URI uri = getURI(UsageHint.STREAM);
        return getBlobProvider().getStream(key, uri);
    }

    @Override
    public URI getURI(UsageHint hint) throws IOException {
        return getBlobProvider().getURI(this, hint);
    }

    @Override
    public InputStream getConvertedStream(String mimeType) throws IOException {
        Map<String, URI> conversions = getAvailableConversions(UsageHint.STREAM);
        URI uri = conversions.get(mimeType);
        if (uri == null) {
            return null;
        }
        return getBlobProvider().getStream(key, uri);
    }

    @Override
    public Map<String, URI> getAvailableConversions(UsageHint hint) throws IOException {
        return getBlobProvider().getAvailableConversions(this, hint);
    }

    @Override
    public InputStream getThumbnail() throws IOException {
        URI uri = getBlobProvider().getThumbnail(this, UsageHint.STREAM);
        return getBlobProvider().getStream(key, uri);
    }

    public BlobProvider getBlobProvider() {
        if (blobProvider == null) {
            // after deserialization the transient will be null
            blobProvider = Framework.getService(BlobManager.class).getBlobProvider(key);
        }
        return blobProvider;
    }

    @Override
    public long getLength() {
        return length == null ? -1 : length.longValue();
    }

}
