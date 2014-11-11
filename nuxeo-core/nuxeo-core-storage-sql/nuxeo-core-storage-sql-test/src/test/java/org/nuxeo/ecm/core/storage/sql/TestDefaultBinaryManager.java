/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDefaultBinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    @Test
    public void testDefaultBinaryManager() throws Exception {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize(new RepositoryDescriptor());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertNull(binary);

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);
        assertEquals(1, countFiles(binaryManager.getStorageDir()));

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));

        // other binary we'll GC
        binaryManager.getBinary(new ByteArrayInputStream(
                "abc".getBytes("UTF-8")));
        assertEquals(2, countFiles(binaryManager.getStorageDir()));

        // sleep before GC to pass its time threshold
        Thread.sleep(3 * 1000);

        // create another binary after time threshold, it won't be GCed
        binaryManager.getBinary(new ByteArrayInputStream(
                "defg".getBytes("UTF-8")));
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
    }

    @Test
    public void testStreamingCopies() throws IOException {
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize(new RepositoryDescriptor());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));
        File file = File.createTempFile("test-", ".data", binaryManager.getStorageDir());
        FileUtils.writeFile(file, CONTENT.getBytes("UTF-8"));
        FileSource source = new FileSource(file);
        binaryManager.storeAndDigest(source);
        assertFalse(file.exists());
        assertTrue(source.getFile().exists());
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
