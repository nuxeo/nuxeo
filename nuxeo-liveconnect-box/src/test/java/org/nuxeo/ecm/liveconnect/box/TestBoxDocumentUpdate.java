/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestBoxDocumentUpdate extends BoxTestCase {

    private static final String TEST_WORKSPACE = "testWorkspace";

    private static final String TEST_FILE_NAME = "BoxFile";

    @Inject
    private CoreSession session;

    @Inject
    private WorkManager workManager;

    @Inject
    private BlobManager blobManager;

    @Test
    public void testDocumentUpdate() throws Exception {
        String initialDigest = UUID.randomUUID().toString();
        // Create test document
        DocumentModel testWorkspace = session.createDocumentModel("/", TEST_WORKSPACE, "Workspace");
        session.createDocument(testWorkspace);
        List<DocumentModel> testFiles = LongStream.range(0, BoxBlobProvider.MAX_RESULT + 10)
                                                  .mapToObj(i -> createDocumentWithBlob(i, initialDigest))
                                                  .collect(Collectors.toList());

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        BatchUpdateBlobProvider provider = (BatchUpdateBlobProvider) blobManager.getBlobProvider(SERVICE_ID);
        provider.processDocumentsUpdate();

        awaitWorks();
        for (DocumentModel testFile : testFiles) {
            testFile = session.getDocument(testFile.getRef());

            SimpleManagedBlob blob = (SimpleManagedBlob) testFile.getPropertyValue("file:content");

            assertTrue(StringUtils.isNotBlank(blob.getDigest()));
            assertNotEquals(initialDigest, blob.getDigest());
        }
    }

    private DocumentModel createDocumentWithBlob(long id, String digest) {
        DocumentModel testFile = session.createDocumentModel('/' + TEST_WORKSPACE, TEST_FILE_NAME + id, "File");
        testFile.setPropertyValue("file:content", createBlob(FILE_1_ID, digest));
        return session.createDocument(testFile);
    }

    protected void awaitWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        boolean allCompleted = workManager.awaitCompletion("blobProviderDocumentUpdate", 20000, TimeUnit.MILLISECONDS);
        assertTrue(allCompleted);

    }

}
