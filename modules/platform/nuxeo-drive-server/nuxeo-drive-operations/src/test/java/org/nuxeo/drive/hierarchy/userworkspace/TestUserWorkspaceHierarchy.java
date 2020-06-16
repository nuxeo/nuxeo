/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.hierarchy.userworkspace;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.nuxeo.ecm.core.test.StorageConfiguration.CORE_MONGODB;
import static org.nuxeo.ecm.core.test.StorageConfiguration.CORE_PROPERTY;

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

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceSyncRootParentFolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceTopLevelFolderItem;
import org.nuxeo.drive.operations.NuxeoDriveAutomationFeature;
import org.nuxeo.drive.operations.NuxeoDriveGetChildren;
import org.nuxeo.drive.operations.NuxeoDriveGetTopLevelFolder;
import org.nuxeo.drive.operations.NuxeoDriveScrollDescendants;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
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
 * Tests the user workspace based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@Deploy("org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-userworkspace-contrib.xml")
public class TestUserWorkspaceHierarchy {

    protected static final String CONTENT_PREFIX = "The content of file ";

    protected static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    protected static final String FILE_TYPE = "File";

    protected static final String FOLDER_TYPE = "Folder";

    protected static final String FILE_CONTENT_PROPERTY = "file:content";

    protected static final String SYNC_ROOT_ID_PREFIX = "userWorkspaceSyncRootFactory#test#";

    protected static final String SYNC_ROOT_PARENT_ID = "userWorkspaceSyncRootParentFactory#";

    protected static final String TOP_LEVEL_ID_PREFIX = "org.nuxeo.drive.hierarchy.userworkspace.factory.UserWorkspaceTopLevelFactory#test#";

    protected static final String USER_1 = "user1";

    protected static final TypeReference<List<DefaultSyncRootFolderItem>> LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM = new TypeReference<List<DefaultSyncRootFolderItem>>() {
    };

    protected static final TypeReference<List<DocumentBackedFileItem>> LIST_DOCUMENT_BACKED_FILE_ITEM = new TypeReference<List<DocumentBackedFileItem>>() {
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

    protected DocumentModel userWorkspace1;

    protected DocumentModel user1Folder1;

    protected DocumentModel user1File1;

    protected DocumentModel user1Folder2;

    protected DocumentModel user1File2;

    protected DocumentModel user1Folder3;

    protected DocumentModel user1File3;

    protected DocumentModel user1Folder4;

    protected DocumentModel user1File4;

    protected String userWorkspace1ItemId;

    protected String userWorkspace1ItemPath;

    protected String syncRootParentItemPath;

    protected HttpAutomationSession clientSession1;

    protected ObjectMapper mapper;

    @BeforeClass
    public static void checkBackend() {
        // NXP-29001: temporarily ignore against MongoDB
        assumeFalse(CORE_MONGODB.equals(System.getProperty(CORE_PROPERTY)));

        // NXP-15969: temporarily ignore under MySQL
        assumeFalse(DatabaseHelper.DATABASE instanceof DatabaseMySQL);
    }

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
     * ...
     * / (root document)
     *   |-- user1Folder3       (synchronization root)
     *   |     |-- user1File3
     *   |-- user1Folder4       (synchronization root)
     *   |     |-- user1File4
     * </pre>
     */
    @Before
    public void init() throws IOException {
        // Create test user
        createUser(USER_1, USER_1);

        // Grant ReadWrite permission to test user on the root document
        setPermission(session.getRootDocument(), USER_1, SecurityConstants.READ_WRITE, true);

        // Open a core session for test user
        session1 = coreFeature.getCoreSession(USER_1);

        // Create user workspace for test user
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(session1);

        userWorkspace1ItemId = TOP_LEVEL_ID_PREFIX + userWorkspace1.getId();
        userWorkspace1ItemPath = "/" + userWorkspace1ItemId; // NOSONAR
        syncRootParentItemPath = userWorkspace1ItemPath + "/" + SYNC_ROOT_PARENT_ID; // NOSONAR

        // Populate test user workspace
        user1Folder1 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder1", FOLDER_TYPE);
        user1File1 = createFile(session1, user1Folder1.getPathAsString(), "user1File1", FILE_TYPE, "user1File1.txt",
                CONTENT_PREFIX + "user1File1");
        user1Folder2 = createFolder(session1, user1Folder1.getPathAsString(), "user1Folder2", FOLDER_TYPE);
        user1File2 = createFile(session1, userWorkspace1.getPathAsString(), "user1File2", FILE_TYPE, "user1File2.txt",
                CONTENT_PREFIX + "user1File2");
        user1Folder3 = createFolder(session1, "/", "user1Folder3", FOLDER_TYPE);
        user1File3 = createFile(session1, user1Folder3.getPathAsString(), "user1File3", FILE_TYPE, "user1File3.txt",
                CONTENT_PREFIX + "user1File3");
        user1Folder4 = createFolder(session1, "/", "user1Folder4", FOLDER_TYPE);
        user1File4 = createFile(session1, user1Folder4.getPathAsString(), "user1File4", FILE_TYPE, "user1File4.txt",
                CONTENT_PREFIX + "user1File4");
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Register synchronization roots for user1
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user1Folder3.getRef()), session1);
        nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user1Folder4.getRef()), session1);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Get an Automation client session for the test user
        clientSession1 = automationClient.getSession(USER_1, USER_1);

        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() {
        // Unregister synchronization roots for user1
        nuxeoDriveManager.unregisterSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user1Folder4.getRef()), session1);
        nuxeoDriveManager.unregisterSynchronizationRoot(session1.getPrincipal(),
                session1.getDocument(user1Folder3.getRef()), session1);

        // Delete test user workspace
        session.removeDocument(userWorkspace1.getRef());

        // Reset test user permissions on the root document
        resetPermissions(session.getRootDocument(), USER_1);

        // Delete test user
        deleteUser(USER_1);
    }

    /**
     * <pre>
     * Expected client side for user1
     * ==============================
     *
     * Nuxeo Drive
     *   |-- My synchronized folders
     *   |     |-- user1Folder3
     *   |     |     |-- user1File3
     *   |     |-- user1Folder4
     *   |     |     |-- user1File4
     *   |-- user1File2
     *   |-- user1Folder1
     *   |     |-- user1File1
     *   |     |-- user1Folder2
     * </pre>
     */
    @Test
    public void testClientSideUser1() throws IOException {
        // ---------------------------------------------
        // Check active factories
        // ---------------------------------------------
        TopLevelFolderItemFactory topLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertEquals("org.nuxeo.drive.hierarchy.userworkspace.factory.UserWorkspaceTopLevelFactory",
                topLevelFolderItemFactory.getName());

        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(4, activeFactories.size());
        assertTrue(activeFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("defaultFileSystemItemFactory"));
        assertTrue(activeFactories.contains("userWorkspaceSyncRootParentFactory"));
        assertTrue(activeFactories.contains("userWorkspaceSyncRootFactory"));

        // ---------------------------------------------
        // Check top level folder: "Nuxeo Drive"
        // ---------------------------------------------
        UserWorkspaceTopLevelFolderItem topLevelFolder = clientSession1.newRequest(NuxeoDriveGetTopLevelFolder.ID) //
                                                                       .executeReturning(
                                                                               UserWorkspaceTopLevelFolderItem.class);
        assertNotNull(topLevelFolder);
        assertEquals(userWorkspace1ItemId, topLevelFolder.getId());
        assertNull(topLevelFolder.getParentId());
        assertEquals(userWorkspace1ItemPath, topLevelFolder.getPath());
        assertEquals("Nuxeo Drive", topLevelFolder.getName());
        assertTrue(topLevelFolder.isFolder());
        assertEquals(USER_1, topLevelFolder.getCreator());
        assertEquals(USER_1, topLevelFolder.getLastContributor());
        assertFalse(topLevelFolder.getCanRename());
        assertFalse(topLevelFolder.getCanDelete());
        assertTrue(topLevelFolder.getCanCreateChild());

        // Check descendants
        assertFalse(topLevelFolder.getCanScrollDescendants());
        String error = clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                                     .set("id", topLevelFolder.getId())
                                     .set("batchSize", 10)
                                     .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);

        // Get children
        ArrayNode topLevelChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                   .set("id", topLevelFolder.getId())
                                                   .executeReturning(ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(3, topLevelChildren.size());

        JsonNode[] topLevelChildrenNodes = sortNodeByName(topLevelChildren);

        // ---------------------------------------------
        // Check synchronization roots
        // ---------------------------------------------
        // My synchronized folders
        UserWorkspaceSyncRootParentFolderItem syncRootParent = readValue(topLevelChildrenNodes[0],
                UserWorkspaceSyncRootParentFolderItem.class);
        assertEquals(SYNC_ROOT_PARENT_ID, syncRootParent.getId());
        assertEquals(userWorkspace1ItemId, syncRootParent.getParentId());
        assertEquals("/" + userWorkspace1ItemId + "/" + SYNC_ROOT_PARENT_ID, syncRootParent.getPath());
        assertEquals("My synchronized folders", syncRootParent.getName());
        assertTrue(syncRootParent.isFolder());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, syncRootParent.getCreator());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, syncRootParent.getLastContributor());
        assertFalse(syncRootParent.getCanRename());
        assertFalse(syncRootParent.getCanDelete());
        assertFalse(syncRootParent.getCanCreateChild());

        // Check descendants
        assertFalse(syncRootParent.getCanScrollDescendants());
        error = clientSession1.newRequest(NuxeoDriveScrollDescendants.ID)
                              .set("id", syncRootParent.getId())
                              .set("batchSize", 10)
                              .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: NuxeoDrive.ScrollDescendants", error);

        // Get children
        List<DefaultSyncRootFolderItem> syncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                                  .set("id", syncRootParent.getId())
                                                                  .executeReturning(LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
        assertNotNull(syncRoots);
        assertEquals(2, syncRoots.size());
        Collections.sort(syncRoots);

        // user1Folder3
        DefaultSyncRootFolderItem syncRootItem = syncRoots.get(0);
        checkFolderItem(syncRootItem, SYNC_ROOT_ID_PREFIX, user1Folder3, SYNC_ROOT_PARENT_ID, syncRootParentItemPath,
                "user1Folder3", USER_1, USER_1);
        List<DocumentBackedFileItem> syncRootItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                                          .set("id", syncRootItem.getId())
                                                                          .executeReturning(
                                                                                  LIST_DOCUMENT_BACKED_FILE_ITEM);
        assertNotNull(syncRootItemChildren);
        assertEquals(1, syncRootItemChildren.size());
        // user1File3
        DocumentBackedFileItem childFileItem = syncRootItemChildren.get(0);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File3, syncRootItem.getId(),
                syncRootItem.getPath(), "user1File3.txt", USER_1, USER_1);
        // user1Folder4
        syncRootItem = syncRoots.get(1);
        checkFolderItem(syncRootItem, SYNC_ROOT_ID_PREFIX, user1Folder4, SYNC_ROOT_PARENT_ID, syncRootParentItemPath,
                "user1Folder4", USER_1, USER_1);
        syncRootItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                             .set("id", syncRootItem.getId())
                                             .executeReturning(LIST_DOCUMENT_BACKED_FILE_ITEM);
        assertNotNull(syncRootItemChildren);
        assertEquals(1, syncRootItemChildren.size());
        // user1File4
        childFileItem = syncRootItemChildren.get(0);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File4, syncRootItem.getId(),
                syncRootItem.getPath(), "user1File4.txt", USER_1, USER_1);

        // ---------------------------------------------
        // Check user workspace children
        // ---------------------------------------------
        // user1File2
        DocumentBackedFileItem fileItem = readValue(topLevelChildrenNodes[1], DocumentBackedFileItem.class);
        checkFileItem(fileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File2, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1File2.txt", USER_1, USER_1);
        // user1Folder1
        DocumentBackedFolderItem folderItem = readValue(topLevelChildrenNodes[2], DocumentBackedFolderItem.class);
        checkFolderItem(folderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder1, userWorkspace1ItemId,
                userWorkspace1ItemPath, "user1Folder1", USER_1, USER_1);
        ArrayNode folderItemChildren = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                                     .set("id", folderItem.getId())
                                                     .executeReturning(ArrayNode.class);
        assertNotNull(folderItemChildren);
        assertEquals(2, folderItemChildren.size());

        JsonNode[] folderItemChildrenNodes = sortNodeByName(folderItemChildren);
        // user1File1
        childFileItem = readValue(folderItemChildrenNodes[0], DocumentBackedFileItem.class);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File1, folderItem.getId(),
                folderItem.getPath(), "user1File1.txt", USER_1, USER_1);
        // user1Folder2
        DocumentBackedFolderItem childFolderItem = readValue(folderItemChildrenNodes[1],
                DocumentBackedFolderItem.class);
        checkFolderItem(childFolderItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1Folder2, folderItem.getId(),
                folderItem.getPath(), "user1Folder2", USER_1, USER_1);

        // ---------------------------------------------
        // Check registering user workspace as a
        // synchronization root is ignored
        // ---------------------------------------------
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(), userWorkspace1, session1);
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            syncRoots = clientSession1.newRequest(NuxeoDriveGetChildren.ID)
                                      .set("id", syncRootParent.getId())
                                      .executeReturning(LIST_DEFAULT_SYNC_ROOT_FOLDER_ITEM);
            assertEquals(2, syncRoots.size());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
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
        assertEquals(((org.nuxeo.ecm.core.api.Blob) doc.getPropertyValue(FILE_CONTENT_PROPERTY)).getDigest(),
                fileItem.getDigest());
        assertEquals(((org.nuxeo.ecm.core.api.Blob) doc.getPropertyValue(FILE_CONTENT_PROPERTY)).getLength(),
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

    protected DocumentModel createFile(CoreSession session, String path, String name, String type, String fileName,
            String content) {

        DocumentModel file = session.createDocumentModel(path, name, type);
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(content);
        blob.setFilename(fileName);
        file.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
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

}
