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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests for {@link NuxeoDriveManager}
 *
 * @author <a href="mailto:ogrise@nuxeo.com">Olivier Grisel</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.drive.core" })
public class TestNuxeoDriveManager {

    @Inject
    CoreSession session;

    @Inject
    RepositorySettings repository;

    @Inject
    NuxeoDriveManager nuxeoDriveManager;

    @Inject
    DirectoryService directoryService;

    @Inject
    UserManager userManager;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Inject
    EventServiceAdmin eventServiceAdmin;

    protected CoreSession user1Session;

    protected CoreSession user2Session;

    protected DocumentRef user1Workspace;

    protected DocumentRef user2Workspace;

    protected DocumentModel workspace_1;

    protected DocumentModel workspace_2;

    protected DocumentModel folder_1_1;

    protected DocumentModel folder_2_1;

    @Before
    public void createUserSessionsAndFolders() throws Exception {
        Session userDir = directoryService.getDirectory("userDirectory").getSession();
        try {
            Map<String, Object> user1 = new HashMap<String, Object>();
            user1.put("username", "user1");
            user1.put("groups", Arrays.asList(new String[] { "members" }));
            userDir.createEntry(user1);
            Map<String, Object> user2 = new HashMap<String, Object>();
            user2.put("username", "user2");
            user2.put("groups", Arrays.asList(new String[] { "members" }));
            userDir.createEntry(user2);
        } finally {
            userDir.close();
        }
        workspace_1 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "workspace-1", "Workspace"));
        folder_1_1 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces/workspace-1", "folder-1-1",
                "Folder"));
        workspace_2 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "workspace-2", "Workspace"));
        folder_2_1 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces/workspace-2", "folder-2-1",
                "Folder"));
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
        permissions.put(SecurityConstants.READ_WRITE, true);
        setPermissions(workspace_1, "members", permissions);
        setPermissions(workspace_2, "members", permissions);

        user1Session = repository.openSessionAs("user1");
        user2Session = repository.openSessionAs("user2");
        // Work around the RepositorySettings API that does not allow to open
        // sessions with real principal objects from the user manager and their
        // groups.
        UserPrincipal user1 = (UserPrincipal) user1Session.getPrincipal();
        user1.setGroups(userManager.getPrincipal("user1").getGroups());
        UserPrincipal user2 = (UserPrincipal) user2Session.getPrincipal();
        user2.setGroups(userManager.getPrincipal("user2").getGroups());

        user1Workspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                user1Session,
                user1Session.getDocument(new PathRef("/default-domain"))).getRef();
        user2Workspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                user2Session,
                user2Session.getDocument(new PathRef("/default-domain"))).getRef();
    }

    @After
    public void closeSessionsAndDeleteUsers() throws Exception {
        if (user1Session != null) {
            user1Session.close();
        }
        if (user2Session != null) {
            user2Session.close();
        }
        Session usersDir = directoryService.getDirectory("userDirectory").getSession();
        try {
            usersDir.deleteEntry("user1");
            usersDir.deleteEntry("user2");
        } finally {
            usersDir.close();
        }
        // Simulate root deletion to cleanup the cache between the tests
        nuxeoDriveManager.handleFolderDeletion((IdRef) doc("/").getRef());
    }

    @Inject RepositorySettings settings;

    @Test
    public void testGetSynchronizationRoots() throws Exception {

        // Register synchronization roots
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                user1Session.getDocument(user1Workspace), user1Session);
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);

        // Check synchronization root references
        Set<IdRef> rootRefs = nuxeoDriveManager.getSynchronizationRootReferences(user1Session);
        assertEquals(2, rootRefs.size());
        assertTrue(rootRefs.contains(user1Workspace));
        assertTrue(rootRefs.contains(new IdRef(user1Session.getDocument(
                new PathRef("/default-domain/workspaces/workspace-2")).getId())));

        // Check synchronization root paths
        Map<String, SynchronizationRoots> synRootMap = nuxeoDriveManager.getSynchronizationRoots(user1Session.getPrincipal());
        Set<String> rootPaths = synRootMap.get(settings.getName()).paths;
        assertEquals(2, rootPaths.size());
        assertTrue(rootPaths.contains("/default-domain/UserWorkspaces/user1"));
        assertTrue(rootPaths.contains("/default-domain/workspaces/workspace-2"));
    }

    @Test
    public void testSynchronizeRootMultiUsers() throws Exception {
        Principal user1 = user1Session.getPrincipal();
        Principal user2 = user2Session.getPrincipal();

        // by default no user has any synchronization registered
        checkRootsCount(user1, 0);
        checkRootsCount(user2, 0);

        // check that users have the right to synchronize their own user
        // workspace
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                user1Session.getDocument(user1Workspace), user1Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 0);

        nuxeoDriveManager.registerSynchronizationRoot(
                user2Session.getPrincipal(),
                user2Session.getDocument(user2Workspace), user2Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 1);

        // users can synchronize to workspaces and folders
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        checkRootsCount(user1, 2);
        checkRootsCount(user2, 1);

        nuxeoDriveManager.registerSynchronizationRoot(
                user2Session.getPrincipal(),
                doc(user2Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user2Session);
        checkRootsCount(user1, 2);
        checkRootsCount(user2, 2);

        // check unsync:
        // XXX: this does not work when fetching document with session instead
        // of user1Session
        nuxeoDriveManager.unregisterSynchronizationRoot(
                user1Session.getPrincipal(),
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 2);

        // make user1 synchronize the same subfolder as user 2
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                doc(user1Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount(user1, 2);
        checkRootsCount(user2, 2);

        // unsyncing unsynced folder does nothing
        nuxeoDriveManager.unregisterSynchronizationRoot(
                user2Session.getPrincipal(),
                doc("/default-domain/workspaces/workspace-2"), user2Session);
        checkRootsCount(user1, 2);
        checkRootsCount(user2, 2);

        nuxeoDriveManager.unregisterSynchronizationRoot(
                user1Session.getPrincipal(),
                session.getDocument(user1Workspace), user1Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 2);

        nuxeoDriveManager.unregisterSynchronizationRoot(
                user1Session.getPrincipal(),
                doc("/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount(user1, 0);
        checkRootsCount(user2, 2);

        // check re-registration
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(),
                doc("/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 2);
    }

    @Test
    public void testSynchronizationRootDeletion() throws Exception {
        // Disable bulk life cycle change listener to avoid exception when
        // trying to apply recursive changes on a removed document
        eventServiceAdmin.setListenerEnabledFlag("bulkLifeCycleChangeListener",
                false);

        Principal user1 = user1Session.getPrincipal();
        Principal user2 = user2Session.getPrincipal();
        checkRootsCount(user1, 0);
        checkRootsCount(user2, 0);

        nuxeoDriveManager.registerSynchronizationRoot(user1,
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        nuxeoDriveManager.registerSynchronizationRoot(
                user2,
                doc(user1Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 1);

        // check deletion by lifecycle
        session.followTransition(
                doc("/default-domain/workspaces/workspace-2/folder-2-1").getRef(),
                "delete");
        session.save();
        checkRootsCount(user1, 1);
        checkRootsCount(user2, 0);

        // check physical deletion of a parent folder
        session.removeDocument(doc("/default-domain").getRef());
        session.save();
        checkRootsCount(user1, 0);
        checkRootsCount(user2, 0);
    }

    @Test
    public void testSyncRootChild() throws ClientException {

        // Make user1 register child of workspace-2: folder-2-1
        assertFalse(isUserSubscribed("user1", folder_2_1));
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(), folder_2_1, user1Session);
        assertTrue(isUserSubscribed("user1", folder_2_1));

        // Make user1 register workspace-2, should unregister child folder-2-1
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(), workspace_2, user1Session);
        folder_2_1 = user1Session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace-2/folder-2-1"));
        assertTrue(isUserSubscribed("user1", workspace_2));
        assertFalse(isUserSubscribed("user1", folder_2_1));

        // Make user1 register folder-2-1, should have no effect
        nuxeoDriveManager.registerSynchronizationRoot(
                user1Session.getPrincipal(), folder_2_1, user1Session);
        folder_2_1 = user1Session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace-2/folder-2-1"));
        assertFalse(isUserSubscribed("user1", folder_2_1));
    }

    @Test
    public void testSyncRootCacheInvalidation() throws ClientException {
        Principal user1Principal = user1Session.getPrincipal();
        // No roots => no sync roots
        Set<String> expectedSyncRootPaths = new HashSet<String>();
        checkRoots(user1Principal, 0, expectedSyncRootPaths);

        // Register sync roots => registration should invalidate the cache
        nuxeoDriveManager.registerSynchronizationRoot(user1Principal,
                workspace_1, user1Session);
        nuxeoDriveManager.registerSynchronizationRoot(user1Principal,
                workspace_2, user1Session);
        expectedSyncRootPaths.add("/default-domain/workspaces/workspace-1");
        expectedSyncRootPaths.add("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 2, expectedSyncRootPaths);

        // Delete sync root => nuxeoDriveCacheInvalidationListener should
        // invalidate the cache
        session.followTransition(workspace_2.getRef(), "delete");
        session.save();
        expectedSyncRootPaths.remove("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 1, expectedSyncRootPaths);

        // Undelete sync root => nuxeoDriveCacheInvalidationListener should
        // invalidate the cache
        session.followTransition(workspace_2.getRef(), "undelete");
        session.save();
        expectedSyncRootPaths.add("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 2, expectedSyncRootPaths);

        // Deny Read permission => nuxeoDriveCacheInvalidationListener should
        // invalidate the cache
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
        permissions.put(SecurityConstants.READ_WRITE, false);
        permissions.put(SecurityConstants.READ, false);
        setPermissions(workspace_2, "user1", permissions);
        expectedSyncRootPaths.remove("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 1, expectedSyncRootPaths);

        // Grant Read permission back => nuxeoDriveCacheInvalidationListener
        // should invalidate the cache
        permissions.put(SecurityConstants.READ_WRITE, true);
        permissions.put(SecurityConstants.READ, true);
        setPermissions(workspace_2, "user1", permissions);
        expectedSyncRootPaths.add("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 2, expectedSyncRootPaths);

        // Remove sync root => nuxeoDriveCacheInvalidationListener should
        // invalidate the cache
        session.removeDocument(workspace_2.getRef());
        expectedSyncRootPaths.remove("/default-domain/workspaces/workspace-2");
        checkRoots(user1Principal, 1, expectedSyncRootPaths);

        // Unregister sync root=> unregistration should invalidate the cache
        nuxeoDriveManager.unregisterSynchronizationRoot(user1Principal,
                workspace_1, user1Session);
        expectedSyncRootPaths.remove("/default-domain/workspaces/workspace-1");
        checkRoots(user1Principal, 0, expectedSyncRootPaths);
    }

    @Test
    public void testSyncRootsWithPathInclusion() throws ClientException {
        Principal user1Principal = user1Session.getPrincipal();

        // Create 2 folders with path inclusion:
        // /default-domain/folder1 includes /default-domain/folder
        DocumentModel folder = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "folder", "Folder"));
        DocumentModel folder1 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "folder1", "Folder"));
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
        permissions.put(SecurityConstants.READ_WRITE, true);
        setPermissions(folder, "members", permissions);
        setPermissions(folder1, "members", permissions);

        // Register folder as a synchronization root
        nuxeoDriveManager.registerSynchronizationRoot(user1Principal, folder,
                user1Session);
        // Register folder1 as a synchronization root
        nuxeoDriveManager.registerSynchronizationRoot(user1Principal, folder1,
                user1Session);

        // Check there are 2 synchronization roots
        Set<IdRef> syncRootrefs = nuxeoDriveManager.getSynchronizationRootReferences(user1Session);
        assertEquals(2, syncRootrefs.size());
        assertTrue(syncRootrefs.contains(folder.getRef()));
        assertTrue(syncRootrefs.contains(folder1.getRef()));
    }

    protected DocumentModel doc(String path) throws ClientException {
        return doc(session, path);
    }

    protected DocumentModel doc(CoreSession session, String path)
            throws ClientException {
        return session.getDocument(new PathRef(path));
    }

    protected void checkRootsCount(Principal principal, int expectedCount)
            throws ClientException {
        assertEquals(
                expectedCount,
                nuxeoDriveManager.getSynchronizationRoots(principal).get(
                        settings.getName()).refs.size());
    }

    protected void checkRoots(Principal principal, int expectedCount,
            Set<String> expectedRootPaths) throws ClientException {
        Map<String, SynchronizationRoots> syncRoots = nuxeoDriveManager.getSynchronizationRoots(principal);
        Set<String> syncRootPaths = syncRoots.get(settings.getName()).paths;
        assertEquals(expectedCount, syncRootPaths.size());
        for (String syncRootPath : expectedRootPaths) {
            assertTrue(syncRootPaths.contains(syncRootPath));
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean isUserSubscribed(String userName, DocumentModel container)
            throws ClientException {
        if (!container.hasFacet(NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET)) {
            return false;
        }
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) container.getPropertyValue(NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY);
        if (subscriptions == null) {
            return false;
        }
        for (Map<String, Object> subscription : subscriptions) {
            if (userName.equals(subscription.get("username"))
                    && (Boolean) subscription.get("enabled")) {
                return true;
            }
        }
        return false;
    }

    protected void setPermissions(DocumentModel doc, String userName,
            Map<String, Boolean> permissions) throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        for (String permission : permissions.keySet()) {
            localACL.add(0,
                    new ACE(userName, permission, permissions.get(permission)));
        }
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

}
