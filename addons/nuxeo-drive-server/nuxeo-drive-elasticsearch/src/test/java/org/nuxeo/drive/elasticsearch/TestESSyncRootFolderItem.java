/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link ESSyncRootFolderItem}.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ NuxeoDriveFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.drive.elasticsearch:OSGI-INF/nuxeodrive-elasticsearch-adapter-contrib.xml")
public class TestESSyncRootFolderItem {

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected WorkManager workManager;

    protected DocumentModel syncRootFolder;

    protected ESSyncRootFolderItemFactory esSyncRootFolderItemFactory;

    @Before
    public void createTestDocs() throws Exception {

        // Drop and initialize ES indexes
        esa.initIndexes(true);

        // Register a sync root
        syncRootFolder = session.createDocumentModel("/", "syncRoot", "Folder");
        syncRootFolder = session.createDocument(syncRootFolder);
        nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), syncRootFolder, session);

        // Build a document tree under the sync root with:
        // - 4 levels
        // - 5 files per folder
        // - 2 folders per folder
        // => 105 documents
        buildAndIndexTree(syncRootFolder.getPathAsString(), 4, 5, 2);

        esSyncRootFolderItemFactory = (ESSyncRootFolderItemFactory) ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "defaultSyncRootFolderItemFactory");
    }

    protected void buildAndIndexTree(String parentPath, int maxLevel, int fileCount, int folderCount) throws Exception {
        startTransaction();
        buildTree(parentPath, maxLevel, 1, fileCount, folderCount);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

    protected void buildTree(String parentPath, int maxLevel, int level, int fileCount, int folderCount) {
        if (level > maxLevel) {
            return;
        }
        for (int i = 0; i < fileCount; i++) {
            String name = "file-" + level + "-" + i;
            DocumentModel file = session.createDocumentModel(parentPath, name, "File");
            file.setPropertyValue("dc:title", name);
            Blob blob = new StringBlob("Content of Joe's file.");
            blob.setFilename("Joe.odt");
            file.setPropertyValue("file:content", (Serializable) blob);
            session.createDocument(file);
        }
        for (int i = 0; i < folderCount; i++) {
            String name = "folder-" + level + "-" + i;
            DocumentModel folder = session.createDocumentModel(parentPath, name, "Folder");
            folder.setPropertyValue("dc:title", name);
            session.createDocument(folder);
            buildTree(parentPath + "/" + name, maxLevel, level + 1, fileCount, folderCount);
        }
    }

    protected void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
    }

    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    @Test
    public void testScrollDescendants() throws Exception {

        FolderItem syncRootFolderItem = (FolderItem) esSyncRootFolderItemFactory.getFileSystemItem(syncRootFolder);
        assertTrue(syncRootFolderItem instanceof ESSyncRootFolderItem);

        // Check scrollDescendants
        assertTrue(syncRootFolderItem.getCanScrollDescendants());

        // Scroll through all descendants in one breath
        ScrollFileSystemItemList descendants = syncRootFolderItem.scrollDescendants(null, 300, 10000);
        assertNotNull(descendants.getScrollId());
        assertEquals(105, descendants.size());
        // Check that descendants are ordered by path
        List<String> expectedFSItemIds = session.query(
                "SELECT * FROM Document WHERE ecm:ancestorId = '" + syncRootFolder.getId() + "' ORDER BY ecm:path")
                                                .stream()
                                                .map(doc -> DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc.getId())
                                                .collect(Collectors.toList());
        assertEquals(expectedFSItemIds,
                descendants.stream().map(fsItem -> fsItem.getId()).collect(Collectors.toList()));

        // Scroll through descendants in several steps
        descendants.clear();
        ScrollFileSystemItemList descendantsBatch;
        int batchSize = 15;
        String scrollId = null;
        while (!(descendantsBatch = syncRootFolderItem.scrollDescendants(scrollId, batchSize, 10000)).isEmpty()) {
            assertEquals(15, descendantsBatch.size());
            scrollId = descendantsBatch.getScrollId();
            descendants.addAll(descendantsBatch);
        }
        assertEquals(105, descendants.size());
        // Check that descendants are ordered by path
        assertEquals(expectedFSItemIds,
                descendants.stream().map(fsItem -> fsItem.getId()).collect(Collectors.toList()));
    }

}
