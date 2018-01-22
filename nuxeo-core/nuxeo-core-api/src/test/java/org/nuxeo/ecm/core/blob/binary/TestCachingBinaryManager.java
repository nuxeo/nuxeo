/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.blob.binary.CachingBinaryManager.DEBUG_READ_CACHED_BINARY;
import static org.nuxeo.ecm.core.blob.binary.CachingBinaryManager.DEBUG_WRITE_CACHED_BINARY;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Tests for the generic part of a caching binary manager.
 *
 * @since 10.1
 */
public class TestCachingBinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    public static class DummyCachingBinaryManager extends CachingBinaryManager {
    }

    public static class DummyFileStorage implements FileStorage {

        protected final Map<String, byte[]> files = new HashMap<>();;

        @Override
        public void storeFile(String key, File file) throws IOException {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            files.put(key, bytes);
        }

        @Override
        public boolean fetchFile(String key, File file) throws IOException {
            byte[] bytes = files.get(key);
            if (bytes == null) {
                return false;
            } else {
                FileUtils.writeByteArrayToFile(file, bytes);
                return true;
            }
        }
    }

    protected CachingBinaryManager cbm;

    protected FileStorage fileStorage;

    @Override
    @Before
    public void setUp() throws Exception {
        cbm = new DummyCachingBinaryManager();
        cbm.initialize("dummy", null);
        String maxSizeStr = "1MB";
        fileStorage = new DummyFileStorage();
        cbm.initializeCache(maxSizeStr, fileStorage);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        cbm.close();
    }

    @Test
    public void testFileInputStream() throws Exception {
        File file = File.createTempFile("test", "");
        try {
            FileUtils.writeStringToFile(file, CONTENT, UTF_8);
            Blob blob = new FileBlob(file);

            Framework.getProperties().remove(DEBUG_WRITE_CACHED_BINARY);

            // store once, which puts it in the cache
            Binary binary = cbm.getBinary(blob);
            assertEquals(CONTENT_MD5, binary.getDigest());
            assertNull(Framework.getProperty(DEBUG_WRITE_CACHED_BINARY));

            // store again, which uses the optimized code path and the cache
            binary = cbm.getBinary(blob);
            assertEquals(CONTENT_MD5, binary.getDigest());
            assertEquals(CONTENT_MD5, Framework.getProperty(DEBUG_WRITE_CACHED_BINARY));
        } finally {
            file.delete();
        }
    }

    @Test
    public void testOtherInputStream() throws Exception {
        Blob blob = new StringBlob(CONTENT, "test/plain", UTF_8.name());

        Framework.getProperties().remove(DEBUG_WRITE_CACHED_BINARY);

        // store once
        Binary binary = cbm.getBinary(blob);
        assertEquals(CONTENT_MD5, binary.getDigest());
        assertNull(Framework.getProperty(DEBUG_WRITE_CACHED_BINARY));

        // store again, which uses the cache
        binary = cbm.getBinary(blob);
        assertEquals(CONTENT_MD5, binary.getDigest());
        assertEquals(CONTENT_MD5, Framework.getProperty(DEBUG_WRITE_CACHED_BINARY));
    }

    @Test
    public void testGetFile() throws Exception {
        // get file not in storage
        Binary binary = cbm.getBinary("nosuchdigest");
        assertNotNull(binary);
        assertNull(binary.getFile());

        // put file in storage
        File file = File.createTempFile("test", "");
        try {
            FileUtils.writeStringToFile(file, CONTENT, UTF_8);
            fileStorage.storeFile(CONTENT_MD5, file);
        } finally {
            file.delete();
        }

        Framework.getProperties().remove(DEBUG_READ_CACHED_BINARY);

        // read once
        binary = cbm.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        File ff = binary.getFile();
        assertNotNull(ff);
        assertNull(Framework.getProperty(DEBUG_READ_CACHED_BINARY));

        // read a second time, now hits the cache
        binary = cbm.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertNotNull(binary.getFile());
        assertEquals(CONTENT_MD5, Framework.getProperty(DEBUG_READ_CACHED_BINARY));
    }

}
