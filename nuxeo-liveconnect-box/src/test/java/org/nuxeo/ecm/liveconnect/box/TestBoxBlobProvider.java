/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

public class TestBoxBlobProvider extends BoxTestCase {

    // same as in test XML contrib
    private static final String PREFIX = "box";

    private static final String FILE_1_ID_JPEG = "5000948880";

    private static final int FILE_1_SIZE = 629644;

    private static final String FILE_1_NAME = "tigers.jpeg";


    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testSupportsUserUpdate() throws Exception {
        BlobProvider blobProvider = blobManager.getBlobProvider(PREFIX);
        assertTrue(blobProvider.supportsUserUpdate());
    }

    @Test
    public void testGetBlob() throws Exception {
        LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(USERID, FILE_1_ID_JPEG);
        Blob blob = ((BoxBlobProvider) blobManager.getBlobProvider(PREFIX)).toBlob(fileInfo);
        assertEquals(FILE_1_SIZE, blob.getLength());
        assertEquals(FILE_1_NAME, blob.getFilename());
    }

}
