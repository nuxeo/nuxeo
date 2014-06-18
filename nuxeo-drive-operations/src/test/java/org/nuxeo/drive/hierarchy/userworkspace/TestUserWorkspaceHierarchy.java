/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.hierarchy.userworkspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
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
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceSyncRootParentFolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceTopLevelFolderItem;
import org.nuxeo.drive.operations.NuxeoDriveGetChildren;
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
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the user workspace based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations",
        "org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-userworkspace-contrib.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestUserWorkspaceHierarchy {

    private static final String TOP_LEVEL_ID_PREFIX = "org.nuxeo.drive.hierarchy.userworkspace.factory.UserWorkspaceTopLevelFactory#test#";

    private static final String SYNC_ROOT_PARENT_ID = "userWorkspaceSyncRootParentFactory#";

    private static final String SYNC_ROOT_ID_PREFIX = "userWorkspaceSyncRootFactory#test#";

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

    protected DocumentModel userWorkspace1;

    protected DocumentModel user1Folder1, user1File1, user1Folder2, user1File2,
            user1Folder3, user1File3, user1Folder4, user1File4;

    protected String userWorkspace1ItemId;

    protected String userWorkspace1ItemPath;

    protected String syncRootParentItemPath;

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
     * ...
     * / (root document)
     *   |-- user1Folder3       (synchronization root)
     *   |     |-- user1File3
     *   |-- user1Folder4       (synchronization root)
     *   |     |-- user1File4
     *
     * </pre>
     */
    @Before
    public void init() throws Exception {

        // Create test user
        createUser("user1", "user1");

        // Grant ReadWrite permission to test user on the root document
        setPermission(session.getRootDocument(), "user1",
                SecurityConstants.READ_WRITE, true);

        // Open a core session for test user
        session1 = repository.openSessionAs("user1");

        // Create user workspace for test user
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session1, null);

        userWorkspace1ItemId = TOP_LEVEL_ID_PREFIX + userWorkspace1.getId();
        userWorkspace1ItemPath = "/" + userWorkspace1ItemId;
        syncRootParentItemPath = userWorkspace1ItemPath + "/"
                + SYNC_ROOT_PARENT_ID;

        // Populate test user workspace
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
        user1Folder3 = createFolder(session1, "/", "user1Folder3", "Folder");
        user1File3 = createFile(session1, user1Folder3.getPathAsString(),
                "user1File3", "File", "user1File3.txt", CONTENT_PREFIX
                        + "user1File3");
        user1Folder4 = createFolder(session1, "/", "user1Folder4", "Folder");
        user1File4 = createFile(session1, user1Folder4.getPathAsString(),
                "user1File4", "File", "user1File4.txt", CONTENT_PREFIX
                        + "user1File4");
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
        clientSession1 = automationClient.getSession("user1", "user1");

        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws ClientException {
        // Close test user core session
        session1.close();

        // Delete test user
        deleteUser("user1");
    }

    /**
     * <pre>
     * Expected client side for user1
     * ==============================
     *
     * Nuxeo Drive
     *   |-- user1Folder1
     *   |     |-- user1File1
     *   |     |-- user1Folder2
     *   |-- user1File2
     *   |-- My synchronized folders
     *   |     |-- user1Folder3
     *   |     |     |-- user1File3
     *   |     |-- user1Folder4
     *   |     |     |-- user1File4
     *
     * </pre>
     */
    @Test
    public void testClientSideUser1() throws Exception {

        // ---------------------------------------------
        // Check active factories
        // ---------------------------------------------
        TopLevelFolderItemFactory topLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertEquals(
                "org.nuxeo.drive.hierarchy.userworkspace.factory.UserWorkspaceTopLevelFactory",
                topLevelFolderItemFactory.getName());

        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(3, activeFactories.size());
        assertTrue(activeFactories.contains("defaultFileSystemItemFactory"));
        assertTrue(activeFactories.contains("userWorkspaceSyncRootParentFactory"));
        assertTrue(activeFactories.contains("userWorkspaceSyncRootFactory"));

        // ---------------------------------------------
        // Check top level folder: "Nuxeo Drive"
        // ---------------------------------------------
        Blob topLevelFolderJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetTopLevelFolder.ID).execute();
        assertNotNull(topLevelFolderJSON);

        UserWorkspaceTopLevelFolderItem topLevelFolder = mapper.readValue(
                topLevelFolderJSON.getStream(),
                UserWorkspaceTopLevelFolderItem.class);
        assertNotNull(topLevelFolder);
        assertEquals(userWorkspace1ItemId, topLevelFolder.getId());
        assertNull(topLevelFolder.getParentId());
        assertEquals(userWorkspace1ItemPath, topLevelFolder.getPath());
        assertEquals("Nuxeo Drive", topLevelFolder.getName());
        assertTrue(topLevelFolder.isFolder());
        assertEquals("user1", topLevelFolder.getCreator());
        assertFalse(topLevelFolder.getCanRename());
        assertFalse(topLevelFolder.getCanDelete());
        assertTrue(topLevelFolder.getCanCreateChild());

        Blob topLevelChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", topLevelFolder.getId()).execute();

        ArrayNode topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(3, topLevelChildren.size());

        // ---------------------------------------------
        // Check user workspace children
        // ---------------------------------------------
        // user1Folder1
        DocumentBackedFolderItem folderItem = mapper.readValue(
                topLevelChildren.get(0), DocumentBackedFolderItem.class);
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
                topLevelChildren.get(1), DocumentBackedFileItem.class);
        checkFileItem(fileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File2,
                userWorkspace1ItemId, userWorkspace1ItemPath, "user1File2.txt",
                "user1");

        // ---------------------------------------------
        // Check synchronization roots
        // ---------------------------------------------
        // My synchronized folders
        UserWorkspaceSyncRootParentFolderItem syncRootParent = mapper.readValue(
                topLevelChildren.get(2),
                UserWorkspaceSyncRootParentFolderItem.class);
        assertEquals(SYNC_ROOT_PARENT_ID, syncRootParent.getId());
        assertEquals(userWorkspace1ItemId, syncRootParent.getParentId());
        assertEquals("/" + userWorkspace1ItemId + "/" + SYNC_ROOT_PARENT_ID,
                syncRootParent.getPath());
        assertEquals("My synchronized folders", syncRootParent.getName());
        assertTrue(syncRootParent.isFolder());
        assertEquals("system", syncRootParent.getCreator());
        assertFalse(syncRootParent.getCanRename());
        assertFalse(syncRootParent.getCanDelete());
        assertFalse(syncRootParent.getCanCreateChild());

        Blob syncRootsJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRootParent.getId()).execute();

        List<DefaultSyncRootFolderItem> syncRoots = mapper.readValue(
                syncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(syncRoots);
        assertEquals(2, syncRoots.size());

        // user1Folder3
        DefaultSyncRootFolderItem syncRootItem = syncRoots.get(0);
        checkFolderItem(syncRootItem, SYNC_ROOT_ID_PREFIX, user1Folder3,
                SYNC_ROOT_PARENT_ID, syncRootParentItemPath, "user1Folder3",
                "user1");
        Blob syncRootItemChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRootItem.getId()).execute();
        List<DocumentBackedFileItem> syncRootItemChildren = mapper.readValue(
                syncRootItemChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(syncRootItemChildren);
        assertEquals(1, syncRootItemChildren.size());
        // user1File3
        childFileItem = syncRootItemChildren.get(0);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1File3, syncRootItem.getId(), syncRootItem.getPath(),
                "user1File3.txt", "user1");
        // user1Folder4
        syncRootItem = syncRoots.get(1);
        checkFolderItem(syncRootItem, SYNC_ROOT_ID_PREFIX, user1Folder4,
                SYNC_ROOT_PARENT_ID, syncRootParentItemPath, "user1Folder4",
                "user1");
        syncRootItemChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRootItem.getId()).execute();
        syncRootItemChildren = mapper.readValue(
                syncRootItemChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(syncRootItemChildren);
        assertEquals(1, syncRootItemChildren.size());
        // user1File4
        childFileItem = syncRootItemChildren.get(0);
        checkFileItem(childFileItem, DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                user1File4, syncRootItem.getId(), syncRootItem.getPath(),
                "user1File4.txt", "user1");

        // ---------------------------------------------
        // Check registering user workspace as a
        // synchronization root is ignored
        // ---------------------------------------------
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.registerSynchronizationRoot(
                    session1.getPrincipal(), userWorkspace1, session1);
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            syncRootsJSON = (Blob) clientSession1.newRequest(
                    NuxeoDriveGetChildren.ID).set("id", syncRootParent.getId()).execute();
            syncRoots = mapper.readValue(syncRootsJSON.getStream(),
                    new TypeReference<List<DefaultSyncRootFolderItem>>() {
                    });
            assertEquals(2, syncRoots.size());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
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
        session.save();
    }

    protected void waitIfMySQLOrSQLServer() throws InterruptedException {
        if (DatabaseHelper.DATABASE instanceof DatabaseMySQL
                || DatabaseHelper.DATABASE instanceof DatabaseSQLServer) {
            Thread.sleep(1000);
        }
    }

}
