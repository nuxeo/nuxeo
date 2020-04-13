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
import org.nuxeo.runtime.api.Framework;

/**
 * Dummy storage in memory.
 */
public class DummyBlobProvider extends AbstractBlobProvider {

    protected static final String FROMDOC = "fromdoc";

    protected static final String FROMDOC2 = "fromdoc2";

    protected static final String MEMWITHBYTERANGE = "memwithbyterange";

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
    public Blob readBlob(BlobInfoContext blobInfoContext) throws IOException {
        if (blobProviderId.equals(FROMDOC)) {
            return readBlobFromDoc(blobInfoContext);
        }
        if (blobProviderId.equals(FROMDOC2)) {
            return readBlobFromDoc2(blobInfoContext);
        }
        return readBlob(blobInfoContext.blobInfo);
    }

    // special blob provider using the document to resolve the blob
    protected Blob readBlobFromDoc(BlobInfoContext blobInfoContext) throws IOException {
        BlobInfo blobInfo = blobInfoContext.blobInfo;
        String key = blobInfo.key;
        int colon = key.indexOf(':');
        String k = colon < 0 ? key : key.substring(colon + 1);
        // this dummy implementation returns a property's content
        String content = (String) blobInfoContext.doc.getPropertyValue(k);
        if (content == null) {
            throw new IOException("Unknown binary: " + key);
        }
        return new SimpleManagedBlob(blobProviderId, blobInfo) {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getStream() throws IOException {
                return new ByteArrayInputStream(content.getBytes(UTF_8));
            }
        };
    }

    // special blob provider using the document to resolve the blob
    // and also uses range requests
    protected Blob readBlobFromDoc2(BlobInfoContext blobInfoContext) throws IOException {
        BlobInfo dbBlobInfo = blobInfoContext.blobInfo;
        // in this dummy implementation we don't care about the blob key
        // as all info is derived from the document

        String subkey = (String) blobInfoContext.doc.getPropertyValue("key");
        long start = (Long) blobInfoContext.doc.getPropertyValue("rangeStart");
        long end = (Long) blobInfoContext.doc.getPropertyValue("rangeEnd");
        ByteRange byteRange = ByteRange.inclusive(start, end);

        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = dbBlobInfo.key; // this must be the same
        // the following may be computed according to business logic
        blobInfo.mimeType = "test/type";
        blobInfo.filename = "extracted-" + start + "-" + end + ".ext";
        blobInfo.length = Long.valueOf(end - start + 1);
        blobInfo.digest = "dead0000cafe";
        return new SimpleManagedBlob(blobProviderId , blobInfo) {
            private static final long serialVersionUID = 1L;

            /*
             * This contains the actual business logic to return a dynamically-computed stream.
             */
            @Override
            public InputStream getStream() throws IOException {
                BlobManager blobManager = Framework.getService(BlobManager.class);
                // use another blob provider from which we can extract byte ranges
                BlobProvider blobProvider = blobManager.getBlobProvider(MEMWITHBYTERANGE);
                InputStream stream = blobProvider.getStream(subkey, byteRange);
                // we could compute other streams and assemble/transform them
                // before returning a result
                return stream;
            }
        };
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
