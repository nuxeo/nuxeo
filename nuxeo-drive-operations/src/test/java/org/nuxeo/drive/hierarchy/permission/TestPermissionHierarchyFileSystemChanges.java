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
package org.nuxeo.drive.hierarchy.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.hierarchy.permission.factory.PermissionSyncRootFactory;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the file system changes in the case of the user workspace and permission based hierarchy.
 *
 * @author Antoine Taillefer
 * @see AuditChangeFinder#getFileSystemChanges
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
@Deploy("org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml")
public class TestPermissionHierarchyFileSystemChanges {

    private static final String USER_SYNC_ROOT_PARENT_ID_PREFIX = "userSyncRootParentFactory#test#";

    private static final String SYNC_ROOT_ID_PREFIX = "permissionSyncRootFactory#test#";

    private static final String CONTENT_PREFIX = "The content of file ";

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserWorkspaceService userWorkspaceService;

    @Inject
    protected EventService eventService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected CloseableCoreSession session1;

    protected CloseableCoreSession session2;

    protected Principal principal1;

    protected Principal principal2;

    protected DocumentModel userWorkspace1;

    protected String userWorkspace1ItemId;

    protected long lastEventLogId;

    @Before
    public void init() throws Exception {
        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", true);

        // Create test users
        createUser("user1", "user1");
        createUser("user2", "user2");

        // Open a core session for each user
        session1 = coreFeature.openCoreSession("user1");
        session2 = coreFeature.openCoreSession("user2");
        principal1 = session1.getPrincipal();
        principal2 = session2.getPrincipal();

        // Create personal workspace for user1
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(session1);
        userWorkspace1ItemId = USER_SYNC_ROOT_PARENT_ID_PREFIX + userWorkspace1.getId();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // Wait for personal workspace creation event to be logged in the audit
        eventService.waitForAsyncCompletion();
    }

    @After
    public void tearDown() {
        // Close core sessions
        if (session1 != null) {
            session1.close();
        }
        if (session2 != null) {
            session2.close();
        }

        // Delete test users
        deleteUser("user1");
        deleteUser("user2");

        // Disable deletion listener for the repository cleanup phase done in
        // CoreFeature#afterTeardown to avoid exception due to no active
        // transaction in FileSystemItemManagerImpl#getSession
        eventServiceAdmin.setListenerEnabledFlag("nuxeoDriveFileSystemDeletionListener", false);

        // Clean up audit log
        cleanUpAuditLog();
    }

    /**
     * This is a utility function to create a tree Like /Folder A/Folder B/Folder C (depth=3) /Folder A/Folder B/Folder
     * C/Folder D (depth=4)
     *
     * @param session to use
     * @param path to start creating the tree
     * @param depth in number of folder
     */
    private void createFolderTree(CoreSession session, String path, Integer depth) {
        Character letter = 'A';
        while (depth-- > 0) {
            createFolder(session, path, "Folder " + letter, "Folder");
            path += "/Folder " + letter++;
        }
    }

    @Test
    public void testRegisteredSyncRootChildChange() throws InterruptedException {

        commitAndWaitForAsyncCompletion();
        DocumentModel folderA;
        DocumentModel folderC;

        // Create the tree structure
        try {
            createFolderTree(session1, userWorkspace1.getPathAsString(), 5);
            folderA = session1.getChild(userWorkspace1.getRef(), "Folder A");
            DocumentModel folderB = session1.getChild(folderA.getRef(), "Folder B");
            folderC = session1.getChild(folderB.getRef(), "Folder C");
            setPermission(session1, folderA, "user2", SecurityConstants.EVERYTHING, true);
            setPermission(session1, folderC, "user2", SecurityConstants.READ, true);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        lastEventLogId = nuxeoDriveManager.getChangeFinder().getUpperBound();
        // Register FolderA as a synchronization root for user2
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session2.getPrincipal(), folderA, session2);
            assertTrue(
                    nuxeoDriveManager.getSynchronizationRootReferences(session2).contains(new IdRef(folderA.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals("rootRegistered", change.getEventId());
            assertEquals("Folder A", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            resetPermissions(session1, folderA.getRef(), "user2");
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals("securityUpdated", change.getEventId());
            assertNull(change.getFileSystemItemName());
            assertNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Register Folder C as a synchronization root for user2
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session2.getPrincipal(), folderC, session2);
            assertFalse(
                    nuxeoDriveManager.getSynchronizationRootReferences(session2).contains(new IdRef(folderA.getId())));
            assertTrue(
                    nuxeoDriveManager.getSynchronizationRootReferences(session2).contains(new IdRef(folderC.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        try {
            assertEquals(1, session2.getChildren(folderC.getRef()).size());
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals("rootRegistered", change.getEventId());
            assertEquals("Folder C", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    /**
     * When an unregistered synchronization root can still be adapted as a FileSystemItem, for example in the case of
     * the "My Docs" virtual folder in the permission based hierarchy implementation, checks that the related
     * {@link FileSystemItemChange} computed by the {@link AuditChangeFinder} contains a null {@code fileSystemItem}
     * attribute, see https://jira.nuxeo.com/browse/NXP-16478.
     */
    @Test
    public void testAdaptableUnregisteredSyncRootChange() throws InterruptedException {

        commitAndWaitForAsyncCompletion();

        lastEventLogId = nuxeoDriveManager.getChangeFinder().getUpperBound();

        // Register user1's personal workspace as a synchronization root for
        // user1
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session1.getPrincipal(), userWorkspace1, session1);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(session1)
                                        .contains(new IdRef(userWorkspace1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        try {
            List<FileSystemItemChange> changes = getChanges(principal1);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(userWorkspace1ItemId, change.getFileSystemItemId());
            assertEquals("My Docs", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Unregister user1's personal workspace as a synchronization root for
        // user1
        try {
            nuxeoDriveManager.unregisterSynchronizationRoot(session1.getPrincipal(), userWorkspace1, session1);
            assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(session1)
                                         .contains(new IdRef(userWorkspace1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        try {
            List<FileSystemItemChange> changes = getChanges(principal1);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals("test#" + userWorkspace1.getId(), change.getFileSystemItemId());
            assertNull(change.getFileSystemItemName());
            assertNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    /**
     * Tests the rootless file system items, typically:
     * <ul>
     * <li>A folder registered as a synchronization root but that is not adaptable as a {@link FileSystemItem}. For
     * example if it is handled by the {@link PermissionSyncRootFactory} and
     * {@link PermissionSyncRootFactory#isFileSystemItem(DocumentModel, boolean)} returns {@code false} because of the
     * missing required permission.</li>
     * <li>A file created in such a folder.</li>
     * <li>A file created in a folder registered as a synchronization root on which the user doesn't have Read
     * access.</li>
     * </ul>
     * For the test, the required permission for a folder to be adapted by the {@link PermissionSyncRootFactory} is
     * Everything.
     *
     * <pre>
     * Server side hierarchy for the test
     * ==================================
     *
     * /user1 (user workspace)
     *   |-- user1Folder1       (registered as a synchronization root with Everything permission for user2)
     *   |-- user1Folder2       (registered as a synchronization root with ReadWrite permission only for user2)
     *   |     |-- user1File1
     *   |     |-- user1File2
     * </pre>
     */
    @Test
    @Deploy("org.nuxeo.drive.operations.test:OSGI-INF/test-nuxeodrive-hierarchy-permission-adapter-contrib.xml")
    public void testRootlessItems() throws Exception {
        commitAndWaitForAsyncCompletion();

        DocumentModel user1Folder1;
        DocumentModel user1Folder2;
        DocumentModel user1File2;

        try {
            // Populate user1's personal workspace
            user1Folder1 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder1", "Folder");
            user1Folder2 = createFolder(session1, userWorkspace1.getPathAsString(), "user1Folder2", "Folder");
            session1.save();
            setPermission(session1, user1Folder1, "user2", SecurityConstants.EVERYTHING, true);
            setPermission(session1, user1Folder2, "user2", SecurityConstants.READ_WRITE, true);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        lastEventLogId = nuxeoDriveManager.getChangeFinder().getUpperBound();

        // Check sync root with Everything permission: user1Folder1 => adaptable
        // so appears in the file system changes
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session2.getPrincipal(), user1Folder1, session2);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(session2)
                                        .contains(new IdRef(user1Folder1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(SYNC_ROOT_ID_PREFIX + user1Folder1.getId(), change.getFileSystemItemId());
            assertEquals("user1Folder1", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check sync root with ReadWrite permission only: user1Folder2 => not
        // adaptable so doesn't appear in the file system changes
        try {
            nuxeoDriveManager.registerSynchronizationRoot(session2.getPrincipal(), user1Folder2, session2);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(session2)
                                        .contains(new IdRef(user1Folder2.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertTrue(changes.isEmpty());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file creation in a sync root with ReadWrite permission only:
        // user1File1 => non adaptable parent user1Folder2 so doesn't appear in
        // the file system changes
        try {
            createFile(session1, user1Folder2.getPathAsString(), "user1File1", "File", "user1File1.txt",
                    CONTENT_PREFIX + "user1File1");
            session1.save();
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertTrue(changes.isEmpty());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file creation in a sync root without Read permission:
        // user1File2 => non accessible parent user1Folder2 so doesn't appear in
        // the file system changes
        try {
            resetPermissions(session1, user1Folder2.getRef(), "user2");
            user1File2 = createFile(session1, user1Folder2.getPathAsString(), "user1File2", "File", "user1File2.txt",
                    CONTENT_PREFIX + "user1File2");
            setPermission(session1, user1File2, "user2", SecurityConstants.READ, true);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            // Security updates
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(2, changes.size());

            FileSystemItemChange change = changes.get(0);
            assertEquals("securityUpdated", change.getEventId());
            assertEquals("test#" + user1File2.getId(), change.getFileSystemItemId());
            assertNull(change.getFileSystemItemName());
            // Not adaptable as a FileSystemItem since parent is not
            assertNull(change.getFileSystemItem());

            change = changes.get(1);
            assertEquals("securityUpdated", change.getEventId());
            assertEquals("test#" + user1Folder2.getId(), change.getFileSystemItemId());
            assertNull(change.getFileSystemItemName());
            // Not adaptable as a FileSystemItem since no Read permission
            assertNull(change.getFileSystemItem());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    protected DocumentModel createFile(CoreSession session, String path, String name, String type, String fileName,
            String content) {

        DocumentModel file = session.createDocumentModel(path, name, type);
        Blob blob = new StringBlob(content);
        blob.setFilename(fileName);
        file.setPropertyValue("file:content", (Serializable) blob);
        return session.createDocument(file);
    }

    protected DocumentModel createFolder(CoreSession session, String path, String name, String type) {

        DocumentModel folder = session.createDocumentModel(path, name, type);
        return session.createDocument(folder);
    }

    protected void createUser(String userName, String password) {
        try (Session userDir = directoryService.open("userDirectory")) {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("username", userName);
            user.put("password", password);
            userDir.createEntry(user);
        }
    }

    protected void deleteUser(String userName) {
        try (Session userDir = directoryService.open("userDirectory")) {
            userDir.deleteEntry(userName);
        }
    }

    protected void setPermission(CoreSession session, DocumentModel doc, String userName, String permission,
            boolean isGranted) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected void resetPermissions(CoreSession session, DocumentRef docRef, String userName) {
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
        session.save();
    }

    @Inject
    TransactionalFeature txFeature;

    protected void commitAndWaitForAsyncCompletion() {
        txFeature.nextTransaction();
    }

    protected List<FileSystemItemChange> getChanges(Principal principal) throws InterruptedException {
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getChangeSummary(principal,
                Collections.<String, Set<IdRef>> emptyMap(), lastEventLogId);
        assertNotNull(changeSummary);
        lastEventLogId = changeSummary.getUpperBound();
        return changeSummary.getFileSystemChanges();
    }

    protected void cleanUpAuditLog() {
        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime()
                                                                            .getComponent(NXAuditEventsService.NAME);
        ((DefaultAuditBackend) auditService.getBackend()).getOrCreatePersistenceProvider().run(true, new RunVoid() {
            @Override
            public void runWith(EntityManager em) {
                em.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
                em.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
                em.createNativeQuery("delete from nxp_logs").executeUpdate();
            }
        });
    }

}
