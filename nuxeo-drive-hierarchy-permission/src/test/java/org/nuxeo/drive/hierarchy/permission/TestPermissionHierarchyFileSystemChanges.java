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
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.drive.service.impl.FileSystemChangeSummary;
import org.nuxeo.drive.service.impl.FileSystemItemChange;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
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
@Features(AuditFeature.class)
// We handle transaction start and commit manually to make it possible to have
// several consecutive transactions in a test method
@TransactionalConfig(autoStart = false)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.drive.core",
        "org.nuxeo.drive.hierarchy.permission" })
public class TestPermissionHierarchyFileSystemChanges {

    private static final String USER_SYNC_ROOT_PARENT_ID_PREFIX = "userSyncRootParentFactory#test#";

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

    protected Principal principal1;

    protected DocumentModel userWorkspace1;

    protected String userWorkspace1ItemId;

    protected long lastSuccessfulSync;

    @Before
    public void init() throws Exception {

        // Enable deletion listener because the tear down disables it
        eventServiceAdmin.setListenerEnabledFlag(
                "nuxeoDriveFileSystemDeletionListener", true);

        // Create test user
        createUser("user1", "user1");

        // Open a core session for user1
        session1 = repository.openSessionAs("user1");
        principal1 = session1.getPrincipal();

        // Create personal workspace for user1
        userWorkspace1 = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session1, null);
        // Wait for creation event to be logged in the audit
        eventService.waitForAsyncCompletion();
        userWorkspace1ItemId = USER_SYNC_ROOT_PARENT_ID_PREFIX
                + userWorkspace1.getId();

        // Set last synchronization date
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();
    }

    @After
    public void tearDown() throws ClientException {

        // Close core sessions
        CoreInstance.getInstance().close(session1);

        // Delete test user
        deleteUser("user1");

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
