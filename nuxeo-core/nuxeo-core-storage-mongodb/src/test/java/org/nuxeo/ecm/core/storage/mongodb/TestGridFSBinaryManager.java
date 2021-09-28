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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.mongodb.GridFSBinaryManager.GridFSBinary;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.mongodb.client.gridfs.model.GridFSFile;

@RunWith(FeaturesRunner.class)
@Features({ MongoDBFeature.class, MockitoFeature.class })
public class TestGridFSBinaryManager {

    protected static final String CONTENT = "this is a file au caf\u00e9";

    protected static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    protected static final String CONTENT2 = "abc";

    protected static final String CONTENT2_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    protected static final String CONTENT3 = "defg";

    protected static final String CONTENT3_MD5 = "025e4da7edac35ede583f5e8d51aa7ec";

    protected static GridFSBinaryManager BINARY_MANAGER;

    @Mock
    @RuntimeService
    protected BlobManager blobManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BINARY_MANAGER = new GridFSBinaryManager();
        Map<String, String> config = new HashMap<>();
        config.put("bucket", Framework.getProperty("nuxeo.mongodb.gridfs.bucket", "test.fs"));
        BINARY_MANAGER.initialize("test", config);
    }

    @AfterClass
    public static void afterClass() {
        if (BINARY_MANAGER != null) {
            BINARY_MANAGER.close();
            BINARY_MANAGER = null;
        }
    }

    @Before
    public void doBefore() throws IOException {
        when(blobManager.getBlobProvider("test")).thenReturn(BINARY_MANAGER);
    }

    protected GridFSBinaryManager getBinaryManager() {
        return BINARY_MANAGER;
    }

    protected Set<String> listObjects() throws IOException {
        Set<String> res = new HashSet<>();
        getBinaryManager().getGridFSBucket().find().map(GridFSFile::getFilename).into(res);
        return res;
    }

    @Test
    public void testSerialization() throws Exception {
        GridFSBinaryManager binaryManager = getBinaryManager();
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertTrue(binary instanceof GridFSBinary);
        byte[] ser = SerializationUtils.serialize(binary);
        Binary binary2 = SerializationUtils.deserialize(ser);
        assertEquals(binary.getDigest(), binary2.getDigest());
        String content;
        try (InputStream in = binary2.getStream()) {
            content = IOUtils.toString(in, "UTF-8");
        }
        assertEquals(CONTENT, content);
    }

    @Test
    public void testStoreFile() throws Exception {
        GridFSBinaryManager binaryManager = getBinaryManager();

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);

        // check binary is here
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(CONTENT, toString(binary.getStream()));

        // check that there is only one entry
        assertEquals(1, listObjects().size());

        // store again
        binaryManager.getBinary(Blobs.createBlob(CONTENT));

        // check that there is still only one entry
        assertEquals(1, listObjects().size());
    }

    @Test
    public void testWriteBlob() throws IOException {
        GridFSBinaryManager binaryManager = getBinaryManager();

        String key = binaryManager.writeBlob(Blobs.createBlob(CONTENT));
        assertEquals(CONTENT_MD5, key);
    }

    /**
     * NOTE THAT THIS TEST WILL REMOVE ALL FILES IN THE BUCKET!!!
     */
    @Test
    public void testBinaryManagerGC() throws Exception {
        GridFSBinaryManager binaryManager = getBinaryManager();

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(CONTENT, toString(binary.getStream()));

        // another binary we'll GC
        binaryManager.getBinary(Blobs.createBlob(CONTENT2));

        // another binary we'll keep
        binaryManager.getBinary(Blobs.createBlob(CONTENT3));

        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5, CONTENT3_MD5)), listObjects());

        // GC in non-delete mode
        BinaryGarbageCollector gc = binaryManager.getGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(CONTENT_MD5);
        gc.mark(CONTENT3_MD5);
        assertTrue(gc.isInProgress());
        gc.stop(false);
        assertFalse(gc.isInProgress());
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5, CONTENT3_MD5)), listObjects());

        // real GC
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.mark(CONTENT3_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT3_MD5)), listObjects());

        // another GC after not marking content3
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(1, status.numBinaries);
        assertEquals(bytes.length, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(4, status.sizeBinariesGC);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());
    }

    protected static String toString(InputStream stream) throws IOException {
        return IOUtils.toString(stream, "UTF-8");
    }

    @Test
    public void testTransientFlag() throws Exception {
        assertFalse(BINARY_MANAGER.isTransient());

        GridFSBinaryManager bm = new GridFSBinaryManager();
        Map<String, String> properties = new HashMap<>();
        properties.put("transient", "true");
        bm.initialize("test", properties);
        assertTrue(bm.isTransient());
    }

    @Test
    public void testNamespace() throws Exception {
        doTestNamespace("test.fs", null, null);
        doTestNamespace("buck", "buck", null);
        doTestNamespace("test.ns.fs", null, "ns");
        doTestNamespace("buck.ns", "buck", "ns");
    }

    protected static void doTestNamespace(String expectedBucket, String bucket, String namespace) throws Exception {
        Map<String, String> properties = new HashMap<>();
        if (bucket != null) {
            properties.put("bucket", bucket);
        }
        if (namespace != null) {
            properties.put("namespace", namespace);
        }
        GridFSBinaryManager bm = new GridFSBinaryManager();
        bm.initialize("test", properties);
        assertEquals(expectedBucket, bm.getGridFSBucket().getBucketName());
    }

}
