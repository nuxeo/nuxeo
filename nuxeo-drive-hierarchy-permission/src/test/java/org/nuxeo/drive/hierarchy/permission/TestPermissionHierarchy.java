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
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionTopLevelFolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.SharedSyncRootParentFolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.UserSyncRootParentFolderItem;
import org.nuxeo.drive.operations.NuxeoDriveGetChildren;
import org.nuxeo.drive.operations.NuxeoDriveGetTopLevelFolder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the permission based hierarchy.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations",
        "org.nuxeo.drive.hierarchy.permission" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestPermissionHierarchy {

    private static final String TOP_LEVEL_ID_SUFFIX = "PermissionTopLevelFactory#";

    private static final String USER_SYNC_ROOT_PARENT_ID = "userSyncRootParentFactory#";

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
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected CoreSession session1;

    protected CoreSession session2;

    protected DocumentModel user1Workspace1;

    protected DocumentModel user2Workspace1;

    protected DocumentModel user1Folder1, user1File1, user1File2, user1Folder2,
            user1Folder3, user1File3, user2Folder1, user2File1, user2File2,
            user2Folder2, user2Folder3, user2File3;

    protected Session clientSession1;

    protected Session clientSession2;

    protected ObjectMapper mapper;

    /**
     * Initializes the test hierarchy.
     *
     * <pre>
     * Server side for user1
     * ==============================
     *
     * /user1Workspace1
     *   |-- user1Folder1       (registered as a synchronization root for user 1)
     *   |     |-- user1File1
     *   |     |-- user1File2
     *   |-- user1Folder2       (registered as a synchronization root for user 1)
     *   |-- user1Folder3       (registered as a synchronization root with ReadWrite permission for user2)
     *   |     |-- user1File3
     *
     * Server side for user2
     * ==============================
     *
     * /user2Workspace1
     *   |-- user2Folder1       (registered as a synchronization root for user 2)
     *   |     |-- user2File1
     *   |     |-- user2File2
     *   |-- user2Folder2
     *   |-- user2Folder3       (registered as a synchronization root for user1 and user2 and with ReadWrite permission for user1)
     *   |     |-- user2File3
     *
     * Expected client side for user1
     * ==============================
     *
     * Nuxeo Drive
     *   |-- My Documents
     *   |     |-- user1Folder1
     *   |     |     |-- user1File1
     *   |     |     |-- user1File2
     *   |     |-- user1Folder2
     *   |
     *   |-- Other Documents
     *   |     |-- user2Folder3
     *   |     |     |-- user2File3
     *
     * Expected client side for user2
     * ==============================
     *
     * Nuxeo Drive
     *   |-- My Documents
     *   |     |-- user2Folder1
     *   |     |     |-- user2File1
     *   |     |     |-- user2File2
     *   |     |-- user2Folder3
     *   |     |     |-- user2File3
     *   |
     *   |-- Other Documents
     *   |     |-- user1Folder3
     *   |     |     |-- user1File3
     * </pre>
     */
    @Before
    public void init() throws Exception {

        // Create test users
        createUser("user1", "user1");
        createUser("user2", "user2");

        // Grant ReadWrite permission on root document for the test users
        setPermission(session.getRootDocument(), "user1",
                SecurityConstants.READ_WRITE, true);
        setPermission(session.getRootDocument(), "user2",
                SecurityConstants.READ_WRITE, true);

        // Open a core session for each user
        session1 = repository.openSessionAs("user1");
        session2 = repository.openSessionAs("user2");

        // Create document hierarchy
        // user1
        user1Workspace1 = createFolder(session1, "/", "user1Workspace1",
                "Workspace");
        user1Folder1 = createFolder(session1,
                user1Workspace1.getPathAsString(), "user1Folder1", "Folder");
        user1File1 = createFile(session1, user1Folder1.getPathAsString(),
                "user1File1", "File", "user1File1.txt", CONTENT_PREFIX
                        + "user1File1");
        user1File2 = createFile(session1, user1Folder1.getPathAsString(),
                "user1File2", "File", "user1File2.txt", CONTENT_PREFIX
                        + "user1File2");
        user1Folder2 = createFolder(session1,
                user1Workspace1.getPathAsString(), "user1Folder2", "Folder");
        user1Folder3 = createFolder(session1,
                user1Workspace1.getPathAsString(), "user1Folder3", "Folder");
        user1File3 = createFile(session1, user1Folder3.getPathAsString(),
                "user1File3", "File", "user1File3.txt", CONTENT_PREFIX
                        + "user1File3");
        session1.save();
        setPermission(user1Folder3, "user2", SecurityConstants.READ_WRITE, true);
        // user2
        user2Workspace1 = createFolder(session2, "/", "user2Workspace1",
                "Workspace");
        user2Folder1 = createFolder(session2,
                user2Workspace1.getPathAsString(), "user2Folder1", "Folder");
        user2File1 = createFile(session2, user2Folder1.getPathAsString(),
                "user2File1", "File", "user2File1.txt", CONTENT_PREFIX
                        + "user2File1");
        user2File2 = createFile(session2, user2Folder1.getPathAsString(),
                "user2File2", "File", "user2File2.txt", CONTENT_PREFIX
                        + "user2File2");
        user2Folder2 = createFolder(session2,
                user2Workspace1.getPathAsString(), "user2Folder2", "Folder");
        user2Folder3 = createFolder(session2,
                user2Workspace1.getPathAsString(), "user2Folder3", "Folder");
        user2File3 = createFile(session2, user2Folder3.getPathAsString(),
                "user2File3", "File", "user2File3.txt", CONTENT_PREFIX
                        + "user2File3");
        session2.save();
        setPermission(user2Folder3, "user1", SecurityConstants.READ_WRITE, true);

        // Register synchronization roots for each user
        // user1
        nuxeoDriveManager.registerSynchronizationRoot("user1", user1Folder1,
                session1);
        nuxeoDriveManager.registerSynchronizationRoot("user1", user1Folder2,
                session1);
        // user2
        nuxeoDriveManager.registerSynchronizationRoot("user2", user2Folder1,
                session2);
        nuxeoDriveManager.registerSynchronizationRoot("user2", user2Folder3,
                session2);

        // Flush user sessions
        session1.save();
        session2.save();

        // Register shared folders as synchronization root for each user
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                session1.getDocument(user2Folder3.getRef()), session1);
        nuxeoDriveManager.registerSynchronizationRoot("user2",
                session2.getDocument(user1Folder3.getRef()), session2);

        // Get an Automation client session for each user
        clientSession1 = automationClient.getSession("user1", "user1");
        clientSession2 = automationClient.getSession("user2", "user2");

        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws ClientException {

        // Close core sessions
        CoreInstance.getInstance().close(session1);
        CoreInstance.getInstance().close(session2);

        // Remove test user permissions
        resetPermissions(session.getRootDocument(), "user1");
        resetPermissions(session.getRootDocument(), "user2");

        // Delete test users
        deleteUser("user1");
        deleteUser("user2");
    }

    @Test
    public void testClientSideUser1() throws Exception {

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
        assertTrue(topLevelFolder.getId().endsWith(TOP_LEVEL_ID_SUFFIX));
        assertNull(topLevelFolder.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolder.getName());
        assertTrue(topLevelFolder.isFolder());
        assertEquals("system", topLevelFolder.getCreator());
        assertFalse(topLevelFolder.getCanRename());
        assertFalse(topLevelFolder.getCanDelete());
        assertFalse(topLevelFolder.getCanCreateChild());

        // ---------------------------------------------
        // Check top level folder children
        // ---------------------------------------------
        Blob topLevelChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", topLevelFolder.getId()).execute();

        ArrayNode topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        // Check "My Documents"
        UserSyncRootParentFolderItem userSyncRootParent = mapper.readValue(
                topLevelChildren.get(0), UserSyncRootParentFolderItem.class);
        assertEquals(USER_SYNC_ROOT_PARENT_ID, userSyncRootParent.getId());
        assertTrue(userSyncRootParent.getParentId().endsWith(
                TOP_LEVEL_ID_SUFFIX));
        assertEquals("My Documents", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals("system", userSyncRootParent.getCreator());
        assertFalse(userSyncRootParent.getCanRename());
        assertFalse(userSyncRootParent.getCanDelete());
        assertFalse(userSyncRootParent.getCanCreateChild());

        // Check "Other Documents"
        SharedSyncRootParentFolderItem sharedSyncRootParent = mapper.readValue(
                topLevelChildren.get(1), SharedSyncRootParentFolderItem.class);
        assertEquals(SHARED_SYNC_ROOT_PARENT_ID, sharedSyncRootParent.getId());
        assertTrue(sharedSyncRootParent.getParentId().endsWith(
                TOP_LEVEL_ID_SUFFIX));
        assertEquals("Other Documents", sharedSyncRootParent.getName());
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

        List<DefaultSyncRootFolderItem> userSyncRoots = mapper.readValue(
                userSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(userSyncRoots);
        assertEquals(2, userSyncRoots.size());

        // user1Folder1
        DefaultSyncRootFolderItem syncRoot = userSyncRoots.get(0);
        checkFolderItem(syncRoot, SYNC_ROOT_ID_PREFIX, user1Folder1,
                USER_SYNC_ROOT_PARENT_ID, "user1Folder1", "user1");
        Blob syncRootChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRoot.getId()).execute();
        List<DocumentBackedFileItem> syncRootChildren = mapper.readValue(
                syncRootChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(syncRootChildren);
        assertEquals(2, syncRootChildren.size());
        // user1File1
        checkFileItem(syncRootChildren.get(0),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File1,
                syncRoot.getId(), "user1File1.txt", "user1");
        // user1File2
        checkFileItem(syncRootChildren.get(1),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user1File2,
                syncRoot.getId(), "user1File2.txt", "user1");
        // user1Folder2
        syncRoot = userSyncRoots.get(1);
        checkFolderItem(syncRoot, SYNC_ROOT_ID_PREFIX, user1Folder2,
                USER_SYNC_ROOT_PARENT_ID, "user1Folder2", "user1");

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
        assertEquals(1, sharedSyncRoots.size());

        // user2Folder3
        DefaultSyncRootFolderItem sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX,
                session1.getDocument(user2Folder3.getRef()),
                sharedSyncRootParent.getId(), "user2Folder3", "user2");
        // user2File3
        Blob sharedSyncRootChildrenJSON = (Blob) clientSession1.newRequest(
                NuxeoDriveGetChildren.ID).set("id", sharedSyncRoot.getId()).execute();
        List<DocumentBackedFileItem> sharedSyncRootChildren = mapper.readValue(
                sharedSyncRootChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        checkFileItem(sharedSyncRootChildren.get(0),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session1.getDocument(user2File3.getRef()),
                sharedSyncRoot.getId(), "user2File3.txt", "user2");
    }

    @Test
    public void testClientSideUser2() throws Exception {

        // ---------------------------------------------
        // Check top level folder: "Nuxeo Drive"
        // ---------------------------------------------
        Blob topLevelFolderJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetTopLevelFolder.ID).execute();
        assertNotNull(topLevelFolderJSON);

        PermissionTopLevelFolderItem topLevelFolder = mapper.readValue(
                topLevelFolderJSON.getStream(),
                PermissionTopLevelFolderItem.class);
        assertNotNull(topLevelFolder);

        // ---------------------------------------------
        // Check top level folder children
        // ---------------------------------------------
        Blob topLevelChildrenJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id", topLevelFolder.getId()).execute();

        ArrayNode topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(), ArrayNode.class);
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        // Check "My Documents"
        UserSyncRootParentFolderItem userSyncRootParent = mapper.readValue(
                topLevelChildren.get(0), UserSyncRootParentFolderItem.class);
        assertEquals(USER_SYNC_ROOT_PARENT_ID, userSyncRootParent.getId());
        assertTrue(userSyncRootParent.getParentId().endsWith(
                TOP_LEVEL_ID_SUFFIX));
        assertEquals("My Documents", userSyncRootParent.getName());
        assertTrue(userSyncRootParent.isFolder());
        assertEquals("system", userSyncRootParent.getCreator());

        // Check "Other Documents"
        SharedSyncRootParentFolderItem sharedSyncRootParent = mapper.readValue(
                topLevelChildren.get(1), SharedSyncRootParentFolderItem.class);
        assertEquals(SHARED_SYNC_ROOT_PARENT_ID, sharedSyncRootParent.getId());
        assertTrue(sharedSyncRootParent.getParentId().endsWith(
                TOP_LEVEL_ID_SUFFIX));
        assertEquals("Other Documents", sharedSyncRootParent.getName());
        assertTrue(sharedSyncRootParent.isFolder());
        assertEquals("system", sharedSyncRootParent.getCreator());

        // --------------------------------------------
        // Check user synchronization roots
        // --------------------------------------------
        Blob userSyncRootsJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id", userSyncRootParent.getId()).execute();

        List<DefaultSyncRootFolderItem> userSyncRoots = mapper.readValue(
                userSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(userSyncRoots);
        assertEquals(2, userSyncRoots.size());

        // user2Folder1
        DefaultSyncRootFolderItem syncRoot = userSyncRoots.get(0);
        checkFolderItem(syncRoot, SYNC_ROOT_ID_PREFIX, user2Folder1,
                USER_SYNC_ROOT_PARENT_ID, "user2Folder1", "user2");
        Blob syncRootChildrenJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRoot.getId()).execute();
        List<DocumentBackedFileItem> syncRootChildren = mapper.readValue(
                syncRootChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(syncRootChildren);
        assertEquals(2, syncRootChildren.size());
        // user2File1
        checkFileItem(syncRootChildren.get(0),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user2File1,
                syncRoot.getId(), "user2File1.txt", "user2");
        // user2File2
        checkFileItem(syncRootChildren.get(1),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user2File2,
                syncRoot.getId(), "user2File2.txt", "user2");
        // user2Folder3
        syncRoot = userSyncRoots.get(1);
        checkFolderItem(syncRoot, SYNC_ROOT_ID_PREFIX, user2Folder3,
                USER_SYNC_ROOT_PARENT_ID, "user2Folder3", "user2");
        syncRootChildrenJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id", syncRoot.getId()).execute();
        syncRootChildren = mapper.readValue(syncRootChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(syncRootChildren);
        assertEquals(1, syncRootChildren.size());
        // user2File3
        checkFileItem(syncRootChildren.get(0),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX, user2File3,
                syncRoot.getId(), "user2File3.txt", "user2");

        // ---------------------------------------------
        // Check shared synchronization roots
        // ---------------------------------------------
        Blob sharedSyncRootsJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                sharedSyncRootParent.getId()).execute();

        List<DefaultSyncRootFolderItem> sharedSyncRoots = mapper.readValue(
                sharedSyncRootsJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(sharedSyncRoots);
        assertEquals(1, sharedSyncRoots.size());

        // user1Folder3
        DefaultSyncRootFolderItem sharedSyncRoot = sharedSyncRoots.get(0);
        checkFolderItem(sharedSyncRoot, SYNC_ROOT_ID_PREFIX,
                session2.getDocument(user1Folder3.getRef()),
                sharedSyncRootParent.getId(), "user1Folder3", "user1");
        // user1File3
        Blob sharedSyncRootChildrenJSON = (Blob) clientSession2.newRequest(
                NuxeoDriveGetChildren.ID).set("id", sharedSyncRoot.getId()).execute();
        List<DocumentBackedFileItem> sharedSyncRootChildren = mapper.readValue(
                sharedSyncRootChildrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(sharedSyncRootChildren);
        assertEquals(1, sharedSyncRootChildren.size());
        checkFileItem(sharedSyncRootChildren.get(0),
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX,
                session2.getDocument(user1File3.getRef()),
                sharedSyncRoot.getId(), "user1File3.txt", "user1");
    }

    protected void checkFileItem(FileItem fileItem, String fileItemIdPrefix,
            DocumentModel doc, String parentId, String name, String creator)
            throws ClientException {

        assertEquals(fileItemIdPrefix + doc.getId(), fileItem.getId());
        assertEquals(parentId, fileItem.getParentId());
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
            String name, String creator) throws ClientException {

        assertEquals(folderItemIdPrefix + doc.getId(), folderItem.getId());
        assertEquals(parentId, folderItem.getParentId());
        assertEquals(name, folderItem.getName());
        assertTrue(folderItem.isFolder());
        assertEquals(creator, folderItem.getCreator());
    }

    protected DocumentModel createFile(CoreSession session, String path,
            String name, String type, String fileName, String content)
            throws ClientException {

        DocumentModel file = session.createDocumentModel(path, name, type);
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                content);
        blob.setFilename(fileName);
        file.setPropertyValue("file:content", (Serializable) blob);
        return session.createDocument(file);
    }

    protected DocumentModel createFolder(CoreSession session, String path,
            String name, String type) throws ClientException {

        DocumentModel folder = session.createDocumentModel(path, name, type);
        return session.createDocument(folder);
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
        session.save();
    }

}
