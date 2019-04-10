/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.fixtures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.drive.listener.NuxeoDriveGroupUpdateListener;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Tests the detection of {@link NuxeoDriveEvents#SECURITY_UPDATED_EVENT} events in the audit logs by the
 * {@link FileSystemChangeFinder} implementations when group changes occur.
 *
 * @see NuxeoDriveGroupUpdateListener
 * @since 9.2
 */
public class GroupChangesTestSuite extends AbstractChangeFinderTestCase {

    private static final Logger log = LogManager.getLogger(GroupChangesTestSuite.class);

    @Inject
    protected UserManager userManager;

    protected CoreSession userSession;

    @Override
    @Before
    public void init() throws Exception {
        lastEventLogId = 0;
        lastSyncActiveRootDefinitions = "";

        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "user");
        userManager.createUser(user);

        List<String> members = Arrays.asList("user");
        createGroup("group1", members, null);
        createGroup("group2", members, null);
        createGroup("parentGroup", null, Arrays.asList("group1"));
        createGroup("grandParentGroup", null, Arrays.asList("parentGroup"));

        commitAndWaitForAsyncCompletion();

        userSession = CoreInstance.openCoreSession(session.getRepositoryName(), userManager.getPrincipal("user"));
    }

    @Override
    @After
    public void tearDown() {
        if (userSession != null) {
            ((CloseableCoreSession) userSession).close();
        }

        deleteGroup("grandParentGroup");
        deleteGroup("parentGroup");
        deleteGroup("group2");
        deleteGroup("group1");

        if (userManager.getUserModel("user") != null) {
            userManager.deleteUser("user");
        }
    }

    protected void createGroup(String name, List<String> members, List<String> subGroups) {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", name);
        if (members != null) {
            group.setPropertyValue("group:members", (Serializable) members);
        }
        if (subGroups != null) {
            group.setPropertyValue("group:subGroups", (Serializable) subGroups);
        }
        userManager.createGroup(group);
    }

    protected void deleteGroup(String name) {
        if (userManager.getGroup(name) != null) {
            userManager.deleteGroup(name);
        }
    }

    protected void updateGroup(String name, List<String> members, List<String> subGroups) {
        DocumentModel group = userManager.getGroupModel(name);
        if (group != null) {
            if (members != null) {
                group.setPropertyValue("group:members", (Serializable) members);
            }
            if (subGroups != null) {
                group.setPropertyValue("group:subGroups", (Serializable) subGroups);
            }
            userManager.updateGroup(group);
        }
    }

    /**
     * Tests changes on a group that has access to a synchronization root.
     */
    @Test
    public void testGroupChangesOnSyncRoot() throws Exception {
        DocumentModel syncRoot;
        try {
            syncRoot = session.createDocument(session.createDocumentModel("/", "syncRoot", "Folder"));
            log.trace("Grant ReadWrite to group1 on syncRoot");
            setPermissions(syncRoot, new ACE("group1", SecurityConstants.READ_WRITE));
            nuxeoDriveManager.registerSynchronizationRoot(userSession.getPrincipal(), syncRoot, userSession);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            List<FileSystemItemChange> changes = getChanges(userSession.getPrincipal());
            // Sync root creation and registration events
            assertEquals(2, changes.size());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        testGroupChanges(syncRoot, "defaultSyncRootFolderItemFactory", "group1", false);
    }

    /**
     * Tests changes on a group that has access to a child of a synchronization root.
     */
    @Test
    public void testGroupChangesOnSyncRootChild() throws Exception {
        DocumentModel child;
        try {
            DocumentModel syncRoot = session.createDocument(session.createDocumentModel("/", "syncRoot", "Folder"));
            child = session.createDocument(session.createDocumentModel("/syncRoot", "child", "Folder"));
            log.trace("Grant ReadWrite to group1 on syncRoot");
            setPermissions(syncRoot, new ACE("group1", SecurityConstants.READ_WRITE));
            log.trace("Block inheritance on child");
            setPermissions(child, ACE.BLOCK);
            log.trace("Grant ReadWrite to group2 on child");
            setPermissions(child, new ACE("group2", SecurityConstants.READ_WRITE));
            nuxeoDriveManager.registerSynchronizationRoot(userSession.getPrincipal(), syncRoot, userSession);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            List<FileSystemItemChange> changes = getChanges(userSession.getPrincipal());
            // Folder creation and sync root registration events
            assertEquals(3, changes.size());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        testGroupChanges(child, "defaultFileSystemItemFactory", "group2", false);
    }

    /**
     * Tests changes on a group that has access to the parent of a synchronization root.
     */
    @Test
    public void testGroupChangesOnSyncRootParent() throws Exception {
        DocumentModel syncRoot;
        try {
            DocumentModel parent = session.createDocument(session.createDocumentModel("/", "parent", "Folder"));
            syncRoot = session.createDocument(session.createDocumentModel("/parent", "syncRoot", "Folder"));
            log.trace("Grant ReadWrite to group1 on parent");
            setPermissions(parent, new ACE("group1", SecurityConstants.READ_WRITE));
            nuxeoDriveManager.registerSynchronizationRoot(userSession.getPrincipal(), syncRoot, userSession);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            List<FileSystemItemChange> changes = getChanges(userSession.getPrincipal());
            // Sync root creation and registration events
            assertEquals(2, changes.size());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        testGroupChanges(syncRoot, "defaultSyncRootFolderItemFactory", "group1", false);
    }

    /**
     * Tests changes on the parent group of a group that has access to a synchronization root.
     */
    @Test
    public void testChangesWithParentGroup() throws Exception {
        testGroupChangesWithAncestorGroups("parentGroup");
    }

    /**
     * Tests changes on the grandparent group of a group that has access to a synchronization root.
     */
    @Test
    public void testChangesWithGrandParentGroup() throws Exception {
        testGroupChangesWithAncestorGroups("grandParentGroup");
    }

    /**
     * Tests changes on the given group that has access to the given document:
     * <ul>
     * <li>Remove the test user from the group</li>
     * <li>Add the test user to the group</li>
     * <li>Delete the group</li>
     * <li>Create the group including the test user</li>
     * </ul>
     */
    protected void testGroupChanges(DocumentModel doc, String factoryName, String groupName, boolean needsParentGroup)
            throws Exception {
        List<FileSystemItemChange> changes;
        try {
            log.trace("Remove user from {}", groupName);
            updateGroup(groupName, Collections.emptyList(), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            changes = getChanges(userManager.getPrincipal("user"));
            // securityUpdated on doc with a null FileSystemItem
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "securityUpdated", "test", "test#" + doc.getId()),
                    toSimpleFileSystemItemChange(change));
            assertNull(change.getFileSystemItem());
            assertNull(change.getFileSystemItemName());

            log.trace("Add user to {}", groupName);
            updateGroup(groupName, Arrays.asList("user"), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            changes = getChanges(userManager.getPrincipal("user"));
            // securityUpdated on doc with a non-null FileSystemItem
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "securityUpdated", "test",
                    factoryName + "#test#" + doc.getId()), toSimpleFileSystemItemChange(change));
            assertNotNull(change.getFileSystemItem());
            assertEquals(doc.getTitle(), change.getFileSystemItemName());

            log.trace("Delete {}", groupName);
            deleteGroup(groupName);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            changes = getChanges(userManager.getPrincipal("user"));
            // securityUpdated on doc with a null FileSystemItem
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "securityUpdated", "test", "test#" + doc.getId()),
                    toSimpleFileSystemItemChange(change));
            assertNull(change.getFileSystemItem());
            assertNull(change.getFileSystemItemName());

            log.trace("Create {}", groupName);
            createGroup(groupName, Arrays.asList("user"), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        if (needsParentGroup) {
            try {
                changes = getChanges(userManager.getPrincipal("user"));
                // No events since the newly created group has not been added yet as a subgroup of parentGroup
                assertTrue(changes.isEmpty());

                log.trace("Add {} as a subgroup of parentGroup", groupName);
                updateGroup("parentGroup", null, Arrays.asList(groupName));

            } finally {
                commitAndWaitForAsyncCompletion();
            }
        }
        try {
            changes = getChanges(userManager.getPrincipal("user"));
            // securityUpdated on doc with a non-null FileSystemItem
            assertEquals(1, changes.size());
            FileSystemItemChange change = changes.get(0);
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "securityUpdated", "test",
                    factoryName + "#test#" + doc.getId()), toSimpleFileSystemItemChange(change));
            assertNotNull(change.getFileSystemItem());
            assertEquals(doc.getTitle(), change.getFileSystemItemName());
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    /**
     * Tests changes on a descendant group of the given group that has access to a synchronization root.
     */
    protected void testGroupChangesWithAncestorGroups(String ancestorGroup) throws Exception {
        List<FileSystemItemChange> changes;
        DocumentModel syncRoot;
        try {
            syncRoot = session.createDocument(session.createDocumentModel("/", "syncRoot", "Folder"));
            log.trace("Grant ReadWrite to {} on syncRoot", ancestorGroup);
            setPermissions(syncRoot, new ACE(ancestorGroup, SecurityConstants.READ_WRITE));
            nuxeoDriveManager.registerSynchronizationRoot(userSession.getPrincipal(), syncRoot, userSession);
        } finally {
            commitAndWaitForAsyncCompletion();
        }
        try {
            changes = getChanges(userSession.getPrincipal());
            // Sync root creation and registration events
            assertEquals(2, changes.size());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        testGroupChanges(syncRoot, "defaultSyncRootFolderItemFactory", "group1", true);
    }

}
