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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.blob.binary.TestDefaultBinaryManager.countFiles;

public class TestFakeBinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String CONTENT_SHA1 = "3f3bdf817537faa28483eabc69a4bb3912cf0c6c";

    @Test
    public void testFakeBinaryManager() throws Exception {
        deployBundle("org.nuxeo.ecm.core");
        deployContrib("org.nuxeo.ecm.core.tests", "OSGI-INF/test-fake-blob-provider.xml");

        FakeBinaryManager binaryManager = new FakeBinaryManager();
        binaryManager.initialize("repo", Collections.emptyMap());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));

        // Ask whatever you get a fake binary
        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);

        // store nothing
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);
        assertEquals(0, countFiles(binaryManager.getStorageDir()));

        // get MD5 binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(-1, binary.getLength());
        assertEquals("MD5", binary.getDigestAlgorithm());

        // check SHA-1 binary
        Binary sha1Binary = new Binary(CONTENT_SHA1, "repo");
        assertEquals("SHA-1", sha1Binary.getDigestAlgorithm());


        binaryManager.close();
    }


}
