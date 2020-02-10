/*
 * (C) Copyright 2012-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.operations;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.adapter.impl.ScrollFileSystemItemListImpl;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests the {@link FileSystemItem} related operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
public class TestFileSystemItemOperations {

    protected static final String ADMINISTRATOR = "Administrator";

    protected static final String BATCH_SIZE = "batchSize";

    protected static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    protected static final String DEST_ID = "destId";

    protected static final String FILE_1 = "file1";

    protected static final String FILE_2 = "file2";

    protected static final String FILE_CONTENT = "file:content";

    protected static final String FOLDER_1 = "folder1";

    protected static final String FOLDER_1_PATH = "/folder1";

    protected static final String FOLDER_2 = "folder2";

    protected static final String FOLDER_2_PATH = "/folder2";

    protected static final String FOLDER_TYPE = "Folder";

    protected static final String SRC_ID = "srcId";

    protected static final String SYNC_ROOT_FOLDER_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    protected static final TypeReference<List<DefaultSyncRootFolderItem>> LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM = new TypeReference<List<DefaultSyncRootFolderItem>>() {
    };

    protected static final TypeReference<List<DocumentBackedFileItem>> LIST_DOCUMENT_BACKED_FILE_ITEM = new TypeReference<List<DocumentBackedFileItem>>() {
    };

    protected static final TypeReference<DefaultTopLevelFolderItem> DEFAULT_TOP_LEVEL_FOLDER_FOLDER_ITEM = new TypeReference<DefaultTopLevelFolderItem>() {
    };

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected TrashService trashService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    @Inject
    protected HttpAutomationSession clientSession;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel file1;

    protected DocumentModel file2;

    protected DocumentModel file3;

    protected DocumentModel file4;

    protected DocumentModel subFolder1;

    /**
     * Initializes the test hierarchy.
     *
     * <pre>
     * topLevel
     *   |-- folder1 (syncRoot1)
     *   |     |-- file1
     *   |     |-- subFolder1
     *   |           |-- file3
     *   |           |-- file4
     *   |-- folder2 (syncRoot2)
     *   |     |-- file2
     * </pre>
     */
    @Before
    public void init() {

        NuxeoPrincipal administrator = session.getPrincipal();
        // Create 2 sync roots
        syncRoot1 = session.createDocument(session.createDocumentModel("/", FOLDER_1, FOLDER_TYPE));
        syncRoot2 = session.createDocument(session.createDocumentModel("/", FOLDER_2, FOLDER_TYPE));

        // Register sync roots
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot2, session);

        // Create 1 file in each sync root
        file1 = session.createDocumentModel(FOLDER_1_PATH, FILE_1, "File");
        Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob("The content of file 1.");
        blob.setFilename("First file.odt"); // NOSONAR
        file1.setPropertyValue(FILE_CONTENT, (Serializable) blob);
        file1 = session.createDocument(file1);
        file2 = session.createDocumentModel(FOLDER_2_PATH, FILE_2, "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob("The content of file 2.");
        blob.setFilename("Second file.odt");
        file2.setPropertyValue(FILE_CONTENT, (Serializable) blob);
        file2 = session.createDocument(file2);

        // Create a sub-folder in sync root 1
        subFolder1 = session.createDocument(session.createDocumentModel(FOLDER_1_PATH, "subFolder1", FOLDER_TYPE));

        // Create 2 files in sub-folder
        file3 = session.createDocumentModel("/folder1/subFolder1", "file3", "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob("The content of file 3.");
        blob.setFilename("Third file.odt");
        file3.setPropertyValue(FILE_CONTENT, (Serializable) blob);
        file3 = session.createDocument(file3);
        file4 = session.createDocumentModel("/folder1/subFolder1", "file4", "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob("The content of file 4.");
        blob.setFilename("Fourth file.odt");
        file4.setPropertyValue(FILE_CONTENT, (Serializable) blob);
        file4 = session.createDocument(file4);

        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testGetTopLevelChildren() throws IOException {

        FolderItem topLevelFolder = clientSession.newRequest(NuxeoDriveGetTopLevelFolder.ID)
                                                 .executeReturning(DEFAULT_TOP_LEVEL_FOLDER_FOLDER_ITEM);
        assertNotNull(topLevelFolder);

        // Check children
        List<DefaultSyncRootFolderItem> topLevelChildren;
        topLevelChildren = clientSession.newRequest(NuxeoDriveGetChildren.ID)
                                        .set("id", topLevelFolder.getId())
                                        .executeReturning(LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        DefaultSyncRootFolderItem child = topLevelChildren.get(0);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(), child.getId());
        assertTrue(child.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals(FOLDER_1, child.getName());
        assertTrue(child.isFolder());
        assertEquals(ADMINISTRATOR, child.getCreator());
        assertEquals(ADMINISTRATOR, child.getLastContributor());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());

        child = topLevelChildren.get(1);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(), child.getId());
        assertTrue(child.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals(FOLDER_2, child.getName());
        assertTrue(child.isFolder());
        assertEquals(ADMINISTRATOR, child.getCreator());
        assertEquals(ADMINISTRATOR, child.getLastContributor());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());

        // Check descendants
        assertFalse(topLevelFolder.getCanScrollDescendants());
        String error = clientSession.newRequest(NuxeoDriveScrollDescendants.ID)
                                    .set("id", topLevelFolder.getId())
                                    .set(BATCH_SIZE, 10)
                                    .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);
    }

    @Test
    public void testFileSystemItemExists() throws IOException {

        // Non existing file system item
        Boolean fileSystemItemExists = clientSession.newRequest(NuxeoDriveFileSystemItemExists.ID)
                                                    .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + "badId")
                                                    .executeReturning(Boolean.class);
        assertFalse(fileSystemItemExists);

        // Existing file system item
        fileSystemItemExists = clientSession.newRequest(NuxeoDriveFileSystemItemExists.ID)
                                            .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                            .executeReturning(Boolean.class);
        assertTrue(fileSystemItemExists);

        // Deleted file system item
        trashService.trashDocument(file1);
        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        fileSystemItemExists = clientSession.newRequest(NuxeoDriveFileSystemItemExists.ID)
                                            .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                            .executeReturning(Boolean.class);
        assertFalse(fileSystemItemExists);
    }

    @Test
    public void testGetFileSystemItem() throws IOException {

        // Get top level folder
        String topLevelFolderItemId = fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                                                  .getTopLevelFolderItem(session.getPrincipal())
                                                                  .getId();
        DefaultTopLevelFolderItem topLevelFolderItem = clientSession.newRequest(NuxeoDriveGetFileSystemItem.ID)
                                                                    .set("id", topLevelFolderItemId)
                                                                    .executeReturning(DefaultTopLevelFolderItem.class);
        assertNotNull(topLevelFolderItem);
        assertEquals(topLevelFolderItemId, topLevelFolderItem.getId());
        assertNull(topLevelFolderItem.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
        assertTrue(topLevelFolderItem.isFolder());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolderItem.getCreator());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolderItem.getLastContributor());
        assertFalse(topLevelFolderItem.getCanRename());
        assertFalse(topLevelFolderItem.getCanDelete());
        assertFalse(topLevelFolderItem.getCanCreateChild());

        // Get sync root
        DefaultSyncRootFolderItem syncRootFolderItem;
        syncRootFolderItem = clientSession.newRequest(NuxeoDriveGetFileSystemItem.ID)
                                          .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                          .executeReturning(DefaultSyncRootFolderItem.class);
        assertNotNull(syncRootFolderItem);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(), syncRootFolderItem.getId());
        assertTrue(syncRootFolderItem.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals(FOLDER_1, syncRootFolderItem.getName());
        assertTrue(syncRootFolderItem.isFolder());
        assertEquals(ADMINISTRATOR, syncRootFolderItem.getCreator());
        assertEquals(ADMINISTRATOR, syncRootFolderItem.getLastContributor());
        assertTrue(syncRootFolderItem.getCanRename());
        assertTrue(syncRootFolderItem.getCanDelete());
        assertTrue(syncRootFolderItem.getCanCreateChild());

        // Get file in sync root
        DocumentBackedFileItem fileItem = clientSession.newRequest(NuxeoDriveGetFileSystemItem.ID)
                                                       .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                                       .executeReturning(DocumentBackedFileItem.class);
        assertNotNull(fileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId(), fileItem.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(), fileItem.getParentId());
        assertEquals("First file.odt", fileItem.getName());
        assertFalse(fileItem.isFolder());
        assertEquals(ADMINISTRATOR, fileItem.getCreator());
        assertEquals(ADMINISTRATOR, fileItem.getLastContributor());
        assertTrue(fileItem.getCanRename());
        assertTrue(fileItem.getCanDelete());
        assertTrue(fileItem.getCanUpdate());
        assertEquals("nxfile/test/" + file1.getId() + "/blobholder:0/First%20file.odt", fileItem.getDownloadURL()); // NOSONAR
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(((Blob) file1.getPropertyValue(FILE_CONTENT)).getDigest(), fileItem.getDigest());

        // Get deleted file
        trashService.trashDocument(file1);
        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        fileItem = clientSession.newRequest(NuxeoDriveGetFileSystemItem.ID)
                                .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                .executeReturning(DocumentBackedFileItem.class);
        assertNull(fileItem);
    }

    @Test
    public void testGetChildren() throws IOException {

        // Get children of sub-folder of sync root 1
        List<DocumentBackedFileItem> children;
        children = clientSession.newRequest(NuxeoDriveGetChildren.ID)
                                .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId())
                                .executeReturning(LIST_DOCUMENT_BACKED_FILE_ITEM);
        assertNotNull(children);
        assertEquals(2, children.size());

        // Ordered
        checkChildren(children, subFolder1.getId(), file3.getId(), file4.getId(), true);
    }

    @Test
    public void testScrollDescendants() throws IOException {

        // Get descendants of sync root 1
        // Scroll through all descendants in one breath
        ScrollFileSystemItemList descendants;
        descendants = clientSession.newRequest(NuxeoDriveScrollDescendants.ID)
                                   .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                   .set(BATCH_SIZE, 10)
                                   .executeReturning(ScrollFileSystemItemListImpl.class);
        assertNotNull(descendants);
        assertNotNull(descendants.getScrollId());
        assertEquals(4, descendants.size());
        List<String> expectedIds = Arrays.asList(file1, subFolder1, file3, file4)
                                         .stream()
                                         .map(doc -> DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc.getId())
                                         .collect(Collectors.toList());
        List<String> descendantIds = descendants.stream().map(FileSystemItem::getId).collect(Collectors.toList());
        // Note that order is not determined
        assertTrue(CollectionUtils.isEqualCollection(expectedIds, descendantIds));

        // Scroll through descendants in several steps
        descendantIds.clear();
        int batchSize = 2;
        String scrollId = "";
        for (;;) {
            ScrollFileSystemItemListImpl descendantsBatch;
            descendantsBatch = clientSession.newRequest(NuxeoDriveScrollDescendants.ID)
                                            .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                            .set(BATCH_SIZE, batchSize)
                                            .set("scrollId", scrollId)
                                            .executeReturning(ScrollFileSystemItemListImpl.class);
            if (descendantsBatch.isEmpty()) {
                break;
            }
            assertFalse(descendantsBatch.isEmpty());
            scrollId = descendantsBatch.getScrollId();
            descendantIds.addAll(descendantsBatch.stream().map(FileSystemItem::getId).collect(Collectors.toList()));
        }
        assertEquals(4, descendantIds.size());
        // Note that order is not determined
        assertTrue(CollectionUtils.isEqualCollection(expectedIds, descendantIds));

        // Check descendants of sub-folder of sync root 1
        JsonNode node = clientSession.newRequest(NuxeoDriveScrollDescendants.ID)
                                     .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId())
                                     .set(BATCH_SIZE, 10)
                                     .execute();
        assertTrue(CollectionUtils.isEqualCollection(Stream.of(file3, file4)
                                                           .map(doc -> DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc.getId())
                                                           .collect(Collectors.toList()),
                node.findValuesAsText("id")));
    }

    @Test
    public void testCreateFolder() throws IOException {

        DocumentBackedFolderItem newFolder;
        newFolder = clientSession.newRequest(NuxeoDriveCreateFolder.ID)
                                 .set("parentId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                 .set("name", "newFolder")
                                 .executeReturning(DocumentBackedFolderItem.class);
        assertNotNull(newFolder);

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        session.save();

        DocumentModel newFolderDoc = session.getDocument(new PathRef("/folder2/newFolder"));
        assertEquals(FOLDER_TYPE, newFolderDoc.getType());
        assertEquals("newFolder", newFolderDoc.getTitle());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFolderDoc.getId(), newFolder.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(), newFolder.getParentId());
        assertEquals("newFolder", newFolder.getName());
        assertTrue(newFolder.isFolder());
        assertEquals(ADMINISTRATOR, newFolder.getCreator());
        assertEquals(ADMINISTRATOR, newFolder.getLastContributor());
        assertTrue(newFolder.getCanRename());
        assertTrue(newFolder.getCanDelete());
        assertTrue(newFolder.getCanCreateChild());
    }

    @Test
    public void testCreateFile() throws IOException {

        Blob blob = Blobs.createBlob("This is the content of a new file.");
        blob.setFilename("New file.odt");
        DocumentBackedFileItem newFile;
        newFile = clientSession.newRequest(NuxeoDriveCreateFile.ID)
                               .set("parentId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId())
                               .setInput(blob)
                               .executeReturning(DocumentBackedFileItem.class);
        assertNotNull(newFile);

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        session.save();

        DocumentModel newFileDoc = session.getDocument(new PathRef("/folder1/subFolder1/New file.odt"));
        assertEquals("File", newFileDoc.getType());
        assertEquals("New file.odt", newFileDoc.getTitle());
        Blob newFileBlob = (Blob) newFileDoc.getPropertyValue(FILE_CONTENT);
        assertNotNull(newFileBlob);
        assertEquals("New file.odt", newFileBlob.getFilename());
        assertEquals("This is the content of a new file.", newFileBlob.getString());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFileDoc.getId(), newFile.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId(), newFile.getParentId());
        assertEquals("New file.odt", newFile.getName());
        assertFalse(newFile.isFolder());
        assertEquals(ADMINISTRATOR, newFile.getCreator());
        assertEquals(ADMINISTRATOR, newFile.getLastContributor());
        assertTrue(newFile.getCanRename());
        assertTrue(newFile.getCanDelete());
        assertTrue(newFile.getCanUpdate());
        assertEquals("nxfile/test/" + newFileDoc.getId() + "/blobholder:0/New%20file.odt", newFile.getDownloadURL());
        assertEquals("MD5", newFile.getDigestAlgorithm());
        assertEquals(newFileBlob.getDigest(), newFile.getDigest());
    }

    @Test
    public void testUpdateFile() throws IOException {

        Blob blob = Blobs.createBlob("This is the updated content of file 1.");
        blob.setFilename("Updated file 1.odt");
        DocumentBackedFileItem updatedFile = clientSession.newRequest(NuxeoDriveUpdateFile.ID)
                                                          .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                                          .setInput(blob)
                                                          .executeReturning(DocumentBackedFileItem.class);
        assertNotNull(updatedFile);

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        session.save();

        DocumentModel updatedFileDoc = session.getDocument(new IdRef(file1.getId()));
        assertEquals("File", updatedFileDoc.getType());
        assertEquals(FILE_1, updatedFileDoc.getTitle());
        Blob updatedFileBlob = (Blob) updatedFileDoc.getPropertyValue(FILE_CONTENT);
        assertNotNull(updatedFileBlob);
        assertEquals("Updated file 1.odt", updatedFileBlob.getFilename());
        assertEquals("This is the updated content of file 1.", updatedFileBlob.getString());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + updatedFileDoc.getId(), updatedFile.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(), updatedFile.getParentId());
        assertEquals("Updated file 1.odt", updatedFile.getName());
        assertFalse(updatedFile.isFolder());
        assertEquals(ADMINISTRATOR, updatedFile.getCreator());
        assertEquals(ADMINISTRATOR, updatedFile.getLastContributor());
        assertTrue(updatedFile.getCanRename());
        assertTrue(updatedFile.getCanDelete());
        assertTrue(updatedFile.getCanUpdate());
        assertEquals("nxfile/test/" + updatedFileDoc.getId() + "/blobholder:0/Updated%20file%201.odt",
                updatedFile.getDownloadURL());
        assertEquals("MD5", updatedFile.getDigestAlgorithm());
        assertEquals(updatedFileBlob.getDigest(), updatedFile.getDigest());
    }

    @Test
    public void testDelete() throws IOException {

        // ------------------------------------------------------
        // Delete file in sync root: should trash it
        // ------------------------------------------------------
        clientSession.newRequest(NuxeoDriveDelete.ID)
                     .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                     .execute();

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel deletedFileDoc = session.getDocument(new IdRef(file1.getId()));
        assertTrue(deletedFileDoc.isTrashed());

        // ------------------------------------------------------
        // Delete sync root: should unregister it
        // ------------------------------------------------------
        clientSession.newRequest(NuxeoDriveDelete.ID)
                     .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                     .execute();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(session).contains(new IdRef(syncRoot2.getId())));

        // ------------------------------------------------------
        // Delete top level folder: should be unsupported
        // ------------------------------------------------------
        String error = clientSession.newRequest(NuxeoDriveDelete.ID)
                                    .set("id",
                                            fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                                                        .getTopLevelFolderItem(session.getPrincipal())
                                                                        .getId())
                                    .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.Delete", error);
    }

    @Test
    public void testRename() throws IOException {

        // ------------------------------------------------------
        // File
        // ------------------------------------------------------
        DocumentBackedFileItem renamedFileItem;
        renamedFileItem = clientSession.newRequest(NuxeoDriveRename.ID)
                                       .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                       .set("name", "Renamed file 1.odt")
                                       .executeReturning(DocumentBackedFileItem.class);
        assertNotNull(renamedFileItem);
        assertEquals("Renamed file 1.odt", renamedFileItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        session.save();

        DocumentModel renamedFileDoc = session.getDocument(new IdRef(file1.getId()));
        assertEquals(FILE_1, renamedFileDoc.getTitle());
        Blob renamedFileBlob = (Blob) renamedFileDoc.getPropertyValue(FILE_CONTENT);
        assertNotNull(renamedFileBlob);
        assertEquals("Renamed file 1.odt", renamedFileBlob.getFilename());
        assertEquals("nxfile/test/" + file1.getId() + "/blobholder:0/Renamed%20file%201.odt",
                renamedFileItem.getDownloadURL());
        assertEquals("MD5", renamedFileItem.getDigestAlgorithm());
        assertEquals(renamedFileBlob.getDigest(), renamedFileItem.getDigest());

        // ------------------------------------------------------
        // Folder
        // ------------------------------------------------------
        DocumentBackedFolderItem renamedFolderItem;
        renamedFolderItem = clientSession.newRequest(NuxeoDriveRename.ID)
                                         .set("id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId())
                                         .set("name", "Renamed sub-folder 1")
                                         .executeReturning(DocumentBackedFolderItem.class);
        assertNotNull(renamedFolderItem);
        assertEquals("Renamed sub-folder 1", renamedFolderItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel renamedFolderDoc = session.getDocument(new IdRef(subFolder1.getId()));
        assertEquals("Renamed sub-folder 1", renamedFolderDoc.getTitle());

        // ------------------------------------------------------
        // Sync root
        // ------------------------------------------------------
        DefaultSyncRootFolderItem renamedSyncRootItem;
        renamedSyncRootItem = clientSession.newRequest(NuxeoDriveRename.ID)
                                           .set("id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                           .set("name", "New name for sync root")
                                           .executeReturning(DefaultSyncRootFolderItem.class);
        assertNotNull(renamedSyncRootItem);
        assertEquals("New name for sync root", renamedSyncRootItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel renamedSyncRootDoc = session.getDocument(new IdRef(syncRoot1.getId()));
        assertEquals("New name for sync root", renamedSyncRootDoc.getTitle());

        // ------------------------------------------------------
        // Top level folder
        // ------------------------------------------------------
        String error = clientSession.newRequest(NuxeoDriveRename.ID)
                                    .set("id",
                                            fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                                                        .getTopLevelFolderItem(session.getPrincipal())
                                                                        .getId())
                                    .set("name", "New name for top level folder")
                                    .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.Rename", error);
    }

    /**
     * @deprecated since 10.3, see {@link NuxeoDriveCanMove}
     */
    @Test
    @Deprecated
    public void testCanMove() throws IOException {

        // ------------------------------------------------------
        // File to File => false
        // ------------------------------------------------------
        Boolean canMoveFSItem = clientSession.newRequest(NuxeoDriveCanMove.ID)
                                             .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                             .set(DEST_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file2.getId())
                                             .executeReturning(Boolean.class);
        assertFalse(canMoveFSItem);

        // ------------------------------------------------------
        // Sync root => false
        // ------------------------------------------------------
        canMoveFSItem = clientSession.newRequest(NuxeoDriveCanMove.ID)
                                     .set(SRC_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                                     .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                     .executeReturning(Boolean.class);
        assertFalse(canMoveFSItem);

        // ------------------------------------------------------
        // Top level folder => false
        // ------------------------------------------------------
        canMoveFSItem = clientSession.newRequest(NuxeoDriveCanMove.ID)
                                     .set(SRC_ID,
                                             fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                                                         .getTopLevelFolderItem(session.getPrincipal())
                                                                         .getId())
                                     .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                     .executeReturning(Boolean.class);
        assertFalse(canMoveFSItem);

        // --------------------------------------------------------
        // No REMOVE permission on the source backing doc => false
        // --------------------------------------------------------
        NuxeoPrincipal joe = createUser("joe", "joe");
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot2, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        HttpAutomationSession joeSession = automationClient.getSession("joe", "joe");
        canMoveFSItem = joeSession.newRequest(NuxeoDriveCanMove.ID)
                                  .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                  .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                  .executeReturning(Boolean.class);
        assertFalse(canMoveFSItem);

        // -------------------------------------------------------------------
        // No ADD_CHILDREN permission on the destination backing doc => false
        // -------------------------------------------------------------------
        setPermission(syncRoot1, "joe", SecurityConstants.WRITE, true);
        canMoveFSItem = joeSession.newRequest(NuxeoDriveCanMove.ID)
                                  .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                  .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                  .executeReturning(Boolean.class);
        assertFalse(canMoveFSItem);

        // ----------------------------------------------------------------------
        // REMOVE permission on the source backing doc + REMOVE_CHILDREN
        // permission on its parent + ADD_CHILDREN permission on the destination
        // backing doc => true
        // ----------------------------------------------------------------------
        setPermission(syncRoot2, "joe", SecurityConstants.WRITE, true);

        nuxeoDriveManager.unregisterSynchronizationRoot(joe, syncRoot2, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        canMoveFSItem = joeSession.newRequest(NuxeoDriveCanMove.ID)
                                  .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                  .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                  .executeReturning(Boolean.class);
        // syncRoot2 is not registered as a sync root for joe
        assertFalse(canMoveFSItem);

        nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot2, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        canMoveFSItem = joeSession.newRequest(NuxeoDriveCanMove.ID)
                                  .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                  .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                  .executeReturning(Boolean.class);
        // syncRoot2 is now a registered root for joe
        assertTrue(canMoveFSItem);

        // ----------------------------------------------------------------------
        // Reset permissions
        // ----------------------------------------------------------------------
        resetPermissions(rootDoc, "joe");
        resetPermissions(syncRoot1, "joe");
        resetPermissions(syncRoot2, "joe");
        deleteUser("joe");
    }

    @Test
    public void testMove() throws IOException {

        // ------------------------------------------------------
        // File to File => fail
        // ------------------------------------------------------
        String error = clientSession.newRequest(NuxeoDriveMove.ID)
                                    .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                    .set(DEST_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file2.getId())
                                    .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        String expectedMessage = String.format(
                "Failed to invoke operation: NuxeoDrive.Move, Failed to invoke operation NuxeoDrive.Move, "
                        + "Cannot move a file system item to file system item with id %s because it is not a folder.",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file2.getId());
        assertEquals(expectedMessage, error);

        // ------------------------------------------------------
        // Sync root => fail
        // ------------------------------------------------------
        error = clientSession.newRequest(NuxeoDriveMove.ID)
                             .set(SRC_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId())
                             .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                             .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.Move", error);

        // ------------------------------------------------------
        // Top level folder => fail
        // ------------------------------------------------------
        error = clientSession.newRequest(NuxeoDriveMove.ID)
                             .set(SRC_ID,
                                     fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                                                 .getTopLevelFolderItem(session.getPrincipal())
                                                                 .getId())
                             .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                             .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.Move", error);

        // ------------------------------------------------------
        // File to Folder => succeed
        // ------------------------------------------------------
        DocumentBackedFileItem movedFileItem;
        movedFileItem = clientSession.newRequest(NuxeoDriveMove.ID)
                                     .set(SRC_ID, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId())
                                     .set(DEST_ID, SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId())
                                     .executeReturning(DocumentBackedFileItem.class);
        assertNotNull(movedFileItem);
        assertEquals("First file.odt", movedFileItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by the file system item
        session.save();

        DocumentModel movedFileDoc = session.getDocument(new IdRef(file1.getId()));
        assertEquals("/folder2/file1", movedFileDoc.getPathAsString());
        assertEquals(FILE_1, movedFileDoc.getTitle());
        Blob movedFileBlob = (Blob) movedFileDoc.getPropertyValue(FILE_CONTENT);
        assertNotNull(movedFileBlob);
        assertEquals("First file.odt", movedFileBlob.getFilename());
        assertEquals("MD5", movedFileItem.getDigestAlgorithm());
        assertEquals(movedFileBlob.getDigest(), movedFileItem.getDigest());
    }

    /**
     * @deprecated since 10.3, see {@link NuxeoDriveGenerateConflictedItemName}
     */
    @Test
    @Deprecated
    public void testConflictedNames() throws IOException {
        // Try a canonical example with the Administrator user
        String newName = clientSession.newRequest(NuxeoDriveGenerateConflictedItemName.ID)
                                      .set("name", "My file (with accents \u00e9).doc")
                                      .executeReturning(String.class);
        assertTrue(newName.startsWith("My file (with accents \u00e9) (Administrator - "));
        assertTrue(newName.endsWith(").doc"));

        // Try with a filename with filename extension
        newName = clientSession.newRequest(NuxeoDriveGenerateConflictedItemName.ID)
                               .set("name", "My file")
                               .executeReturning(String.class);
        assertTrue(newName.startsWith("My file (Administrator - "));
        assertTrue(newName.endsWith(")"));

        // Test with a user that has a firstname and a lastname
        // Joe Strummer likes conflicting files
        createUser("joe", "joe", "Joe", "Strummer");
        HttpAutomationSession joeSession = automationClient.getSession("joe", "joe");
        newName = joeSession.newRequest(NuxeoDriveGenerateConflictedItemName.ID)
                            .set("name", "The Clashing File.xls")
                            .executeReturning(String.class);
        assertTrue(newName.startsWith("The Clashing File (Joe Strummer - "));
        assertTrue(newName.endsWith(").xls"));

        deleteUser("joe");
    }

    protected NuxeoPrincipal createUser(String userName, String password) {
        return createUser(userName, password, null, null);
    }

    protected NuxeoPrincipal createUser(String userName, String password, String firstName, String lastName) {
        try (org.nuxeo.ecm.directory.Session userDir = directoryService.open("userDirectory")) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", userName);
            user.put("password", password);
            user.put("firstName", firstName);
            user.put("lastName", lastName);
            userDir.createEntry(user);
        }
        // commit directory changes
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        UserManager userManager = Framework.getService(UserManager.class);
        return userManager.getPrincipal(userName);
    }

    protected void deleteUser(String userName) {
        try (org.nuxeo.ecm.directory.Session userDir = directoryService.open("userDirectory")) {
            userDir.deleteEntry(userName);
        }
    }

    protected void setPermission(DocumentModel doc, String userName, String permission, boolean isGranted) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected void resetPermissions(DocumentModel doc, String userName) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Iterator<ACE> localACLIt = localACL.iterator();
        while (localACLIt.hasNext()) {
            ACE ace = localACLIt.next();
            if (userName.equals(ace.getUsername())) {
                localACLIt.remove();
            }
        }
        session.setACP(doc.getRef(), acp, true);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected void checkChildren(List<DocumentBackedFileItem> folderChildren, String folderId, String child1Id,
            String child2Id, boolean ordered) {

        boolean isChild1Found = false;
        boolean isChild2Found = false;
        int childrenCount = 0;

        for (DocumentBackedFileItem fsItem : folderChildren) {
            // Check child 1
            if (!isChild1Found && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + child1Id).equals(fsItem.getId())) {
                if (!ordered || childrenCount == 0) { // NOSONAR
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Third file.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    assertEquals(ADMINISTRATOR, fsItem.getCreator());
                    assertEquals(ADMINISTRATOR, fsItem.getLastContributor());
                    assertTrue(fsItem.getCanRename());
                    assertTrue(fsItem.getCanDelete());
                    assertTrue(fsItem.getCanUpdate());
                    assertEquals("nxfile/test/" + file3.getId() + "/blobholder:0/Third%20file.odt",
                            fsItem.getDownloadURL());
                    assertEquals("MD5", fsItem.getDigestAlgorithm());
                    assertEquals(((Blob) file3.getPropertyValue(FILE_CONTENT)).getDigest(), fsItem.getDigest());
                    isChild1Found = true;
                    childrenCount++;
                }
            }
            // Check child 2
            else if (!isChild2Found && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + child2Id).equals(fsItem.getId())) { // NOSONAR
                if (!ordered || childrenCount == 1) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Fourth file.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    assertEquals(ADMINISTRATOR, fsItem.getCreator());
                    assertEquals(ADMINISTRATOR, fsItem.getLastContributor());
                    assertTrue(fsItem.getCanRename());
                    assertTrue(fsItem.getCanDelete());
                    assertTrue(fsItem.getCanUpdate());
                    assertEquals("nxfile/test/" + file4.getId() + "/blobholder:0/Fourth%20file.odt",
                            fsItem.getDownloadURL());
                    assertEquals("MD5", fsItem.getDigestAlgorithm());
                    assertEquals(((Blob) file4.getPropertyValue(FILE_CONTENT)).getDigest(), fsItem.getDigest());
                }
            } else {
                fail(String.format("FileSystemItem %s doesn't match any expected.", fsItem.getId()));
            }
        }
    }

}
