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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.blob.BulkDownload;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
public class TestBulkDownloadOperation {

    private static final int MAX_DOC = 10;

    @Inject
    protected DownloadService downloadService;

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    CoreFeature coreFeature;

    @Test
    public void iCanBulkDownloadFiles() throws IOException, OperationException, InterruptedException {
        testWithNbFiles(MAX_DOC);
    }

    /**
     * @since 10.1
     */
    @Test
    public void iCanBulkDownloadASingleFile() throws IOException, OperationException, InterruptedException {
        testWithNbFiles(1);
    }

    protected void testWithNbFiles(int nbDocs) throws IOException, OperationException, InterruptedException {
        DocumentModelList docs = new DocumentModelListImpl();
        for (int i = 0; i < nbDocs; i++) {
            DocumentModel doc = session.createDocumentModel("/", "TestFile" + i, "File");
            doc.setProperty("dublincore", "title", "TestTitle" + i);

            Blob blob1 = Blobs.createBlob("test" + i);
            String blobFilename = "test" + i + ".txt";
            blob1.setFilename(blobFilename);

            BlobHolder bh1 = doc.getAdapter(BlobHolder.class);
            bh1.setBlob(blob1);
            doc = session.createDocument(doc);
            session.save();

            docs.add(doc);
        }

        coreFeature.waitForAsyncCompletion();

        OperationChain chain = new OperationChain("test-chain");
        chain.add(BulkDownload.ID);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docs);

        Blob resultBlob = (Blob) automationService.run(ctx, BulkDownload.ID);

        assertNotNull(resultBlob);
        assertTrue(resultBlob instanceof AsyncBlob);

        coreFeature.waitForAsyncCompletion();

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        List<Blob> blobs = ts.getBlobs(((AsyncBlob) resultBlob).getKey());
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        Blob actualZip = resultBlob = blobs.get(0);
        assertTrue(actualZip instanceof FileBlob);
        assertTrue(actualZip.getLength() > 0);
        assertEquals("application/zip", actualZip.getMimeType());

        try (CloseableFile source = actualZip.getCloseableFile()) {
            try (ZipFile zip = new ZipFile(source.getFile())) {
                assertEquals(nbDocs, zip.size());
            }
        }
    }

}
