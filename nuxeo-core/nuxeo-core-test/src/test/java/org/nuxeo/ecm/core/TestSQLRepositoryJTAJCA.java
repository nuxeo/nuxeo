/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyAsyncRetryListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
public class TestSQLRepositoryJTAJCA {

    private static final Log log = LogFactory.getLog(TestSQLRepositoryJTAJCA.class);

    @SuppressWarnings("deprecation")
    private static final String ADMINISTRATOR = SecurityConstants.ADMINISTRATOR;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected RepositoryService repositoryService;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    /**
     * Test that connection sharing allows use of several sessions at the same time.
     */
    @Test
    public void testSessionSharing() {
        String repositoryName = session.getRepositoryName();
        Repository repo = repositoryService.getRepository(repositoryName);
        session.getRootDocument(); // use the session at least once
        assertEquals(1, repo.getActiveSessionsCount());

        try (CloseableCoreSession session2 = CoreInstance.openCoreSession(repositoryName, ADMINISTRATOR)) {
            assertEquals(1, repo.getActiveSessionsCount());
            DocumentModel doc = session.createDocumentModel("/", "doc", "Document");
            doc = session.createDocument(doc);
            session.save();
            // check that this is immediately seen from other connection
            // (underlying ManagedConnection is the same)
            assertTrue(session2.exists(new PathRef("/doc")));
        }
        assertEquals(1, repo.getActiveSessionsCount());
    }

    /**
     * Test that a commit implicitly does a save.
     */
    @Test
    public void testSaveOnCommit() throws Exception {
        // first transaction
        DocumentModel doc = session.createDocumentModel("/", "doc", "Document");
        doc = session.createDocument(doc);
        // let commit do an implicit save
        nextTransaction();

        Thread t = new Thread(() -> {
            try {
                TransactionHelper.startTransaction();
                try (CloseableCoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), ADMINISTRATOR)) {
                    assertTrue(session2.exists(new PathRef("/doc")));
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } catch (Exception e) {
                fail(e.toString());
            }
        });
        t.start();
        t.join();
    }

    /**
     * Cannot use session after close if no tx.
     */
    @Test
    public void testAccessWithoutTx() {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            session.getRootDocument();
            fail("should throw");
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Cannot use a CoreSession outside a transaction"));
        }
        TransactionHelper.startTransaction();
    }

    /**
     * Testing that if 2 modifications are done at the same time on the same document on 2 separate transactions, one is
     * rejected (TransactionRuntimeException)
     */
    // not working as is
    @Ignore
    @Test
    public void testConcurrentModification() throws Exception {
        // first transaction
        DocumentModel doc = session.createDocumentModel("/", "doc", "Note");
        doc.getProperty("dc:title").setValue("initial");
        doc = session.createDocument(doc);
        // let commit do an implicit save
        nextTransaction();
        // release cx
        coreFeature.releaseCoreSession();

        final DocumentRef ref = new PathRef("/doc");
        TransactionHelper.startTransaction();
        // openSession();
        doc = session.getDocument(ref);
        doc.getProperty("dc:title").setValue("first");
        session.saveDocument(doc);
        Thread t = new Thread(() -> {
            try {
                TransactionHelper.startTransaction();
                try (CloseableCoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), ADMINISTRATOR)) {
                    DocumentModel doc1 = session2.getDocument(ref);
                    doc1.getProperty("dc:title").setValue("second update");
                    session2.saveDocument(doc1);
                } catch (Exception e) {
                    log.error("Catched error while setting title", e);
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } catch (Exception e) {
                fail(e.toString());
            }
        });
        t.start();
        t.join();
        try {
            TransactionHelper.commitOrRollbackTransaction(); // release cx
            fail("expected TransactionRuntimeException");
        } catch (TransactionRuntimeException e) {
            // expected
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.sql.test:OSGI-INF/test-listeners-async-retry-contrib.xml")
    public void testAsyncListenerRetry() {
        DummyAsyncRetryListener.clear();

        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setProperty("dublincore", "title", "title1");
        doc = session.createDocument(doc);
        session.save();

        waitForAsyncCompletion();

        assertEquals(2, DummyAsyncRetryListener.getCountHandled());
        assertEquals(1, DummyAsyncRetryListener.getCountOk());
    }

    @Test
    public void testAcquireThroughSessionId() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        assertNotNull(file.getCoreSession());
    }

    @Test
    public void testReconnectAfterClose() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();
        CoreSession closedSession = session;

        waitForAsyncCompletion();
        coreFeature.releaseCoreSession();

        // use a closed session. because of tx, we can reconnect it
        assertNotNull(closedSession.getRootDocument());

        // reopen session for rest of the code
        session = coreFeature.createCoreSession();
    }

    @Test
    public void testReconnectAfterCommit() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // keep existing CoreSession whose Session was implicitly closed
        // by commit
        // reconnect possible through tx
        assertNotNull(session.getRootDocument());
    }

    @Test
    public void testReconnectAfterCloseNoTx() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();
        CoreSession closedSession = session;

        waitForAsyncCompletion();
        coreFeature.releaseCoreSession();
        TransactionHelper.commitOrRollbackTransaction();

        // no startTransaction
        // use a closed session -> exception
        try {
            closedSession.getRootDocument();
            fail("should throw");
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Cannot use a CoreSession outside a transaction"));
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    /**
     * DocumentModel.getCoreSession cannot reconnect through a sid that does not exist anymore.
     */
    @Test
    public void testReconnectAfterCloseThroughSessionId() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        session = coreFeature.reopenCoreSession();

        assertNull(file.getCoreSession());
    }

    @Test
    public void testMultiThreaded() throws Exception {
        assertNotNull(session.getRootDocument());

        final CoreSession finalSession = session;
        Thread t = new Thread(() -> {
            try {
                TransactionHelper.startTransaction();
                try {
                    assertNotNull(finalSession.getRootDocument());
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } catch (Exception e) {
                fail(e.toString());
            }
        });
        t.start();
        t.join();

        assertNotNull(session.getRootDocument());
    }

    @Test
    public void testMultiThreadedNeedsTx() throws Exception {
        assertNotNull(session.getRootDocument());

        final CoreSession finalSession = session;
        final Exception[] threadException = new Exception[1];
        Thread t = new Thread(() -> {
            try {
                // no tx
                finalSession.getRootDocument();
            } catch (Exception e) {
                threadException[0] = e;
            }
        });
        t.start();
        t.join();
        Exception e = threadException[0];
        assertNotNull(e);
        assertTrue(e.getMessage(), e instanceof NuxeoException);
    }

    @Test
    public void testCloseFromOtherTx() throws Exception {
        assertNotNull(session.getRootDocument());

        Thread t = new Thread(() -> {
            try {
                coreFeature.releaseCoreSession();
            } catch (Exception e) {
                fail(e.toString());
            }
        });
        t.start();
        t.join();
        session = coreFeature.createCoreSession();
    }

    @Test
    public void testPreemptiveTransactionTimeout() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction(1); // 1s transaction
        Thread.sleep(2000); // 2s sleep
        try {
            // any use of the session after the timeout is enough to trigger the exception
            session.getRootDocument();
            fail("should have preemptively timed out");
        } catch (TransactionRuntimeException e) {
            assertEquals("Transaction has timed out", e.getMessage());
        }
        try {
            TransactionHelper.commitOrRollbackTransaction();
            fail("commit after timeout should raise an exception");
        } catch (TransactionRuntimeException e) {
            assertEquals("Unable to commit: Transaction timeout", e.getMessage());
        }
        TransactionHelper.startTransaction();
    }

}
