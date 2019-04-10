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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.impl.FileSystemItemHelper;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the {@link DefaultFileSystemItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.dublincore", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.collections.core", "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.runtime.reload" })
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestDefaultFileSystemItemFactory {

    private static final Log log = LogFactory.getLog(TestDefaultFileSystemItemFactory.class);

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String DEFAULT_SYNC_ROOT_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory#test#";

    // needs to be bigger than 1s for MySQL
    private static final int VERSIONING_DELAY = 3000; // ms

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected RepositorySettings repository;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected CollectionManager collectionManager;

    protected Principal principal;

    protected String syncRootItemId;

    protected DocumentModel syncRootFolder;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    protected VersioningFileSystemItemFactory defaultFileSystemItemFactory;

    /**
     * For databases that don't have sub-second resolution, sleep a bit to get to the next second.
     */
    protected void maybeSleepToNextSecond() {
        DatabaseHelper.DATABASE.maybeSleepToNextSecond();
    }

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
        folderishFile = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFolderishFile", "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel(syncRootFolder.getPathAsString(), "notAFileSystemItem",
                "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);

        session.save();

        // Get default file system item factory
        defaultFileSystemItemFactory = (VersioningFileSystemItemFactory) ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory("defaultFileSystemItemFactory");
        assertTrue(defaultFileSystemItemFactory instanceof VersioningFileSystemItemFactory);

        // Set versioning delay to 1 second
        defaultFileSystemItemFactory.setVersioningDelay(VERSIONING_DELAY / 1000.0);
        assertEquals(VERSIONING_DELAY / 1000.0, defaultFileSystemItemFactory.getVersioningDelay(), .01);
        assertEquals(VersioningOption.MINOR, defaultFileSystemItemFactory.getVersioningOption());
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
        custom.followTransition("delete");
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
        DocumentRef versionRef = session.checkIn(note.getRef(), VersioningOption.MINOR, null);
        DocumentModel version = session.getDocument(versionRef);
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(version));

        // Proxy
        DocumentModel proxy = session.createProxy(note.getRef(), folder.getRef());
        assertFalse(defaultFileSystemItemFactory.isFileSystemItem(proxy));

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
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(0, children.size());

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
        try (CoreSession joeSession = repository.openSessionAs("joe")) {

            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            // Under Oracle, the READ ACL optims are not visible from the joe
            // session while the transaction has not been committed.
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CoreSession joeSession = repository.openSessionAs("joe")) {

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
    public void testPermissionCheckOptimized() throws Exception {
        Framework.getProperties().put("org.nuxeo.drive.permissionCheckOptimized", "true");
        harness.deployContrib("org.nuxeo.drive.core.test", "OSGI-INF/test-nuxeodrive-permissions-contrib.xml");
        setPermission(syncRootFolder, "joe", SecurityConstants.READ, true);
        try (CoreSession joeSession = repository.openSessionAs("joe")) {
            log.trace("Register the sync root for Joe's account");
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, joeSession);
            folder = joeSession.getDocument(folder.getRef());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Read granted on parent folder");
            FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Write granted on folder, AddChildren not granted on folder and RemoveChildren not granted on parent folder");
            setPermission(folder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // True here as optimized => no explicit check of AddChildren on folder nor RemoveChildren on parent folder
            assertTrue(folderItem.getCanDelete());
            assertTrue(folderItem.getCanCreateChild());

            log.trace("Check canDelete flag on folder for user joe with Write (thus RemoveChildren) granted on parent folder");
            setPermission(syncRootFolder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // Still true with RemoveChildren on the parent folder
            assertTrue(folderItem.getCanDelete());

            log.trace("Check canCreateChild flag on folder for user joe with AddChildren granted on folder");
            setPermission(folder, "joe", SecurityConstants.ADD_CHILDREN, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // Still true with AddChildren on the folder
            assertTrue(folderItem.getCanCreateChild());
        } finally {
            resetPermissions(folder, "joe");
            resetPermissions(syncRootFolder, "joe");
            harness.undeployContrib("org.nuxeo.drive.core.test", "OSGI-INF/test-nuxeodrive-permissions-contrib.xml");
            Framework.getProperties().remove("org.nuxeo.drive.permissionCheckOptimized");
        }
    }

    @Test
    public void testPermissionCheckNotOptimized() throws Exception {
        harness.deployContrib("org.nuxeo.drive.core.test", "OSGI-INF/test-nuxeodrive-permissions-contrib.xml");
        setPermission(syncRootFolder, "joe", SecurityConstants.READ, true);
        try (CoreSession joeSession = repository.openSessionAs("joe")) {
            log.trace("Register the sync root for Joe's account");
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, joeSession);
            folder = joeSession.getDocument(folder.getRef());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Read granted on parent folder");
            FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace("Check canDelete/canCreateChild flags on folder for user joe with Write granted on folder, AddChildren not granted on folder and RemoveChildren not granted on parent folder");
            setPermission(folder, "joe", SecurityConstants.WRITE, true);
            folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
            // False here as not optimized => explicit check of RemoveChildren on parent folder and AddChildren on
            // folder
            assertFalse(folderItem.getCanDelete());
            assertFalse(folderItem.getCanCreateChild());

            log.trace("Check canDelete flag on folder for user joe with Write (thus RemoveChildren) granted on parent folder");
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
        } finally {
            resetPermissions(folder, "joe");
            resetPermissions(syncRootFolder, "joe");
            harness.undeployContrib("org.nuxeo.drive.core.test", "OSGI-INF/test-nuxeodrive-permissions-contrib.xml");
        }
    }

    @Test
    public void testExists() throws Exception {

        // Bad id
        try {
            defaultFileSystemItemFactory.exists("badId", principal);
            fail("Should not be able to check existence for bad id.");
        } catch (ClientException e) {
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
        assertFalse(defaultFileSystemItemFactory.exists(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + notAFileSystemItem.getId(), principal));
        // Deleted
        file.followTransition("delete");
        assertFalse(defaultFileSystemItemFactory.exists(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file.getId(), principal));
    }

    @Test
    public void testGetFileSystemItemById() throws Exception {

        // Non existent doc id, must return null
        assertNull(defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + "nonExistentDocId", principal));
        // File without a blob
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        session.save();
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + file.getId(), principal);
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
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + folder.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());
        // Not adaptable as a FileSystemItem
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX
                + notAFileSystemItem.getId(), principal);
        assertNull(fsItem);
        // Deleted
        custom.followTransition("delete");
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + custom.getId(), principal);
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
        assertEquals("nxbigfile/test/" + file.getId() + "/blobholder:0/Joe.odt", downloadURL);

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

        try (CoreSession joeSession = repository.openSessionAs("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);
            // Under Oracle, the READ ACL optims are not visible from the
            // joe session while the transaction has not been committed.
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CoreSession joeSession = repository.openSessionAs("joe")) {
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
            maybeSleepToNextSecond();
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
            session.saveDocument(file);
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            fileItem.rename("Renamed file.txt");
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("Renamed file.txt", updatedBlob.getFilename());
            // Check versioning => should not be versioned since same
            // contributor and last modification was done before the
            // versioning delay
            assertVersion("0.2", file);

            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            fileItem.rename("Renamed again.txt");
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("Renamed again.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since last
            // modification was done after the versioning delay
            assertVersion("0.3", file);
            fileVersions = session.getVersions(file.getRef());
            assertEquals(3, fileVersions.size());
            lastFileVersion = fileVersions.get(2);
            updatedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("Renamed file.txt", updatedBlob.getFilename());

            // Update file with another contributor
            maybeSleepToNextSecond();
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            fileItem.rename("File renamed by Joe.txt");
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File renamed by Joe.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned since updated by a
            // different contributor
            assertVersion("0.4", file);
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
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

        try (CoreSession joeSession = repository.openSessionAs("joe")) {
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
        folderItem.createFile(childBlob);
        // File
        childBlob = new StringBlob("This is the File child.");
        childBlob.setFilename("File child.odt");
        childBlob.setMimeType("application/vnd.oasis.opendocument.text");
        folderItem.createFile(childBlob);
        // Folder
        folderItem.createFolder("Sub-folder");

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

        // ------------------------------------------------------
        // FolderItem#getChildren
        // ------------------------------------------------------
        // Create another child adaptable as a FileSystemItem => should be
        // retrieved
        DocumentModel adaptableChild = session.createDocumentModel("/syncRoot/aFolder", "adaptableChild", "File");
        Blob adaptableChildBlob = new StringBlob("Content of another file.");
        adaptableChildBlob.setFilename("Another file.odt");
        adaptableChild.setPropertyValue("file:content", (Serializable) adaptableChildBlob);
        adaptableChild = session.createDocument(adaptableChild);
        // Create another child not adaptable as a FileSystemItem => should
        // not
        // be retrieved
        session.createDocument(session.createDocumentModel("/syncRoot/aFolder", "notAdaptableChild",
                "NotSynchronizable"));
        session.save();

        List<FileSystemItem> folderChildren = folderItem.getChildren();
        assertEquals(4, folderChildren.size());
        // Don't check children order against MySQL database because of the
        // milliseconds limitation
        boolean ordered = !(DatabaseHelper.DATABASE instanceof DatabaseMySQL);
        checkChildren(folderChildren, folder.getId(), note.getId(), file.getId(), subFolder.getId(),
                adaptableChild.getId(), ordered);
    }

    @Test
    public void testLockedDocument() {
        setPermission(syncRootFolder, "joe", SecurityConstants.READ_WRITE, true);
        setPermission(syncRootFolder, "jack", SecurityConstants.READ_WRITE, true);
        try (CoreSession joeSession = repository.openSessionAs("joe")) {
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

            // Check that the lock info is not fetched for FileSystemItem adaptation when calling getChildren
            FileSystemItemFactory defaultSyncRootFolderItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory("defaultSyncRootFolderItemFactory");
            FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(syncRootFolder);
            List<FileSystemItem> children = syncRootFolderItem.getChildren();
            assertEquals(5, children.size());
            for (FileSystemItem child : children) {
                assertNull(child.getLockInfo());
            }

            try (CoreSession jackSession = repository.openSessionAs("jack")) {
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
        nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), syncRootFolder, session);
        FileSystemItemFactory defaultSyncRootFolderItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory("defaultSyncRootFolderItemFactory");
        FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(syncRootFolder);
        assertEquals(5, syncRootFolderItem.getChildren().size());

        harness.deployContrib("org.nuxeo.drive.core.test",
                "OSGI-INF/test-nuxeodrive-pageproviders-contrib-override.xml");
        reload();
        assertEquals(2, syncRootFolderItem.getChildren().size());
        harness.undeployContrib("org.nuxeo.drive.core.test",
                "OSGI-INF/test-nuxeodrive-pageproviders-contrib-override.xml");
        reload();
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
        doc = session.getDocument(doc.getRef());
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
        doc = session.getDocument(doc.getRef());
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItem(doc);
        assertNotNull(fsItem);

        log.trace("Adapt a document member of a sync root collection only");
        collectionManager.removeFromCollection(nonSyncrootCollection, doc, session);
        doc = session.getDocument(doc.getRef());
        assertEquals(fsItem, defaultFileSystemItemFactory.getFileSystemItem(doc));
    }

    @Test
    public void testCreateFoldersWithSameName() {
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        FolderItem subFolderItem = folderItem.createFolder("subfolder01");
        FolderItem subFolderItem2 = folderItem.createFolder("subfolder01");
        assertNotEquals(subFolderItem.getId(), subFolderItem2.getId());
    }
    
    @Test
    public void testCreateFilesWithSameName() {
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        Blob blob = new StringBlob("This is a blob.");
        blob.setFilename("File01.txt");
        FileItem fileItem = folderItem.createFile(blob);
        FileItem fileItem2 = folderItem.createFile(blob);
        assertNotEquals(fileItem.getId(), fileItem2.getId());
    }

    protected void setPermission(DocumentModel doc, String userName, String permission, boolean isGranted)
            throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected void resetPermissions(DocumentModel doc, String userName) throws ClientException {
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

    protected long getMajor(DocumentModel doc) throws ClientException {
        return getVersion(doc, VersioningService.MAJOR_VERSION_PROP);
    }

    protected long getMinor(DocumentModel doc) throws ClientException {
        return getVersion(doc, VersioningService.MINOR_VERSION_PROP);
    }

    protected long getVersion(DocumentModel doc, String prop) throws ClientException {
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
                    List<FileSystemItem> childFolderChildren = ((FolderItem) fsItem).getChildren();
                    assertNotNull(childFolderChildren);
                    assertEquals(0, childFolderChildren.size());
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

    void reload() throws Exception {
        Properties lastProps = Framework.getProperties();
        try {
            Framework.getLocalService(ReloadService.class).reload();
        } finally {
            Framework.getProperties().putAll(lastProps);
        }
    }
}
