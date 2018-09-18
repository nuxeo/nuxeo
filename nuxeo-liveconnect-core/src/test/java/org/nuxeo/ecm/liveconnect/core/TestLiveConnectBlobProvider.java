/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.liveconnect.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestLiveConnectBlobProvider extends LiveConnectTestCase {

    private static final String TEST_WORKSPACE = "testWorkspace";

    private static final String TEST_FILE_NAME = "LiveConnectFile";

    @Inject
    private CoreSession session;

    @Inject
    private WorkManager workManager;

    @Inject
    private BlobManager blobManager;

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    private MockLiveConnectBlobProvider blobProvider;

    @Before
    public void before() {
        blobProvider = (MockLiveConnectBlobProvider) blobManager.getBlobProvider(SERVICE_ID);
        assertNotNull(blobProvider);
    }

    @After
    public void after() {
        Framework.getService(CacheService.class).getCache(SERVICE_ID).invalidateAll();
    }

    @Test
    public void testSupportsUserUpdate() {
        assertTrue(blobProvider.supportsUserUpdate());
    }

    @Test
    public void testReadBlob() throws Exception {
        BlobInfo blobInfo = createBlobInfo(FILE_1_ID);
        Blob blob = blobProvider.readBlob(blobInfo);
        assertTrue(blob instanceof SimpleManagedBlob);
        assertEquals(blobInfo.key, ((SimpleManagedBlob) blob).getKey());
        assertEquals(FILE_1_NAME, blob.getFilename());
        assertEquals("image/jpeg", blob.getMimeType());
        assertNull(blob.getEncoding());
        assertEquals(FILE_1_SIZE, blob.getLength());
        byte[] bytes;
        try (InputStream is = blob.getStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        assertArrayEquals(FILE_1_BYTES, bytes);
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR")
    public void testReadBrokenBlob() throws Exception {
        logFeature.hideErrorFromConsoleLog();
        try {
            BlobInfo blobInfo = createBlobInfo(INVALID_FILE_ID);
            Blob blob = blobProvider.readBlob(blobInfo);

            List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
            assertEquals(1, caughtEvents.size());
            assertEquals("Failed to access file: LiveConnectFileInfo{user=tester@example.com, fileId=invalid-file-id}",
                    caughtEvents.get(0).getRenderedMessage());
            logCaptureResult.clear();

            assertTrue(blob instanceof SimpleManagedBlob);
            assertEquals(blobInfo.key, ((SimpleManagedBlob) blob).getKey());
            // check error blob returned:
            assertEquals(ErrorLiveConnectFile.FILENAME, blob.getFilename());
            assertEquals(ErrorLiveConnectFile.MIME_TYPE, blob.getMimeType());
            assertNull(blob.getEncoding());
            assertEquals(0, blob.getLength());
            byte[] bytes;
            try (InputStream is = blob.getStream()) {
                bytes = IOUtils.toByteArray(is);
            }
            assertEquals(0, bytes.length);

            caughtEvents = logCaptureResult.getCaughtEvents();
            assertEquals(1, caughtEvents.size());
            assertEquals("Failed to access file: core:tester@example.com:invalid-file-id",
                    caughtEvents.get(0).getRenderedMessage());
        } finally {
            logFeature.restoreConsoleLog();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWriteBlob() throws Exception {
        blobProvider.writeBlob(createBlob(FILE_1_ID));
    }

    @Test
    public void testCheckChangesAndUpdateBlobWithUpdate() {
        DocumentModel doc = session.createDocumentModel("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID, ""));
        List<DocumentModel> docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertFalse(docs.isEmpty());

        doc = session.createDocumentModel("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID));
        docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertFalse(docs.isEmpty());
    }

    @Test
    public void testCheckChangesAndUpdateBlobWithoutUpdate() {
        DocumentModel doc = session.createDocumentModel("parent", "file-1", "File");
        List<DocumentModel> docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertTrue(docs.isEmpty());

        doc = session.createDocumentModel("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID, FILE_1_DIGEST));
        docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertTrue(docs.isEmpty());

        doc = session.createDocumentModel("parent", "file-1", "File");
        doc.setPropertyValue("content", createBlob(FILE_1_ID, FILE_1_DIGEST, UUID.randomUUID().toString()));
        docs = blobProvider.checkChangesAndUpdateBlob(Collections.singletonList(doc));
        assertTrue(docs.isEmpty());
    }

    @Test
    public void testDocumentUpdate() throws Exception {
        String initialDigest = UUID.randomUUID().toString();
        // Create test document
        DocumentModel testWorkspace = session.createDocumentModel("/", TEST_WORKSPACE, "Workspace");
        session.createDocument(testWorkspace);
        List<DocumentModel> testFiles = LongStream.range(0, BatchUpdateBlobProvider.MAX_RESULT + 10)
                                                  .mapToObj(i -> createDocumentWithBlob(i, initialDigest))
                                                  .collect(Collectors.toList());

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        blobProvider.processDocumentsUpdate();

        awaitWorks();
        for (DocumentModel testFile : testFiles) {
            testFile = session.getDocument(testFile.getRef());

            SimpleManagedBlob blob = (SimpleManagedBlob) testFile.getPropertyValue("file:content");

            assertTrue(StringUtils.isNotBlank(blob.getDigest()));
            assertNotEquals(initialDigest, blob.getDigest());
        }
    }

    @Test
    public void testGetOAuth2Provider() {
        OAuth2ServiceProvider oAuth2Provider = blobProvider.getOAuth2Provider();
        assertNotNull(oAuth2Provider);
    }

    @Test
    public void testToBlobWithFile() {
        LiveConnectFile file = new MockLiveConnectFile(new LiveConnectFileInfo(USERID, FILE_1_ID), FILE_1_NAME,
                FILE_1_SIZE, FILE_1_DIGEST);
        SimpleManagedBlob blob = blobProvider.toBlob(file);
        assertEquals(SERVICE_ID + ':' + USERID + ':' + FILE_1_ID, blob.getKey());
        assertEquals(FILE_1_NAME, blob.getFilename());
        assertEquals(FILE_1_SIZE, blob.getLength());
        assertEquals(FILE_1_DIGEST, blob.getDigest());
    }

    @Test
    public void testToBlobWithFileWithRevision() {
        String revision = UUID.randomUUID().toString();
        LiveConnectFile file = new MockLiveConnectFile(new LiveConnectFileInfo(USERID, FILE_1_ID, revision),
                FILE_1_NAME, FILE_1_SIZE, FILE_1_DIGEST);
        SimpleManagedBlob blob = blobProvider.toBlob(file);
        assertEquals(SERVICE_ID + ':' + USERID + ':' + FILE_1_ID + ':' + revision, blob.getKey());
        assertEquals(FILE_1_NAME, blob.getFilename());
        assertEquals(FILE_1_SIZE, blob.getLength());
        assertEquals(FILE_1_DIGEST, blob.getDigest());
    }

    @Test
    public void testToBlobWithFileInfo() throws Exception {
        LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(USERID, FILE_1_ID);
        SimpleManagedBlob blob = blobProvider.toBlob(fileInfo);
        assertEquals(SERVICE_ID + ':' + USERID + ':' + FILE_1_ID, blob.getKey());
        assertEquals(FILE_1_NAME, blob.getFilename());
        assertEquals(FILE_1_SIZE, blob.getLength());
        assertEquals(FILE_1_DIGEST, blob.getDigest());
    }

    @Test
    public void testToFileInfo() {
        SimpleManagedBlob blob = createBlob(FILE_1_ID);
        LiveConnectFileInfo fileInfo = blobProvider.toFileInfo(blob);
        assertEquals(USERID, fileInfo.getUser());
        assertEquals(FILE_1_ID, fileInfo.getFileId());
        assertFalse(fileInfo.getRevisionId().isPresent());
    }

    @Test
    public void testToFileInfoWithRevision() {
        String revision = UUID.randomUUID().toString();
        BlobInfo blobInfo = createBlobInfo(FILE_1_ID, FILE_1_DIGEST, revision);
        LiveConnectFileInfo fileInfo = blobProvider.toFileInfo(new SimpleManagedBlob(blobInfo));
        assertEquals(USERID, fileInfo.getUser());
        assertEquals(FILE_1_ID, fileInfo.getFileId());
        assertTrue(fileInfo.getRevisionId().isPresent());
        assertEquals(revision, fileInfo.getRevisionId().get());
    }

    @Test
    public void testFileCache() {
        // Test with nothing
        String id = UUID.randomUUID().toString();
        LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(USERID, id);

        LiveConnectFile file = blobProvider.getFileFromCache(fileInfo);
        assertNull(file);

        // Put something and then retrieve it
        file = new MockLiveConnectFile(fileInfo, FILE_1_NAME, FILE_1_SIZE, FILE_1_DIGEST);
        blobProvider.putFileInCache(file);

        file = blobProvider.getFileFromCache(fileInfo);
        assertNotNull(file);
        assertEquals(id, file.getInfo().getFileId());
        assertEquals(FILE_1_NAME, file.getFilename());
    }

    @Test
    public void testAsUriError() {
        URI uri = blobProvider.asURI("http://");
        assertNull(uri);
    }

    @Test
    public void testAsUriValid() {
        URI uri = blobProvider.asURI("http://www.nuxeo.com/");
        assertNotNull(uri);
    }

    private DocumentModel createDocumentWithBlob(long id, String digest) {
        DocumentModel testFile = session.createDocumentModel('/' + TEST_WORKSPACE, TEST_FILE_NAME + id, "File");
        testFile.setPropertyValue("file:content", createBlob(FILE_1_ID, digest));
        return session.createDocument(testFile);
    }

    private void awaitWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        boolean allCompleted = workManager.awaitCompletion("blobProviderDocumentUpdate", 20, TimeUnit.SECONDS);
        assertTrue(allCompleted);
    }

}
