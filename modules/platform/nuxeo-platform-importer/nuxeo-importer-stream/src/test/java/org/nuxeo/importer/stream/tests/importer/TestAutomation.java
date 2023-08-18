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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.importer.stream")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.importer.stream:test-core-type-contrib.xml")
public class TestAutomation {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Inject
    protected StreamService streamService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File(FeaturesRunner.getBuildDirectory()));

    protected OperationContext ctx;

    protected List<String> streamsToClean = new ArrayList<>();

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContextAndCleanStream() {
        ctx.close();
        LogManager manager = streamService.getLogManager();
        streamsToClean.forEach(name -> manager.delete(Name.ofUrn(name)));
        streamsToClean.clear();
    }

    @Test
    public void testRandomBlobImport() throws Exception {
        final int nbThreads = 4;

        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", 100);
        params.put("nbThreads", nbThreads);
        params.put("logSize", 2 * nbThreads);
        params.put("logName", "import/randomBlobImport");
        streamsToClean.add("import/randomBlobImport");
        automationService.run(ctx, RandomBlobProducers.ID, params);

        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("logName", "import/randomBlobImport");
        automationService.run(ctx, BlobConsumers.ID, params);
    }

    @Test
    public void testFileBlobImport() throws Exception {
        final int nbThreads = 4;

        Map<String, Object> params = new HashMap<>();
        params.put("nbBlobs", 10);
        params.put("nbThreads", nbThreads);
        params.put("logSize", nbThreads);
        params.put("logName", "import/fileBlob");
        streamsToClean.add("import/fileBlob");
        params.put("basePath", this.getClass().getClassLoader().getResource("files").getPath());
        params.put("listFile", this.getClass().getClassLoader().getResource("files/list.txt").getPath());
        automationService.run(ctx, FileBlobProducers.ID, params);

        params.clear();
        params.put("blobProviderName", "test");
        params.put("logName", "import/fileBlob");
        params.put("nbThreads", nbThreads);
        params.put("watermark", "foo");
        automationService.run(ctx, BlobConsumers.ID, params);
    }

    @Test
    public void testDocumentImport() throws Exception {
        final int nbThreads = 4;
        final long nbDocuments = 100;

        Map<String, Object> params = new HashMap<>();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        params.put("logName", "import/docImport");
        streamsToClean.add("import/docImport");
        automationService.run(ctx, RandomDocumentProducers.ID, params);

        params.clear();
        params.put("rootFolder", "/");
        params.put("logName", "import/docImport");
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
        params.put("logName", "import/blobDoc-blob");
        streamsToClean.add("import/blobDoc-blob");
        automationService.run(ctx, RandomBlobProducers.ID, params);

        // 2. import blobs into the binarystore, saving blob info into csv
        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/blobDoc-blobInfo");
        streamsToClean.add("import/blobDoc-blobInfo");
        params.put("logName", "import/blobDoc-blob");
        automationService.run(ctx, BlobConsumers.ID, params);

        // 3. generates random document messages with blob references
        params.clear();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/blobDoc-blobInfo");
        params.put("logName", "import/blobDoc-doc");
        streamsToClean.add("import/blobDoc-doc");
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
        params.put("logName", "import/blobDoc-doc");
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
        params.put("logName", "import/fileBlobDoc-blob");
        streamsToClean.add("import/fileBlobDoc-blob");
        automationService.run(ctx, FileBlobProducers.ID, params);

        // 2. import blobs into the binarystore, saving blob info into a log
        params.clear();
        params.put("blobProviderName", "test");
        params.put("nbThreads", nbThreads);
        params.put("logBlobInfo", "import/fileBlobDoc-blobInfo");
        streamsToClean.add("import/fileBlobDoc-blobInfo");
        params.put("logName", "import/fileBlobDoc-blob");
        automationService.run(ctx, BlobConsumers.ID, params);

        // 3. generates random document messages with blob references
        params.clear();
        params.put("nbDocuments", nbDocuments);
        params.put("nbThreads", nbThreads);
        params.put("countFolderAsDocument", false);
        params.put("logBlobInfo", "import/fileBlobDoc-blobInfo");
        params.put("logName", "import/fileBlobDoc-doc");
        streamsToClean.add("import/fileBlobDoc-doc");
        automationService.run(ctx, RandomDocumentProducers.ID, params);

        // 4. import document into the repository
        params.clear();
        params.put("rootFolder", "/");
        params.put("logName", "import/fileBlobDoc-doc");
        params.put("nbThreads", nbThreads);
        params.put("useBulkMode", true);
        params.put("blockDefaultSyncListeners", true);
        params.put("blockPostCommitListeners", true);
        params.put("blockAsyncListeners", true);
        params.put("blockIndexing", true);
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
