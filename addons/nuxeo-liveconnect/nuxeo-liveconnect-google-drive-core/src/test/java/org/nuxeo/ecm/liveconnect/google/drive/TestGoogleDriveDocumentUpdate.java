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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.liveconnect.google.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestGoogleDriveDocumentUpdate extends GoogleDriveTestCase {

    // same as in test XML contrib
    private static final String PREFIX = "googledrive";

    private static final String TEST_FILE_NAME = "GoogleDriveFile";

    @Inject
    CoreSession session;

    @Inject
    WorkManager workManager;

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testDocumentUpdate() throws Exception {
        // Create test document
        DocumentModel testWorkspace = session.createDocumentModel("/", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        List<DocumentModel> testFiles = new ArrayList<DocumentModel>();
        for (int i = 0; i < GoogleDriveBlobProvider.MAX_RESULT + 10; i++) {
            DocumentModel testFile = session.createDocumentModel("/testWorkspace", TEST_FILE_NAME + i, "File");
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = PREFIX + ":" + USERID + ":" + JPEG_FILEID;
            blobInfo.digest = "pouet";
            blobInfo.filename = "oldfilename";
            SimpleManagedBlob blob = new SimpleManagedBlob(blobInfo);
            testFile.setPropertyValue("content", blob);
            testFile = session.createDocument(testFile);
            testFiles.add(testFile);
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        BatchUpdateBlobProvider provider = (BatchUpdateBlobProvider) blobManager.getBlobProvider("googledrive");
        provider.processDocumentsUpdate();

        awaitWorks();
        for (DocumentModel testFile : testFiles) {
            testFile = session.getDocument(testFile.getRef());

            SimpleManagedBlob blob = (SimpleManagedBlob) testFile.getPropertyValue("content");

            assertTrue(StringUtils.isNotBlank(blob.getDigest()));
            assertNotEquals("pouet", blob.getDigest());

            assertEquals("testimage.jpg", blob.getFilename());
        }

    }

    protected void awaitWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        final boolean allCompleted = workManager.awaitCompletion("blobProviderDocumentUpdate", 20000,
                TimeUnit.MILLISECONDS);
        assertTrue(allCompleted);

    }

}
