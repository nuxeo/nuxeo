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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;

/**
 * Dummy storage in memory.
 */
public class DummyBlobProvider implements BlobProvider {

    protected Map<String, byte[]> blobs;

    protected AtomicLong counter;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) {
        blobs = new HashMap<>();
        counter = new AtomicLong();
    }

    @Override
    public void close() {
        blobs.clear();
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) {
        return new SimpleManagedBlob(blobInfo) {
            private static final long serialVersionUID = 1L;

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
    public String writeBlob(Blob blob, Document doc) throws IOException {
        byte[] bytes;
        try (InputStream in = blob.getStream()) {
            bytes = IOUtils.toByteArray(in);
        }
        String k = String.valueOf(counter.incrementAndGet());
        blobs.put(k, bytes);
        return k;
    }

}
