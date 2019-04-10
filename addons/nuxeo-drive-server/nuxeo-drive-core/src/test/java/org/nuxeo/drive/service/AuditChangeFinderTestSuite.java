/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests the {@link FileSystemChangeFinder}.
 *
 * @since 8.2
 */
@Deploy("org.nuxeo.ecm.platform.web.common")
public class AuditChangeFinderTestSuite extends AbstractChangeFinderTestCase {

    private static final Log log = LogFactory.getLog(AuditChangeFinderTestSuite.class);

    @Inject
    protected CollectionManager collectionManager;

    @Test
    public void testFindChanges() throws Exception {
        List<FileSystemItemChange> changes;
        DocumentModel doc1;
        DocumentModel doc2;
        DocumentModel doc3;
        DocumentModel docToCopy;
        DocumentModel copiedDoc;
        DocumentModel docToVersion;

        commitAndWaitForAsyncCompletion();
        try {
            // No sync roots
            changes = getChanges();
            assertNotNull(changes);
            assertTrue(changes.isEmpty());

            log.trace("Sync roots for Administrator");
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Get changes for Administrator
            changes = getChanges();
            // Root registration events
            assertEquals(2, changes.size());

            log.trace("Create 3 documents, only 2 in sync roots");
            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1."));
            doc1 = session.createDocument(doc1);
            doc2 = session.createDocumentModel("/folder2", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2."));
            doc2 = session.createDocument(doc2);
            doc3 = session.createDocumentModel("/folder3", "doc3", "File");
            doc3.setPropertyValue("file:content", new StringBlob("The content of file 3."));
            doc3 = session.createDocument(doc3);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(doc2.getId(), "documentCreated", "test"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "documentCreated", "test"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            // No changes since last successful sync
            changes = getChanges();
            assertTrue(changes.isEmpty());

            log.trace("Update both synchronized documents and unsynchronize a root");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1, updated."));
            session.saveDocument(doc1);
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2, updated."));
            session.saveDocument(doc2);
            nuxeoDriveManager.unregisterSynchronizationRoot(session.getPrincipal(), folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(2, changes.size());
            // The root unregistration is mapped to a fake deletion from the
            // client's point of view
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "deleted", "test"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "documentModified", "test"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Delete a document with a lifecycle transition (trash)");
            session.followTransition(doc1.getRef(), "delete");
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc1.getId(), "deleted", "test", "test#" + doc1.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Restore a deleted document and move a document in a newly synchronized root");
            session.followTransition(doc1.getRef(), "undelete");
            session.move(doc3.getRef(), folder2.getRef(), null);
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(3, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder2.getId()));
            expectedChanges.add(new SimpleFileSystemItemChange(doc3.getId(), "documentMoved", "test"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "lifecycle_transition_event", "test"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Physical deletion without triggering the delete transition first");
            session.removeDocument(doc3.getRef());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc3.getId(), "deleted", "test", "test#" + doc3.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Create a doc and copy it from a sync root to another one");
            docToCopy = session.createDocumentModel("/folder1", "docToCopy", "File");
            docToCopy.setPropertyValue("file:content", new StringBlob("The content of file to copy."));
            docToCopy = session.createDocument(docToCopy);
            copiedDoc = session.copy(docToCopy.getRef(), folder2.getRef(), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(copiedDoc.getId(), "documentCreatedByCopy", "test",
                    "defaultFileSystemItemFactory#test#" + copiedDoc.getId(), "docToCopy"));
            expectedChanges.add(new SimpleFileSystemItemChange(docToCopy.getId(), "documentCreated", "test",
                    "defaultFileSystemItemFactory#test#" + docToCopy.getId(), "docToCopy"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Remove file from a document, mapped to a fake deletion from the client's point of view");
            doc1.setPropertyValue("file:content", null);
            session.saveDocument(doc1);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc1.getId(), "deleted", "test"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Move a doc from a sync root to another sync root");
            session.move(copiedDoc.getRef(), folder1.getRef(), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(copiedDoc.getId(), "documentMoved", "test"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Move a doc from a sync root to a non synchronized folder");
            session.move(copiedDoc.getRef(), folder3.getRef(), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(copiedDoc.getId(), "deleted", "test"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Create a doc, create a version of it, update doc and restore the version");
            docToVersion = session.createDocumentModel("/folder1", "docToVersion", "File");
            docToVersion.setPropertyValue("file:content", new StringBlob("The content of file to version."));
            docToVersion = session.createDocument(docToVersion);
            docToVersion.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
            session.saveDocument(docToVersion);
            docToVersion.setPropertyValue("file:content", new StringBlob("Updated content of the versioned file."));
            session.saveDocument(docToVersion);
            List<DocumentModel> versions = session.getVersions(docToVersion.getRef());
            assertEquals(1, versions.size());
            DocumentModel version = versions.get(0);
            session.restoreToVersion(docToVersion.getRef(), version.getRef());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges();
            // Expecting 4 (among which 3 distinct) changes:
            // - documentRestored for docToVersion
            // - documentModified for docToVersion (2 occurrences)
            // - documentCreated for docToVersion
            assertEquals(4, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(docToVersion.getId(), "documentRestored"));
            expectedChanges.add(new SimpleFileSystemItemChange(docToVersion.getId(), "documentModified"));
            expectedChanges.add(new SimpleFileSystemItemChange(docToVersion.getId(), "documentCreated"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Too many changes");
            session.followTransition(doc1.getRef(), "delete");
            session.followTransition(doc2.getRef(), "delete");
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit", "1");
        FileSystemChangeSummary changeSummary = getChangeSummary(session.getPrincipal());
        assertEquals(true, changeSummary.getHasTooManyChanges());
    }

    @Test
    public void testFindSecurityChanges() throws Exception {
        List<FileSystemItemChange> changes;
        DocumentModel subFolder;

        try {
            // No sync roots
            changes = getChanges();
            assertTrue(changes.isEmpty());

            // Create a folder in a sync root
            subFolder = user1Session.createDocumentModel("/folder1", "subFolder", "Folder");
            subFolder = user1Session.createDocument(subFolder);

            // Sync roots for user1
            nuxeoDriveManager.registerSynchronizationRoot(user1Session.getPrincipal(), folder1, user1Session);
            nuxeoDriveManager.registerSynchronizationRoot(user1Session.getPrincipal(), folder2, user1Session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Get changes for user1
            changes = getChanges(user1Session.getPrincipal());
            // Folder creation and sync root registration events
            assertEquals(3, changes.size());

            // Permission changes: deny Read
            // Deny Read to user1 on a regular doc
            setPermissions(subFolder, new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING), ACE.BLOCK);
            // Deny Read to user1 on a sync root
            setPermissions(folder2, new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING), ACE.BLOCK);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changes = getChanges(user1Session.getPrincipal());
            assertEquals(2, changes.size());

            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "securityUpdated", "test", "test#"
                    + folder2.getId(), "folder2"));
            expectedChanges.add(new SimpleFileSystemItemChange(subFolder.getId(), "securityUpdated", "test", "test#"
                    + subFolder.getId(), "subFolder"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
            // Changed documents are not adaptable as a FileSystemItem since no Read permission
            for (FileSystemItemChange change : changes) {
                assertNull(change.getFileSystemItem());
            }

            // Permission changes: grant Read
            // Grant Read to user1 on a regular doc
            setPermissions(subFolder, new ACE("user1", SecurityConstants.READ));
            // Grant Read to user1 on a sync root
            setPermissions(folder2, new ACE("user1", SecurityConstants.READ));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        changes = getChanges(user1Session.getPrincipal());
        assertEquals(2, changes.size());
        Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
        expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "securityUpdated", "test",
                "defaultSyncRootFolderItemFactory#test#" + folder2.getId(), "folder2"));
        expectedChanges.add(new SimpleFileSystemItemChange(subFolder.getId(), "securityUpdated", "test",
                "defaultFileSystemItemFactory#test#" + subFolder.getId(), "subFolder"));
        assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
        // Changed documents are adaptable as a FileSystemItem since Read permission
        for (FileSystemItemChange change : changes) {
            assertNotNull(change.getFileSystemItem());
        }
    }

    @Test
    public void testGetChangeSummary() throws Exception {
        FileSystemChangeSummary changeSummary;
        Principal admin = new NuxeoPrincipalImpl("Administrator");
        DocumentModel doc1;
        DocumentModel doc2;

        try {
            // No sync roots => shouldn't find any changes
            changeSummary = getChangeSummary(admin);
            assertNotNull(changeSummary);
            assertTrue(changeSummary.getFileSystemChanges().isEmpty());
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

            // Register sync roots => should find changes: the newly
            // synchronized root folders as they are updated by the
            // synchronization
            // registration process
            nuxeoDriveManager.registerSynchronizationRoot(admin, folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(admin, folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changeSummary = getChangeSummary(admin);
            assertEquals(2, changeSummary.getFileSystemChanges().size());
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // Create 3 documents, only 2 in sync roots => should find 2 changes
        try {
            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1."));
            doc1 = session.createDocument(doc1);
            doc2 = session.createDocumentModel("/folder2", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2."));
            doc2 = session.createDocument(doc2);
            session.createDocument(session.createDocumentModel("/folder3", "doc3", "File"));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changeSummary = getChangeSummary(admin);

            List<FileSystemItemChange> changes = changeSummary.getFileSystemChanges();
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            SimpleFileSystemItemChange simpleChange = new SimpleFileSystemItemChange(doc2.getId(), "documentCreated",
                    "test");
            simpleChange.setLifeCycleState("project");
            expectedChanges.add(simpleChange);
            simpleChange = new SimpleFileSystemItemChange(doc1.getId(), "documentCreated", "test");
            simpleChange.setLifeCycleState("project");
            expectedChanges.add(simpleChange);
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

            // Create a document that should not be synchronized because not
            // adaptable as a FileSystemItem (not Folderish nor a BlobHolder
            // with a
            // blob) => should not be considered as a change
            session.createDocument(session.createDocumentModel("/folder1", "notSynchronizableDoc", "NotSynchronizable"));
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changeSummary = getChangeSummary(admin);
            assertTrue(changeSummary.getFileSystemChanges().isEmpty());
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

            // Create 2 documents in the same sync root: "/folder1" and 1 document in another sync root => should find 2
            // changes for "/folder1"
            DocumentModel doc3 = session.createDocumentModel("/folder1", "doc3", "File");
            doc3.setPropertyValue("file:content", new StringBlob("The content of file 3."));
            doc3 = session.createDocument(doc3);
            DocumentModel doc4 = session.createDocumentModel("/folder1", "doc4", "File");
            doc4.setPropertyValue("file:content", new StringBlob("The content of file 4."));
            doc4 = session.createDocument(doc4);
            DocumentModel doc5 = session.createDocumentModel("/folder2", "doc5", "File");
            doc5.setPropertyValue("file:content", new StringBlob("The content of file 5."));
            doc5 = session.createDocument(doc5);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            changeSummary = getChangeSummary(admin);
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());
            assertEquals(3, changeSummary.getFileSystemChanges().size());

            // No changes since last successful sync
            changeSummary = getChangeSummary(admin);
            assertTrue(changeSummary.getFileSystemChanges().isEmpty());
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

            // Test too many changes
            session.followTransition(doc1.getRef(), "delete");
            session.followTransition(doc2.getRef(), "delete");
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit", "1");
        changeSummary = getChangeSummary(admin);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.TRUE, changeSummary.getHasTooManyChanges());
    }

    @Test
    public void testGetChangeSummaryOnRootDocuments() throws Exception {
        Principal admin = new NuxeoPrincipalImpl("Administrator");
        Principal otherUser = new NuxeoPrincipalImpl("some-other-user");
        Set<IdRef> activeRootRefs;
        FileSystemChangeSummary changeSummary;
        List<FileSystemItemChange> changes;

        try {
            // No root registered by default: no changes
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());

            changeSummary = getChangeSummary(admin);
            assertNotNull(changeSummary);
            assertTrue(changeSummary.getFileSystemChanges().isEmpty());
            assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

            // Register a root for someone else
            nuxeoDriveManager.registerSynchronizationRoot(otherUser, folder1, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Administrator does not see any change
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());

            changeSummary = getChangeSummary(admin);
            assertNotNull(changeSummary);
            assertTrue(changeSummary.getFileSystemChanges().isEmpty());
            assertFalse(changeSummary.getHasTooManyChanges());

            // Register a new sync root
            nuxeoDriveManager.registerSynchronizationRoot(admin, folder1, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertEquals(1, activeRootRefs.size());
            assertEquals(folder1.getRef(), activeRootRefs.iterator().next());

            // The new sync root is detected in the change summary
            changeSummary = getChangeSummary(admin);
            assertNotNull(changeSummary);
            changes = changeSummary.getFileSystemChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));

            // Check that root unregistration is detected as a deletion
            nuxeoDriveManager.unregisterSynchronizationRoot(admin, folder1, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());
            changeSummary = getChangeSummary(admin);
            changes = changeSummary.getFileSystemChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(folder1.getId(), "deleted", "test", "test#" + folder1.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));

            // Register back the root, it's activity is again detected by the
            // client
            nuxeoDriveManager.registerSynchronizationRoot(admin, folder1, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertEquals(activeRootRefs.size(), 1);

            changeSummary = getChangeSummary(admin);
            changes = changeSummary.getFileSystemChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));

            // Test deletion of a root
            session.followTransition(folder1.getRef(), "delete");
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());

            // The root is no longer active
            activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());

            // The deletion of the root itself is mapped as filesystem
            // deletion event
            changeSummary = getChangeSummary(admin);
            changes = changeSummary.getFileSystemChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(folder1.getId(), "deleted", "test", "test#" + folder1.getId()),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testSyncUnsyncRootsAsAnotherUser() throws Exception {
        Principal user1Principal = user1Session.getPrincipal();
        List<FileSystemItemChange> changes;

        try {
            // No sync roots expected for user1
            changes = getChanges(user1Principal);
            assertNotNull(changes);
            assertTrue(changes.isEmpty());

            // Register sync roots for user1 as Administrator
            nuxeoDriveManager.registerSynchronizationRoot(user1Principal, folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(user1Principal, folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // user1 should have 2 sync roots
            Set<IdRef> activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(user1Session);
            assertNotNull(activeRootRefs);
            assertEquals(2, activeRootRefs.size());
            assertTrue(activeRootRefs.contains(folder1.getRef()));
            assertTrue(activeRootRefs.contains(folder2.getRef()));

            // There should be 2 changes detected in the audit
            changes = getChanges(user1Principal);
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder2.getId(), "folder2"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId(), "folder1"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
            for (FileSystemItemChange change : changes) {
                assertNotNull(change.getFileSystemItem());
            }

            // Unregister sync roots for user1 as Administrator
            nuxeoDriveManager.unregisterSynchronizationRoot(user1Principal, folder1, session);
            nuxeoDriveManager.unregisterSynchronizationRoot(user1Principal, folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // user1 should have no sync roots
            Set<IdRef> activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(user1Session);
            assertNotNull(activeRootRefs);
            assertTrue(activeRootRefs.isEmpty());

            // There should be 2 changes detected in the audit
            changes = getChanges(user1Principal);
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder2.getId(), "deleted", "test", "test#"
                    + folder2.getId(), "folder2"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "deleted", "test", "test#"
                    + folder1.getId(), "folder1"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
            // Not adaptable as a FileSystemItem since unregistered
            for (FileSystemItemChange change : changes) {
                assertNull(change.getFileSystemItem());
            }
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testRegisterSyncRootAndUpdate() throws Exception {

        try {
            // Register folder1 as a sync root for Administrator
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder1, session);

            // Update folder1 title
            folder1.setPropertyValue("dc:title", "folder1 updated");
            session.saveDocument(folder1);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 3:
            // - documentModified
            // - rootRegistered
            // - documentCreated (at init)
            List<FileSystemItemChange> changes = getChanges();
            assertEquals(3, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "documentModified"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "documentCreated"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            // Unregister folder1 as a sync root for Administrator
            nuxeoDriveManager.unregisterSynchronizationRoot(session.getPrincipal(), folder1, session);

            // Update folder1 title
            folder1.setPropertyValue("dc:title", "folder1 updated twice");
            session.saveDocument(folder1);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 1: deleted
            List<FileSystemItemChange> changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(folder1.getId(), "deleted"),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testMoveToOtherUsersSyncRoot() throws Exception {
        DocumentModel subFolder;
        List<FileSystemItemChange> changes;
        try {
            // Create a subfolder in folder1 as Administrator
            subFolder = session.createDocument(session.createDocumentModel(folder1.getPathAsString(), "subFolder",
                    "Folder"));
            // Register folder1 as a sync root for user1
            nuxeoDriveManager.registerSynchronizationRoot(user1Session.getPrincipal(), folder1, user1Session);
            // Register folder2 as a sync root for Administrator
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder2, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes for user1, expecting 3:
            // - rootRegistered for folder1
            // - documentCreated for subFolder1
            // - documentCreated for folder1 at init
            changes = getChanges(user1Session.getPrincipal());
            assertEquals(3, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered"));
            expectedChanges.add(new SimpleFileSystemItemChange(subFolder.getId(), "documentCreated"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "documentCreated"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            // As Administrator, move subfolder from folder1 (sync root for
            // user1) to folder2 (sync root for Administrator but not for
            // user1)
            session.move(subFolder.getRef(), folder2.getRef(), null);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes for user1, expecting 1: deleted for subFolder
            changes = getChanges(user1Session.getPrincipal());
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(subFolder.getId(), "deleted"),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testCollectionEvents() throws Exception {
        DocumentModel doc1;
        DocumentModel doc2;
        DocumentModel doc3;
        List<FileSystemItemChange> changes;
        DocumentModel locallyEditedCollection;
        try {
            log.trace("Create 2 test docs and them to the 'Locally Edited' collection");
            doc1 = session.createDocumentModel(folder1.getPathAsString(), "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("File content."));
            doc1 = session.createDocument(doc1);
            doc2 = session.createDocumentModel(folder1.getPathAsString(), "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("File content."));
            doc2 = session.createDocument(doc2);
            nuxeoDriveManager.addToLocallyEditedCollection(session, doc1);
            nuxeoDriveManager.addToLocallyEditedCollection(session, doc2);
            DocumentModel userCollections = collectionManager.getUserDefaultCollections(folder1, session);
            DocumentRef locallyEditedCollectionRef = new PathRef(userCollections.getPath().toString(),
                    NuxeoDriveManager.LOCALLY_EDITED_COLLECTION_NAME);
            locallyEditedCollection = session.getDocument(locallyEditedCollectionRef);
            // Re-fetch documents to get rid of the disabled events in context
            // data
            doc1 = session.getDocument(doc1.getRef());
            doc2 = session.getDocument(doc2.getRef());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 8 (among which 7 distinct) changes:
            // - addedToCollection for doc2
            // - documentModified for 'Locally Edited' collection (2 occurrences)
            // - rootRegistered for 'Locally Edited' collection
            // - addedToCollection for doc1
            // - documentCreated for 'Locally Edited' collection
            // - documentCreated for doc2
            // - documentCreated for doc1
            changes = getChanges(session.getPrincipal());
            assertEquals(8, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(doc2.getId(), "addedToCollection"));
            expectedChanges.add(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "documentModified"));
            expectedChanges.add(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "rootRegistered"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "addedToCollection"));
            expectedChanges.add(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "documentCreated"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc2.getId(), "documentCreated"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "documentCreated"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Update doc1 member of the 'Locally Edited' collection");
            doc1.setPropertyValue("file:content", new StringBlob("Updated file content."));
            session.saveDocument(doc1);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 1 change: documentModified for doc1
            changes = getChanges(session.getPrincipal());
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc1.getId(), "documentModified"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Remove doc1 from the 'Locally Edited' collection, delete doc2 and add doc 3 to the collection");
            collectionManager.removeFromCollection(locallyEditedCollection, doc1, session);
            doc2.followTransition(LifeCycleConstants.DELETE_TRANSITION);
            doc3 = session.createDocumentModel(folder1.getPathAsString(), "doc3", "File");
            doc3.setPropertyValue("file:content", new StringBlob("File content."));
            doc3 = session.createDocument(doc3);
            collectionManager.addToCollection(locallyEditedCollection, doc3, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 6 (among which 5 distinct) changes:
            // - addedToCollection for doc3
            // - documentModified for 'Locally Edited' collection (2 occurrences)
            // - documentCreated for doc3
            // - deleted for doc2
            // - deleted for doc1
            changes = getChanges(session.getPrincipal());
            assertEquals(6, changes.size());
            List<SimpleFileSystemItemChange> expectedChanges = new ArrayList<>();
            expectedChanges.add(new SimpleFileSystemItemChange(doc3.getId(), "addedToCollection"));
            expectedChanges.add(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "documentModified"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc3.getId(), "documentCreated"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc2.getId(), "deleted"));
            expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "deleted"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            log.trace("Unregister the 'Locally Edited' collection as a sync root");
            nuxeoDriveManager.unregisterSynchronizationRoot(session.getPrincipal(), locallyEditedCollection, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 1 change: deleted for 'Locally Edited' collection
            changes = getChanges(session.getPrincipal());
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "deleted"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Register the 'Locally Edited' collection back as a sync root");
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), locallyEditedCollection, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 1 change: rootRegistered for 'Locally Edited'
            // collection
            changes = getChanges(session.getPrincipal());
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "rootRegistered"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Delete the 'Locally Edited' collection");
            locallyEditedCollection.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Expecting 1 change: deleted for 'Locally Edited' collection
            changes = getChanges(session.getPrincipal());
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(locallyEditedCollection.getId(), "deleted"),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testRegisterParentSyncRoot() throws Exception {
        DocumentModel subFolder;
        List<FileSystemItemChange> changes;
        try {
            // Create a subfolder in folder1
            subFolder = session.createDocument(session.createDocumentModel(folder1.getPathAsString(), "subFolder",
                    "Folder"));
            // Register subfolder as a sync root
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), subFolder, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 2:
            // - rootRegistered for subfolder
            // - documentCreated for subFolder
            changes = getChanges(session.getPrincipal());
            assertEquals(2, changes.size());

            // Register folder1 as a sync root
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder1, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 2:
            // - rootRegistered for folder1
            // - deleted for subFolder
            changes = getChanges(session.getPrincipal());
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId(), "folder1"));
            expectedChanges.add(new SimpleFileSystemItemChange(subFolder.getId(), "deleted", "test", "test#"
                    + subFolder.getId(), "subFolder"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testSection() throws Exception {
        DocumentModel section;
        DocumentModel doc1;
        DocumentModel proxy1;
        DocumentModel proxy2;
        List<FileSystemItemChange> changes;
        try {
            // Create a Section and register it as a synchronization root
            section = session.createDocument(session.createDocumentModel("/", "sectionSyncRoot", "Section"));
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), section, session);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 2:
            // - rootRegistered for section
            // - documentCreated for section
            changes = getChanges(session.getPrincipal());
            assertEquals(2, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(section.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + section.getId(), "sectionSyncRoot"));
            expectedChanges.add(new SimpleFileSystemItemChange(section.getId(), "documentCreated", "test",
                    "defaultSyncRootFolderItemFactory#test#" + section.getId(), "sectionSyncRoot"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            // Publish 2 documents in the section
            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1."));
            doc1 = session.createDocument(doc1);
            proxy1 = session.publishDocument(doc1, section);
            DocumentModel doc2 = session.createDocumentModel("/folder1", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2."));
            doc2 = session.createDocument(doc2);
            proxy2 = session.publishDocument(doc2, section);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 4:
            // - documentProxyPublished for proxy2
            // - documentCreated for proxy2
            // - documentProxyPublished for proxy1
            // - documentCreated for proxy1
            changes = getChanges();
            assertEquals(4, changes.size());
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<SimpleFileSystemItemChange>();
            expectedChanges.add(new SimpleFileSystemItemChange(proxy2.getId(), "documentProxyPublished", "test",
                    "defaultFileSystemItemFactory#test#" + proxy2.getId(), "doc2"));
            expectedChanges.add(new SimpleFileSystemItemChange(proxy2.getId(), "documentCreated", "test",
                    "defaultFileSystemItemFactory#test#" + proxy2.getId(), "doc2"));
            expectedChanges.add(new SimpleFileSystemItemChange(proxy1.getId(), "documentProxyPublished", "test",
                    "defaultFileSystemItemFactory#test#" + proxy1.getId(), "doc1"));
            expectedChanges.add(new SimpleFileSystemItemChange(proxy1.getId(), "documentCreated", "test",
                    "defaultFileSystemItemFactory#test#" + proxy1.getId(), "doc1"));
            assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));

            // Update an existing proxy
            doc1.setPropertyValue("file:content", new StringBlob("The updated content of file 1."));
            session.saveDocument(doc1);
            session.publishDocument(doc1, section);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 1:
            // - documentProxyPublished for proxy1
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(proxy1.getId(), "documentProxyPublished", "test",
                    "defaultFileSystemItemFactory#test#" + proxy1.getId(), "doc1"),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

    @Test
    public void testLockUnlock() throws Exception {
        DocumentModel doc;
        List<FileSystemItemChange> changes;
        try {
            log.trace("Register a sync root and create a document inside it");
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder1, session);
            doc = session.createDocumentModel("/folder1", "doc", "File");
            doc.setPropertyValue("file:content", new StringBlob("The file content"));
            doc = session.createDocument(doc);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 3:
            // - documentCreated for doc
            // - rootRegistered for folder1
            // - documentCreated for folder1
            changes = getChanges(session.getPrincipal());
            assertEquals(3, changes.size());

            log.trace("Lock doc");
            session.setLock(doc.getRef());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 1:
            // - documentLocked for doc
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "documentLocked", "test",
                    "defaultFileSystemItemFactory#test#" + doc.getId(), "doc"),
                    toSimpleFileSystemItemChange(changes.get(0)));

            log.trace("Unlock doc");
            session.removeLock(doc.getRef());
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // Check changes, expecting 1:
            // - documentUnlocked for doc
            changes = getChanges();
            assertEquals(1, changes.size());
            assertEquals(new SimpleFileSystemItemChange(doc.getId(), "documentUnlocked", "test",
                    "defaultFileSystemItemFactory#test#" + doc.getId(), "doc"),
                    toSimpleFileSystemItemChange(changes.get(0)));
        } finally {
            commitAndWaitForAsyncCompletion();
        }
    }

}
