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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import static org.junit.Assert.*;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager.S3LazyBinary;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * ***** NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!! *****
 * <p>
 * This test must be run with at least the following system properties set:
 * <ul>
 * <li>nuxeo.s3storage.bucket</li>
 * <li>nuxeo.s3storage.awsid</li>
 * <li>nuxeo.s3storage.awssecret</li>
 * </ul>
 * <p>
 * ***** NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!! *****
 */
public class TestS3BinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String CONTENT2 = "abc";

    private static final String CONTENT2_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    private static final String CONTENT3 = "defg";

    private static final String CONTENT3_MD5 = "025e4da7edac35ede583f5e8d51aa7ec";

    protected boolean DISABLED = true;

    protected S3BinaryManager binaryManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Properties properties = Framework.getProperties();
        // NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!!
        // ********** NEVER COMMIT THE SECRET KEYS !!! **********
        properties.setProperty(S3BinaryManager.BUCKET_NAME_KEY, "CHANGETHIS");
        properties.setProperty(S3BinaryManager.AWS_ID_KEY, "CHANGETHIS");
        properties.setProperty(S3BinaryManager.AWS_SECRET_KEY, "CHANGETHIS");
        // ********** NEVER COMMIT THE SECRET KEYS !!! **********

        DISABLED = "CHANGETHIS".equals(Framework.getProperty(S3BinaryManager.BUCKET_NAME_KEY));
        if (!DISABLED) {
            binaryManager = new S3BinaryManager();
            binaryManager.initialize(new RepositoryDescriptor());
            removeObjects();
        }
    }

    @Override
    public void tearDown() throws Exception {
        if (!DISABLED) {
            removeObjects();
        }
        super.tearDown();
    }

    @Test
    public void testS3BinaryManager() throws Exception {
        if (DISABLED) {
            return; // test disabled
        }

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof S3LazyBinary);
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
    public void testS3BinaryManagerGC() throws Exception {
        if (DISABLED) {
            return; // test disabled
        }

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof S3LazyBinary);

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
        assertEquals(bytes.length + 4, status.sizeBinaries);
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
        assertEquals(bytes.length + 4, status.sizeBinaries);
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
        assertEquals(bytes.length, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC);
        assertEquals(4, status.sizeBinariesGC);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());
    }

    /**
     * Lists all objects that look like MD5 digests.
     */
    protected Set<String> listObjects() {
        Set<String> digests = new HashSet<String>();
        ObjectListing list = null;
        do {
            if (list == null) {
                list = binaryManager.amazonS3.listObjects(binaryManager.bucketName);
            } else {
                list = binaryManager.amazonS3.listNextBatchOfObjects(list);
            }
            for (S3ObjectSummary summary : list.getObjectSummaries()) {
                String digest = summary.getKey();
                if (!S3BinaryManager.isMD5(digest)) {
                    continue;
                }
                digests.add(digest);
            }
        } while (list.isTruncated());
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
