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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

public class TestBoxBlobProvider extends BoxTestCase {

    @Inject
    private RuntimeHarness harness;

    @Inject
    private BlobManager blobManager;

    private BoxBlobProvider blobProvider;

    @Before
    public void before() {
        blobProvider = (BoxBlobProvider) blobManager.getBlobProvider(SERVICE_ID);
        assertNotNull(blobProvider);
    }

    @Test
    public void testSupportsUserUpdate() throws Exception {
        assertTrue(blobProvider.supportsUserUpdate());
    }

    @Test
    public void testGetBlob() throws Exception {
        LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(USERID, FILE_1_ID);
        Blob blob = blobProvider.toBlob(fileInfo);
        assertEquals(FILE_1_SIZE, blob.getLength());
        assertEquals(FILE_1_NAME, blob.getFilename());
    }

    @Test
    public void testCheckChangesAndUpdateBlobWithUpdate() {
        DocumentModel doc = new DocumentModelImpl("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID, ""));
        List<DocumentModel> docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertFalse(docs.isEmpty());

        doc = new DocumentModelImpl("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID));
        docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertFalse(docs.isEmpty());

    }

    @Test
    public void testCheckChangesAndUpdateBlobWithoutUpdate() {
        DocumentModel doc = new DocumentModelImpl("parent", "file-1", "File");
        List<DocumentModel> docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertTrue(docs.isEmpty());

        doc = new DocumentModelImpl("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID, "134b65991ed521fcfe4724b7d814ab8ded5185dc"));
        docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertTrue(docs.isEmpty());
    }

}
