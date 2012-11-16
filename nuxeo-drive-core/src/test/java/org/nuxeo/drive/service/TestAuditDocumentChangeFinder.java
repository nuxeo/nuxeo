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
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.impl.AuditDocumentChangeFinder;
import org.nuxeo.drive.service.impl.DocumentChange;
import org.nuxeo.drive.service.impl.DocumentChangeSummary;
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
public class TestAuditDocumentChangeFinder {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected DocumentChangeFinder documentChangeFinder;

    protected long lastSuccessfulSync;

    protected Set<String> syncRootPaths;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    @Before
    public void init() throws Exception {

        documentChangeFinder = new AuditDocumentChangeFinder();
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
    public void testFindDocumentChanges() throws Exception {

        // No sync roots
        List<DocumentChange> docChanges = getDocumentChanges();
        assertNotNull(docChanges);
        assertTrue(docChanges.isEmpty());

        // Sync roots but no changes
        syncRootPaths.add("/folder1");
        syncRootPaths.add("/folder2");
        docChanges = getDocumentChanges();
        assertTrue(docChanges.isEmpty());

        // Create 3 documents, only 2 in sync roots
        TransactionHelper.startTransaction();
        DocumentModel doc1 = session.createDocument(session.createDocumentModel(
                "/folder1", "doc1", "File"));
        DocumentModel doc2 = session.createDocument(session.createDocumentModel(
                "/folder2", "doc2", "File"));
        DocumentModel doc3 = session.createDocument(session.createDocumentModel(
                "/folder3", "doc3", "File"));
        commitAndWaitForAsyncCompletion();

        docChanges = getDocumentChanges();
        assertEquals(2, docChanges.size());
        DocumentChange docChange = docChanges.get(0);
        assertEquals("documentCreated", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc2", docChange.getDocPath());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = docChanges.get(1);
        assertEquals("documentCreated", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // No changes since last successful sync
        docChanges = getDocumentChanges();
        assertTrue(docChanges.isEmpty());

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
        docChanges = getDocumentChanges();
        assertEquals(1, docChanges.size());
        docChange = docChanges.get(0);
        assertEquals("documentModified", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // Delete a document
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        commitAndWaitForAsyncCompletion();

        docChanges = getDocumentChanges();
        assertEquals(1, docChanges.size());
        docChange = docChanges.get(0);
        assertEquals("lifecycle_transition_event", docChange.getEventId());
        assertEquals("deleted", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        // Restore a deleted document and move a document in a newly
        // synchronized root
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "undelete");
        session.move(doc3.getRef(), folder2.getRef(), null);
        commitAndWaitForAsyncCompletion();

        syncRootPaths.add("/folder2");
        docChanges = getDocumentChanges();
        assertEquals(2, docChanges.size());
        docChange = docChanges.get(0);
        assertEquals("documentMoved", docChange.getEventId());
        assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc3", docChange.getDocPath());
        assertEquals(doc3.getId(), docChange.getDocUuid());
        docChange = docChanges.get(1);
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
        } catch (TooManyDocumentChangesException e) {
            // Expected
        }
    }

    @Test
    public void testGetDocumentChangeSummary() throws Exception {

        // No sync roots => shouldn't find any changes
        DocumentChangeSummary docChangeSummary = getDocumentChangeSummary("Administrator");
        assertNotNull(docChangeSummary);
        assertTrue(docChangeSummary.getDocumentChanges().isEmpty());
        assertTrue(docChangeSummary.getChangedDocModels().isEmpty());
        assertEquals("no_changes", docChangeSummary.getStatusCode());

        // Register sync roots => should find changes: the newly
        // synchronized root folders as they are updated by the synchronization
        // registration process
        // TODO: uncomment if not needed
        // TransactionHelper.startTransaction();
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder1,
                session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator", folder2,
                session);
        // commitAndWaitForAsyncCompletion();

        docChangeSummary = getDocumentChangeSummary("Administrator");
        assertEquals(2, docChangeSummary.getDocumentChanges().size());
        assertEquals(2, docChangeSummary.getChangedDocModels().size());
        assertEquals("found_changes", docChangeSummary.getStatusCode());

        // Create 3 documents, only 2 in sync roots => should find 2 changes
        TransactionHelper.startTransaction();
        DocumentModel doc1 = session.createDocument(session.createDocumentModel(
                "/folder1", "doc1", "File"));
        DocumentModel doc2 = session.createDocument(session.createDocumentModel(
                "/folder2", "doc2", "File"));
        session.createDocument(session.createDocumentModel("/folder3", "doc3",
                "File"));
        commitAndWaitForAsyncCompletion();

        docChangeSummary = getDocumentChangeSummary("Administrator");
        List<DocumentChange> docChanges = docChangeSummary.getDocumentChanges();
        assertEquals(2, docChanges.size());
        DocumentChange docChange = docChanges.get(0);
        assertEquals("documentCreated", docChange.getEventId());
        // TODO: understand why the life cycle is not good
        // assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder2/doc2", docChange.getDocPath());
        assertEquals(doc2.getId(), docChange.getDocUuid());
        docChange = docChanges.get(1);
        assertEquals("documentCreated", docChange.getEventId());
        // TODO: understand why the life cycle is not good
        // assertEquals("project", docChange.getDocLifeCycleState());
        assertEquals("/folder1/doc1", docChange.getDocPath());
        assertEquals(doc1.getId(), docChange.getDocUuid());

        Map<String, DocumentModel> changedDocModels = docChangeSummary.getChangedDocModels();
        assertEquals(2, changedDocModels.size());
        DocumentModel changedDoc = changedDocModels.get(doc1.getId());
        assertNotNull(changedDoc);
        assertEquals(doc1.getId(), changedDoc.getId());
        assertEquals("/folder1/doc1", changedDoc.getPathAsString());
        assertEquals("doc1", changedDoc.getName());
        assertEquals("doc1", changedDoc.getTitle());
        assertEquals("File", changedDoc.getType());
        assertEquals("project", changedDoc.getCurrentLifeCycleState());
        changedDoc = changedDocModels.get(doc2.getId());
        assertNotNull(changedDoc);
        assertEquals(doc2.getId(), changedDoc.getId());
        assertEquals("/folder2/doc2", changedDoc.getPathAsString());
        assertEquals("doc2", changedDoc.getName());
        assertEquals("doc2", changedDoc.getTitle());
        assertEquals("File", changedDoc.getType());
        assertEquals("project", changedDoc.getCurrentLifeCycleState());

        assertEquals("found_changes", docChangeSummary.getStatusCode());

        // No changes since last successful sync
        docChangeSummary = getDocumentChangeSummary("Administrator");
        assertTrue(docChangeSummary.getDocumentChanges().isEmpty());
        assertTrue(docChangeSummary.getChangedDocModels().isEmpty());
        assertEquals("no_changes", docChangeSummary.getStatusCode());

        // Too many changes
        TransactionHelper.startTransaction();
        session.followTransition(doc1.getRef(), "delete");
        session.followTransition(doc2.getRef(), "delete");
        commitAndWaitForAsyncCompletion();

        Framework.getProperties().put("org.nuxeo.drive.document.change.limit",
                "1");
        docChangeSummary = getDocumentChangeSummary("Administrator");
        assertTrue(docChangeSummary.getDocumentChanges().isEmpty());
        assertTrue(docChangeSummary.getChangedDocModels().isEmpty());
        assertEquals("too_many_changes", docChangeSummary.getStatusCode());
    }

    /**
     * Gets the document changes using the {@link AuditDocumentChangeFinder} and
     * updates the {@link #lastSuccessfulSync} date.
     */
    protected List<DocumentChange> getDocumentChanges()
            throws TooManyDocumentChangesException {
        List<DocumentChange> docChanges = documentChangeFinder.getDocumentChanges(
                session,
                syncRootPaths,
                lastSuccessfulSync,
                Integer.parseInt(Framework.getProperty("org.nuxeo.drive.document.change.limit")));
        assertNotNull(docChanges);
        lastSuccessfulSync = System.currentTimeMillis();
        return docChanges;

    }

    /**
     * Gets the document changes summary using the {@link NuxeoDriveManager} and
     * updates the {@link #lastSuccessfulSync} date.
     */
    protected DocumentChangeSummary getDocumentChangeSummary(String userName)
            throws ClientException {
        DocumentChangeSummary docChangeSummary = nuxeoDriveManager.getDocumentChangeSummary(
                userName, session, lastSuccessfulSync);
        assertNotNull(docChangeSummary);
        lastSuccessfulSync = docChangeSummary.getSyncDate();
        return docChangeSummary;
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
