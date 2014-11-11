/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventTransactionListener;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestSQLRepositoryJTAJCA extends TXSQLRepositoryTestCase {

    /**
     * Test that connection sharing allows use of several sessions at the same
     * time.
     */
    public void testSessionSharing() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            // no pooling conf available
            return;
        }

        Repository repo = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                REPOSITORY_NAME);
        assertEquals(1, repo.getActiveSessionsCount()); // 1 low level session

        CoreSession session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
        assertEquals(1, repo.getActiveSessionsCount());
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Document");
            doc = session.createDocument(doc);
            session.save();
            // check that this is immediately seen from other connection
            // (underlying ManagedConnection is the same)
            assertTrue(session2.exists(new PathRef("/doc")));
        } finally {
            closeSession(session2);
        }
        assertEquals(1, repo.getActiveSessionsCount());
    }

    /**
     * Test that a commit implicitly does a save.
     */
    public void testSaveOnCommit() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            // no pooling conf available
            return;
        }

        // first transaction
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Document");
        doc = session.createDocument(doc);
        // let commit do an implicit save
        TransactionHelper.commitOrRollbackTransaction(); // release cx
        TransactionHelper.startTransaction();
        openSession(); // reopen cx and hold open

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    CoreSession session2;
                    session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
                    try {
                        assertTrue(session2.exists(new PathRef("/doc")));
                    } finally {
                        closeSession(session2);
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();
    }

    /**
     * Test that the TransactionalCoreSessionWrapper does its job.
     */
    public void testRollbackOnException() throws Exception {
        if (!(database instanceof DatabaseH2)) {
            // no pooling conf available
            return;
        }

        assertTrue(TransactionHelper.isTransactionActive());
        try {
            session.getDocument(new PathRef("/nosuchdoc"));
            fail("Missing document should throw");
        } catch (Exception e) {
            // ok
        }
        // tx still active because CoreSession.getDocument is marked
        // @NoRollbackOnException
        assertTrue(TransactionHelper.isTransactionActive());
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction();
        openSession();
        assertTrue(TransactionHelper.isTransactionActive());
        DocumentModel doc = new DocumentModelImpl("/nosuchdir", "doc",
                "Document");
        try {
            session.createDocument(doc);
            fail("Missing parent should throw");
        } catch (Exception e) {
            // ok
        }
        // tx not active anymore because CoreSession.createDocument is not
        // marked @NoRollbackOnException
        assertTrue(TransactionHelper.isTransactionMarkedRollback());
    }

    protected static class HelperEventTransactionListener implements
            EventTransactionListener {
        public boolean committed;

        public void transactionStarted() {
        }

        public void transactionCommitted() {
            committed = true;
        }

        public void transactionRollbacked() {
        }
    }

    public void testAfterCompletion() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        HelperEventTransactionListener listener = new HelperEventTransactionListener();
        eventService.addTransactionListener(listener);
        assertFalse(listener.committed);
        TransactionHelper.commitOrRollbackTransaction();
        assertTrue(listener.committed);
    }

}
