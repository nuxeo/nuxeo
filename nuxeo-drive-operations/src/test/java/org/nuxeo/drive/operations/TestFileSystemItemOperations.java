/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.StringBlob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItem} related operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestFileSystemItemOperations {

    private static final String SYNC_ROOT_FOLDER_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    @Inject
    protected Session clientSession;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel file1;

    protected DocumentModel file2;

    protected DocumentModel file3;

    protected DocumentModel file4;

    protected DocumentModel subFolder1;

    protected ObjectMapper mapper;

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
    public void init() throws Exception {

        Principal administrator = session.getPrincipal();
        // Create 2 sync roots
        syncRoot1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));

        // Register sync roots
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot1,
                session);
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot2,
                session);

        // Create 1 file in each sync root
        file1 = session.createDocumentModel("/folder1", "file1", "File");
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 1.");
        blob.setFilename("First file.odt");
        file1.setPropertyValue("file:content", (Serializable) blob);
        file1 = session.createDocument(file1);
        file2 = session.createDocumentModel("/folder2", "file2", "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 2.");
        blob.setFilename("Second file.odt");
        file2.setPropertyValue("file:content", (Serializable) blob);
        file2 = session.createDocument(file2);

        // Create a sub-folder in sync root 1
        subFolder1 = session.createDocument(session.createDocumentModel(
                "/folder1", "subFolder1", "Folder"));

        // Create 2 files in sub-folder
        file3 = session.createDocumentModel("/folder1/subFolder1", "file3",
                "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 3.");
        blob.setFilename("Third file.odt");
        file3.setPropertyValue("file:content", (Serializable) blob);
        file3 = session.createDocument(file3);
        file4 = session.createDocumentModel("/folder1/subFolder1", "file4",
                "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 4.");
        blob.setFilename("Fourth file.odt");
        file4.setPropertyValue("file:content", (Serializable) blob);
        file4 = session.createDocument(file4);

        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        mapper = new ObjectMapper();
    }

    @Test
    public void testGetTopLevelChildren() throws Exception {

        Blob topLevelFolderJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetTopLevelFolder.ID).execute();
        assertNotNull(topLevelFolderJSON);

        FolderItem topLevelFolder = mapper.readValue(
                topLevelFolderJSON.getStream(),
                new TypeReference<DefaultTopLevelFolderItem>() {
                });

        Blob topLevelChildrenJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetChildren.ID).set("id", topLevelFolder.getId()).execute();
        List<DefaultSyncRootFolderItem> topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        DefaultSyncRootFolderItem child = topLevelChildren.get(0);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                child.getId());
        assertTrue(child.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory#"));
        assertEquals("folder1", child.getName());
        assertTrue(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());

        child = topLevelChildren.get(1);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(),
                child.getId());
        assertTrue(child.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory#"));
        assertEquals("folder2", child.getName());
        assertTrue(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());
    }

    @Test
    public void testFileSystemItemExists() throws Exception {

        // Non existing file system item
        Blob fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + "badId").execute();
        assertNotNull(fileSystemItemExistsJSON);

        String fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("false", fileSystemItemExists);

        // Existing file system item
        fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).execute();
        assertNotNull(fileSystemItemExistsJSON);

        fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("true", fileSystemItemExists);

        // Deleted file system item
        file1.followTransition("delete");
        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).execute();
        assertNotNull(fileSystemItemExistsJSON);

        fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("false", fileSystemItemExists);
    }

    @Test
    public void testGetFileSystemItem() throws Exception {

        // Get top level folder
        String topLevelFolderItemId = fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                session.getPrincipal()).getId();
        Blob fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id", topLevelFolderItemId).execute();
        assertNotNull(fileSystemItemJSON);

        DefaultTopLevelFolderItem topLevelFolderItem = mapper.readValue(
                fileSystemItemJSON.getStream(), DefaultTopLevelFolderItem.class);
        assertNotNull(topLevelFolderItem);
        assertEquals(topLevelFolderItemId, topLevelFolderItem.getId());
        assertNull(topLevelFolderItem.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
        assertTrue(topLevelFolderItem.isFolder());
        assertEquals("system", topLevelFolderItem.getCreator());
        assertFalse(topLevelFolderItem.getCanRename());
        assertFalse(topLevelFolderItem.getCanDelete());
        assertFalse(topLevelFolderItem.getCanCreateChild());

        // Get sync root
        fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DefaultSyncRootFolderItem syncRootFolderItem = mapper.readValue(
                fileSystemItemJSON.getStream(), DefaultSyncRootFolderItem.class);
        assertNotNull(syncRootFolderItem);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                syncRootFolderItem.getId());
        assertTrue(syncRootFolderItem.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory#"));
        assertEquals("folder1", syncRootFolderItem.getName());
        assertTrue(syncRootFolderItem.isFolder());
        assertEquals("Administrator", syncRootFolderItem.getCreator());
        assertTrue(syncRootFolderItem.getCanRename());
        assertTrue(syncRootFolderItem.getCanDelete());
        assertTrue(syncRootFolderItem.getCanCreateChild());

        // Get file in sync root
        fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DocumentBackedFileItem fileItem = mapper.readValue(
                fileSystemItemJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(fileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId(),
                fileItem.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                fileItem.getParentId());
        assertEquals("First file.odt", fileItem.getName());
        assertFalse(fileItem.isFolder());
        assertEquals("Administrator", fileItem.getCreator());
        assertTrue(fileItem.getCanRename());
        assertTrue(fileItem.getCanDelete());
        assertTrue(fileItem.getCanUpdate());
        assertEquals("nxbigfile/test/" + file1.getId()
                + "/blobholder:0/First%20file.odt", fileItem.getDownloadURL());
        assertEquals("md5", fileItem.getDigestAlgorithm());
        assertEquals(
                ((org.nuxeo.ecm.core.api.Blob) file1.getPropertyValue("file:content")).getDigest(),
                fileItem.getDigest());

        // Get deleted file
        file1.followTransition("delete");
        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        assertNull(mapper.readValue(fileSystemItemJSON.getStream(),
                Object.class));
    }

    @Test
    public void testGetChildren() throws Exception {

        // Get children of sub-folder of sync root 1
        Blob childrenJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId()).execute();
        assertNotNull(childrenJSON);

        List<DocumentBackedFileItem> children = mapper.readValue(
                childrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(children);
        assertEquals(2, children.size());

        // Don't check children order against MySQL database because of the
        // milliseconds limitation
        boolean ordered = !(DatabaseHelper.DATABASE instanceof DatabaseMySQL);
        checkChildren(children, subFolder1.getId(), file3.getId(),
                file4.getId(), ordered);
    }

    @Test
    public void testCreateFolder() throws Exception {

        Blob newFolderJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCreateFolder.ID).set("parentId",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).set(
                "name", "newFolder").execute();
        assertNotNull(newFolderJSON);

        DocumentBackedFolderItem newFolder = mapper.readValue(
                newFolderJSON.getStream(), DocumentBackedFolderItem.class);
        assertNotNull(newFolder);

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        session.save();

        DocumentModel newFolderDoc = session.getDocument(new PathRef(
                "/folder2/newFolder"));
        assertEquals("Folder", newFolderDoc.getType());
        assertEquals("newFolder", newFolderDoc.getTitle());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFolderDoc.getId(),
                newFolder.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(),
                newFolder.getParentId());
        assertEquals("newFolder", newFolder.getName());
        assertTrue(newFolder.isFolder());
        assertEquals("Administrator", newFolder.getCreator());
        assertTrue(newFolder.getCanRename());
        assertTrue(newFolder.getCanDelete());
        assertTrue(newFolder.getCanCreateChild());
    }

    @Test
    public void testCreateFile() throws Exception {

        StringBlob blob = new StringBlob("This is the content of a new file.");
        blob.setFileName("New file.odt");
        Blob newFileJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCreateFile.ID).set("parentId",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId()).setInput(
                blob).execute();
        assertNotNull(newFileJSON);

        DocumentBackedFileItem newFile = mapper.readValue(
                newFileJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(newFile);

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        session.save();

        DocumentModel newFileDoc = session.getDocument(new PathRef(
                "/folder1/subFolder1/New file.odt"));
        assertEquals("File", newFileDoc.getType());
        assertEquals("New file.odt", newFileDoc.getTitle());
        org.nuxeo.ecm.core.api.Blob newFileBlob = (org.nuxeo.ecm.core.api.Blob) newFileDoc.getPropertyValue("file:content");
        assertNotNull(newFileBlob);
        assertEquals("New file.odt", newFileBlob.getFilename());
        assertEquals("This is the content of a new file.",
                newFileBlob.getString());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFileDoc.getId(),
                newFile.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId(),
                newFile.getParentId());
        assertEquals("New file.odt", newFile.getName());
        assertFalse(newFile.isFolder());
        assertEquals("Administrator", newFile.getCreator());
        assertTrue(newFile.getCanRename());
        assertTrue(newFile.getCanDelete());
        assertTrue(newFile.getCanUpdate());
        assertEquals("nxbigfile/test/" + newFileDoc.getId()
                + "/blobholder:0/New%20file.odt", newFile.getDownloadURL());
        assertEquals("md5", newFile.getDigestAlgorithm());
        assertEquals(newFileBlob.getDigest(), newFile.getDigest());
    }

    @Test
    public void testUpdateFile() throws Exception {

        StringBlob blob = new StringBlob(
                "This is the updated content of file 1.");
        blob.setFileName("Updated file 1.odt");
        Blob updatedFileJSON = (Blob) clientSession.newRequest(
                NuxeoDriveUpdateFile.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).setInput(
                blob).execute();
        assertNotNull(updatedFileJSON);

        DocumentBackedFileItem updatedFile = mapper.readValue(
                updatedFileJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(updatedFile);

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        session.save();

        DocumentModel updatedFileDoc = session.getDocument(new IdRef(
                file1.getId()));
        assertEquals("File", updatedFileDoc.getType());
        assertEquals("file1", updatedFileDoc.getTitle());
        org.nuxeo.ecm.core.api.Blob updatedFileBlob = (org.nuxeo.ecm.core.api.Blob) updatedFileDoc.getPropertyValue("file:content");
        assertNotNull(updatedFileBlob);
        assertEquals("Updated file 1.odt", updatedFileBlob.getFilename());
        assertEquals("This is the updated content of file 1.",
                updatedFileBlob.getString());

        assertEquals(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + updatedFileDoc.getId(),
                updatedFile.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                updatedFile.getParentId());
        assertEquals("Updated file 1.odt", updatedFile.getName());
        assertFalse(updatedFile.isFolder());
        assertEquals("Administrator", updatedFile.getCreator());
        assertTrue(updatedFile.getCanRename());
        assertTrue(updatedFile.getCanDelete());
        assertTrue(updatedFile.getCanUpdate());
        assertEquals("nxbigfile/test/" + updatedFileDoc.getId()
                + "/blobholder:0/Updated%20file%201.odt",
                updatedFile.getDownloadURL());
        assertEquals("md5", updatedFile.getDigestAlgorithm());
        assertEquals(updatedFileBlob.getDigest(), updatedFile.getDigest());
    }

    @Test
    public void testDelete() throws Exception {

        // ------------------------------------------------------
        // Delete file in sync root: should trash it
        // ------------------------------------------------------
        clientSession.newRequest(NuxeoDriveDelete.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).execute();

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel deletedFileDoc = session.getDocument(new IdRef(
                file1.getId()));
        assertEquals("deleted", deletedFileDoc.getCurrentLifeCycleState());

        // ------------------------------------------------------
        // Delete sync root: should unregister it
        // ------------------------------------------------------
        clientSession.newRequest(NuxeoDriveDelete.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(session).contains(
                new IdRef(syncRoot2.getId())));

        // ------------------------------------------------------
        // Delete top level folder: should be unsupported
        // ------------------------------------------------------
        try {
            clientSession.newRequest(NuxeoDriveDelete.ID).set(
                    "id",
                    fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                            session.getPrincipal()).getId()).execute();
            fail("Top level folder item deletion should be unsupported.");
        } catch (Exception e) {
            assertEquals("Failed to execute operation: NuxeoDrive.Delete",
                    e.getMessage());
        }
    }

    @Test
    public void testRename() throws Exception {

        // ------------------------------------------------------
        // File
        // ------------------------------------------------------
        Blob renamedFSItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveRename.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set("name",
                "Renamed file 1.odt").execute();
        assertNotNull(renamedFSItemJSON);

        DocumentBackedFileItem renamedFileItem = mapper.readValue(
                renamedFSItemJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(renamedFileItem);
        assertEquals("Renamed file 1.odt", renamedFileItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        session.save();

        DocumentModel renamedFileDoc = session.getDocument(new IdRef(
                file1.getId()));
        assertEquals("file1", renamedFileDoc.getTitle());
        org.nuxeo.ecm.core.api.Blob renamedFileBlob = (org.nuxeo.ecm.core.api.Blob) renamedFileDoc.getPropertyValue("file:content");
        assertNotNull(renamedFileBlob);
        assertEquals("Renamed file 1.odt", renamedFileBlob.getFilename());
        assertEquals("nxbigfile/test/" + file1.getId()
                + "/blobholder:0/Renamed%20file%201.odt",
                renamedFileItem.getDownloadURL());
        assertEquals("md5", renamedFileItem.getDigestAlgorithm());
        assertEquals(renamedFileBlob.getDigest(), renamedFileItem.getDigest());

        // ------------------------------------------------------
        // Folder
        // ------------------------------------------------------
        renamedFSItemJSON = (Blob) clientSession.newRequest(NuxeoDriveRename.ID).set(
                "id", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId()).set(
                "name", "Renamed sub-folder 1").execute();
        assertNotNull(renamedFSItemJSON);

        DocumentBackedFolderItem renamedFolderItem = mapper.readValue(
                renamedFSItemJSON.getStream(), DocumentBackedFolderItem.class);
        assertNotNull(renamedFolderItem);
        assertEquals("Renamed sub-folder 1", renamedFolderItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel renamedFolderDoc = session.getDocument(new IdRef(
                subFolder1.getId()));
        assertEquals("Renamed sub-folder 1", renamedFolderDoc.getTitle());

        // ------------------------------------------------------
        // Sync root
        // ------------------------------------------------------
        renamedFSItemJSON = (Blob) clientSession.newRequest(NuxeoDriveRename.ID).set(
                "id", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).set(
                "name", "New name for sync root").execute();
        assertNotNull(renamedFSItemJSON);

        DefaultSyncRootFolderItem renamedSyncRootItem = mapper.readValue(
                renamedFSItemJSON.getStream(), DefaultSyncRootFolderItem.class);
        assertNotNull(renamedSyncRootItem);
        assertEquals("New name for sync root", renamedSyncRootItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String
        // repositoryName, Principal principal)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel renamedSyncRootDoc = session.getDocument(new IdRef(
                syncRoot1.getId()));
        assertEquals("New name for sync root", renamedSyncRootDoc.getTitle());

        // ------------------------------------------------------
        // Top level folder
        // ------------------------------------------------------
        try {
            clientSession.newRequest(NuxeoDriveRename.ID).set(
                    "id",
                    fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                            session.getPrincipal()).getId()).set("name",
                    "New name for top level folder").execute();
            fail("Top level folder renaming shoud be unsupported.");
        } catch (Exception e) {
            assertEquals("Failed to execute operation: NuxeoDrive.Rename",
                    e.getMessage());
        }
    }

    @Test
    public void testCanMove() throws Exception {

        // ------------------------------------------------------
        // File to File => false
        // ------------------------------------------------------
        Blob canMoveFSItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCanMove.ID).set("srcId",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        String canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        assertEquals("false", canMoveFSItem);

        // ------------------------------------------------------
        // Sync root => false
        // ------------------------------------------------------
        canMoveFSItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCanMove.ID).set("srcId",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        assertEquals("false", canMoveFSItem);

        // ------------------------------------------------------
        // Top level folder => false
        // ------------------------------------------------------
        canMoveFSItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCanMove.ID).set(
                "srcId",
                fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                        session.getPrincipal()).getId()).set("destId",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        assertEquals("false", canMoveFSItem);

        // --------------------------------------------------------
        // No REMOVE permission on the source backing doc => false
        // --------------------------------------------------------
        Principal joe = createUser("joe", "joe");
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

            nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot1,
                    session);
            nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot2,
                    session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        Session joeSession = automationClient.getSession("joe", "joe");
        canMoveFSItemJSON = (Blob) joeSession.newRequest(NuxeoDriveCanMove.ID).set(
                "srcId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        assertEquals("false", canMoveFSItem);

        // -------------------------------------------------------------------
        // No ADD_CHILDREN permission on the destination backing doc => false
        // -------------------------------------------------------------------
        setPermission(syncRoot1, "joe", SecurityConstants.WRITE, true);
        canMoveFSItemJSON = (Blob) joeSession.newRequest(NuxeoDriveCanMove.ID).set(
                "srcId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        assertEquals("false", canMoveFSItem);

        // ----------------------------------------------------------------------
        // REMOVE permission on the source backing doc + REMOVE_CHILDREN
        // permission on its parent + ADD_CHILDREN permission on the destination
        // backing doc => true
        // ----------------------------------------------------------------------
        setPermission(syncRoot2, "joe", SecurityConstants.WRITE, true);

        nuxeoDriveManager.unregisterSynchronizationRoot(joe, syncRoot2, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        canMoveFSItemJSON = (Blob) joeSession.newRequest(NuxeoDriveCanMove.ID).set(
                "srcId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);

        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);

        // syncRoot2 is not registered as a sync root for joe
        assertEquals("false", canMoveFSItem);


        nuxeoDriveManager.registerSynchronizationRoot(joe, syncRoot2, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        canMoveFSItemJSON = (Blob) joeSession.newRequest(NuxeoDriveCanMove.ID).set(
                "srcId", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(canMoveFSItemJSON);
        canMoveFSItem = mapper.readValue(canMoveFSItemJSON.getStream(),
                String.class);
        // syncRoot2 is now a registered root for joe
        assertEquals("true", canMoveFSItem);

        // ----------------------------------------------------------------------
        // Reset permissions
        // ----------------------------------------------------------------------
        resetPermissions(rootDoc, "joe");
        resetPermissions(syncRoot1, "joe");
        resetPermissions(syncRoot2, "joe");
        deleteUser("joe");
    }

    @Test
    public void testMove() throws Exception {

        // ------------------------------------------------------
        // File to File => fail
        // ------------------------------------------------------
        try {
            clientSession.newRequest(NuxeoDriveMove.ID).set("srcId",
                    DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                    "destId",
                    DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file2.getId()).execute();
            fail("Move to a non folder item should fail.");
        } catch (Exception e) {
            assertEquals("Failed to execute operation: NuxeoDrive.Move",
                    e.getMessage());
        }

        // ------------------------------------------------------
        // Sync root => fail
        // ------------------------------------------------------
        try {
            clientSession.newRequest(NuxeoDriveMove.ID).set("srcId",
                    SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).set(
                    "destId",
                    SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
            fail("Should not be able to move a synchronization root folder item.");
        } catch (Exception e) {
            assertEquals("Failed to execute operation: NuxeoDrive.Move",
                    e.getMessage());
        }

        // ------------------------------------------------------
        // Top level folder => fail
        // ------------------------------------------------------
        try {
            clientSession.newRequest(NuxeoDriveMove.ID).set(
                    "srcId",
                    fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                            session.getPrincipal()).getId()).set("destId",
                    SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
            fail("Should not be able to move the top level folder item.");
        } catch (Exception e) {
            assertEquals("Failed to execute operation: NuxeoDrive.Move",
                    e.getMessage());
        }

        // ------------------------------------------------------
        // File to Folder => succeed
        // ------------------------------------------------------
        Blob movedFSItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveMove.ID).set("srcId",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).set(
                "destId", SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).execute();
        assertNotNull(movedFSItemJSON);

        DocumentBackedFileItem movedFileItem = mapper.readValue(
                movedFSItemJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(movedFileItem);
        assertEquals("First file.odt", movedFileItem.getName());

        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item obtained by
        // FileSystemItemManager#getSession(String repositoryName, Principal
        // principal)
        session.save();

        DocumentModel movedFileDoc = session.getDocument(new IdRef(
                file1.getId()));
        assertEquals("/folder2/file1", movedFileDoc.getPathAsString());
        assertEquals("file1", movedFileDoc.getTitle());
        org.nuxeo.ecm.core.api.Blob movedFileBlob = (org.nuxeo.ecm.core.api.Blob) movedFileDoc.getPropertyValue("file:content");
        assertNotNull(movedFileBlob);
        assertEquals("First file.odt", movedFileBlob.getFilename());
        assertEquals("md5", movedFileItem.getDigestAlgorithm());
        assertEquals(movedFileBlob.getDigest(), movedFileItem.getDigest());
    }

    @Test
    public void testConflictedNames() throws Exception {
        // Try a canonical example with the Administrator user
        Blob jsonOut = (Blob) clientSession.newRequest(
                NuxeoDriveGenerateConflictedItemName.ID).set("name",
                "My file (with accents \u00e9).doc").execute();
        assertNotNull(jsonOut);
        String newName = mapper.readValue(jsonOut.getStream(), String.class);
        assertTrue(newName.startsWith("My file (with accents \u00e9) (Administrator - "));
        assertTrue(newName.endsWith(").doc"));

        // Try with a filename with filename extension
        jsonOut = (Blob) clientSession.newRequest(
                NuxeoDriveGenerateConflictedItemName.ID).set("name", "My file").execute();
        assertNotNull(jsonOut);
        newName = mapper.readValue(jsonOut.getStream(), String.class);
        assertTrue(newName.startsWith("My file (Administrator - "));
        assertTrue(newName.endsWith(")"));

        // Test with a user that has a firstname and a lastname
        // Joe Strummer likes conflicting files
        createUser("joe", "joe", "Joe", "Strummer");
        Session joeSession = automationClient.getSession("joe", "joe");
        jsonOut = (Blob) joeSession.newRequest(
                NuxeoDriveGenerateConflictedItemName.ID).set("name",
                "The Clashing File.xls").execute();
        assertNotNull(jsonOut);
        newName = mapper.readValue(jsonOut.getStream(), String.class);
        assertTrue(newName.startsWith("The Clashing File (Joe Strummer - "));
        assertTrue(newName.endsWith(").xls"));

        deleteUser("joe");
    }

    protected NuxeoPrincipal createUser(String userName, String password)
            throws ClientException {
        return createUser(userName, password, null, null);
    }

    protected NuxeoPrincipal createUser(String userName, String password,
            String firstName, String lastName) throws ClientException {
        org.nuxeo.ecm.directory.Session userDir = directoryService.getDirectory(
                "userDirectory").getSession();
        try {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("username", userName);
            user.put("password", password);
            user.put("firstName", firstName);
            user.put("lastName", lastName);
            userDir.createEntry(user);
        } finally {
            userDir.close();
        }
        UserManager userManager = Framework.getLocalService(UserManager.class);
        return userManager.getPrincipal(userName);
    }

    protected void deleteUser(String userName) throws ClientException {
        org.nuxeo.ecm.directory.Session userDir = directoryService.getDirectory(
                "userDirectory").getSession();
        try {
            userDir.deleteEntry(userName);
        } finally {
            userDir.close();
        }
    }

    protected void setPermission(DocumentModel doc, String userName,
            String permission, boolean isGranted) throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected void resetPermissions(DocumentModel doc, String userName)
            throws ClientException {
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

    protected void checkChildren(List<DocumentBackedFileItem> folderChildren,
            String folderId, String child1Id, String child2Id, boolean ordered)
            throws Exception {

        boolean isChild1Found = false;
        boolean isChild2Found = false;
        int childrenCount = 0;

        for (DocumentBackedFileItem fsItem : folderChildren) {
            // Check child 1
            if (!isChild1Found
                    && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + child1Id).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 0) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId,
                            fsItem.getParentId());
                    assertEquals("Third file.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    assertEquals("Administrator", fsItem.getCreator());
                    assertTrue(fsItem.getCanRename());
                    assertTrue(fsItem.getCanDelete());
                    assertTrue(fsItem.getCanUpdate());
                    assertEquals("nxbigfile/test/" + file3.getId()
                            + "/blobholder:0/Third%20file.odt",
                            fsItem.getDownloadURL());
                    assertEquals("md5", fsItem.getDigestAlgorithm());
                    assertEquals(
                            ((org.nuxeo.ecm.core.api.Blob) file3.getPropertyValue("file:content")).getDigest(),
                            fsItem.getDigest());
                    isChild1Found = true;
                    childrenCount++;
                }
            }
            // Check child 2
            else if (!isChild2Found
                    && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + child2Id).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 1) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId,
                            fsItem.getParentId());
                    assertEquals("Fourth file.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    assertEquals("Administrator", fsItem.getCreator());
                    assertTrue(fsItem.getCanRename());
                    assertTrue(fsItem.getCanDelete());
                    assertTrue(fsItem.getCanUpdate());
                    assertEquals("nxbigfile/test/" + file4.getId()
                            + "/blobholder:0/Fourth%20file.odt",
                            fsItem.getDownloadURL());
                    assertEquals("md5", fsItem.getDigestAlgorithm());
                    assertEquals(
                            ((org.nuxeo.ecm.core.api.Blob) file4.getPropertyValue("file:content")).getDigest(),
                            fsItem.getDigest());
                }
            } else {
                fail(String.format(
                        "FileSystemItem %s doesn't match any expected.",
                        fsItem.getId()));
            }
        }
    }
}
