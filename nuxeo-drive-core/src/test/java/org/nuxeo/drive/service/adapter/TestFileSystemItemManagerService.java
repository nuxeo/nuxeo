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
package org.nuxeo.drive.service.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.security.Principal;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link FileSystemItemManager}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.dublincore", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.core.io",
        "org.nuxeo.ecm.core.cache", "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.drive.core.test:OSGI-INF/test-nuxeodrive-sync-root-cache-contrib.xml" })
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestFileSystemItemManagerService {

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemManager fileSystemItemManagerService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected Principal principal;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel folder;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    protected DocumentModel subFolder;

    @Before
    public void createTestDocs() throws Exception {

        principal = session.getPrincipal();

        // Create and register 2 synchronization roots for Administrator
        syncRoot1 = session.createDocument(session.createDocumentModel("/", "syncRoot1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/", "syncRoot2", "Folder"));
        Principal administrator = session.getPrincipal();
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot2, session);

        // Folder
        folder = session.createDocumentModel(syncRoot1.getPathAsString(), "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder = session.createDocument(folder);

        // File
        file = session.createDocumentModel(folder.getPathAsString(), "aFile", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        // Note
        note = session.createDocumentModel(folder.getPathAsString(), "aNote", "Note");
        note.setPropertyValue("note:note", "Content of Bob's note.");
        note = session.createDocument(note);

        // Custom doc type with the "file" schema
        custom = session.createDocumentModel(folder.getPathAsString(), "aCustomDoc", "Custom");
        blob = new StringBlob("Content of Bonnie's file.");
        blob.setFilename("Bonnie's file.odt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        // FolderishFile: doc type with the "file" schema and the "Folderish"
        // facet
        folderishFile = session.createDocumentModel(folder.getPathAsString(), "aFolderishFile", "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel(folder.getPathAsString(), "notAFileSystemItem",
                "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);

        // Sub folder
        subFolder = session.createDocumentModel(folder.getPathAsString(), "aSubFolder", "Folder");
        subFolder.setPropertyValue("dc:title", "Tony's sub folder");
        subFolder = session.createDocument(subFolder);

        session.save();
    }

    @Test
    public void testReadOperations() throws Exception {

        // ------------------------------------------------------
        // Check #getTopLevelChildren
        // ------------------------------------------------------
        List<FileSystemItem> topLevelChildren = fileSystemItemManagerService.getTopLevelFolder(principal).getChildren();
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        FileSystemItem childFsItem = topLevelChildren.get(0);
        assertTrue(childFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals("defaultSyncRootFolderItemFactory#test#" + syncRoot1.getId(), childFsItem.getId());
        assertTrue(childFsItem.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals("syncRoot1", childFsItem.getName());

        childFsItem = topLevelChildren.get(1);
        assertTrue(childFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals("defaultSyncRootFolderItemFactory#test#" + syncRoot2.getId(), childFsItem.getId());
        assertTrue(childFsItem.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals("syncRoot2", childFsItem.getName());

        // ------------------------------------------------------
        // Check #exists
        // ------------------------------------------------------
        // Non existent doc id
        assertFalse(fileSystemItemManagerService.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + "nonExistentId", principal));
        // File
        assertTrue(fileSystemItemManagerService.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), principal));
        // Not adaptable as a FileSystemItem
        assertFalse(fileSystemItemManagerService.exists(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + notAFileSystemItem.getId(), principal));
        // Deleted
        custom.followTransition("delete");
        assertFalse(fileSystemItemManagerService.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(), principal));

        // ------------------------------------------------------------
        // Check #getFileSystemItemById(String id, Principal principal)
        // ------------------------------------------------------------
        // Folder
        FileSystemItem fsItem = fileSystemItemManagerService.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + folder.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getId());
        String expectedSyncRoot1Id = DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX + syncRoot1.getId();
        assertEquals(expectedSyncRoot1Id, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(fsItem.getCanRename());
        assertTrue(fsItem.getCanDelete());
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(4, children.size());

        // File
        fsItem = fileSystemItemManagerService.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(),
                principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getParentId());
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertTrue(fsItem.getCanRename());
        assertTrue(fsItem.getCanDelete());
        FileItem fileFsItem = (FileItem) fsItem;
        assertTrue(fileFsItem.getCanUpdate());
        assertEquals("nxfile/test/" + file.getId() + "/blobholder:0/Joe.odt", fileFsItem.getDownloadURL());
        assertEquals("MD5", fileFsItem.getDigestAlgorithm());
        assertEquals(file.getAdapter(BlobHolder.class).getBlob().getDigest(), fileFsItem.getDigest());
        Blob fileItemBlob = fileFsItem.getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // FolderishFile
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderishFile.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderishFile.getId(), fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getParentId());
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(fsItem.getCanRename());
        assertTrue(fsItem.getCanDelete());
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());

        // Not adaptable as a FileSystemItem
        fsItem = fileSystemItemManagerService.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + notAFileSystemItem.getId(), principal);
        assertNull(fsItem);

        // Deleted
        assertNull(fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(), principal));

        // Sub folder
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder.getId(), fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getParentId());
        assertEquals("Tony's sub folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(fsItem.getCanRename());
        assertTrue(fsItem.getCanDelete());
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());

        // -------------------------------------------------------------------
        // Check #getFileSystemItemById(String id, String parentId, Principal
        // principal)
        // -------------------------------------------------------------------
        fsItem = fileSystemItemManagerService.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), principal);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getParentId());

        // ------------------------------------------------------
        // Check #getChildren
        // ------------------------------------------------------
        // Need to flush VCS cache for the session used in DocumentBackedFolderItem#getChildren() to be aware of changes
        // in the current session
        session.save();
        children = fileSystemItemManagerService.getChildren(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(),
                principal);
        assertNotNull(children);

        assertEquals(4, children.size());
        // Don't check children order against MySQL database because of the
        // milliseconds limitation
        boolean ordered = coreFeature.getStorageConfiguration().hasSubSecondResolution();
        checkChildren(children, folder.getId(), file.getId(), note.getId(), folderishFile.getId(), subFolder.getId(),
                ordered);

        children = fileSystemItemManagerService.getChildren(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder.getId(),
                principal);
        assertTrue(children.isEmpty());

        // ------------------------------------------------------
        // Check #canMove
        // ------------------------------------------------------
        // Not allowed to move a file system item to a non FolderItem
        String srcFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId();
        String destFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId();
        assertFalse(fileSystemItemManagerService.canMove(srcFsItemId, destFsItemId, principal));

        // Not allowed to move a file system item if no REMOVE permission on the
        // source backing doc
        Principal joePrincipal = new NuxeoPrincipalImpl("joe");
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);
        nuxeoDriveManager.registerSynchronizationRoot(joePrincipal, syncRoot1, session);

        // Under Oracle, the READ ACL optims are not visible from the joe
        // session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        destFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder.getId();
        assertFalse(fileSystemItemManagerService.canMove(srcFsItemId, destFsItemId, joePrincipal));

        // Not allowed to move a file system item if no ADD_CHILDREN permission
        // on the destination backing doc
        setPermission(folder, "joe", SecurityConstants.WRITE, true);
        setPermission(subFolder, "joe", SecurityConstants.READ, true);
        setPermission(subFolder, SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING, true);
        setPermission(subFolder, SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        assertFalse(fileSystemItemManagerService.canMove(srcFsItemId, destFsItemId, joePrincipal));

        // OK: REMOVE permission on the source backing doc + REMOVE_CHILDREN
        // permission on its parent + ADD_CHILDREN permission on the destination
        // backing doc
        resetPermissions(subFolder, SecurityConstants.EVERYONE);
        resetPermissions(subFolder, "joe");
        setPermission(subFolder, "joe", SecurityConstants.WRITE, true);
        assertTrue(fileSystemItemManagerService.canMove(srcFsItemId, destFsItemId, joePrincipal));

        // Reset permissions
        resetPermissions(rootDoc, "joe");
        resetPermissions(folder, "joe");
        resetPermissions(subFolder, "joe");
    }

    @Test
    public void testWriteOperations() throws Exception {

        // ------------------------------------------------------
        // Check #createFolder
        // ------------------------------------------------------
        // Not allowed to create a folder in a non FolderItem
        try {
            fileSystemItemManagerService.createFolder(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(),
                    "A new folder", principal);
            fail("Folder creation in a non folder item should fail.");
        } catch (NuxeoException e) {
            assertEquals(String.format(
                    "Cannot create a folder in file system item with id %s because it is not a folder but is: "
                            + "DocumentBackedFileItem(id=\"%s\", name=\"Joe.odt\")", DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                            + file.getId(), DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId()), e.getMessage());
        }

        // Folder creation
        FolderItem newFolderItem = fileSystemItemManagerService.createFolder(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + folder.getId(), "A new folder", principal);
        assertNotNull(newFolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), newFolderItem.getParentId());
        assertEquals("A new folder", newFolderItem.getName());
        DocumentModelList folderChildren = session.query(String.format(
                "select * from Document where ecm:parentId = '%s' and ecm:primaryType = 'Folder' order by dc:title asc",
                folder.getId()));
        DocumentModel newFolder = folderChildren.get(0);
        assertTrue(newFolder.isFolder());
        assertEquals("A new folder", newFolder.getTitle());

        // Parent folder children check
        assertEquals(
                6,
                fileSystemItemManagerService.getChildren(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), principal).size());

        // NXP-21854: Check overwrite parameter
        // Test overwrite=false
        FolderItem differentFolderItem = fileSystemItemManagerService.createFolder(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), "A new folder", principal, false);
        assertNotNull(differentFolderItem);
        assertNotEquals(newFolderItem.getId(), differentFolderItem.getId());
        assertEquals("A new folder", differentFolderItem.getName());
        // Test overwrite=true
        FolderItem otherFolderItem = fileSystemItemManagerService.createFolder(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), "Test overwrite", principal, false);
        assertNotNull(otherFolderItem);
        assertEquals("Test overwrite", otherFolderItem.getName());
        FolderItem sameFolderItem = fileSystemItemManagerService.createFolder(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), "Test overwrite", principal, true);
        assertNotNull(sameFolderItem);
        assertEquals(otherFolderItem.getId(), sameFolderItem.getId());
        assertEquals("Test overwrite", sameFolderItem.getName());

        // ------------------------------------------------------
        // Check #createFile
        // ------------------------------------------------------
        // File creation
        Blob blob = new StringBlob("Content of a new file.");
        blob.setFilename("New file.odt");
        blob.setMimeType("application/vnd.oasis.opendocument.text");
        FileItem fileItem = fileSystemItemManagerService.createFile(newFolderItem.getId(), blob, principal);
        assertNotNull(fileItem);
        assertEquals(newFolderItem.getId(), fileItem.getParentId());
        assertEquals("New file.odt", fileItem.getName());
        folderChildren = session.query(String.format("select * from Document where ecm:parentId = '%s'",
                newFolder.getId()));
        assertEquals(1, folderChildren.size());
        DocumentModel newFile = folderChildren.get(0);
        assertEquals("File", newFile.getType());
        assertEquals("New file.odt", newFile.getTitle());
        assertEquals("/syncRoot1/aFolder/A new folder/New file.odt", newFile.getPathAsString());
        Blob newFileBlob = (Blob) newFile.getPropertyValue("file:content");
        assertEquals("New file.odt", newFileBlob.getFilename());
        assertEquals("Content of a new file.", newFileBlob.getString());
        assertEquals("nxfile/test/" + newFile.getId() + "/blobholder:0/New%20file.odt", fileItem.getDownloadURL());
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(newFileBlob.getDigest(), fileItem.getDigest());

        // NXP-21854: Check overwrite parameter
        // Test overwrite=false
        FileItem differentFileItem = fileSystemItemManagerService.createFile(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), blob, principal, false);
        assertNotNull(differentFileItem);
        assertNotEquals(fileItem.getId(), differentFileItem.getId());
        assertEquals("New file.odt", differentFileItem.getName());
        // Test overwrite=true
        Blob otherBlob = new StringBlob("Content of a new file.");
        otherBlob.setFilename("Test overwrite.odt");
        otherBlob.setMimeType("application/vnd.oasis.opendocument.text");
        FileItem otherFileItem = fileSystemItemManagerService.createFile(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), otherBlob, principal, false);
        assertNotNull(otherFileItem);
        assertEquals("Test overwrite.odt", otherFileItem.getName());
        FileItem sameFileItem = fileSystemItemManagerService.createFile(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), otherBlob, principal, true);
        assertNotNull(sameFileItem);
        assertEquals(otherFileItem.getId(), sameFileItem.getId());
        assertEquals("Test overwrite.odt", sameFileItem.getName());

        // Parent folder children check
        assertEquals(1, fileSystemItemManagerService.getChildren(newFolderItem.getId(), principal).size());

        // ------------------------------------------------------
        // Check #updateFile
        // ------------------------------------------------------
        String fileItemId = fileItem.getId();
        String fileItemParentId = fileItem.getParentId();
        blob = new StringBlob("Modified content of an existing file.");
        fileItem = fileSystemItemManagerService.updateFile(fileItemId, blob, principal);
        assertNotNull(fileItem);
        assertEquals(fileItemId, fileItem.getId());
        assertEquals(fileItemParentId, fileItem.getParentId());
        assertEquals("New file.odt", fileItem.getName());
        folderChildren = session.query(String.format("select * from Document where ecm:parentId = '%s'",
                newFolder.getId()));
        assertEquals(1, folderChildren.size());
        DocumentModel updatedFile = folderChildren.get(0);
        assertEquals("File", updatedFile.getType());
        assertEquals("New file.odt", updatedFile.getTitle());
        assertEquals("/syncRoot1/aFolder/A new folder/New file.odt", updatedFile.getPathAsString());
        Blob updatedFileBlob = (Blob) updatedFile.getPropertyValue("file:content");
        assertEquals("New file.odt", updatedFileBlob.getFilename());
        assertEquals("Modified content of an existing file.", updatedFileBlob.getString());
        assertEquals("nxfile/test/" + updatedFile.getId() + "/blobholder:0/New%20file.odt", fileItem.getDownloadURL());
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(updatedFileBlob.getDigest(), fileItem.getDigest());

        // ------------------------------------------------------
        // Check #delete
        // ------------------------------------------------------
        // File deletion
        fileSystemItemManagerService.delete(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + updatedFile.getId(), principal);
        updatedFile = session.getDocument(new IdRef(updatedFile.getId()));
        assertEquals("deleted", updatedFile.getCurrentLifeCycleState());

        // Parent folder children check
        assertTrue(fileSystemItemManagerService.getChildren(newFolderItem.getId(), principal).isEmpty());

        // ------------------------------------------------------
        // Check #rename
        // ------------------------------------------------------
        // Folder rename
        String fsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId();
        FileSystemItem fsItem = fileSystemItemManagerService.rename(fsItemId, "Jack's folder has a new name", principal);
        assertEquals(fsItemId, fsItem.getId());
        String expectedSyncRoot1Id = DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX + syncRoot1.getId();
        assertEquals(expectedSyncRoot1Id, fsItem.getParentId());
        assertEquals("Jack's folder has a new name", fsItem.getName());
        folder = session.getDocument(folder.getRef());
        assertEquals("Jack's folder has a new name", folder.getTitle());

        // File rename with title != filename
        // => should rename filename but not title
        assertEquals("aFile", file.getTitle());
        assertEquals("Joe.odt", ((Blob) file.getPropertyValue("file:content")).getFilename());
        fsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId();
        fsItem = fileSystemItemManagerService.rename(fsItemId, "File new name.odt", principal);
        assertEquals(fsItemId, fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getParentId());
        assertEquals("File new name.odt", fsItem.getName());
        file = session.getDocument(file.getRef());
        assertEquals("aFile", file.getTitle());
        Blob fileBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("File new name.odt", fileBlob.getFilename());
        fileItem = (FileItem) fsItem;
        assertEquals("nxfile/test/" + file.getId() + "/blobholder:0/File%20new%20name.odt", fileItem.getDownloadURL());
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(fileBlob.getDigest(), fileItem.getDigest());

        // File rename with title == filename
        // => should rename filename and title
        blob = new StringBlob("File for a doc with title == filename.");
        blob.setFilename("Title-filename equality.odt");
        blob.setMimeType("application/vnd.oasis.opendocument.text");
        fileItem = fileSystemItemManagerService.createFile(newFolderItem.getId(), blob, principal);
        // Note that the PathSegmentService truncates doc title at 24 characters
        newFile = session.getDocument(new PathRef("/syncRoot1/aFolder/A new folder/Title-filename equality."));
        assertEquals("Title-filename equality.odt", newFile.getTitle());
        assertEquals("Title-filename equality.odt", ((Blob) newFile.getPropertyValue("file:content")).getFilename());
        fileItem = (FileItem) fileSystemItemManagerService.rename(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFile.getId(),
                "Renamed title-filename equality.odt", principal);
        assertEquals("Renamed title-filename equality.odt", fileItem.getName());
        newFile = session.getDocument(newFile.getRef());
        assertEquals("Renamed title-filename equality.odt", newFile.getTitle());
        newFileBlob = (Blob) newFile.getPropertyValue("file:content");
        assertEquals("Renamed title-filename equality.odt", newFileBlob.getFilename());
        assertEquals("nxfile/test/" + newFile.getId() + "/blobholder:0/Renamed%20title-filename%20equality.odt",
                fileItem.getDownloadURL());
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(newFileBlob.getDigest(), fileItem.getDigest());

        // ------------------------------------------------------
        // Check #move
        // ------------------------------------------------------
        // Not allowed to move a file system item to a non FolderItem
        String srcFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId();
        String destFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId();
        try {
            fileSystemItemManagerService.move(srcFsItemId, destFsItemId, principal);
            fail("Move to a non folder item should fail.");
        } catch (NuxeoException e) {
            assertEquals(String.format(
                    "Cannot move a file system item to file system item with id %s because it is not a folder.",
                    DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId()), e.getMessage());
        }

        // Move to a FolderItem
        destFsItemId = DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder.getId();
        FileSystemItem movedFsItem = fileSystemItemManagerService.move(srcFsItemId, destFsItemId, principal);
        assertEquals(srcFsItemId, movedFsItem.getId());
        assertEquals(destFsItemId, movedFsItem.getParentId());
        assertEquals("aNote.txt", movedFsItem.getName());
        note = session.getDocument(note.getRef());
        assertEquals("/syncRoot1/aFolder/aSubFolder/aNote", note.getPathAsString());
        assertEquals("aNote", note.getTitle());
    }

    protected void setPermission(DocumentModel doc, String userName, String permission, boolean isGranted) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
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
        session.save();
    }

    protected void checkChildren(List<FileSystemItem> folderChildren, String folderId, String fileId, String noteId,
            String folderishFileId, String subFolderId, boolean ordered) throws Exception {

        boolean isFileFound = false;
        boolean isNoteFound = false;
        boolean isFolderishFileFound = false;
        boolean isSubFolderFound = false;
        int childrenCount = 0;

        for (FileSystemItem fsItem : folderChildren) {
            // Check File
            if (!isFileFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + fileId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 0) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Joe.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    isFileFound = true;
                    childrenCount++;
                }
            }
            // Check Note
            else if (!isNoteFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + noteId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 1) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("aNote.txt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    isNoteFound = true;
                    childrenCount++;
                }
            }
            // Check folderish File
            else if (!isFolderishFileFound
                    && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderishFileId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 3) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Sarah's folderish file", fsItem.getName());
                    assertTrue(fsItem.isFolder());
                    isFolderishFileFound = true;
                    childrenCount++;
                }
            }
            // Check sub-Folder
            else if (!isSubFolderFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolderId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 4) {
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Tony's sub folder", fsItem.getName());
                    assertTrue(fsItem.isFolder());
                    isSubFolderFound = true;
                    childrenCount++;
                }
            } else {
                fail(String.format("FileSystemItem %s doesn't match any expected.", fsItem.getId()));
            }
        }
    }

}
