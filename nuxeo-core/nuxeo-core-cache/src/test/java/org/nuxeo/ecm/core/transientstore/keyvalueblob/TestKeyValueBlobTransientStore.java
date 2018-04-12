/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.transientstore.keyvalueblob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(KeyValueBlobTransientStoreFeature.class)
public class TestKeyValueBlobTransientStore {

    protected static final String NAME = "testkvb";

    protected static final String NAME2 = "testkvb2";

    @Inject
    protected TransientStoreService tss;

    protected TransientStoreProvider tsp;

    protected TransientStore ts;

    @Before
    public void setUp() {
        setUp(NAME);
    }

    protected void setUp(String name) {
        ts = tss.getStore(name);
        tsp = (TransientStoreProvider) ts;
    }

    @After
    public void tearDown() {
        tsp.removeAll();
    }

    protected void createBlob(String key, String content) {
        Blob blob = new StringBlob(content);
        blob.setFilename("fake.txt");
        blob.setMimeType("text/plain");
        blob.setDigest(DigestUtils.md5Hex(content));
        ts.putBlobs(key, Collections.singletonList(blob));
    }

    @Test
    public void verifyStorage() throws Exception {

        String key = "mykey";
        String content = "FakeContentWithBinary\u00e9";
        int contentByteLength = content.getBytes(UTF_8).length;
        assertNotEquals(contentByteLength, content.length());
        String contentMD5 = DigestUtils.md5Hex(content);

        String content2 = "FakeContent2";
        int content2ByteLength = content2.getBytes(UTF_8).length;

        // start empty
        long size = tsp.getStorageSize();
        assertEquals(0, size);

        // create content
        ts.putParameter(key, "A", "1");
        ts.putParameter(key, "B", "b");
        createBlob(key, content);

        // check that entry is stored
        assertTrue(ts.exists(key));
        assertFalse(ts.isCompleted(key));
        assertEquals(new HashSet<>(Arrays.asList(key)), tsp.keySet());
        assertEquals(contentByteLength, ts.getSize(key));
        assertEquals("1", ts.getParameter(key, "A"));
        assertEquals("b", ts.getParameter(key, "B"));
        List<Blob> blobs = ts.getBlobs(key);
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("fake.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(contentMD5, blob.getDigest());
        try (InputStream stream = blob.getStream()) {
            assertEquals(content, IOUtils.toString(stream, UTF_8));
        }

        size = tsp.getStorageSize();
        assertEquals(contentByteLength, size);

        // update the entry
        Blob otherBlob = new StringBlob(content2);
        otherBlob.setFilename("fake2.txt");
        otherBlob.setMimeType("text/plain");
        blobs.add(otherBlob);
        ts.putBlobs(key, blobs);

        // check update
        assertTrue(ts.exists(key));
        assertEquals(new HashSet<>(Arrays.asList(key)), tsp.keySet());
        assertEquals(contentByteLength + content2ByteLength, ts.getSize(key));
        blobs = ts.getBlobs(key);
        assertEquals(2, blobs.size());
        assertEquals("fake.txt", blobs.get(0).getFilename());
        assertEquals("fake2.txt", blobs.get(1).getFilename());
        size = tsp.getStorageSize();
        assertEquals(contentByteLength + content2ByteLength, size);

        // move to deletable entries
        // check that still here
        ts.release(key);
        assertTrue(ts.exists(key));
        assertEquals(1, tsp.keySet().size());
        assertTrue(tsp.keySet().contains(key));

        // check Remove
        ts.remove(key);
        assertFalse(ts.exists(key));
        assertEquals(0, tsp.keySet().size());

        size = tsp.getStorageSize();
        assertEquals(0, size);
    }

    @Test
    public void verifyStorageWithExplicitConfiguration() throws Exception {
        setUp(NAME2);
        verifyStorage();
    }

    @Test
    public void verifyDuplicateParam() throws Exception {
        String key = "mykey";
        ts.putParameter(key, "A", "1");
        ts.putParameter(key, "A", "2");
        assertEquals("2", ts.getParameter(key, "A"));
        assertEquals(Collections.singletonMap("A", "2"), ts.getParameters(key));
    }

    @Test
    public void verifyNullCases() throws Exception {
        // Non existing entry
        assertFalse(ts.exists("fakeEntry"));
        assertNull(ts.getParameters("fakeEntry"));
        assertNull(ts.getParameter("fakeEntry", "fakeParameter"));
        assertNull(ts.getBlobs("fakeEntry"));
        assertEquals(-1, ts.getSize("fakeEntry"));
        assertFalse(ts.isCompleted("fakeEntry"));

        // Entry with parameters only
        ts.putParameter("testEntry", "param1", "value");
        assertTrue(ts.exists("testEntry"));
        Map<String, Serializable> params = ts.getParameters("testEntry");
        assertNotNull(params);
        assertEquals(1, params.size());
        assertNotNull(ts.getParameter("testEntry", "param1"));
        assertNull(ts.getParameter("testEntry", "param2"));
        List<Blob> blobs = ts.getBlobs("testEntry");
        assertNotNull(blobs);
        assertTrue(blobs.isEmpty());

        // Entry with blobs only
        ts.putBlobs("otherEntry", Collections.singletonList(new StringBlob("joe")));
        assertTrue(ts.exists("otherEntry"));
        params = ts.getParameters("otherEntry");
        assertNotNull(params);
        assertTrue(params.isEmpty());
        blobs = ts.getBlobs("otherEntry");
        assertNotNull(blobs);
        assertEquals(1, blobs.size());
    }

    @Test
    public void verifyMaxSizeException() throws Exception {
        // store is configured for 1MB max
        byte[] bytes = new byte[1024 * 1024 + 1];
        Blob blob = Blobs.createBlob(bytes);
        ts.putBlobs("foo", Collections.singletonList(blob));
        // store another one but we've exceeded the size allowed
        try {
            ts.putBlobs("bar", Collections.singletonList(Blobs.createBlob("x")));
            fail("Should have exceeded maximum transient space");
        } catch (MaximumTransientSpaceExceeded e) {
            assertEquals("Maximum Transient Space Exceeded", e.getMessage());
        }
    }

    @Test
    public void testGC() throws Exception {
        String content = "SomeContent";
        int contentByteLength = content.getBytes(UTF_8).length;

        // two entries with same content
        createBlob("foo", content);
        createBlob("bar", content);
        assertTrue(ts.exists("foo"));
        assertTrue(ts.exists("bar"));
        assertEquals(contentByteLength * 2, tsp.getStorageSize());

        // do GC
        tsp.doGC();

        // entry is still here
        assertTrue(ts.exists("foo"));
        assertTrue(ts.exists("bar"));
        assertEquals(contentByteLength * 2, tsp.getStorageSize());

        // now remove one entry and do GC
        ts.remove("bar");
        tsp.doGC();

        // one entry is still here
        assertTrue(ts.exists("foo"));
        assertFalse(ts.exists("bar"));
        assertEquals(contentByteLength, tsp.getStorageSize());

        // remove last entry
        ts.remove("foo");
        tsp.doGC();

        // entry is gone
        assertFalse(ts.exists("foo"));
        assertFalse(ts.exists("bar"));
        assertEquals(0, tsp.getStorageSize());
    }

}
