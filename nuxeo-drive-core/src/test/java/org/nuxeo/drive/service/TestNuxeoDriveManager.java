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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
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
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.annotations.Granularity;
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
@Features(PlatformFeature.class)
@RepositoryConfig(repositoryName = "default", init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
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

    protected CoreSession user1Session;

    protected CoreSession user2Session;

    protected DocumentRef user1Workspace;

    protected DocumentRef user2Workspace;

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
        session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "workspace-1", "Workspace"));
        session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces/workspace-1", "folder-1-1",
                "Folder"));
        DocumentModel workspace_2 = session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces", "workspace-2", "Workspace"));
        session.createDocument(session.createDocumentModel(
                "/default-domain/workspaces/workspace-2", "folder-2-1",
                "Folder"));
        ACP acp = session.getACP(workspace_2.getRef());
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("members", SecurityConstants.READ_WRITE, true));
        session.setACP(workspace_2.getRef(), acp, true);
        session.save();

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
            CoreInstance.getInstance().close(user1Session);
        }
        if (user2Session != null) {
            CoreInstance.getInstance().close(user2Session);
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

    protected void checkRootsCount(String userName, CoreSession session,
            int expectedCount) throws ClientException {
        assertEquals(
                expectedCount,
                nuxeoDriveManager.getSynchronizationRootReferences(userName,
                        session).size());
    }

    public DocumentModel doc(String path) throws ClientException {
        return doc(session, path);
    }

    public DocumentModel doc(CoreSession session, String path)
            throws ClientException {
        return session.getDocument(new PathRef(path));
    }

    @Test
    public void testGetSynchronizationRoots() throws Exception {

        // Register synchronization roots
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                user1Session.getDocument(user1Workspace), user1Session);
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);

        // Check synchronization root references
        Set<IdRef> rootRefs = nuxeoDriveManager.getSynchronizationRootReferences(
                "user1", user1Session);
        assertEquals(2, rootRefs.size());
        assertTrue(rootRefs.contains(user1Workspace));
        assertTrue(rootRefs.contains(new IdRef(user1Session.getDocument(
                new PathRef("/default-domain/workspaces/workspace-2")).getId())));

        // Check synchronization root paths
        Map<String, SynchronizationRoots> synRootMap = nuxeoDriveManager.getSynchronizationRoots(
                true, "user1", user1Session);
        Set<String> rootPaths = synRootMap.get("default").paths;
        assertEquals(2, rootPaths.size());
        assertTrue(rootPaths.contains("/default-domain/UserWorkspaces/user1"));
        assertTrue(rootPaths.contains("/default-domain/workspaces/workspace-2"));
    }

    @Test
    public void testSynchronizeRootMultiUsers() throws Exception {
        // by default no user has any synchronization registered
        checkRootsCount("user1", session, 0);
        checkRootsCount("user1", user1Session, 0);
        checkRootsCount("user1", user2Session, 0);
        checkRootsCount("user2", session, 0);
        checkRootsCount("user2", user1Session, 0);
        checkRootsCount("user2", user2Session, 0);

        // check that users have the right to synchronize their own user
        // workspace
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                user1Session.getDocument(user1Workspace), user1Session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 0);

        nuxeoDriveManager.registerSynchronizationRoot("user2",
                user2Session.getDocument(user2Workspace), user2Session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 1);

        // check that users cannot synchronize workspaces and folders where they
        // don't have content creation access to.
        try {
            nuxeoDriveManager.registerSynchronizationRoot(
                    "user1",
                    doc(user1Session, "/default-domain/workspaces/workspace-1"),
                    user1Session);
            fail("user1 should not have the permission to use Workspace 1 as sync root.");
        } catch (SecurityException se) {
            // expected
        }

        // this check should also fail even if the CoreSession is opened by the
        // Administrator
        try {
            nuxeoDriveManager.registerSynchronizationRoot("user1",
                    doc("/default-domain/workspaces/workspace-1"), session);
        } catch (SecurityException se) {
            // expected
        }

        // users can synchronize to workspaces and folder where they have
        // write access
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        checkRootsCount("user1", session, 2);
        checkRootsCount("user2", session, 1);

        nuxeoDriveManager.registerSynchronizationRoot(
                "user2",
                doc(user2Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user2Session);
        checkRootsCount("user1", session, 2);
        checkRootsCount("user2", session, 2);

        // check unsync:
        // XXX: this does not work when fetching document with session instead
        // of user1Session
        nuxeoDriveManager.unregisterSynchronizationRoot("user1",
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 2);

        // make user1 synchronize the same subfolder as user 2
        nuxeoDriveManager.registerSynchronizationRoot(
                "user1",
                doc(user1Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount("user1", session, 2);
        checkRootsCount("user2", session, 2);

        // unsyncing unsynced folder does nothing
        nuxeoDriveManager.unregisterSynchronizationRoot("user2",
                doc("/default-domain/workspaces/workspace-2"), session);
        checkRootsCount("user1", session, 2);
        checkRootsCount("user2", session, 2);

        nuxeoDriveManager.unregisterSynchronizationRoot("user1",
                session.getDocument(user1Workspace), session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 2);

        nuxeoDriveManager.unregisterSynchronizationRoot("user1",
                doc("/default-domain/workspaces/workspace-2/folder-2-1"),
                session);
        checkRootsCount("user1", session, 0);
        checkRootsCount("user2", session, 2);

        // check re-registration
        nuxeoDriveManager.registerSynchronizationRoot("user1",
                doc("/default-domain/workspaces/workspace-2/folder-2-1"),
                session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 2);
    }

    @Test
    public void testSynchronizationRootDeletion() throws Exception {
        checkRootsCount("user1", session, 0);
        checkRootsCount("user2", session, 0);

        nuxeoDriveManager.registerSynchronizationRoot("user1",
                doc(user1Session, "/default-domain/workspaces/workspace-2"),
                user1Session);
        nuxeoDriveManager.registerSynchronizationRoot(
                "user2",
                doc(user1Session,
                        "/default-domain/workspaces/workspace-2/folder-2-1"),
                user1Session);
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 1);

        // check deletion by lifecycle
        session.followTransition(
                doc("/default-domain/workspaces/workspace-2/folder-2-1").getRef(),
                "delete");
        session.save();
        checkRootsCount("user1", session, 1);
        checkRootsCount("user2", session, 0);

        // check physical deletion of a parent folder
        session.removeDocument(doc("/default-domain").getRef());
        session.save();
        checkRootsCount("user1", session, 0);
        checkRootsCount("user2", session, 0);
    }

}
