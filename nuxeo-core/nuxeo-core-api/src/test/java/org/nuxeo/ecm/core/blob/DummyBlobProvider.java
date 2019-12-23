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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Dummy storage in memory.
 */
public class DummyBlobProvider extends AbstractBlobProvider {

    protected Map<String, byte[]> blobs;

    protected AtomicLong counter;

    protected static List<AtomicLong> COUNTERS = new ArrayList<>();

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        blobs = new HashMap<>();
        counter = new AtomicLong();
        COUNTERS.add(counter);
    }

    public static void resetAllCounters() {
        for (AtomicLong counter : COUNTERS) {
            counter.set(0);
        }
    }

    @Override
    public void close() {
        blobs.clear();
        COUNTERS.remove(counter);
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        String key = blobInfo.key;
        int colon = key.indexOf(':');
        String k = colon < 0 ? key : key.substring(colon + 1);
        if (!blobs.containsKey(k)) {
            throw new IOException("Unknown blob: " + key);
        }
        return new SimpleManagedBlob(blobProviderId, blobInfo) {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getStream() throws IOException {
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

}
