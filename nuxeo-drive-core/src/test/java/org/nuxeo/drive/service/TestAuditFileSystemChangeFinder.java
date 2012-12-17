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
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
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

        // No sync roots => shouldn't find any changes
        FileSystemChangeSummary changeSummary = getChangeSummary("Administrator");
        assertNotNull(changeSummary);
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register sync roots => should find changes: the newly
        // synchronized root folders as they are updated by the synchronization
        // registration process
        // TODO: uncomment if needed or remove
        // TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder1,
                session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder2,
                session);
        // commitAndWaitForAsyncCompletion();

        changeSummary = getChangeSummary("Administrator");
        assertEquals(2, changeSummary.getFileSystemChanges().size());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Create 3 documents, only 2 in sync roots => should find 2 changes
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

        changeSummary = getChangeSummary("Administrator");

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

        changeSummary = getChangeSummary("Administrator");
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

        changeSummary = getFolderChangeSummary("/folder1");
        assertEquals(2, changeSummary.getFileSystemChanges().size());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // No changes since last successful sync
        changeSummary = getChangeSummary("Administrator");
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Too many changes
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        session.followTransition(doc2.getRef(), "delete");
        commitAndWaitForAsyncCompletion();

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit",
                "1");
        changeSummary = getChangeSummary("Administrator");
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.TRUE, changeSummary.getHasTooManyChanges());
    }

    /**
     * Gets the document changes using the {@link AuditDocumentChangeFinder} and
     * updates the {@link #lastSuccessfulSync} date.
     */
    protected List<FileSystemItemChange> getDocumentChanges()
            throws TooManyChangesException, InterruptedException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        long syncDate = cal.getTimeInMillis();
        List<FileSystemItemChange> changes = changeFinder.getFileSystemChanges(
                true,
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
    protected FileSystemChangeSummary getChangeSummary(String userName)
            throws ClientException, InterruptedException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getDocumentChangeSummary(
                true, userName, session, lastSuccessfulSync);
        assertNotNull(changeSummary);
        lastSuccessfulSync = changeSummary.getSyncDate();
        return changeSummary;
    }

    /**
     * Gets the document changes summary for the given folder using the
     * {@link NuxeoDriveManager} and updates the {@link #lastSuccessfulSync}
     * date.
     */
    protected FileSystemChangeSummary getFolderChangeSummary(
            String folderPath) throws ClientException, InterruptedException {
        // Wait 1 second as the audit change finder relies on steps of 1 second
        Thread.sleep(1000);
        FileSystemChangeSummary changeSummary = nuxeoDriveManager.getFolderChangeSummary(
                folderPath, session, lastSuccessfulSync);
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
