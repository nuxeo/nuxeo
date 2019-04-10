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
package org.nuxeo.drive.fixtures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.adapter.impl.FileSystemItemHelper;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link DefaultFileSystemItemFactory}.
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-versioning-filter-contrib.xml")
public class DefaultFileSystemItemFactoryFixture {

    private static final Logger log = LogManager.getLogger(DefaultFileSystemItemFactoryFixture.class);

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    private static final int VERSIONING_DELAY = 1000; // ms

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected CollectionManager collectionManager;

    @Inject
    protected TrashService trashService;

    protected NuxeoPrincipal principal;

    protected String syncRootItemId;

    protected DocumentModel syncRootFolder;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    protected FileSystemItemFactory defaultFileSystemItemFactory;

    protected FileSystemItemFactory defaultSyncRootFolderItemFactory;

    @Before
    public void createTestDocs() throws Exception {
        principal = session.getPrincipal();
        syncRootFolder = session.createDocumentModel("/", "syncRoot", "Folder");
        syncRootFolder = session.createDocument(syncRootFolder);
        nuxeoDriveManager.registerSynchronizationRoot(principal, syncRootFolder, session);

        // Expected sync root FS item id
        syncRootItemId = DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX + syncRootFolder.getId();

        // File
        file = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFile", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        // Note
        note = session.createDocumentModel(syncRootFolder.getPathAsString(), "aNote", "Note");
        note.setPropertyValue("note:note", "Content of Bob's note.");
        note = session.createDocument(note);

        // Custom doc type with the "file" schema
        custom = session.createDocumentModel(syncRootFolder.getPathAsString(), "aCustomDoc", "Custom");
        blob = new StringBlob("Content of Bonnie's file.");
        blob.setFilename("Bonnie's file.odt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        // Folder
        folder = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder = session.createDocument(folder);

        // FolderishFile: doc type with the "file" schema and the "Folderish"
        // facet
        folderishFile = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFolderishFile",
                "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel(syncRootFolder.getPathAsString(), "notAFileSystemItem",
                "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);

        session.save();

        // Get default file system item factory
        FileSystemItemAdapterServiceImpl fileSystemItemAdapterServiceImpl = (FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService;
        defaultFileSystemItemFactory = fileSystemItemAdapterServiceImpl.getFileSystemItemFactory(
                "defaultFileSystemItemFactory");
        // Get the default sync root folder item factory
        defaultSyncRootFolderItemFactory = fileSystemItemAdapterServiceImpl.getFileSystemItemFactory(
                "defaultSyncRootFolderItemFactory");
    }

    @Test
    public void testGetFileSystemItem() throws Exception {

        // ------------------------------------------------------
        // Check downloadable FileSystemItems
        // ------------------------------------------------------
        // File
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(file));
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItem(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        assertEquals("Administrator", fsItem.getLastContributor());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // Note
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(note));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        assertEquals("Administrator", fsItem.getLastContributor());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());

        // Custom doc type with the "file" schema
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(custom));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(custom);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Bonnie's file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        assertEquals("Administrator", fsItem.getLastContributor());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.odt", fileItemBlob.getFilename());
        assertEquals("Content of Bonnie's file.", fileItemBlob.getString());

        // File without a blob => not adaptable as a FileSystemItem
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(file));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(file);
        assertNull(fsItem);

        // Deleted file => not adaptable as a FileSystemItem
        trashService.trashDocument(custom);
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(custom));
        assertNull(defaultFileSystemItemFactory.getFileSystemItem(custom));

        // Deleted file with explicit "includeDeleted" => adaptable as a
        // FileSystemItem
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(custom, true));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(custom, true);
        assertNotNull(fsItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(), fsItem.getId());
        assertEquals("Bonnie's file.odt", fsItem.getName());

        // Version
        // Note is now automatically versioned at each save
        assertEquals("0.1", note.getVersionLabel());
        note.checkOut();
        DocumentRef versionRef = session.checkIn(note.getRef(), VersioningOption.MINOR, null);
        DocumentModel version = session.getDocument(versionRef);
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(version));

        // Proxy
        DocumentModel proxy = session.createProxy(note.getRef(), folder.getRef());
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(proxy));

        // HiddenInNavigation
        note.addFacet("HiddenInNavigation");
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(note));
        note.removeFacet("HiddenInNavigation");

        // ------------------------------------------------------
        // Check folderish FileSystemItems
        // ------------------------------------------------------
        // Folder
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(folder));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        assertEquals("Administrator", fsItem.getLastContributor());
        FolderItem folderItem = (FolderItem) fsItem;
        List<FileSystemItem> children = folderItem.getChildren();
        assertNotNull(children);
        assertEquals(0, children.size());
        assertTrue(folderItem.getCanScrollDescendants());
        ScrollFileSystemItemList descendants = folderItem.scrollDescendants(null, 10, 1000);
        assertNotNull(descendants);
        assertNotNull(descendants.getScrollId());
        assertEquals(0, descendants.size());

        // FolderishFile => adaptable as a FolderItem since the default
        // FileSystemItem factory gives precedence to the Folderish facet
        assertTrue(defaultFileSystemItemFactory.isFileSystemItem(folderishFile));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(folderishFile);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderishFile.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        assertEquals("Administrator", fsItem.getLastContributor());

        // ------------------------------------------------------
        // Check not downloadable nor folderish
        // ------------------------------------------------------
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(notAFileSystemItem));
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(notAFileSystemItem);
        assertNull(fsItem);

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, FolderItem parentItem)
        // -------------------------------------------------------------
        FolderItem syncRootSystemItem = (FolderItem) fileSystemItemAdapterService.getFileSystemItemFactoryForId(
                syncRootItemId).getFileSystemItemById(syncRootItemId, principal);
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note, syncRootSystemItem);
        assertEquals(syncRootItemId, fsItem.getParentId());

        // Passing a null parent will force a null parentId
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note, null);
        assertNull(fsItem.getParentId());

        // ------------------------------------------------------------------
        // Check FileSystemItem#getCanRename and FileSystemItem#getCanDelete
        // ------------------------------------------------------------------
        // As Administrator
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note);
        assertTrue(fsItem.getCanRename());
        assertTrue(fsItem.getCanDelete());

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the joe
        // session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            note = joeSession.getDocument(note.getRef());
            fsItem = defaultFileSystemItemFactory.getFileSystemItem(note);
            assertFalse(fsItem.getCanRename());
            assertFalse(fsItem.getCanDelete());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            fsItem = defaultFileSystemItemFactory.getFileSystemItem(note);
            assertTrue(fsItem.getCanRename());
            assertTrue(fsItem.getCanDelete());
        }
        resetPermissions(rootDoc, "joe");
    }

    @Test
    @Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-permissions-contrib.xml")
    public void testPermissionCheckOptimized() {
        setPermission(syncRootFolder, "joe", SecurityConstants.READ, true);
        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            log.trace("Register the sync root for Joe's account");
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, joeSession);
            folder = joeSession.getDocument(folder.getRef());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Read granted on parent folder");
            FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace(
                    "Check canDelete/canCreateChild flags on folder for user joe with Write granted on folder, AddChildren not granted on folder and RemoveChildren not granted on parent folder");
            setPermission(folder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // True here as optimized => no explicit check of AddChildren on
            // folder nor RemoveChildren on parent folder
            assertTrue(folderItem.getCanDelete());
            assertTrue(folderItem.getCanCreateChild());

            log.trace(
                    "Check canDelete flag on folder for user joe with Write (thus RemoveChildren) granted on parent folder");
            setPermission(syncRootFolder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // Still true with RemoveChildren on the parent folder
            assertTrue(folderItem.getCanDelete());

            log.trace("Check canCreateChild flag on folder for user joe with AddChildren granted on folder");
            setPermission(folder, "joe", SecurityConstants.ADD_CHILDREN, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // Still true with AddChildren on the folder
            assertTrue(folderItem.getCanCreateChild());
        }
        resetPermissions(folder, "joe");
        resetPermissions(syncRootFolder, "joe");
    }

    @Test
    @Deploy({ "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-permissions-contrib.xml",
            "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-permission-check-not-optimized-contrib.xml" })
    public void testPermissionCheckNotOptimized() {
        setPermission(syncRootFolder, "joe", SecurityConstants.READ, true);
        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            log.trace("Register the sync root for Joe's account");
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, joeSession);
            folder = joeSession.getDocument(folder.getRef());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Read granted on parent folder");
            FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace(
                    "Check canDelete/canCreateChild flags on folder for user joe with Write granted on folder, AddChildren not granted on folder and RemoveChildren not granted on parent folder");
            setPermission(folder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // False here as not optimized => explicit check of RemoveChildren
            // on parent folder and AddChildren on
            // folder
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace(
                    "Check canDelete flag on folder for user joe with Write (thus RemoveChildren) granted on parent folder");
            setPermission(syncRootFolder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // True here thanks to RemoveChildren on the parent folder
            assertTrue(folderItem.getCanDelete());
            // Still false here because of missing AddChildren on folder
            assertFalse(folderItem.getCanCreateChild());

            log.trace("Check canCreateChild flag on folder for user joe with AddChildren granted on folder");
            setPermission(folder, "joe", SecurityConstants.ADD_CHILDREN, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // True here thanks to AddChildren on folder
            assertTrue(folderItem.getCanCreateChild());
        }
        resetPermissions(folder, "joe");
        resetPermissions(syncRootFolder, "joe");
    }

    @Test
    public void testExists() throws Exception {

        // Bad id
        try {
            defaultFileSystemItemFactory.exists("badId", principal);
            fail("Should not be able to check existence for bad id.");
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "FileSystemItem id badId cannot be handled by factory named defaultFileSystemItemFactory. Should match the 'fileSystemItemFactoryName#repositoryName#docId' pattern.",
                    e.getMessage());
        }
        // Non existent doc id
        assertFalse(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + "nonExistentDocId",
                principal));
        // File
        assertTrue(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), principal));
        // Note
        assertTrue(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(), principal));
        // Not adaptable as a FileSystemItem
        assertFalse(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + notAFileSystemItem.getId(),
                principal));
        // Deleted
        trashService.trashDocument(file);
        assertFalse(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), principal));
    }

    @Test
    public void testGetFileSystemItemById() throws Exception {

        // Non existent doc id, must return null
        assertNull(defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + "nonExistentDocId", principal));
        // File without a blob
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        session.save();
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), principal);
        assertNull(fsItem);
        // Note
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(),
                principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());
        // Folder
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(),
                principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        FolderItem folderItem = (FolderItem) fsItem;
        assertTrue(folderItem.getChildren().isEmpty());
        assertTrue(folderItem.getCanScrollDescendants());
        assertTrue(folderItem.scrollDescendants(null, 10, 1000).isEmpty());
        // Not adaptable as a FileSystemItem
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + notAFileSystemItem.getId(), principal);
        assertNull(fsItem);
        // Deleted
        trashService.trashDocument(custom);
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(),
                principal);
        assertNull(fsItem);
        // Use parent id
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(),
                syncRootItemId, principal);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + note.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
    }

    @Test
    public void testFileItem() throws Exception {

        // ------------------------------------------------------
        // FileItem#getDownloadURL
        // ------------------------------------------------------
        FileItem fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
        String downloadURL = fileItem.getDownloadURL();
        assertEquals("nxfile/test/" + file.getId() + "/blobholder:0/Joe.odt", downloadURL);

        // ------------------------------------------------------------
        // FileItem#getDigestAlgorithm
        // ------------------------------------------------------------
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        FileItem noteItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(note);
        assertEquals("MD5", noteItem.getDigestAlgorithm());

        // ------------------------------------------------------------
        // FileItem#getDigest
        // ------------------------------------------------------------
        assertEquals(file.getAdapter(BlobHolder.class).getBlob().getDigest(), fileItem.getDigest());
        String noteDigest = FileSystemItemHelper.getMD5Digest(note.getAdapter(BlobHolder.class).getBlob());
        assertEquals(noteDigest, noteItem.getDigest());
        assertEquals(custom.getAdapter(BlobHolder.class).getBlob().getDigest(),
                ((FileItem) defaultFileSystemItemFactory.getFileSystemItem(custom)).getDigest());

        // ------------------------------------------------------------
        // FileItem#getCanUpdate
        // ------------------------------------------------------------
        // As Administrator
        assertTrue(fileItem.getCanUpdate());

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the
        // joe session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertFalse(fileItem.getCanUpdate());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertTrue(fileItem.getCanUpdate());

            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

            // ------------------------------------------------------
            // FileItem#getBlob
            // ------------------------------------------------------
            Blob fileItemBlob = fileItem.getBlob();
            assertEquals("Joe.odt", fileItemBlob.getFilename());
            assertEquals("Content of Joe's file.", fileItemBlob.getString());
            // Check versioning
            assertVersion("0.0", file);

            // ------------------------------------------------------
            // FileItem#setBlob and versioning
            // ------------------------------------------------------
            Blob newBlob = new StringBlob("This is a new file.");
            newBlob.setFilename("New blob.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("New blob.txt", updatedBlob.getFilename());
            assertEquals("This is a new file.", updatedBlob.getString());
            // Check versioning => should not be versioned since same
            // contributor
            // and last modification was done before the versioning delay
            assertVersion("0.0", file);

            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since last
            // modification was done after the versioning delay
            assertVersion("0.1", file);
            List<DocumentModel> fileVersions = session.getVersions(file.getRef());
            assertEquals(1, fileVersions.size());
            DocumentModel lastFileVersion = fileVersions.get(0);
            Blob versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("New blob.txt", versionedBlob.getFilename());

            // Update file with another contributor
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since updated by a
            // different contributor
            assertVersion("0.2", file);
            fileVersions = session.getVersions(file.getRef());
            assertEquals(2, fileVersions.size());
            lastFileVersion = fileVersions.get(1);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // DocumentBackedFileItem#rename and versioning
            // ------------------------------------------------------
            // Save document to trigger the DublinCoreListener and update
            // dc:lastContributor to "Administrator"
            // rename the file to enable dc listener (disable if not dirty)
            file.setPropertyValue("file:content/name", "newTitle");
            file = session.saveDocument(file);
            // Check versioning => should be versioned cause last contributor has changed
            assertVersion("0.3", file);
            // Switch back to Administrator as last contributor
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            ensureJustModified(file, session);
            fileItem.rename("Renamed file.txt");
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("Renamed file.txt", updatedBlob.getFilename());
            // Check versioning => should not be versioned since same
            // contributor and last modification was done before the
            // versioning delay
            assertVersion("0.3", file);

            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            fileItem.rename("Renamed again.txt");
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("Renamed again.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since last
            // modification was done after the versioning delay
            assertVersion("0.4", file);
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
            updatedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("Renamed file.txt", updatedBlob.getFilename());

            // Update file with another contributor
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            fileItem.rename("File renamed by Joe.txt");
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File renamed by Joe.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since updated by a
            // different contributor
            assertVersion("0.5", file);
            fileVersions = session.getVersions(file.getRef());
            assertEquals(5, fileVersions.size());
            lastFileVersion = fileVersions.get(4);
            updatedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("Renamed again.txt", updatedBlob.getFilename());
        }
        resetPermissions(rootDoc, "joe");
    }

    @Test
    public void testFolderItem() throws Exception {

        // ------------------------------------------------------
        // FolderItem#canCreateChild
        // ------------------------------------------------------
        // As Administrator
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        assertTrue(folderItem.getCanCreateChild());

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the joe
        // session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            folder = joeSession.getDocument(folder.getRef());

            // By default folder is not under any sync root for Joe, hence
            // should not be mappable as an fs item.
            try {
                defaultFileSystemItemFactory.getFileSystemItem(folder);
                fail("Should have raised RootlessItemException as ");
            } catch (RootlessItemException e) {
                // expected
            }

            // Register the sync root for Joe's account
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertFalse(folderItem.getCanCreateChild());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertTrue(folderItem.getCanCreateChild());
        }
        resetPermissions(rootDoc, "joe");

        // ------------------------------------------------------
        // FolderItem#createFile and FolderItem#createFolder
        // ------------------------------------------------------
        folder = session.getDocument(folder.getRef());
        folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        // Note
        Blob childBlob = new StringBlob("This is the Note child.");
        childBlob.setFilename("Note child.txt");
        folderItem.createFile(childBlob, false);
        // File
        childBlob = new StringBlob("This is the File child.");
        childBlob.setFilename("File child.odt");
        childBlob.setMimeType("application/vnd.oasis.opendocument.text");
        folderItem.createFile(childBlob, false);
        // Folder
        folderItem.createFolder("Sub-folder", false);

        DocumentModelList children = session.query(String.format(
                "select * from Document where ecm:parentId = '%s' order by ecm:primaryType asc", folder.getId()));
        assertEquals(3, children.size());
        // Check File
        DocumentModel file = children.get(0);
        assertEquals("File", file.getType());
        assertEquals("File child.odt", file.getTitle());
        childBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("File child.odt", childBlob.getFilename());
        assertEquals("This is the File child.", childBlob.getString());
        // Check Folder
        DocumentModel subFolder = children.get(1);
        assertEquals("Folder", subFolder.getType());
        assertEquals("Sub-folder", subFolder.getTitle());
        // Check Note
        DocumentModel note = children.get(2);
        assertEquals("Note", note.getType());
        assertEquals("Note child.txt", note.getTitle());
        childBlob = note.getAdapter(BlobHolder.class).getBlob();
        assertEquals("Note child.txt", childBlob.getFilename());
        assertEquals("This is the Note child.", childBlob.getString());

        // --------------------------------------------------------------------------------------------
        // FolderItem#getChildren, FolderItem#getCanScrollDescendants and
        // FolderItem#scrollDescendants
        // --------------------------------------------------------------------------------------------
        // Create another child adaptable as a FileSystemItem => should be
        // retrieved
        DocumentModel adaptableChild = session.createDocumentModel("/syncRoot/aFolder", "adaptableChild", "File");
        Blob adaptableChildBlob = new StringBlob("Content of another file.");
        adaptableChildBlob.setFilename("Another file.odt");
        adaptableChild.setPropertyValue("file:content", (Serializable) adaptableChildBlob);
        adaptableChild = session.createDocument(adaptableChild);
        // Create another child not adaptable as a FileSystemItem => should
        // not be retrieved
        session.createDocument(
                session.createDocumentModel("/syncRoot/aFolder", "notAdaptableChild", "NotSynchronizable"));
        session.save();

        // Check getChildren
        List<FileSystemItem> folderChildren = folderItem.getChildren();
        assertEquals(4, folderChildren.size());
        // Ordered
        checkChildren(folderChildren, folder.getId(), note.getId(), file.getId(), subFolder.getId(),
                adaptableChild.getId(), true);

        // Check scrollDescendants
        assertTrue(folderItem.getCanScrollDescendants());
        // Scroll through all descendants in one breath
        ScrollFileSystemItemList folderDescendants = folderItem.scrollDescendants(null, 10, 1000);
        String scrollId = folderDescendants.getScrollId();
        assertNotNull(scrollId);
        assertEquals(4, folderDescendants.size());
        // Order is not determined
        checkChildren(folderDescendants, folder.getId(), note.getId(), file.getId(), subFolder.getId(),
                adaptableChild.getId(), false);
        // Check that next call to scrollDescendants returns an empty list
        assertTrue(folderItem.scrollDescendants(scrollId, 10, 1000).isEmpty());
        // Scroll through descendants in several steps
        folderDescendants.clear();
        ScrollFileSystemItemList descendantsBatch;
        int batchSize = 2;
        scrollId = null;
        while (!(descendantsBatch = folderItem.scrollDescendants(scrollId, batchSize, 1000)).isEmpty()) {
            assertTrue(descendantsBatch.size() > 0);
            scrollId = descendantsBatch.getScrollId();
            folderDescendants.addAll(descendantsBatch);
        }
        assertEquals(4, folderDescendants.size());
        // Order is not determined
        checkChildren(folderDescendants, folder.getId(), note.getId(), file.getId(), subFolder.getId(),
                adaptableChild.getId(), false);

        // Check batch size limit
        try {
            folderItem.scrollDescendants(null, 10000, 1000);
            fail("Should not be able to scroll through more descendants than the maximum batch size allowed.");
        } catch (NuxeoException e) {
            log.trace(e);
        }
    }

    @Test
    public void testSection() {
        // Check that a Section is adaptable as a FileSystemItem by the
        // defaultSyncRootFolderItemFactory
        DocumentModel section = session.createDocument(session.createDocumentModel("/", "sectionSyncRoot", "Section"));
        nuxeoDriveManager.registerSynchronizationRoot(principal, section, session);
        FolderItem sectionItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(section);
        assertNotNull(sectionItem);
        assertFalse(sectionItem.getCanCreateChild());
        assertFalse(sectionItem.getCanRename());
        assertTrue(sectionItem.getCanDelete());

        // Publish documents in the Section and check its children
        session.publishDocument(file, section);
        session.publishDocument(note, section);
        session.save();
        List<FileSystemItem> children = sectionItem.getChildren();
        assertEquals(2, children.size());
        FileSystemItem child = children.get(0);
        assertFalse(child.getCanRename());
        assertFalse(child.getCanDelete());
    }

    @Test
    public void testLockedDocument() {
        setPermission(syncRootFolder, "joe", SecurityConstants.READ_WRITE, true);
        setPermission(syncRootFolder, "jack", SecurityConstants.READ_WRITE, true);
        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, joeSession);
            DocumentModel joeFile = joeSession.getDocument(file.getRef());

            log.trace("Check readonly flags on an unlocked document");
            FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItem(joeFile);
            assertTrue(fsItem.getCanRename());
            assertTrue(fsItem.getCanDelete());
            assertTrue(((FileItem) fsItem).getCanUpdate());
            assertNull(fsItem.getLockInfo());

            log.trace("Check readonly flags on an document locked by the current user");
            joeSession.setLock(joeFile.getRef());
            // Re-fetch document to clear lock info
            joeFile = joeSession.getDocument(file.getRef());
            fsItem = defaultFileSystemItemFactory.getFileSystemItem(joeFile);
            assertTrue(fsItem.getCanRename());
            assertTrue(fsItem.getCanDelete());
            assertTrue(((FileItem) fsItem).getCanUpdate());
            Lock lockInfo = fsItem.getLockInfo();
            assertNotNull(lockInfo);
            assertEquals("joe", lockInfo.getOwner());
            assertNotNull(lockInfo.getCreated());

            // Check that the lock info is not fetched for FileSystemItem
            // adaptation when calling getChildren or
            // scrollDescendants
            FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(
                    syncRootFolder);
            List<FileSystemItem> children = syncRootFolderItem.getChildren();
            assertEquals(5, children.size());
            for (FileSystemItem child : children) {
                assertNull(child.getLockInfo());
            }
            children = syncRootFolderItem.scrollDescendants(null, 10, 1000);
            assertEquals(5, children.size());
            for (FileSystemItem child : children) {
                assertNull(child.getLockInfo());
            }

            try (CloseableCoreSession jackSession = coreFeature.openCoreSession("jack")) {
                nuxeoDriveManager.registerSynchronizationRoot(jackSession.getPrincipal(), syncRootFolder, jackSession);
                DocumentModel jackFile = jackSession.getDocument(file.getRef());

                log.trace("Check readonly flags for a non administrator on a document locked by another user");
                fsItem = defaultFileSystemItemFactory.getFileSystemItem(jackFile);
                assertFalse(fsItem.getCanRename());
                assertFalse(fsItem.getCanDelete());
                assertFalse(((FileItem) fsItem).getCanUpdate());
                lockInfo = fsItem.getLockInfo();
                assertNotNull(lockInfo);
                assertEquals("joe", lockInfo.getOwner());
                assertNotNull(lockInfo.getCreated());

                log.trace("Check readonly flags for an administrator on a document locked by another user");
                fsItem = defaultFileSystemItemFactory.getFileSystemItem(file);
                assertTrue(fsItem.getCanRename());
                assertTrue(fsItem.getCanDelete());
                assertTrue(((FileItem) fsItem).getCanUpdate());
                lockInfo = fsItem.getLockInfo();
                assertNotNull(lockInfo);
                assertEquals("joe", lockInfo.getOwner());
                assertNotNull(lockInfo.getCreated());

                log.trace("Check readonly flags for a non administrator on an unlocked document");
                joeSession.removeLock(joeFile.getRef());
                // Re-fetch document to clear lock info
                jackFile = jackSession.getDocument(file.getRef());
                fsItem = defaultFileSystemItemFactory.getFileSystemItem(jackFile);
                assertTrue(fsItem.getCanRename());
                assertTrue(fsItem.getCanDelete());
                assertTrue(((FileItem) fsItem).getCanUpdate());
                assertNull(fsItem.getLockInfo());
            }
        }
        resetPermissions(syncRootFolder, "jack");
        resetPermissions(syncRootFolder, "joe");
    }

    @Test
    public void testFolderItemChildrenPageProviderOverride() throws Exception {
        assumeFalse("Cannot test reload for in-memory repository", coreFeature.getStorageConfiguration().isDBSMem());

        nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), syncRootFolder, session);
        FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(syncRootFolder);
        assertEquals(5, syncRootFolderItem.getChildren().size());

        TransactionHelper.commitOrRollbackTransaction(); // should save documents before runtime reset
        try {
            deployer.deploy("org.nuxeo.drive.core.test:OSGI-INF/test-nuxeodrive-pageproviders-contrib-override.xml");
        } finally {
            TransactionHelper.startTransaction();
        }
        assertEquals(2, syncRootFolderItem.getChildren().size());
    }

    @Test
    public void testCollectionMembership() {
        DocumentModel doc = session.createDocumentModel(session.getRootDocument().getPathAsString(), "testDoc", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        log.trace("Try to adapt a document not member of any collection");
        try {
            defaultFileSystemItemFactory.getFileSystemItem(doc);
            fail("Trying to adapt doc as a FileSystemItem should throw a RootlessItemException");
        } catch (RootlessItemException e) {
            log.trace(e);
        }

        log.trace("Try to adapt a document member of a non sync root collection");
        DocumentModel nonSyncrootCollection = collectionManager.createCollection(session, "Non sync root collection",
                "", session.getRootDocument().getPathAsString());
        collectionManager.addToCollection(nonSyncrootCollection, doc, session);
        try {
            defaultFileSystemItemFactory.getFileSystemItem(doc);
            fail("Trying to adapt doc as a FileSystemItem should throw a RootlessItemException");
        } catch (RootlessItemException e) {
            log.trace(e);
        }

        log.trace("Adapt a document member of a non sync root colllection and a sync root collection");
        DocumentModel syncRootCollection = collectionManager.createCollection(session, "Sync root collection", "",
                session.getRootDocument().getPathAsString());
        nuxeoDriveManager.registerSynchronizationRoot(principal, syncRootCollection, session);
        collectionManager.addToCollection(syncRootCollection, doc, session);
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItem(doc);
        assertNotNull(fsItem);

        log.trace("Adapt a document member of a sync root collection only");
        collectionManager.removeFromCollection(nonSyncrootCollection, doc, session);
        assertEquals(fsItem, defaultFileSystemItemFactory.getFileSystemItem(doc));
    }

    @Test
    public void testScrollDescendantsIncludingCollections() {
        log.trace(
                "Add a document to a new collection \"testCollection\" created in \"/default-domain/UserWorkspaces/Administrator/Collections\"");
        collectionManager.addToNewCollection("testCollection", null, file, session);
        DocumentModel userCollections = collectionManager.getUserDefaultCollections(session);
        DocumentModel userWorkspace = session.getParentDocument(userCollections.getRef());

        log.trace("Create \"testFolder\" in \"/default-domain/UserWorkspaces/Administrator\"");
        DocumentModel testFolder = session.createDocumentModel(userWorkspace.getPathAsString(), "testFolder", "Folder");
        testFolder = session.createDocument(testFolder);

        log.trace(
                "Register \"/default-domain/UserWorkspaces/Administrator\" as a synchronization root for Administrator");
        nuxeoDriveManager.registerSynchronizationRoot(principal, userWorkspace, session);

        log.trace(
                "Scroll through the descendants of \"/default-domain/UserWorkspaces/Administrator\", expecting one: \"testFolder\", "
                        + "the \"Collections\" folder and its descendants being ignored");
        FolderItem userWorkspaceFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(
                userWorkspace);
        ScrollFileSystemItemList descendants = userWorkspaceFolderItem.scrollDescendants(null, 10, 1000);
        assertEquals(1, descendants.size());
        FileSystemItem descendant = descendants.get(0);
        assertTrue(descendant.isFolder());
        assertEquals("testFolder", descendant.getName());
    }

    /**
     * Tests the following hierarchy:
     *
     * <pre>
     * syncRoot             Synchronization root for joe with Read access
     * |-- folder           Blocked inheritance
     * |   |-- placeless    Read access for joe (placeless document)
     * |
     * |-- descendant1      Read access for joe (inheritance)
     * |-- descendant2      Read access for joe (inheritance)
     * |-- ...              Read access for joe (inheritance)
     * </pre>
     *
     * syncRoot should be synchronized with its directly accessible descendants to obtain the following hierarchy
     * client-side:
     *
     * <pre>
     * syncRoot
     * |-- descendant1
     * |-- descendant2
     * |-- ...
     * </pre>
     *
     * The placeless document will not be synchronized.
     *
     * @since 10.3
     */
    @Test
    public void testScrollDescendantsWithBlockedInheritance() {
        log.trace("Create \"placelessDocument\" in \"/syncRoot/folder\"");
        DocumentModel placeless = session.createDocumentModel(folder.getPathAsString(), "placeless", "File");
        Blob blob = new StringBlob("This is a placeless file for joe.");
        blob.setFilename("Placeless.odt");
        placeless.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(placeless);

        log.trace(
                "Set permissions for joe: Read on \"syncRoot\", Blocked inheritance on \"folder\", Read on \"placeless\"");
        setPermission(syncRootFolder, "joe", SecurityConstants.READ, true);
        setPermission(folder, ACE.BLOCK);
        setPermission(placeless, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the joe
        // session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            log.trace("Register \"/syncRoot\" as a synchronization root for joe");
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            log.trace(
                    "Scroll through the descendants of \"/syncRoot\", expecting its 4 directly accessible descendants, "
                            + "the blocked \"folder\" and its descendants being ignored");
            syncRootFolder = joeSession.getDocument(syncRootFolder.getRef());
            FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(
                    syncRootFolder);
            ScrollFileSystemItemList descendants = syncRootFolderItem.scrollDescendants(null, 10, 1000);
            assertEquals(4, descendants.size());
        }
        resetPermissions(syncRootFolder, "joe");
    }

    @Test
    @Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-blobholder-factory-contrib.xml")
    public void testBlobException() throws Exception {
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(file));
    }

    @Test
    public void testCreateFoldersWithSameName() {
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        FolderItem subFolderItem = folderItem.createFolder("subfolder01", false);
        FolderItem subFolderItem2 = folderItem.createFolder("subfolder01", false);
        assertNotEquals(subFolderItem.getId(), subFolderItem2.getId());
    }

    @Test
    public void testCreateFilesWithSameName() {
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        Blob blob = new StringBlob("This is a blob.");
        blob.setFilename("File01.txt");
        FileItem fileItem = folderItem.createFile(blob, false);
        FileItem fileItem2 = folderItem.createFile(blob, false);
        assertNotEquals(fileItem.getId(), fileItem2.getId());
    }

    @Test
    public void testExcludeOneToManyFileImporters() throws IOException {
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        File csvImport = FileUtils.getResourceFileFromContext("csv-import.zip");
        Blob blob = Blobs.createBlob(csvImport);
        FileItem fileItem = folderItem.createFile(blob, false);
        assertNotNull(fileItem);
        assertEquals("csv-import.zip", fileItem.getName());
    }

    protected void setPermission(DocumentModel doc, String userName, String permission, boolean isGranted) {
        setPermission(doc, new ACE(userName, permission, isGranted));
    }

    protected void setPermission(DocumentModel doc, ACE ace) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(ace);
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

    protected void assertVersion(String expected, DocumentModel doc) throws Exception {
        assertEquals(expected, getMajor(doc) + "." + getMinor(doc));
    }

    protected long getMajor(DocumentModel doc) {
        return getVersion(doc, VersioningService.MAJOR_VERSION_PROP);
    }

    protected long getMinor(DocumentModel doc) {
        return getVersion(doc, VersioningService.MINOR_VERSION_PROP);
    }

    protected long getVersion(DocumentModel doc, String prop) {
        Object propVal = doc.getPropertyValue(prop);
        if (propVal == null || !(propVal instanceof Long)) {
            return -1;
        } else {
            return ((Long) propVal).longValue();
        }
    }

    protected void checkChildren(List<FileSystemItem> folderChildren, String folderId, String noteId, String fileId,
            String subFolderId, String otherFileId, boolean ordered) throws Exception {

        boolean isNoteFound = false;
        boolean isFileFound = false;
        boolean isSubFolderFound = false;
        boolean isOtherFileFound = false;
        int childrenCount = 0;

        for (FileSystemItem fsItem : folderChildren) {
            // Check Note
            if (!isNoteFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + noteId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 0) {
                    assertTrue(fsItem instanceof FileItem);
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Note child.txt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    Blob fileItemBlob = ((FileItem) fsItem).getBlob();
                    assertEquals("Note child.txt", fileItemBlob.getFilename());
                    assertEquals("This is the Note child.", fileItemBlob.getString());
                    isNoteFound = true;
                    childrenCount++;
                }
            }
            // Check File
            else if (!isFileFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + fileId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 1) {
                    assertTrue(fsItem instanceof FileItem);
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("File child.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    Blob fileItemBlob = ((FileItem) fsItem).getBlob();
                    assertEquals("File child.odt", fileItemBlob.getFilename());
                    assertEquals("This is the File child.", fileItemBlob.getString());
                    isFileFound = true;
                    childrenCount++;
                }
            }
            // Check sub-Folder
            else if (!isSubFolderFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolderId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 2) {
                    assertTrue(fsItem instanceof FolderItem);
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Sub-folder", fsItem.getName());
                    assertTrue(fsItem.isFolder());
                    FolderItem folderItem = (FolderItem) fsItem;
                    List<FileSystemItem> childFolderChildren = folderItem.getChildren();
                    assertNotNull(childFolderChildren);
                    assertEquals(0, childFolderChildren.size());
                    assertTrue(folderItem.getCanScrollDescendants());
                    ScrollFileSystemItemList childFolderDescendants = folderItem.scrollDescendants(null, 10, 1000);
                    assertNotNull(childFolderDescendants);
                    assertNotNull(childFolderDescendants.getScrollId());
                    assertEquals(0, childFolderDescendants.size());
                    isSubFolderFound = true;
                    childrenCount++;
                }
            }
            // Check other File
            else if (!isOtherFileFound && (DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + otherFileId).equals(fsItem.getId())) {
                if (!ordered || ordered && childrenCount == 3) {
                    assertTrue(fsItem instanceof FileItem);
                    assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folderId, fsItem.getParentId());
                    assertEquals("Another file.odt", fsItem.getName());
                    assertFalse(fsItem.isFolder());
                    Blob fileItemBlob = ((FileItem) fsItem).getBlob();
                    assertEquals("Another file.odt", fileItemBlob.getFilename());
                    assertEquals("Content of another file.", fileItemBlob.getString());
                    isOtherFileFound = true;
                    childrenCount++;
                }
            } else {
                fail(String.format("FileSystemItem %s doesn't match any expected.", fsItem.getId()));
            }
        }
    }

    void reload() throws InterruptedException {
        Properties lastProps = Framework.getProperties();
        try {
            Framework.getService(ReloadService.class).reload();
        } finally {
            Framework.getProperties().putAll(lastProps);
        }
    }

    /**
     * Ensures that the given document has just been modified to avoid a false positive when checking if versioning is
     * needed in {@link DefaultFileSystemItemFactory#needsVersioning(DocumentModel)}.
     */
    protected void ensureJustModified(DocumentModel doc, CoreSession session) {
        doc.setPropertyValue("dc:modified", Calendar.getInstance());
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        session.saveDocument(doc);
    }
}
