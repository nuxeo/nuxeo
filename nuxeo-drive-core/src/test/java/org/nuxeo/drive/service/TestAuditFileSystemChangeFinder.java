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
package org.nuxeo.drive.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.impl.AuditDocumentChangeFinder;
import org.nuxeo.drive.service.impl.FileSystemChangeSummary;
import org.nuxeo.drive.service.impl.FileSystemItemChange;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test the {@link AuditDocumentChangeFinder}.
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
// We handle transaction start and commit manually to make it possible to have
// several consecutive transactions in a test method
@TransactionalConfig(autoStart = false)
@Deploy("org.nuxeo.drive.core")
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestAuditFileSystemChangeFinder {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected FileSystemChangeFinder changeFinder;

    protected long lastSuccessfulSync;

    protected Set<String> syncRootPaths;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    @Before
    public void init() throws Exception {

        changeFinder = new AuditDocumentChangeFinder();
        lastSuccessfulSync = Calendar.getInstance().getTimeInMillis();
        syncRootPaths = new HashSet<String>();
        Framework.getProperties().put("org.nuxeo.drive.document.change.limit",
                "10");

        dispose(session);
        TransactionHelper.startTransaction();
        folder1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));
        session.createDocument(session.createDocumentModel("/", "folder3",
                "Folder"));
        commitAndWaitForAsyncCompletion();
    }

    @Test
    public void testFindChanges() throws Exception {

        // No sync roots
        List<FileSystemItemChange> changes = getDocumentChanges();
        assertNotNull(changes);
        assertTrue(changes.isEmpty());

        // Sync roots but no changes
        syncRootPaths.add("/folder1");
        syncRootPaths.add("/folder2");
        changes = getDocumentChanges();
        assertTrue(changes.isEmpty());

        // Create 3 documents, only 2 in sync roots
        TransactionHelper.startTransaction();
        DocumentModel doc1 = session.createDocument(session.createDocumentModel(
                "/folder1", "doc1", "File"));
        Thread.sleep(1000);
        DocumentModel doc2 = session.createDocument(session.createDocumentModel(
                "/folder2", "doc2", "File"));
        DocumentModel doc3 = session.createDocument(session.createDocumentModel(
                "/folder3", "doc3", "File"));
        commitAndWaitForAsyncCompletion();

        changes = getDocumentChanges();
        assertEquals(2, changes.size());
        FileSystemItemChange docChange = changes.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentCreated", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc2", docChange.getDocPath());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = changes.get(1);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentCreated", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // No changes since last successful sync
        changes = getDocumentChanges();
        assertTrue(changes.isEmpty());

        // Update both synchronized documents and unsynchronize a root
        TransactionHelper.startTransaction();
        doc1.setPropertyValue("file:content", new StringBlob(
                "The content of file 1."));
        session.saveDocument(doc1);
        doc2.setPropertyValue("file:content", new StringBlob(
                "The content of file 2."));
        session.saveDocument(doc2);
        commitAndWaitForAsyncCompletion();

        syncRootPaths.remove("/folder2");
        changes = getDocumentChanges();
        assertEquals(1, changes.size());
        docChange = changes.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentModified", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // Delete a document
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        commitAndWaitForAsyncCompletion();

        changes = getDocumentChanges();
        assertEquals(1, changes.size());
        docChange = changes.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("lifecycle_transition_event", docChange.getEventId());
        assertEquals("deleted", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // Restore a deleted document and move a document in a newly
        // synchronized root
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "undelete");
        Thread.sleep(1000);
        session.move(doc3.getRef(), folder2.getRef(), null);
        commitAndWaitForAsyncCompletion();

        syncRootPaths.add("/folder2");
        changes = getDocumentChanges();
        assertEquals(2, changes.size());
        docChange = changes.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentMoved", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc3", docChange.getDocPath());
        assertEquals(doc3.getId(), docChange.getDocUuid());
        docChange = changes.get(1);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("lifecycle_transition_event", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // Too many changes
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        session.followTransition(doc2.getRef(), "delete");
        commitAndWaitForAsyncCompletion();

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit",
                "1");
        try {
            getDocumentChanges();
            fail("An exception of type TooManyDocumentChangesException should have been thrown since the document change limit is exceeded.");
        } catch (TooManyChangesException e) {
            // Expected
        }
    }

    @Test
    public void testGetChangeSummary() throws Exception {
        TransactionHelper.startTransaction();
        Principal admin = new NuxeoPrincipalImpl("Administrator");

        // No sync roots => shouldn't find any changes
        FileSystemChangeSummary changeSummary = getChangeSummary(admin);
        assertNotNull(changeSummary);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register sync roots => should find changes: the newly
        // synchronized root folders as they are updated by the synchronization
        // registration process
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder1,
                session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder2,
                session);
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        changeSummary = getChangeSummary(admin);
        assertEquals(2, changeSummary.getFileSystemChanges().size());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Create 3 documents, only 2 in sync roots => should find 2 changes
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        DocumentModel doc1 = session.createDocumentModel("/folder1", "doc1",
                "File");
        doc1.setPropertyValue("file:content", new StringBlob(
                "The content of file 1."));
        doc1 = session.createDocument(doc1);
        Thread.sleep(1000);
        DocumentModel doc2 = session.createDocumentModel("/folder2", "doc2",
                "File");
        doc2.setPropertyValue("file:content", new StringBlob(
                "The content of file 2."));
        doc2 = session.createDocument(doc2);
        session.createDocument(session.createDocumentModel("/folder3", "doc3",
                "File"));
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        changeSummary = getChangeSummary(admin);

        List<FileSystemItemChange> changes = changeSummary.getFileSystemChanges();
        assertEquals(2, changes.size());
        FileSystemItemChange docChange = changes.get(0);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentCreated", docChange.getEventId());
        // TODO: understand why the life cycle is not good
        // assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc2", docChange.getDocPath());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = changes.get(1);
        assertEquals("test", docChange.getRepositoryId());
        assertEquals("documentCreated", docChange.getEventId());
        // TODO: understand why the life cycle is not good
        // assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Create a document that should not be synchronized because not
        // adaptable as a FileSystemItem (not Folderish nor a BlobHolder with a
        // blob) => should not be considered as a change
        TransactionHelper.startTransaction();
        session.createDocument(session.createDocumentModel("/folder1",
                "notSynchronizableDoc", "NotSynchronizable"));
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        changeSummary = getChangeSummary(admin);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Create 2 documents in the same sync root: "/folder1" and 1 document
        // in another sync root => should find 2 changes for "/folder1"
        TransactionHelper.startTransaction();
        DocumentModel doc3 = session.createDocumentModel("/folder1", "doc3",
                "File");
        doc3.setPropertyValue("file:content", new StringBlob(
                "The content of file 3."));
        doc3 = session.createDocument(doc3);
        DocumentModel doc4 = session.createDocumentModel("/folder1", "doc4",
                "File");
        doc4.setPropertyValue("file:content", new StringBlob(
                "The content of file 4."));
        doc4 = session.createDocument(doc4);
        DocumentModel doc5 = session.createDocumentModel("/folder2", "doc5",
                "File");
        doc5.setPropertyValue("file:content", new StringBlob(
                "The content of file 5."));
        doc5 = session.createDocument(doc5);
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();
        changeSummary = getChangeSummary(admin);
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());
        assertEquals(3, changeSummary.getFileSystemChanges().size());

        // No changes since last successful sync
        changeSummary = getChangeSummary(admin);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Test too many changes
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        session.followTransition(doc2.getRef(), "delete");
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit",
                "1");
        changeSummary = getChangeSummary(admin);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.TRUE, changeSummary.getHasTooManyChanges());
        TransactionHelper.commitOrRollbackTransaction();
    }

    @Test
    public void testGetChangeSummaryOnRootDocuments() throws Exception {
        TransactionHelper.startTransaction();
        Principal admin = new NuxeoPrincipalImpl("Administrator");
        Principal otherUser = new NuxeoPrincipalImpl("some-other-user");

        // No root registered by default: no changes
        Set<IdRef> activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
        assertNotNull(activeRootRefs);
        assertTrue(activeRootRefs.isEmpty());

        FileSystemChangeSummary changeSummary = getChangeSummary(admin);
        assertNotNull(changeSummary);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register a root for someone else
        nuxeoDriveManager.registerSynchronizationRoot(otherUser.getName(), folder1,
                session);

        // Administrator does not see any change
        activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
        assertNotNull(activeRootRefs);
        assertTrue(activeRootRefs.isEmpty());

        changeSummary = getChangeSummary(admin);
        assertNotNull(changeSummary);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register a new sync root
        nuxeoDriveManager.registerSynchronizationRoot(admin.getName(), folder1,
                session);
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
        assertNotNull(activeRootRefs);
        assertEquals(1, activeRootRefs.size());
        assertEquals(folder1.getRef(), activeRootRefs.iterator().next());

        changeSummary = getChangeSummary(admin);
        assertNotNull(changeSummary);

        List<FileSystemItemChange> changes = changeSummary.getFileSystemChanges();
        assertEquals(1, changes.size());
        FileSystemItemChange fsItemChange = changes.get(0);
        // TODO: this should be detected has an filesystem item
        // creation rather than modification
        assertEquals("documentModified", fsItemChange.getEventId());
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + folder1.getId(),
                fsItemChange.getFileSystemItem().getId());

        // Test deletion of a root
        TransactionHelper.startTransaction();
        session.followTransition(folder1.getRef(), "delete");
        commitAndWaitForAsyncCompletion();
        TransactionHelper.startTransaction();

        // The root is no longer active
        activeRootRefs = nuxeoDriveManager.getSynchronizationRootReferences(session);
        assertNotNull(activeRootRefs);
        assertTrue(activeRootRefs.isEmpty());

        // The deletion of the root itself is mapped as filesystem
        // deletion event
        changeSummary = getChangeSummary(admin);
        changes = changeSummary.getFileSystemChanges();
        assertEquals(1, changes.size());
        fsItemChange = changes.get(0);
        assertEquals("deleted", fsItemChange.getEventId());
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + folder1.getId(),
                fsItemChange.getFileSystemItem().getId());

        // TODO: check root unregistration too
    }

    /**
     * Gets the document changes using the {@link AuditDocumentChangeFinder} and
     * updates the {@link #lastSuccessfulSync} date.
     * @throws ClientException
     */
    protected List<FileSystemItemChange> getDocumentChanges()
            throws InterruptedException, ClientException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        long syncDate = cal.getTimeInMillis();
        List<FileSystemItemChange> changes = changeFinder.getFileSystemChanges(
                session,
                syncRootPaths,
                lastSuccessfulSync,
                syncDate,
                Integer.parseInt(Framework.getProperty("org.nuxeo.drive.document.change.limit")));
        assertNotNull(changes);
        lastSuccessfulSync = syncDate;
        return changes;
    }

    /**
     * Gets the document changes summary for the given user's synchronization
     * roots using the {@link NuxeoDriveManager} and updates the
     * {@link #lastSuccessfulSync} date.
     */
    protected FileSystemChangeSummary getChangeSummary(Principal principal)
            throws ClientException, InterruptedException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getDocumentChangeSummary(
                principal, lastSuccessfulSync);
        assertNotNull(changeSummary);
        lastSuccessfulSync = changeSummary.getSyncDate();
        return changeSummary;
    }

    protected void commitAndWaitForAsyncCompletion() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        dispose(session);
        eventService.waitForAsyncCompletion();
    }

    protected void dispose(CoreSession session) throws Exception {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                Field field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
                field.setAccessible(true);
                session = (CoreSession) field.get(handler);
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new UnsupportedOperationException(
                    "Cannot dispose session of class " + session.getClass());
        }
        ((LocalSession) session).getSession().dispose();
    }
}
