/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.importer.stream.tests.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.importer.stream.automation.BlobConsumers;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.automation.FileBlobProducers;
import org.nuxeo.importer.stream.automation.RandomBlobProducers;
import org.nuxeo.importer.stream.automation.RandomDocumentProducers;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.importer.stream:test-core-type-contrib.xml")
public abstract class TestAutomation {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    public abstract void addExtraParams(Map<String, Object> params);

    @Test
    public void testRandomBlobImport() throws Exception {
        final int nbThreads = 4;

        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", 100);
        params.put("nbThreads", nbThreads);
        params.put("logSize", 2 * nbThreads);
        addExtraParams(params);
        automationService.run(ctx, RandomBlobProducers.ID, params);

        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        addExtraParams(params);
        automationService.run(ctx, BlobConsumers.ID, params);
    }

    @Test
    public void testFileBlobImport() throws Exception {
        final int nbThreads = 4;

        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", 10);
        params.put("nbThreads", nbThreads);
        params.put("logSize", nbThreads);
        params.put("basePath", this.getClass().getClassLoader().getResource("files").getPath());
        params.put("listFile", this.getClass().getClassLoader().getResource("files/list.txt").getPath());
        addExtraParams(params);
        automationService.run(ctx, FileBlobProducers.ID, params);

        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("watermark", "foo");
        addExtraParams(params);
        automationService.run(ctx, BlobConsumers.ID, params);
    }

    @Test
    public void testDocumentImport() throws Exception {
        final int nbThreads = 4;
        final long nbDocuments = 100;


        Map<String, Object> params = new HashMap<>();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        addExtraParams(params);
        automationService.run(ctx, RandomDocumentProducers.ID, params);

        params.clear();
        params.put("rootFolder", "/");
        addExtraParams(params);
        automationService.run(ctx, DocumentConsumers.ID, params);

        // start a new transaction to prevent db isolation to hide our new documents
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModelList ret = session.query("SELECT * FROM Document WHERE ecm:primaryType IN ('File', 'Folder')");
        assertEquals(nbThreads * nbDocuments, ret.size());
    }

    @Test
    public void testBlobAndDocumentImport() throws Exception {
        final int nbBlobs = 10;
        final int nbDocuments = 100;
        final int nbThreads = 4;
        final String marker = "youknowforsearch";

        // 1. generates random blob messages
        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", nbBlobs);
        params.put("nbThreads", nbThreads);
        params.put("marker", marker);
        addExtraParams(params);
        automationService.run(ctx, RandomBlobProducers.ID, params);

        // 2. import blobs into the binarystore, saving blob info into csv
        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/blob-info");
        addExtraParams(params);
        automationService.run(ctx, BlobConsumers.ID, params);

        // 3. generates random document messages with blob references
        params.clear();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/blob-info");
        addExtraParams(params);
        automationService.run(ctx, RandomDocumentProducers.ID, params);

        // 4. import document into the repository
        params.clear();
        params.put("rootFolder", "/");
        params.put("nbThreads", nbThreads);
        params.put("useBulkMode", true);
        params.put("blockDefaultSyncListeners", true);
        params.put("blockPostCommitListeners", true);
        params.put("blockAsyncListeners", true);
        params.put("blockIndexing", true);
        addExtraParams(params);
        automationService.run(ctx, DocumentConsumers.ID, params);

        // WorkManager service = Framework.getService(WorkManager.class);
        // assertTrue(service.awaitCompletion(10, TimeUnit.SECONDS));

        // start a new transaction to prevent db isolation to hide our new documents
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModelList ret = session.query("SELECT * FROM Document WHERE ecm:primaryType IN ('File', 'Folder')");
        assertEquals(nbThreads * nbDocuments, ret.size());

        int createdFiles = session.query("SELECT * FROM Document WHERE ecm:primaryType IN ('File')").size();
        assertTrue("No file created", createdFiles > 0);

        // Check that all files has a non null blob
        int createdBlobs = session.query("SELECT * FROM Document WHERE  content/length > 0").size();
        assertEquals(createdFiles, createdBlobs);
    }

    @Test
    public void testFileBlobAndDocumentImport() throws Exception {
        final int nbBlobs = 10;
        final int nbDocuments = 20;
        final int nbThreads = 2;

        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", nbBlobs);
        params.put("nbThreads", nbThreads);
        params.put("basePath", this.getClass().getClassLoader().getResource("files").getPath());
        params.put("listFile", this.getClass().getClassLoader().getResource("files/list.txt").getPath());
        addExtraParams(params);
        automationService.run(ctx, FileBlobProducers.ID, params);

        // 2. import blobs into the binarystore, saving blob info into a log
        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/blob-info");
        addExtraParams(params);
        automationService.run(ctx, BlobConsumers.ID, params);

        // 3. generates random document messages with blob references
        params.clear();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        params.put("countFolderAsDocument", false);
        params.put("logBlobInfo", "import/blob-info");
        addExtraParams(params);
        automationService.run(ctx, RandomDocumentProducers.ID, params);

        // 4. import document into the repository
        params.clear();
        params.put("rootFolder", "/");
        params.put("nbThreads", nbThreads);
        params.put("useBulkMode", true);
        params.put("blockDefaultSyncListeners", true);
        params.put("blockPostCommitListeners", true);
        params.put("blockAsyncListeners", true);
        params.put("blockIndexing", true);
        addExtraParams(params);
        automationService.run(ctx, DocumentConsumers.ID, params);

        // WorkManager service = Framework.getService(WorkManager.class);
        // assertTrue(service.awaitCompletion(10, TimeUnit.SECONDS));

        // start a new transaction to prevent db isolation to hide our new documents
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        int createdDocuments = session.query(
                "SELECT * FROM Document WHERE ecm:primaryType IN ('File', 'Picture', 'Video')").size();
        assertEquals(nbThreads * nbDocuments, createdDocuments);

        // Check that all documents have a blob
        int createdBlobs = session.query("SELECT * FROM Document WHERE  content/length > 0").size();
        assertEquals(createdDocuments, createdBlobs);
    }

}
