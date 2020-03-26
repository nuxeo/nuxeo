/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.hierarchy.permission;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
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
import org.nuxeo.drive.operations.NuxeoDriveAutomationFeature;
import org.nuxeo.drive.operations.NuxeoDriveGetChildren;
import org.nuxeo.drive.operations.NuxeoDriveGetFileSystemItem;
import org.nuxeo.drive.operations.NuxeoDriveGetTopLevelFolder;
import org.nuxeo.drive.operations.NuxeoDriveScrollDescendants;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Tests the user workspace and permission based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@Deploy("org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml")
public class TestPermissionHierarchy {

    protected static final String BATCH_SIZE = "batchSize";

    protected static final String CONTENT_PREFIX = "The content of file ";

    protected static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    protected static final String FOLDER_TYPE = "Folder";

    protected static final String SHARED_SYNC_ROOT_PARENT_ID = "sharedSyncRootParentFactory#";

    protected static final String SYNC_ROOT_ID_PREFIX = "permissionSyncRootFactory#test#";

    protected static final String TOP_LEVEL_ID = "org.nuxeo.drive.hierarchy.permission.factory.PermissionTopLevelFactory#";

    protected static final String USER_1 = "user1";

    protected static final String USER_2 = "user2";

    protected static final String USER_SYNC_ROOT_PARENT_ID_PREFIX = "userSyncRootParentFactory#test#";

    protected static final TypeReference<List<DefaultSyncRootFolderItem>> LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM = new TypeReference<List<DefaultSyncRootFolderItem>>() {
    };

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

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

    protected DocumentModel user1Folder1;

    protected DocumentModel user1File1;

    protected DocumentModel user1Folder2;

    protected DocumentModel user1File2;

    protected DocumentModel user1Folder3;

    protected DocumentModel user1File3;

    protected DocumentModel user1Folder4;

    protected DocumentModel user2Folder1;

    protected DocumentModel user2File1;

    protected DocumentModel user2Folder2;

    protected DocumentModel user2File2;

    protected DocumentModel user2Folder3;

    protected DocumentModel user2File3;

    protected String userWorkspace1ItemId;

    protected String userWorkspace1ItemPath;

    protected HttpAutomationSession clientSession1;

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
     * </pre>
     */
    @Before
    public void init() throws IOException {

        // Create test users
        createUser(USER_1, USER_1);
        createUser(USER_2, USER_2);

        // Open a core session for each user
        session1 = coreFeature.getCoreSession(USER_1);
        session2 = coreFeature.getCoreSession(USER_2);

        // Create user workspace for each user
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(session1);
        userWorkspace2 = userWorkspaceService.getCurrentUserPersonalWorkspace(session2);

        userWorkspace1ItemId = USER_SYNC_ROOT_PARENT_ID_PREFIX + userWorkspace1.getId();
        userWorkspace1ItemPath = "/" + TOP_LEVEL_ID + "/" + userWorkspace1ItemId;

        // Populate user workspaces
        // user1
        user1Folder1 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder1", FOLDER_TYPE);
        user1File1 = createFile(session1, user1Folder1.getPathAsString(), "user1File1", "File", "user1File1.txt",
                CONTENT_PREFIX + "user1File1");
        user1Folder2 = createFolder(session1, user1Folder1.getPathAsString(), "user1Folder2", FOLDER_TYPE);
        user1File2 = createFile(session1, userWorkspace1.getPathAsString(), "user1File2", "File", "user1File2.txt",
                CONTENT_PREFIX + "user1File2");
        user1Folder3 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder3", FOLDER_TYPE);
        user1File3 = createFile(session1, user1Folder3.getPathAsString(), "user1File3", "File", "user1File3.txt",
                CONTENT_PREFIX + "user1File3");
        user1Folder4 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder4", FOLDER_TYPE);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // user2
        user2Folder1 = createFolder(session2, userWorkspace2.getPathAsString(), "user2Folder1", FOLDER_TYPE);
        user2File1 = createFile(session2, user2Folder1.getPathAsString(), "user2File1", "File", "user2File1.txt",
                CONTENT_PREFIX + "user2File1");
        user2Folder2 = createFolder(session2, user2Folder1.getPathAsString(), "user2Folder2", FOLDER_TYPE);
        user2File2 = createFile(session2, userWorkspace2.getPathAsString(), "user2File2", "File", "user2File2.txt",
                CONTENT_PREFIX + "user2File2");
        user2Folder3 = createFolder(session2, userWorkspace2.getPathAsString(), "user2Folder3", FOLDER_TYPE);
        user2File3 = createFile(session2, user2Folder3.getPathAsString(), "user2File3", "File", "user2File3.txt",
                CONTENT_PREFIX + "user2File3");
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        setPermission(user2Folder1, USER_1, SecurityConstants.READ_WRITE, true);
        setPermission(user2Folder3, USER_1, SecurityConstants.READ_WRITE, true);

        // Register shared folders as synchronization roots for user1
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user2Folder1.getRef()), session1);
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user2Folder3.getRef()), session1);

        // Get an Automation client session for user1
        clientSession1 = automationClient.getSession(USER_1, USER_1);

        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() {
        // Delete test users
        deleteUser(USER_1);
        deleteUser(USER_2);
    }

    @Test
    public void testClientSideUser1() throws IOException {

        // ---------------------------------------------
        // Check active factories
        // ---------------------------------------------
        TopLevelFolderItemFactory topLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertEquals("org.nuxeo.drive.hierarchy.permission.factory.PermissionTopLevelFactory",
                topLevelFolderItemFactory.getName());

        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(5, activeFactories.size());
        assertTrue(activeFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("defaultFileSystemItemFactory"));
        assertTrue(activeFactories.contains("userSyncRootParentFactory"));
        assertTrue(activeFactories.contains("permissionSyncRootFactory"));
        assertTrue(activeFactories.contains("sharedSyncRootParentFactory"));

        // ---------------------------------------------
        // Check top level folder: "Nuxeo Drive"
        // ---------------------------------------------
        PermissionTopLevelFolderItem topLevelFolder = clientSession1.newRequest(NuxeoDriveGetTopLevelFolder.ID)
                                                                    .executeReturning(
                                                                            PermissionTopLevelFolderItem.class);
        assertNotNull(topLevelFolder);
        assertEquals(TOP_LEVEL_ID, topLevelFolder.getId());
        assertNull(topLevelFolder.getParentId());
        assertEquals("/" + TOP_LEVEL_ID, topLevelFolder.getPath());
        assertEquals("Nuxeo Drive", topLevelFolder.getName());
        assertTrue(topLevelFolder.isFolder());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolder.getCreator());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolder.getLastContributor());
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
         *   |     |-- user1File2
         *   |     |-- user1Folder1
         *   |     |     |-- user1File1
         *   |     |     |-- user1Folder2
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
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(), userWorkspace1, session1);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ---------------------------------------------
        // Check top level folder children
        // ---------------------------------------------
        // Check descendants
        assertFalse(topLevelFolder.getCanScrollDescendants());
        String error = clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                                     .set("id", topLevelFolder.getId())
                                     .set(BATCH_SIZE, 10)
                                     .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);

        // Get children
        ArrayNode topLevelChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                   .set("id", topLevelFolder.getId())
                                                   .executeReturning(ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        // Check "My Docs"
        UserSyncRootParentFolderItem userSyncRootParent = readValue(topLevelChildren.get(0),
                UserSyncRootParentFolderItem.class);
        assertEquals(userWorkspace1ItemId, userSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, userSyncRootParent.getParentId());
        assertEquals(userWorkspace1ItemPath, userSyncRootParent.getPath());
        assertEquals("My Docs", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals(USER_1, userSyncRootParent.getCreator());
        assertEquals(USER_1, userSyncRootParent.getLastContributor());
        assertFalse(userSyncRootParent.getCanRename());
        assertFalse(userSyncRootParent.getCanDelete());
        // Can create a child since "My Docs" is the user workspace
        assertTrue(userSyncRootParent.getCanCreateChild());

        // Check "Other Docs"
        SharedSyncRootParentFolderItem sharedSyncRootParent = readValue(topLevelChildren.get(1),
                SharedSyncRootParentFolderItem.class);
        assertEquals(SHARED_SYNC_ROOT_PARENT_ID, sharedSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, sharedSyncRootParent.getParentId());
        assertEquals("/" + TOP_LEVEL_ID + "/" + SHARED_SYNC_ROOT_PARENT_ID, sharedSyncRootParent.getPath());
        assertEquals("Other Docs", sharedSyncRootParent.getName());
        assertTrue(sharedSyncRootParent.isFolder());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, sharedSyncRootParent.getCreator());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, sharedSyncRootParent.getLastContributor());
        assertFalse(sharedSyncRootParent.getCanRename());
        assertFalse(sharedSyncRootParent.getCanDelete());
        assertFalse(sharedSyncRootParent.getCanCreateChild());

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        // Check descendants
        assertTrue(userSyncRootParent.getCanScrollDescendants());
        assertTrue(CollectionUtils.isEqualCollection(
                Stream.of(user1File2, user1Folder1, user1File1, user1Folder2, user1Folder3, user1File3, user1Folder4)
                      .map(doc -> DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc.getId())
                      .collect(Collectors.toList()),
                clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                              .set("id", userSyncRootParent.getId())
                              .set(BATCH_SIZE, 10)
                              .execute()
                              .findValuesAsText("id")));

        // Get children
        ArrayNode userSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                .set("id", userSyncRootParent.getId())
                                                .executeReturning(ArrayNode.class);
        assertNotNull(userSyncRoots);
        assertEquals(4, userSyncRoots.size());

        DocumentBackedFolderItem folderItem;
        DocumentBackedFileItem childFileItem;
        DocumentBackedFileItem fileItem;
        DocumentBackedFolderItem childFolderItem;

        JsonNode[] rootNodes = sortNodeByName(userSyncRoots);

        // user1File2
        fileItem = readValue(rootNodes[0], DocumentBackedFileItem.class);
        checkFileItem(fileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File2, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1File2.txt", USER_1, USER_1);

        // user1Folder1
        folderItem = readValue(rootNodes[1], DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder1, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1Folder1", USER_1, USER_1);
        ArrayNode folderItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                     .set("id", folderItem.getId())
                                                     .executeReturning(ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(2, folderItemChildren.size());

        JsonNode[] nodes = sortNodeByName(folderItemChildren);

        // user1File1
        childFileItem = readValue(nodes[0], DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File1, folderItem.getId(),
                folderItem.getPath(), "user1File1.txt", USER_1, USER_1);

        // user1Folder2
        childFolderItem = readValue(nodes[1], DocumentBackedFolderItem.class);
        checkFolderItem(childFolderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder2, folderItem.getId(),
                folderItem.getPath(), "user1Folder2", USER_1, USER_1);

        // user1Folder3
        folderItem = readValue(rootNodes[2], DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder3, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1Folder3", USER_1, USER_1);

        folderItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                           .set("id", folderItem.getId())
                                           .executeReturning(ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(1, folderItemChildren.size());
        // user1File3
        childFileItem = readValue(folderItemChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File3, folderItem.getId(),
                folderItem.getPath(), "user1File3.txt", USER_1, USER_1);

        // user1Folder4
        folderItem = readValue(rootNodes[3], DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder4, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1Folder4", USER_1, USER_1);

        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        // Check descendants
        assertFalse(topLevelFolder.getCanScrollDescendants());
        error = clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                              .set("id", sharedSyncRootParent.getId())
                              .set(BATCH_SIZE, 10)
                              .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);

        // Get children
        List<DefaultSyncRootFolderItem> sharedSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                                        .set("id", sharedSyncRootParent.getId())
                                                                        .executeReturning(
                                                                                LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
        Collections.sort(sharedSyncRoots);

        assertNotNull(sharedSyncRoots);
        assertEquals(2, sharedSyncRoots.size());

        // user2Folder1
        DefaultSyncRootFolderItem sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX, session1.getDocument(user2Folder1.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(), "user2Folder1", USER_2, USER_1);
        ArrayNode sharedSyncRootChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                         .set("id", sharedSyncRoot.getId())
                                                         .executeReturning(ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(2, sharedSyncRootChildren.size());
        DocumentBackedFolderItem sharedSyncRootChildFolderItem;
        DocumentBackedFileItem sharedSyncRootChildFileItem;

        nodes = sortNodeByName(sharedSyncRootChildren);
        // user2File1
        sharedSyncRootChildFileItem = readValue(nodes[0], DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File1.getRef()), sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File1.txt", USER_2, USER_2);
        // user2Folder2
        sharedSyncRootChildFolderItem = readValue(nodes[1], DocumentBackedFolderItem.class);
        checkFolderItem(sharedSyncRootChildFolderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2Folder2.getRef()), sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2Folder2", USER_2, USER_2);

        // user2Folder3
        sharedSyncRoot = sharedSyncRoots.get(1);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX, session1.getDocument(user2Folder3.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(), "user2Folder3", USER_2, USER_1);
        sharedSyncRootChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                               .set("id", sharedSyncRoot.getId())
                                               .executeReturning(ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        // user2File3
        sharedSyncRootChildFileItem = readValue(sharedSyncRootChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File3.getRef()), sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File3.txt", USER_2, USER_2);

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
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.unregisterSynchronizationRoot(session1.getPrincipal(), userWorkspace1, session1);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // ---------------------------------------------
        // Check "My Docs"
        // ---------------------------------------------
        userSyncRootParent = clientSession1.newRequest(NuxeoDriveGetFileSystemItem.ID)
                                           .set("id", userWorkspace1ItemId)
                                           .executeReturning(UserSyncRootParentFolderItem.class);
        assertEquals(userWorkspace1ItemId, userSyncRootParent.getId());
        assertEquals(TOP_LEVEL_ID, userSyncRootParent.getParentId());
        assertEquals(userWorkspace1ItemPath, userSyncRootParent.getPath());
        assertEquals("My Docs", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals(USER_1, userSyncRootParent.getCreator());
        assertEquals(USER_1, userSyncRootParent.getLastContributor());
        assertFalse(userSyncRootParent.getCanRename());
        assertFalse(userSyncRootParent.getCanDelete());
        // Cannot create a child since "My Docs" is only the parent of the
        // synchronization roots, not the user workspace
        assertFalse(userSyncRootParent.getCanCreateChild());

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        // Check descendants
        assertFalse(topLevelFolder.getCanScrollDescendants());
        error = clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                              .set("id", userSyncRootParent.getId())
                              .set(BATCH_SIZE, 10)
                              .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);

        // Get children
        userSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                      .set("id", userSyncRootParent.getId())
                                      .executeReturning(ArrayNode.class);
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
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(), user1Folder3, session1);
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(), user1Folder4, session1);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        userSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                      .set("id", userSyncRootParent.getId())
                                      .executeReturning(ArrayNode.class);
        assertNotNull(userSyncRoots);
        assertEquals(2, userSyncRoots.size());

        // user1Folder3
        folderItem = readValue(userSyncRoots.get(0), DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, SYNC_ROOT_ID_PREFIX, user1Folder3, userWorkspace1ItemId, userWorkspace1ItemPath,
                "user1Folder3", USER_1, USER_1);
        folderItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                           .set("id", folderItem.getId())
                                           .executeReturning(ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(1, folderItemChildren.size());
        // user1File3
        childFileItem = readValue(folderItemChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File3, folderItem.getId(),
                folderItem.getPath(), "user1File3.txt", USER_1, USER_1);
        // user1Folder4
        folderItem = readValue(userSyncRoots.get(1), DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, SYNC_ROOT_ID_PREFIX, user1Folder4, userWorkspace1ItemId, userWorkspace1ItemPath,
                "user1Folder4", USER_1, USER_1);

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
         * </pre>
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        nuxeoDriveManager.unregisterSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user2Folder1.getRef()), session1);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        sharedSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                        .set("id", sharedSyncRootParent.getId())
                                        .executeReturning(LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
        assertNotNull(sharedSyncRoots);
        assertEquals(1, sharedSyncRoots.size());

        // user2Folder3
        sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX, session1.getDocument(user2Folder3.getRef()),
                sharedSyncRootParent.getId(), sharedSyncRootParent.getPath(), "user2Folder3", USER_2, USER_1);
        sharedSyncRootChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                               .set("id", sharedSyncRoot.getId())
                                               .executeReturning(ArrayNode.class);
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        // user2File3
        sharedSyncRootChildFileItem = readValue(sharedSyncRootChildren.get(0), DocumentBackedFileItem.class);
        checkFileItem(sharedSyncRootChildFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File3.getRef()), sharedSyncRoot.getId(), sharedSyncRoot.getPath(),
                "user2File3.txt", USER_2, USER_2);

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
         * </pre>
         */
        resetPermissions(user2Folder3.getRef(), USER_1);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        sharedSyncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                        .set("id", sharedSyncRootParent.getId())
                                        .executeReturning(LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
        assertNotNull(sharedSyncRoots);
        assertEquals(0, sharedSyncRoots.size());
    }

    protected <T> T readValue(JsonNode node, Class<T> klass) throws IOException {
        try (JsonParser tokens = mapper.treeAsTokens(node)) {
            return mapper.readValue(tokens, klass);
        }
    }

    protected JsonNode[] sortNodeByName(ArrayNode array) {
        JsonNode[] nodes = new JsonNode[array.size()];
        for (int i = 0; i < array.size(); ++i) {
            nodes[i] = array.get(i);
        }
        Arrays.sort(nodes, Comparator.comparing(node -> node.get("name").asText()));
        return nodes;
    }

    protected void checkFileItem(FileItem fileItem, String fileItemIdPrefix, DocumentModel doc, String parentId,
            String parentPath, String name, String creator, String lastContributor) {

        String expectedFileItemId = fileItemIdPrefix + doc.getId();
        assertEquals(expectedFileItemId, fileItem.getId());
        assertEquals(parentId, fileItem.getParentId());
        assertEquals(parentPath + "/" + expectedFileItemId, fileItem.getPath());
        assertEquals(name, fileItem.getName());
        assertFalse(fileItem.isFolder());
        assertEquals(creator, fileItem.getCreator());
        assertEquals(lastContributor, fileItem.getLastContributor());
        assertEquals("nxfile/test/" + doc.getId() + "/blobholder:0/" + name, fileItem.getDownloadURL());
        assertEquals("MD5", fileItem.getDigestAlgorithm());
        assertEquals(((org.nuxeo.ecm.core.api.Blob) doc.getPropertyValue("file:content")).getDigest(),
                fileItem.getDigest());
        assertEquals(((org.nuxeo.ecm.core.api.Blob) doc.getPropertyValue("file:content")).getLength(),
                fileItem.getSize());
    }

    protected void checkFolderItem(FolderItem folderItem, String folderItemIdPrefix, DocumentModel doc, String parentId,
            String parentPath, String name, String creator, String lastContributor) {

        String expectedFolderItemId = folderItemIdPrefix + doc.getId();
        assertEquals(expectedFolderItemId, folderItem.getId());
        assertEquals(parentId, folderItem.getParentId());
        assertEquals(parentPath + "/" + expectedFolderItemId, folderItem.getPath());
        assertEquals(name, folderItem.getName());
        assertTrue(folderItem.isFolder());
        assertEquals(creator, folderItem.getCreator());
        assertEquals(lastContributor, folderItem.getLastContributor());
    }

    protected void checkFolderItemChildren(FolderItem folderItem, ArrayNode folderItemChildren) throws IOException {
        JsonNode[] nodes = sortNodeByName(folderItemChildren);

        // user1File1
        DocumentBackedFileItem childFileItem = readValue(nodes[0], DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File1, folderItem.getId(),
                folderItem.getPath(), "user1File1.txt", USER_1, USER_1);

        // user1Folder2
        DocumentBackedFolderItem childFolderItem = readValue(nodes[1], DocumentBackedFolderItem.class);
        checkFolderItem(childFolderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder2, folderItem.getId(),
                folderItem.getPath(), "user1Folder2", USER_1, USER_1);
    }

    protected DocumentModel createFile(CoreSession session, String path, String name, String type, String fileName,
            String content) {

        DocumentModel file = session.createDocumentModel(path, name, type);
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(content);
        blob.setFilename(fileName);
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);
        return file;
    }

    protected DocumentModel createFolder(CoreSession session, String path, String name, String type) {

        DocumentModel folder = session.createDocumentModel(path, name, type);
        folder = session.createDocument(folder);
        return folder;
    }

    protected void createUser(String userName, String password) {
        try (org.nuxeo.ecm.directory.Session userDir = directoryService.open("userDirectory")) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", userName);
            user.put("password", password);
            userDir.createEntry(user);
        }
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

    protected void resetPermissions(DocumentRef docRef, String userName) {
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

}
