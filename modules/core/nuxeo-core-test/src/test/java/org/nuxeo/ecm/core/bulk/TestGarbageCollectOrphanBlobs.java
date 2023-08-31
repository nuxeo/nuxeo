/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.DRY_RUN_PARAM;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_DELETED_SIZE_KEY;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_TOTAL_SIZE_KEY;
import static org.nuxeo.ecm.core.blob.scroll.RepositoryBlobScroll.SCROLL_NAME;
import static org.nuxeo.ecm.core.blob.stream.StreamOrphanBlobGC.ENABLED_PROPERTY_NAME;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.action.DeletionAction;
import org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2023
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
@WithFrameworkProperty(name = ENABLED_PROPERTY_NAME, value = "false")
public class TestGarbageCollectOrphanBlobs {

    protected static final String CONTENT = "hello world";

    protected static final String NXQL = "SELECT * from File";

    protected static final int BUCKET_SIZE = 7;

    protected static final int NB_FILE = 20;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected BulkService service;

    @Inject
    protected CoreSession session;

    @Inject
    protected ScrollService scrollService;

    protected long sizeOfBinaries;

    @Before
    public void createDocuments() {
        sizeOfBinaries = 0L;
        for (int i = 0; i < NB_FILE; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "File");
            Blob blob = Blobs.createBlob(CONTENT + i);
            sizeOfBinaries += blob.getLength();
            doc.setPropertyValue("file:content", (Serializable) blob);
            session.createDocument(doc);
        }
        coreFeature.waitForAsyncCompletion();
    }

    protected void testGCBlobsAction(boolean dryRun) {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        assertEquals(NB_FILE, session.query(NXQL).size());

        // Perform 1st GC -> no blob deleted
        doGC(dryRun, true, NB_FILE, 0, NB_FILE, sizeOfBinaries, 0, NB_FILE);

        // Remove all documents
        BulkCommand command = new BulkCommand.Builder(DeletionAction.ACTION_NAME, NXQL,
                session.getPrincipal().getName()).repository(session.getRepositoryName()).build();
        String commandId = service.submit(command);
        coreFeature.waitForAsyncCompletion();
        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());

        // Perform 2nd GC -> all blobs deleted
        doGC(dryRun, true, 0, sizeOfBinaries, NB_FILE, sizeOfBinaries, 0, NB_FILE);
    }

    protected void doGC(boolean dryRun, boolean success, int skipped, long deletedSize, int processed, long totalSize,
            int errorCount, int total) {
        BulkCommand command = new BulkCommand.Builder(GarbageCollectOrphanBlobsAction.ACTION_NAME,
                session.getRepositoryName(), session.getPrincipal().getName()).repository(session.getRepositoryName())
                                                                              .useGenericScroller()
                                                                              .bucket(BUCKET_SIZE)
                                                                              .param(DRY_RUN_PARAM, dryRun)
                                                                              .scroller(SCROLL_NAME)
                                                                              .build();
        String commandId = service.submit(command);
        coreFeature.waitForAsyncCompletion();

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(processed, status.getProcessed());
        assertEquals(!success, status.hasError());
        assertEquals(errorCount, status.getErrorCount());
        assertEquals(total, status.getTotal());
        assertEquals(skipped, status.getSkipCount());
        assertEquals(deletedSize, ((Number) status.getResult().get(RESULT_DELETED_SIZE_KEY)).longValue());
        assertEquals(totalSize, ((Number) status.getResult().get(RESULT_TOTAL_SIZE_KEY)).longValue());

        // Check at blob store level
        assertEquals(dryRun ? NB_FILE : skipped, getBlobCount());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testGCBlobsAction() {
        testGCBlobsAction(false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testDryRunGCBlobsAction() {
        testGCBlobsAction(true);
    }

    @Test
    public void testUnsupportedDeleteOrphanedBlobOnVCS() {
        assumeTrue("This test is to make sure Full GC cannot be done on repos without ecm:blobKeys capabilities.",
                coreFeature.getStorageConfiguration().isVCS());
        assertdoGCNotImplemented();
    }

    @Test
    public void testUnsupportedDeleteBlobOnUnsupportedProvider() {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        assertdoGCNotImplemented();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-shared-storage-delete.xml")
    public void testDeleteBlobOnSharedStorageMonoRepository() {
        testGCBlobsAction(false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-cross-repo-provider-delete.xml")
    public void testBlobDeleteCrossRepositoryProvider() throws IOException {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        assertdoGCNotImplemented();
    }

    protected void assertdoGCNotImplemented() {
        assertThrows(NuxeoException.class, () -> {
            BulkCommand command = new BulkCommand.Builder(GarbageCollectOrphanBlobsAction.ACTION_NAME,
                    session.getRepositoryName(), session.getPrincipal().getName()).repository(
                            session.getRepositoryName()).useGenericScroller().scroller(SCROLL_NAME).build();
            service.submit(command);
        });
    }

    protected int getBlobCount() {
        ScrollRequest request = GenericScrollRequest.builder(SCROLL_NAME, "test").size(NB_FILE + 1).build();
        assertTrue(scrollService.exists(request));
        int blobCount = 0;
        try (Scroll scroll = scrollService.scroll(request)) {
            while (scroll.hasNext()) {
                blobCount += scroll.next().size();
            }
        }
        return blobCount;
    }

}
