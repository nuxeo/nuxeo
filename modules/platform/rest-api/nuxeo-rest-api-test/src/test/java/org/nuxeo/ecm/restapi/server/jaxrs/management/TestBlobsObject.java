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

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.DRY_RUN_PARAM;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_DELETED_SIZE_KEY;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_TOTAL_SIZE_KEY;
import static org.nuxeo.ecm.core.api.Blobs.createBlob;
import static org.nuxeo.ecm.core.api.impl.blob.AbstractBlob.TEXT_PLAIN;
import static org.nuxeo.ecm.core.api.impl.blob.AbstractBlob.UTF_8;
import static org.nuxeo.ecm.core.blob.stream.StreamOrphanBlobGC.ENABLED_PROPERTY_NAME;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_RESULT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SKIP_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.blob.scroll.RepositoryBlobScroll;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2023
 */
@RunWith(FeaturesRunner.class)
@Features(ManagementFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
@WithFrameworkProperty(name = ENABLED_PROPERTY_NAME, value = "false")
public class TestBlobsObject extends ManagementBaseTest {

    protected static final int NB_BLOBS = 10;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ScrollService scrollService;

    @Test
    @Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-storage-blobstore-contrib.xml")
    public void testDeleteOrphanedBlobs() throws IOException {
        testDeleteOrphanedBlobs(false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-storage-blobstore-contrib.xml")
    public void testDryRunDeleteOrphanedBlobs() throws IOException {
        testDeleteOrphanedBlobs(true);
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
    public void testDeleteBlobOnSharedStorageMonoRepository() throws IOException {
        testDeleteOrphanedBlobs(false);
    }

    protected void assertdoGCNotImplemented() {
        try (CloseableClientResponse response = httpClientRule.delete("/management/blobs/orphaned")) {
            assertEquals(SC_NOT_IMPLEMENTED, response.getStatus());
        }
    }

    protected void testDeleteOrphanedBlobs(boolean dryRun) throws IOException {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        // Create a file document with some blobs
        DocumentModel document = session.createDocumentModel("/", "myFile", "File");

        List<Map<String, Blob>> files = new ArrayList<>(NB_BLOBS);
        long sizeOfBinaries = 0L;
        for (int num = 0; num < NB_BLOBS; num++) {
            Blob blob = createBlob(String.format("Blob N%d", num), TEXT_PLAIN, UTF_8, String.format("file%d.txt", num));
            sizeOfBinaries += blob.getLength();
            files.add(Map.of("file", blob));
        }

        document.setPropertyValue("files:files", (Serializable) files);
        document = session.createDocument(document);
        coreFeature.waitForAsyncCompletion();

        doGC(dryRun, true, NB_BLOBS, 0, NB_BLOBS, sizeOfBinaries, 0, NB_BLOBS);

        // Remove the document which will let orphaned blobs
        session.removeDocument(document.getRef());
        coreFeature.waitForAsyncCompletion();
        assertFalse(session.exists(document.getRef()));

        doGC(dryRun, true, 0, sizeOfBinaries, NB_BLOBS, sizeOfBinaries, 0, NB_BLOBS);
    }

    protected void doGC(boolean dryRun, boolean success, int skipped, long deletedSize, int processed, long totalSize,
            int errorCount, int total) throws IOException {
        String commandId;
        try (CloseableClientResponse response = httpClientRule.delete(
                "/management/blobs/orphaned?" + DRY_RUN_PARAM + "=" + dryRun)) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertBulkStatusScheduled(node);
            commandId = getBulkCommandId(node);
        }

        // waiting for the asynchronous gc
        coreFeature.waitForAsyncCompletion();

        try (CloseableClientResponse response = httpClientRule.get("/management/bulk/" + commandId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(SC_OK, response.getStatus());
            assertBulkStatusCompleted(node);
            assertEquals(!success, node.get(STATUS_HAS_ERROR).asBoolean());
            assertEquals(skipped, node.get(STATUS_SKIP_COUNT).asInt());
            assertEquals(deletedSize, node.get(STATUS_RESULT).get(RESULT_DELETED_SIZE_KEY).asLong());
            assertEquals(processed, node.get(STATUS_PROCESSED).asInt());
            assertEquals(totalSize, node.get(STATUS_RESULT).get(RESULT_TOTAL_SIZE_KEY).asLong());
            assertEquals(dryRun, node.get(STATUS_RESULT).get(DRY_RUN_PARAM).asBoolean());
            assertEquals(errorCount, node.get(STATUS_ERROR_COUNT).asInt());
            assertEquals(total, node.get(STATUS_TOTAL).asInt());
        }
        // Check at blob store level
        assertEquals(dryRun ? NB_BLOBS : skipped, getBlobCount());
    }

    protected int getBlobCount() {
        ScrollRequest request = GenericScrollRequest.builder(RepositoryBlobScroll.SCROLL_NAME, "test")
                                                    .size(NB_BLOBS + 1)
                                                    .build();
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
