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
package org.nuxeo.drive.hierarchy.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
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
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the file system changes in the case of the user workspace and
 * permission based hierarchy.
 *
 * @author Antoine Taillefer
 * @see AuditChangeFinder#getFileSystemChanges
 */
@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, AuditFeature.class})
// We handle transaction start and commit manually to make it possible to have
// several consecutive transactions in a test method
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.runtime.reload", "org.nuxeo.drive.core",
        "org.nuxeo.drive.core:OSGI-INF/nuxeodrive-hierarchy-permission-contrib.xml" })
public class TestPermissionHierarchyFileSystemChanges {

    private static final String USER_SYNC_ROOT_PARENT_ID_PREFIX = "userSyncRootParentFactory#test#";

    private static final String SYNC_ROOT_ID_PREFIX = "permissionSyncRootFactory#test#";

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String CONTENT_PREFIX = "The content of file ";

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected RepositorySettings repository;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserWorkspaceService userWorkspaceService;

    @Inject
    protected EventService eventService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected CoreSession session1;

    protected CoreSession session2;

    protected Principal principal1;

    protected Principal principal2;

    protected DocumentModel userWorkspace1;

    protected String userWorkspace1ItemId;

    protected long lastSuccessfulSync;

    @Before
    public void init() throws Exception {
        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag(
                "nuxeoDriveFileSystemDeletionListener", true);

        // Create test users
        createUser("user1", "user1");
        createUser("user2", "user2");

        // Open a core session for each user
        session1 = repository.openSessionAs("user1");
        session2 = repository.openSessionAs("user2");
        principal1 = session1.getPrincipal();
        principal2 = session2.getPrincipal();

        // Create personal workspace for user1
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session1, null);
        userWorkspace1ItemId = USER_SYNC_ROOT_PARENT_ID_PREFIX
                + userWorkspace1.getId();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // Wait for personal workspace creation event to be logged in the audit
        eventService.waitForAsyncCompletion();
    }

    @After
    public void tearDown() throws ClientException {
        // needed for session cleanup
        TransactionHelper.startTransaction();

        // Close core sessions
        session1.close();
        session2.close();

        // Delete test users
        deleteUser("user1");
        deleteUser("user2");

        // Disable deletion listener for the repository cleanup phase done in
        // CoreFeature#afterTeardown to avoid exception due to no active
        // transaction in FileSystemItemManagerImpl#getSession
        eventServiceAdmin.setListenerEnabledFlag(
                "nuxeoDriveFileSystemDeletionListener", false);
    }

    /**
     * When an unregistered synchronization root can still be adapted as a
     * FileSystemItem, for example in the case of the "My Docs" virtual folder
     * in the permission based hierarchy implementation, checks that the related
     * {@link FileSystemItemChange} computed by the {@link AuditChangeFinder}
     * contains a non null {@code fileSystemItem} attribute, that is to be used
     * by the client.
     */
    @Test
    public void testAdaptableUnregisteredSyncRootChange()
            throws ClientException, InterruptedException {

        TransactionHelper.commitOrRollbackTransaction();

        // Set last synchronization date
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();

        // Register user1's personal workspace as a synchronization root for
        // user1
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.registerSynchronizationRoot(
                    session1.getPrincipal(), userWorkspace1, session1);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(
                    session1).contains(new IdRef(userWorkspace1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        TransactionHelper.startTransaction();
        try {
            List<FileSystemItemChange> changes = getChanges(principal1);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(userWorkspace1ItemId, change.getFileSystemItemId());
            assertEquals("My Docs", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        // Unregister user1's personal workspace as a synchronization root for
        // user1
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.unregisterSynchronizationRoot(
                    session1.getPrincipal(), userWorkspace1, session1);
            assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(
                    session1).contains(new IdRef(userWorkspace1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Check file system item change
        TransactionHelper.startTransaction();
        try {
            List<FileSystemItemChange> changes = getChanges(principal1);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(userWorkspace1ItemId, change.getFileSystemItemId());
            assertEquals("My Docs", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /**
     * Tests the rootless file system items, typically:
     * <ul>
     * <li>A folder registered as a synchronization root but that is not
     * adaptable as a {@link FileSystemItem}. For example if it is handled by
     * the {@link PermissionSyncRootFactory} and
     * {@link PermissionSyncRootFactory#isFileSystemItem(DocumentModel, boolean)}
     * returns {@code false} because of the missing required permission.</li>
     * <li>A file created in such a folder.</li>
     * <li>A file created in a folder registered as a synchronization root on
     * which the user doesn't have Read access.</li>
     * </ul>
     * For the test, the required permission for a folder to be adapted by the
     * {@link PermissionSyncRootFactory} is Everything.
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
     *
     * </pre>
     */
    @Test
    public void testRootlessItems() throws Exception {

        TransactionHelper.commitOrRollbackTransaction();

        DocumentModel user1Folder1;
        DocumentModel user1Folder2;
        DocumentModel user1File2;

        harness.deployContrib("org.nuxeo.drive.operations.test",
                "OSGI-INF/test-nuxeodrive-hierarchy-permission-adapter-contrib.xml");
        Framework.getLocalService(ReloadService.class).reload();

        TransactionHelper.startTransaction();
        try {
            // Populate user1's personal workspace
            user1Folder1 = createFolder(session1,
                    userWorkspace1.getPathAsString(), "user1Folder1", "Folder");
            user1Folder2 = createFolder(session1,
                    userWorkspace1.getPathAsString(), "user1Folder2", "Folder");
            session1.save();
            setPermission(session1, user1Folder1, "user2",
                    SecurityConstants.EVERYTHING, true);
            setPermission(session1, user1Folder2, "user2",
                    SecurityConstants.READ_WRITE, true);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        // Wait for creation events to be logged in the audit
        eventService.waitForAsyncCompletion();

        // Set last synchronization date
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();

        // Check sync root with Everything permission: user1Folder1 => adaptable
        // so appears in the file system changes
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.registerSynchronizationRoot(
                    session2.getPrincipal(), user1Folder1, session2);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(
                    session2).contains(new IdRef(user1Folder1.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        TransactionHelper.startTransaction();
        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(SYNC_ROOT_ID_PREFIX + user1Folder1.getId(),
                    change.getFileSystemItemId());
            assertEquals("user1Folder1", change.getFileSystemItemName());
            assertNotNull(change.getFileSystemItem());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        // Check sync root with ReadWrite permission only: user1Folder2 => not
        // adaptable so doesn't appear in the file system changes
        TransactionHelper.startTransaction();
        try {
            nuxeoDriveManager.registerSynchronizationRoot(
                    session2.getPrincipal(), user1Folder2, session2);
            assertTrue(nuxeoDriveManager.getSynchronizationRootReferences(
                    session2).contains(new IdRef(user1Folder2.getId())));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        TransactionHelper.startTransaction();
        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertTrue(changes.isEmpty());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        // Check file creation in a sync root with ReadWrite permission only:
        // user1File1 => non adaptable parent user1Folder2 so doesn't appear in
        // the file system changes
        TransactionHelper.startTransaction();
        try {
            createFile(session1, user1Folder2.getPathAsString(), "user1File1",
                    "File", "user1File1.txt", CONTENT_PREFIX + "user1File1");
            session1.save();
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        TransactionHelper.startTransaction();
        try {
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertTrue(changes.isEmpty());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        // Check file creation in a sync root without Read permission:
        // user1File2 => non accessible parent user1Folder2 so doesn't appear in
        // the file system changes
        TransactionHelper.startTransaction();
        try {
            resetPermissions(session1, user1Folder2.getRef(), "user2");
            user1File2 = createFile(session1, user1Folder2.getPathAsString(),
                    "user1File2", "File", "user1File2.txt", CONTENT_PREFIX
                            + "user1File2");
            setPermission(session1, user1File2, "user2",
                    SecurityConstants.READ, true);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        // Wait for creation events to be logged in the audit
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        try {
            // Security updates
            List<FileSystemItemChange> changes = getChanges(principal2);
            assertEquals(2, changes.size());

            FileSystemItemChange change = changes.get(0);
            assertEquals("securityUpdated", change.getEventId());
            assertEquals(
                    DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + user1File2.getId(),
                    change.getFileSystemItemId());
            assertEquals("user1File2.txt", change.getFileSystemItemName());
            // Not adaptable as a FileSystemItem since parent is not
            assertNull(change.getFileSystemItem());

            change = changes.get(1);
            assertEquals("securityUpdated", change.getEventId());
            assertEquals(SYNC_ROOT_ID_PREFIX + user1Folder2.getId(),
                    change.getFileSystemItemId());
            assertEquals("user1Folder2", change.getFileSystemItemName());
            // Not adaptable as a FileSystemItem since no Read permission
            assertNull(change.getFileSystemItem());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        harness.undeployContrib("org.nuxeo.drive.operations.test",
                "OSGI-INF/test-nuxeodrive-hierarchy-permission-adapter-contrib.xml");
        Framework.getLocalService(ReloadService.class).reload();
    }

    protected DocumentModel createFile(CoreSession session, String path,
            String name, String type, String fileName, String content)
            throws ClientException {

        DocumentModel file = session.createDocumentModel(path, name, type);
        Blob blob = new StringBlob(content);
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
        Session userDir = directoryService.getDirectory("userDirectory").getSession();
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

    protected void setPermission(CoreSession session, DocumentModel doc,
            String userName, String permission, boolean isGranted)
            throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected void resetPermissions(CoreSession session, DocumentRef docRef,
            String userName) throws ClientException {
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

    protected void commitAndWaitForAsyncCompletion() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected List<FileSystemItemChange> getChanges(Principal principal)
            throws ClientException, InterruptedException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getChangeSummary(
                principal, Collections.<String, Set<IdRef>> emptyMap(),
                lastSuccessfulSync);
        assertNotNull(changeSummary);
        lastSuccessfulSync = changeSummary.getSyncDate();
        return changeSummary.getFileSystemChanges();
    }

}
