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
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * This test must be run with at least the following system properties set:
 * <ul>
 * <li>nuxeo.s3storage.bucket</li>
 * <li>nuxeo.s3storage.awsid</li>
 * <li>nuxeo.s3storage.awssecret</li>
 * </ul>
 */
public class TestS3BinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Properties properties = Framework.getProperties();
        // !!!!!!!!!!!!!!! NEVER COMMIT THIS !!!!!!!!!!!!!!!
        properties.setProperty(S3BinaryManager.BUCKET_NAME_KEY, "CHANGETHIS");
        properties.setProperty(S3BinaryManager.AWS_ID_KEY, "CHANGETHIS");
        properties.setProperty(S3BinaryManager.AWS_SECRET_KEY, "CHANGETHIS");
        // !!!!!!!!!!!!!!! NEVER COMMIT THIS !!!!!!!!!!!!!!!
    }

    public void testS3BinaryManager() throws Exception {
        if ("CHANGETHIS".equals(Framework.getProperty(S3BinaryManager.BUCKET_NAME_KEY))) {
            return; // test disabled
        }

        S3BinaryManager s3binaryManager = new S3BinaryManager();
        s3binaryManager.initialize(new RepositoryDescriptor());

        Binary binary = s3binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary); // lazy binary
        if (binary.getStream() != null) {
            // the tests have already been run
            // make sure we delete it from the bucket first
            s3binaryManager.removeBinary(CONTENT_MD5);
            s3binaryManager.fileCache.clear();
        }

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = s3binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);

        // get binary (from cache)
        binary = s3binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));

        // get binary (clean cache)
        s3binaryManager.fileCache.clear();
        binary = s3binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));
    }
}
