/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestDefaultBinaryManager {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String CONTENT_SHA1 = "3f3bdf817537faa28483eabc69a4bb3912cf0c6c";

    @After
    public void after() throws Exception {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.emptyMap());
        File directory = binaryManager.getStorageDir();
        binaryManager.close();
        Assert.assertTrue(FileUtils.deleteQuietly(directory));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.api")
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-default-blob-provider.xml")
    public void testDefaultBinaryManager() throws Exception {

        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.emptyMap());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertNull(binary);

        // check digest algorithm
        assertEquals("MD5", binaryManager.getDigestAlgorithm());

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);
        assertEquals(1, countFiles(binaryManager.getStorageDir()));

        // get MD5 binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        try (InputStream stream = binary.getStream()) {
            assertEquals(CONTENT, IOUtils.toString(stream, "UTF-8"));
        }
        assertEquals("MD5", binary.getDigestAlgorithm());
        assertEquals(CONTENT_MD5, binary.getDigest());

        // check SHA-1 binary
        Binary sha1Binary = new Binary(CONTENT_SHA1, "repo");
        assertEquals("SHA-1", sha1Binary.getDigestAlgorithm());
        assertEquals(CONTENT_SHA1, sha1Binary.getDigest());

        // other binary we'll GC
        binaryManager.getBinary(Blobs.createBlob("abc"));
        assertEquals(2, countFiles(binaryManager.getStorageDir()));

        // sleep before GC to pass its time threshold
        Thread.sleep(3 * 1000);

        // create another binary after time threshold, it won't be GCed
        binaryManager.getBinary(Blobs.createBlob("defg"));
        assertEquals(3, countFiles(binaryManager.getStorageDir()));

        // GC in non-delete mode
        BinaryGarbageCollector gc = binaryManager.getGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(CONTENT_MD5);
        assertTrue(gc.isInProgress());
        gc.stop(false);
        assertFalse(gc.isInProgress());
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        // still there
        assertEquals(3, countFiles(binaryManager.getStorageDir()));

        // real GC
        gc = binaryManager.getGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(CONTENT_MD5);
        assertTrue(gc.isInProgress());
        gc.stop(true);
        assertFalse(gc.isInProgress());
        status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        // one file gone
        assertEquals(2, countFiles(binaryManager.getStorageDir()));

        binaryManager.close();
    }

    @Test
    public void testTemporaryCopies() throws IOException {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.emptyMap());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));
        FileBlob source = new FileBlob(new ByteArrayInputStream(CONTENT.getBytes("UTF-8")));
        File originalFile = source.getFile();
        binaryManager.storeAndDigest(source);
        assertFalse(originalFile.exists());
        assertTrue(source.getFile().exists());

        binaryManager.close();
    }

    @Test
    public void testPath() throws IOException {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.emptyMap());
        File dir = binaryManager.getStorageDir();
        String path = dir.getPath().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/binaries/data"));
        binaryManager.close();
    }

    @Test
    public void testPathWithNamespace() throws IOException {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.singletonMap("namespace", "myns"));
        File dir = binaryManager.getStorageDir();
        String path = dir.getPath().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/binaries_myns/data"));
        binaryManager.close();
    }

    @Test
    public void testPathDotDot() throws IOException {
        // make sure the "binaries" base path doesn't exist
        File oldbinaries = new File(Environment.getDefault().getData(), "binaries");
        FileUtils.deleteDirectory(oldbinaries);
        // put binaries at a path that has to be canonicalized
        String pathProp = "binaries/../newbinaries";
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize("repo", Collections.singletonMap("path", pathProp));
        File dir = binaryManager.getStorageDir();
        String path = dir.getPath().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/newbinaries/data"));
        binaryManager.close();
    }

    protected static int countFiles(File dir) {
        int n = 0;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                n += countFiles(f);
            } else {
                n++;
            }
        }
        return n;
    }

}
