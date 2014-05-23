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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerDescriptor;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.binary.LazyBinary;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestJCloudsBinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String CONTENT2 = "abc";

    private static final String CONTENT2_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    private static final String CONTENT3 = "defg";

    private static final String CONTENT3_MD5 = "025e4da7edac35ede583f5e8d51aa7ec";

    protected JCloudsBinaryManager binaryManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Properties properties = Framework.getProperties();
        properties.setProperty(JCloudsBinaryManager.BLOBSTORE_PROVIDER_KEY, "transient");
        properties.setProperty(JCloudsBinaryManager.BLOBSTORE_MAP_NAME_KEY, "nuxeojunittest");
        properties.setProperty(JCloudsBinaryManager.BLOBSTORE_IDENTITY_KEY, "unused");
        properties.setProperty(JCloudsBinaryManager.BLOBSTORE_SECRET_KEY, "unused");

        binaryManager = new JCloudsBinaryManager();
        binaryManager.initialize(new BinaryManagerDescriptor());

    }

    @Override
    public void tearDown() throws Exception {
        removeObjects();
        super.tearDown();
    }

    @Test
    public void testJCloudsBinaryManager() throws Exception {

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof LazyBinary);
        if (binary.getStream() != null) {
            // the tests have already been run
            // make sure we delete it from the bucket first
            binaryManager.removeBinary(CONTENT_MD5);
            binaryManager.fileCache.clear();
        }

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);

        // get binary (from cache)
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));

        // get binary (clean cache)
        binaryManager.fileCache.clear();
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));
    }

    /**
     * NOTE THAT THIS TEST WILL REMOVE ALL FILES IN THE BUCKET!!!
     */
    @Test
    public void testJCloudsBinaryManagerGC() throws Exception {

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof LazyBinary);

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));

        // another binary we'll GC
        binaryManager.getBinary(new ByteArrayInputStream(
                CONTENT2.getBytes("UTF-8")));

        // another binary we'll keep
        binaryManager.getBinary(new ByteArrayInputStream(
                CONTENT3.getBytes("UTF-8")));

        assertEquals(
                new HashSet<String>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5,
                        CONTENT3_MD5)), listObjects());

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
        // binaries size not computed
        // assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        assertEquals(
                new HashSet<String>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5,
                        CONTENT3_MD5)), listObjects());

        // real GC
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.mark(CONTENT3_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        // binaries size not computed
        // assertEquals(bytes.length + 4, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(3, status.sizeBinariesGC);
        assertEquals(
                new HashSet<String>(Arrays.asList(CONTENT_MD5, CONTENT3_MD5)),
                listObjects());

        // another GC after not marking content3
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(1, status.numBinaries);
        // binaries size not computed
        // assertEquals(bytes.length, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(4, status.sizeBinariesGC);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());
    }

    /**
     * Lists all objects that look like MD5 digests.
     */
    protected Set<String> listObjects() {
        Set<String> keyList = binaryManager.storeMap.keySet();
        Set<String> digests = new HashSet<String>();
        for (String digest : keyList) {
            if (!JCloudsBinaryManager.isMD5(digest)) {
                continue;
            }
            digests.add(digest);
        }
        return digests;
    }

    /**
     * Removes all objects that look like MD5 digests.
     */
    protected void removeObjects() {
        for (String digest : listObjects()) {
            binaryManager.removeBinary(digest);
        }
    }

}
