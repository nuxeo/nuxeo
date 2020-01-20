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
package org.nuxeo.ecm.core.opencmis.impl;

import static org.nuxeo.ecm.core.blob.binary.AbstractBinaryManager.MD5_DIGEST;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;

/**
 * Dummy storage in memory.
 */
public class DummyCmisBlobProvider extends AbstractBlobProvider {

    protected Map<String, byte[]> blobs;

    protected AtomicLong counter;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        blobs = new HashMap<>();
        counter = new AtomicLong();
    }

    @Override
    public void close() {
        blobs.clear();
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) {
        return new SimpleManagedBlob(blobProviderId, blobInfo) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getDigestAlgorithm() {
                return MD5_DIGEST;
            }

            @Override
            public String getDigest() {
                String digest = super.getDigest();
                if (digest == null) {
                    try {
                        digest = DigestUtils.md5Hex(getStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return digest;
            }

            @Override
            public InputStream getStream() throws IOException {
                int colon = key.indexOf(':');
                String k = colon < 0 ? key : key.substring(colon + 1);
                byte[] bytes = blobs.get(k);
                return new ByteArrayInputStream(bytes);
            }
        };
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        byte[] bytes;
        try (InputStream in = blob.getStream()) {
            bytes = IOUtils.toByteArray(in);
        }
        String k = String.valueOf(counter.incrementAndGet());
        blobs.put(k, bytes);
        return k;
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest request) throws IOException {
        if (usage != UsageHint.DOWNLOAD) {
            return null;
        }
        if (request.getParameter("testredirect") == null) {
            return null;
        }
        return URI.create("http://example.com/dummyedirect");
    }

}
