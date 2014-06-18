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
package org.nuxeo.drive.hierarchy.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionTopLevelFolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.SharedSyncRootParentFolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.UserSyncRootParentFolderItem;
import org.nuxeo.drive.operations.NuxeoDriveGetChildren;
import org.nuxeo.drive.operations.NuxeoDriveGetFileSystemItem;
import org.nuxeo.drive.operations.NuxeoDriveGetTopLevelFolder;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.core.storage.sql.DatabaseSQLServer;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the user workspace and permission based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, EmbeddedAutomationServerFeature.class})
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations",
        "org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
@TransactionalConfig(autoStart=true)
public class TestPermissionHierarchy {

    private static final String TOP_LEVEL_ID = "org.nuxeo.drive.hierarchy.permission.factory.PermissionTopLevelFactory#";

    private static final String USER_SYNC_ROOT_PARENT_ID_PREFIX = "userSyncRootParentFactory#test#";

    private static final String SHARED_SYNC_ROOT_PARENT_ID = "sharedSyncRootParentFactory#";

    private static final String SYNC_ROOT_ID_PREFIX = "permissionSyncRootFactory#test#";

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String CONTENT_PREFIX = "The content of file ";

    @Inject
    protected CoreSession session;

    @Inject
    protected RepositorySettings repository;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserWorkspaceService userWorkspaceService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected HttpAutomationClient automationClient;

    protected CoreSession session1;

    protected CoreSession session2;

    protected DocumentModel userWorkspace1;

    protected DocumentModel userWorkspace2;

    protected DocumentModel user1Folder1, user1File1, user1Folder2, user1File2,
            user1Folder3, user1File3, user1Folder4, user2Folder1, user2File1,
            user2Folder2, user2File2, user2Folder3, user2File3;

    protected String userWorkspace1ItemId;

    protected String userWorkspace1ItemPath;

    protected Session clientSession1;

    protected ObjectMapper mapper;

    /**
     * Initializes the test hierarchy.
     *
     * <pre>
     * Server side for user1
     * ==============================
     *
     * /user1 (user workspace)
     *   |-- user1Folder1
     *   |     |-- user1File1
     *   |     |-- user1Folder2
     *   |-- user1File2
     *   |-- user1Folder3
     *   |     |-- user1File3
     *   |-- user1Folder4
     *
     * Server side for user2
     * ==============================
     *
     * /user2 (user workspace)
     *   |-- user2Folder1       (registered as a synchronization root with ReadWrite permission for user1)
     *   |     |-- user2File1
     *   |     |-- user2Folder2
     *   |-- user2File2
     *   |-- user2Folder3       (registered as a synchronization root with ReadWrite permission for user1)
     *   |     |-- user2File3
     *
     * </pre>
     */
    @Before
    public void init() throws Exception {

        // Create test users
        createUser("user1", "user1");
        createUser("user2", "user2");

        // Open a core session for each user
        session1 = repository.openSessionAs("user1");
        session2 = repository.openSessionAs("user2");

        // Create user workspace for each user
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session1, null);
        userWorkspace2 = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session2, null);

        userWorkspace1ItemId = USER_SYNC_ROOT_PARENT_ID_PREFIX
                + userWorkspace1.getId();
        userWorkspace1ItemPath = "/" + TOP_LEVEL_ID + "/"
                + userWorkspace1ItemId;

        // Populate user workspaces
        // user1
        user1Folder1 = createFolder(session1, userWorkspace1.getPathAsString(),
                "user1Folder1", "Folder");
        user1File1 = createFile(session1, user1Folder1.getPathAsString(),
                "user1File1", "File", "user1File1.txt", CONTENT_PREFIX
                        + "user1File1");
        user1Folder2 = createFolder(session1, user1Folder1.getPathAsString(),
                "user1Folder2", "Folder");
        user1File2 = createFile(session1, userWorkspace1.getPathAsString(),
                "user1File2", "File", "user1File2.txt", CONTENT_PREFIX
                        + "user1File2");
        user1Folder3 = createFolder(session1, userWorkspace1.getPathAsString(),
                "user1Folder3", "Folder");
        user1File3 = createFile(session1, user1Folder3.getPathAsString(),
                "user1File3", "File", "user1File3.txt", CONTENT_PREFIX
                        + "user1File3");
        user1Folder4 = createFolder(session1, userWorkspace1.getPathAsString(),
                "user1Folder4", "Folder");
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // user2
        user2Folder1 = createFolder(session2, userWorkspace2.getPathAsString(),
                "user2Folder1", "Folder");
        user2File1 = createFile(session2, user2Folder1.getPathAsString(),
                "user2File1", "File", "user2File1.txt", CONTENT_PREFIX
                        + "user2File1");
        user2Folder2 = createFolder(session2, user2Folder1.getPathAsString(),
                "user2Folder2", "Folder");
        user2File2 = createFile(session2, userWorkspace2.getPathAsString(),
                "user2File2", "File", "user2File2.txt", CONTENT_PREFIX
                        + "user2File2");
        user2Folder3 = createFolder(session2, userWorkspace2.getPathAsString(),
                "user2Folder3", "Folder");
        user2File3 = createFile(session2, user2Folder3.getPathAsString(),
                "user2File3", "File", "user2File3.txt", CONTENT_PREFIX
                        + "user2File3");
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        setPermission(user2Folder1, "user1", SecurityConstants.READ_WRITE, true);
        setPermission(user2Folder3, "user1", SecurityConstants.READ_WRITE, true);

        // Register shared folders as synchronization roots for user1
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user2Folder1.getRef()), session1);
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user2Folder3.getRef()), session1);

        // Get an Automation client session for user1
        clientSession1 = automationClient.getSession("user1", "user1");

        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws ClientException {
        // Close core sessions
        session1.close();
        session2.close();

        // Delete test users
        deleteUser("user1");
        deleteUser("user2");
    }

    @Test
    public void testClientSideUser1() throws Exception {

        // ---------------------------------------------
        // Check active factories
        // ---------------------------------------------
        TopLevelFolderItemFactory topLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertEquals(
                "org.nuxeo.drive.hierarchy.permission.factory.PermissionTopLevelFactory",
                topLevelFolderItemFactory.getName());

        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(4, activeFactories.size());
        assertTrue(activeFactories.contains("defaultFileSystemItemFactory"));
        assertTrue(activeFactories.contains("userSyncRootParentFactory"));
        assertTrue(activeFactories.contains("permissionSyncRootFactory"));
        assertTrue(activeFactories.contains("sharedSyncRootParentFactory"));

        // ---------------------------------------------
        // Check top level folder: "Nuxeo Drive"
        // ---------------------------------------------
        Blob topLevelFolderJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetTopLevelFolder.ID).execute();
        assertNotNull(topLevelFolderJSON);

        PermissionTopLevelFolderItem topLevelFolder = mapper.readValue(
                topLevelFolderJSON.getStream(),
                PermissionTopLevelFolderItem.class);

        assertNotNull(topLevelFolder);
        assertEquals(TOP_LEVEL_ID, topLevelFolder.getId());
        assertNull(topLevelFolder.getParentId());
        assertEquals("/" + TOP_LEVEL_ID, topLevelFolder.getPath());
        assertEquals("Nuxeo Drive", topLevelFolder.getName());
        assertTrue(topLevelFolder.isFolder());
        assertEquals("system", topLevelFolder.getCreator());
        assertFalse(topLevelFolder.getCanRename());
        assertFalse(topLevelFolder.getCanDelete());
        assertFalse(topLevelFolder.getCanCreateChild());

        /**
         * <pre>
         * ===================================================
         * User workspace registered as a synchronization root
         * ===================================================
         * => Expected client side for user1:
         *
         * Nuxeo Drive
         *   |-- My Docs
         *   |     |-- user1Folder1
         *   |     |     |-- user1File1
         *   |     |     |-- user1Folder2
         *   |     |-- user1File2
         *   |     |-- user1Folder3
         *   |     |      |-- user1File3
         *   |     |-- user1Folder4
         *   |
         *   |-- Other Docs
         *   |     |-- user2Folder1
         *   |     |     |-- user2File1
         *   |     |     |-- user2Folder2
         *   |     |-- user2Folder3
         *   |     |     |-- user2File3
         *
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                userWorkspace1, session1);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ---------------------------------------------
        // Check top level folder children
        // ---------------------------------------------
        Blob topLevelChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", topLevelFolder.getId()).execute();

        ArrayNode topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        // Check "My Docs"
        UserSyncRootParentFolderItem userSyncRootParent = mapper.readValue(
                topLevelChildren.get(0), UserSyncRootParentFolderItem.class);
        assertEquals(userWorkspace1ItemId, userSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, userSyncRootParent.getParentId());
        assertEquals(userWorkspace1ItemPath, userSyncRootParent.getPath());
        assertEquals("My Docs", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals("user1", userSyncRootParent.getCreator());
        assertFalse(userSyncRootParent.getCanRename());
        assertFalse(userSyncRootParent.getCanDelete());
        // Can create a child since "My Docs" is the user workspace
        assertTrue(userSyncRootParent.getCanCreateChild());

        // Check "Other Docs"
        SharedSyncRootParentFolderItem sharedSyncRootParent = mapper.readValue(
                topLevelChildren.get(1), SharedSyncRootParentFolderItem.class);
        assertEquals(SHARED_SYNC_ROOT_PARENT_ID, sharedSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, sharedSyncRootParent.getParentId());
        assertEquals("/" + TOP_LEVEL_ID + "/" + SHARED_SYNC_ROOT_PARENT_ID,
                sharedSyncRootParent.getPath());
        assertEquals("Other Docs", sharedSyncRootParent.getName());
        assertTrue(sharedSyncRootParent.isFolder());
        assertEquals("system", sharedSyncRootParent.getCreator());
        assertFalse(sharedSyncRootParent.getCanRename());
        assertFalse(sharedSyncRootParent.getCanDelete());
        assertFalse(sharedSyncRootParent.getCanCreateChild());

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        Blob userSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", userSyncRootParent.getId()).execute();

        ArrayNode userSyncRoots = mapper.readValue(
                userSyncRootsJSON.getStream(), ArrayNode.class);
        assertNotNull(userSyncRoots);
        assertEquals(4, userSyncRoots.size());

        // user1Folder1
        DocumentBackedFolderItem folderItem = mapper.readValue(
                userSyncRoots.get(0), DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1Folder1, userWorkspace1ItemId, userWorkspace1ItemPath,
                "user1Folder1", "user1");
        Blob folderItemChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", folderItem.getId()).execute();
        ArrayNode folderItemChildren = mapper.readValue(
                folderItemChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(2, folderItemChildren.size());
        // user1File1
        DocumentBackedFileItem childFileItem = mapper.readValue(
                folderItemChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1File1, folderItem.getId(), folderItem.getPath(),
                "user1File1.txt", "user1");
        // user1Folder2
        DocumentBackedFolderItem childFolderItem = mapper.readValue(
                folderItemChildren.get(1), DocumentBackedFolderItem.class);
        checkFolderItem(childFolderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1Folder2, folderItem.getId(), folderItem.getPath(),
                "user1Folder2", "user1");
        // user1File2
        DocumentBackedFileItem fileItem = mapper.readValue(
                userSyncRoots.get(1), DocumentBackedFileItem.class);
        checkFileItem(fileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File2,
                userWorkspace1ItemId, userWorkspace1ItemPath, "user1File2.txt",
                "user1");
        // user1Folder3
        folderItem = mapper.readValue(userSyncRoots.get(2),
                DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1Folder3, userWorkspace1ItemId, userWorkspace1ItemPath,
                "user1Folder3", "user1");
        folderItemChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", folderItem.getId()).execute();
        folderItemChildren = mapper.readValue(
                folderItemChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(1, folderItemChildren.size());
        // user1File3
        childFileItem = mapper.readValue(folderItemChildren.get(0),
                DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1File3, folderItem.getId(), folderItem.getPath(),
                "user1File3.txt", "user1");
        // user1Folder4
        folderItem = mapper.readValue(userSyncRoots.get(3),
                DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1Folder4, userWorkspace1ItemId, userWorkspace1ItemPath,
                "user1Folder4", "user1");

        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        Blob sharedSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                sharedSyncRootParent.getId()).execute();

        List<DefaultSyncRootFolderItem> sharedSyncRoots = mapper.readValue(
                sharedSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(sharedSyncRoots);
        assertEquals(2, sharedSyncRoots.size());

        // user2Folder1
        DefaultSyncRootFolderItem sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX,
                session1.getDocument(user2Folder1.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(),
                "user2Folder1", "user2");
        Blob sharedSyncRootChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", sharedSyncRoot.getId()).execute();
        ArrayNode sharedSyncRootChildren = mapper.readValue(
                sharedSyncRootChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(2, sharedSyncRootChildren.size());
        // user2File1
        DocumentBackedFileItem sharedSyncRootChildFileItem = mapper.readValue(
                sharedSyncRootChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem,
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File1.getRef()),
                sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File1.txt", "user2");
        // user2Folder2
        DocumentBackedFolderItem sharedSyncRootChildFolderItem = mapper.readValue(
                sharedSyncRootChildren.get(1), DocumentBackedFolderItem.class);
        checkFolderItem(sharedSyncRootChildFolderItem,
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2Folder2.getRef()),
                sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2Folder2", "user2");
        // user2Folder3
        sharedSyncRoot = sharedSyncRoots.get(1);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX,
                session1.getDocument(user2Folder3.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(),
                "user2Folder3", "user2");
        sharedSyncRootChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", sharedSyncRoot.getId()).execute();
        sharedSyncRootChildren = mapper.readValue(
                sharedSyncRootChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        // user2File3
        sharedSyncRootChildFileItem = mapper.readValue(
                sharedSyncRootChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem,
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File3.getRef()),
                sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File3.txt", "user2");

        /**
         * <pre>
         * =======================================================
         * User workspace NOT registered as a synchronization root
         * =======================================================
         * => Expected client side for user1:
         *
         * Nuxeo Drive
         *   |-- My Docs
         *   |
         *   |-- Other Docs (unchanged)
         *   |     |-- user2Folder1
         *   |     |     |-- user2File1
         *   |     |     |-- user2Folder2
         *   |     |-- user2Folder3
         *   |     |     |-- user2File3
         *
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.unregisterSynchronizationRoot(
                session1.getPrincipal(), userWorkspace1, session1);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // ---------------------------------------------
        // Check "My Docs"
        // ---------------------------------------------
        Blob userSyncRootParentJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id", userWorkspace1ItemId).execute();
        assertNotNull(userSyncRootParentJSON);

        userSyncRootParent = mapper.readValue(
                userSyncRootParentJSON.getStream(),
                UserSyncRootParentFolderItem.class);
        assertEquals(userWorkspace1ItemId, userSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, userSyncRootParent.getParentId());
        assertEquals(userWorkspace1ItemPath, userSyncRootParent.getPath());
        assertEquals("My Docs", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals("user1", userSyncRootParent.getCreator());
        assertFalse(userSyncRootParent.getCanRename());
        assertFalse(userSyncRootParent.getCanDelete());
        // Cannot create a child since "My Docs" is only the parent of the
        // synchronization roots, not the user workspace
        assertFalse(userSyncRootParent.getCanCreateChild());

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        userSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", userSyncRootParent.getId()).execute();

        userSyncRoots = mapper.readValue(userSyncRootsJSON.getStream(),
                ArrayNode.class);
        assertNotNull(userSyncRoots);
        assertEquals(0, userSyncRoots.size());

        /**
         * <pre>
         * =======================================================
         * User workspace NOT registered as a synchronization root
         * but specific folders yes: user1Folder3, user1Folder4
         * =======================================================
         * => Expected client side for user1:
         *
         * Nuxeo Drive
         *   |-- My Docs
         *   |     |-- user1Folder3
         *   |     |      |-- user1File3
         *   |     |-- user1Folder4
         *   |
         *   |-- Other Docs (unchanged)
         *   |     |-- user2Folder1
         *   |     |     |-- user2File1
         *   |     |     |-- user2Folder2
         *   |     |-- user2Folder3
         *   |     |     |-- user2File3
         *
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                user1Folder3, session1);
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                user1Folder4, session1);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        userSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", userSyncRootParent.getId()).execute();

        userSyncRoots = mapper.readValue(userSyncRootsJSON.getStream(),
                ArrayNode.class);
        assertNotNull(userSyncRoots);
        assertEquals(2, userSyncRoots.size());

        // user1Folder3
        folderItem = mapper.readValue(userSyncRoots.get(0),
                DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, SYNC_ROOT_ID_PREFIX, user1Folder3,
                userWorkspace1ItemId, userWorkspace1ItemPath, "user1Folder3",
                "user1");
        folderItemChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", folderItem.getId()).execute();
        folderItemChildren = mapper.readValue(
                folderItemChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(1, folderItemChildren.size());
        // user1File3
        childFileItem = mapper.readValue(folderItemChildren.get(0),
                DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1File3, folderItem.getId(), folderItem.getPath(),
                "user1File3.txt", "user1");
        // user1Folder4
        folderItem = mapper.readValue(userSyncRoots.get(1),
                DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, SYNC_ROOT_ID_PREFIX, user1Folder4,
                userWorkspace1ItemId, userWorkspace1ItemPath, "user1Folder4",
                "user1");

        /**
         * <pre>
         * =======================================================
         * Unregister a shared folder: user2Folder1
         * =======================================================
         * => Expected client side for user1:
         *
         * Nuxeo Drive
         *   |-- My Docs (unchanged)
         *   |     |-- user1Folder3
         *   |     |      |-- user1File3
         *   |     |-- user1Folder4
         *   |
         *   |-- Other Docs
         *   |     |-- user2Folder3
         *   |     |     |-- user2File3
         *
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.unregisterSynchronizationRoot(
                session1.getPrincipal(),
                session1.getDocument(user2Folder1.getRef()), session1);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        sharedSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                sharedSyncRootParent.getId()).execute();

        sharedSyncRoots = mapper.readValue(sharedSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(sharedSyncRoots);
        assertEquals(1, sharedSyncRoots.size());

        // user2Folder3
        sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX,
                session1.getDocument(user2Folder3.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(),
                "user2Folder3", "user2");
        sharedSyncRootChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", sharedSyncRoot.getId()).execute();
        sharedSyncRootChildren = mapper.readValue(
                sharedSyncRootChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        // user2File3
        sharedSyncRootChildFileItem = mapper.readValue(
                sharedSyncRootChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem,
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File3.getRef()),
                sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File3.txt", "user2");

        /**
         * <pre>
         * =======================================================
         * Remove permission on a shared folder: user2Folder3
         * =======================================================
         * => Expected client side for user1:
         *
         * Nuxeo Drive
         *   |-- My Docs (unchanged)
         *   |     |-- user1Folder3
         *   |     |      |-- user1File3
         *   |     |-- user1Folder4
         *   |
         *   |-- Other Docs
         *
         * </pre>
         */
        resetPermissions(user2Folder3.getRef(), "user1");

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        sharedSyncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                sharedSyncRootParent.getId()).execute();

        sharedSyncRoots = mapper.readValue(sharedSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(sharedSyncRoots);
        assertEquals(0, sharedSyncRoots.size());
    }

    protected void checkFileItem(FileItem fileItem, String fileItemIdPrefix,
            DocumentModel doc, String parentId, String parentPath, String name,
            String creator) throws ClientException {

        String expectedFileItemId = fileItemIdPrefix + doc.getId();
        assertEquals(expectedFileItemId, fileItem.getId());
        assertEquals(parentId, fileItem.getParentId());
        assertEquals(parentPath + "/" + expectedFileItemId, fileItem.getPath());
        assertEquals(name, fileItem.getName());
        assertFalse(fileItem.isFolder());
        assertEquals(creator, fileItem.getCreator());
        assertEquals("nxbigfile/test/" + doc.getId() + "/blobholder:0/" + name,
                fileItem.getDownloadURL());
        assertEquals("md5", fileItem.getDigestAlgorithm());
        assertEquals(
                ((org.nuxeo.ecm.core.api.Blob) doc.getPropertyValue("file:content")).getDigest(),
                fileItem.getDigest());
    }

    protected void checkFolderItem(FolderItem folderItem,
            String folderItemIdPrefix, DocumentModel doc, String parentId,
            String parentPath, String name, String creator)
            throws ClientException {

        String expectedFolderItemId = folderItemIdPrefix + doc.getId();
        assertEquals(expectedFolderItemId, folderItem.getId());
        assertEquals(parentId, folderItem.getParentId());
        assertEquals(parentPath + "/" + expectedFolderItemId,
                folderItem.getPath());
        assertEquals(name, folderItem.getName());
        assertTrue(folderItem.isFolder());
        assertEquals(creator, folderItem.getCreator());
    }

    protected DocumentModel createFile(CoreSession session, String path,
            String name, String type, String fileName, String content)
            throws ClientException, InterruptedException {

        DocumentModel file = session.createDocumentModel(path, name, type);
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                content);
        blob.setFilename(fileName);
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);
        // If the test is run against MySQL or SQL Server, because of its
        // milliseconds limitation, we need to wait for 1 second between each
        // document creation to ensure correct ordering when fetching a folder's
        // children, the default page provider query being ordered by ascendant
        // creation date.
        waitIfMySQLOrSQLServer();
        return file;
    }

    protected DocumentModel createFolder(CoreSession session, String path,
            String name, String type) throws ClientException,
            InterruptedException {

        DocumentModel folder = session.createDocumentModel(path, name, type);
        folder = session.createDocument(folder);
        // If the test is run against MySQL or SQL Server, because of its
        // milliseconds limitation, we need to wait for 1 second between each
        // document creation to ensure correct ordering when fetching a folder's
        // children, the default page provider query being ordered by ascendant
        // creation date.
        waitIfMySQLOrSQLServer();
        return folder;
    }

    protected void createUser(String userName, String password)
            throws ClientException {
        org.nuxeo.ecm.directory.Session userDir = directoryService.getDirectory(
                "userDirectory").getSession();
        try {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("username", userName);
            user.put("password", password);
            userDir.createEntry(user);
        } finally {
            userDir.close();
        }
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

    protected void resetPermissions(DocumentRef docRef, String userName)
            throws ClientException {
        ACP acp = session.getACP(docRef);
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Iterator<ACE> localACLIt = localACL.iterator();
        while (localACLIt.hasNext()) {
            ACE ace = localACLIt.next();
            if (userName.equals(ace.getUsername())) {
                localACLIt.remove();
            }
        }
        session.setACP(docRef, acp, true);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected void waitIfMySQLOrSQLServer() throws InterruptedException {
        if (DatabaseHelper.DATABASE instanceof DatabaseMySQL
                || DatabaseHelper.DATABASE instanceof DatabaseSQLServer) {
            Thread.sleep(1000);
        }
    }

}
