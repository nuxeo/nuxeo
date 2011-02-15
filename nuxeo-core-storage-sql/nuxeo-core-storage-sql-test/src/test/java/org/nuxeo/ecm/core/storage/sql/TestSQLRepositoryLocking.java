/*
 * (C) Copyright 2008-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests locking behavior under transaction. Subclass tests with no transaction.
 */
public class TestSQLRepositoryLocking extends TXSQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestSQLRepositoryJTAJCA.class);

    protected static final String SYSTEM = SecurityConstants.SYSTEM_USERNAME;

    @SuppressWarnings("deprecation")
    protected static final String ADMINISTRATOR = SecurityConstants.ADMINISTRATOR;

    // subclassed to disable transactions
    protected boolean useTX() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!useTX()) {
            closeSession();
            TransactionHelper.commitOrRollbackTransaction();
            openSession();
        }
    }

    protected void nextTX() throws Exception {
        closeSession();
        if (useTX()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        openSession();
    }

    @SuppressWarnings("deprecation")
    public void testLocking() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }

        DocumentModel root = session.getRootDocument();

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc = session.createDocument(doc);
        assertNull(doc.getLock()); // old
        assertNull(doc.getLockInfo());
        assertFalse(doc.isLocked());
        session.save();

        nextTX();

        doc = session.getChild(root.getRef(), "doc");
        doc.setLock();

        assertEquals(ADMINISTRATOR, doc.getLockInfo().getOwner());
        assertNotNull(doc.getLockInfo().getCreated());
        assertTrue(doc.getLock().startsWith(ADMINISTRATOR + ':')); // old
        assertTrue(doc.isLocked());

        nextTX();

        doc = session.getChild(root.getRef(), "doc");

        assertEquals(ADMINISTRATOR, doc.getLockInfo().getOwner());
        assertNotNull(doc.getLockInfo().getCreated());
        assertTrue(doc.getLock().startsWith(ADMINISTRATOR + ':')); // old
        assertTrue(doc.isLocked());

        nextTX();

        doc = session.getChild(root.getRef(), "doc");
        doc.removeLock();

        assertNull(doc.getLockInfo());
        assertFalse(doc.isLocked());

        nextTX();

        doc = session.getChild(root.getRef(), "doc");

        assertFalse(doc.isLocked());
    }

    public void testLockingBeforeSave() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.setLock();
        session.save();

        nextTX();

        doc = session.getChild(root.getRef(), "doc");
        assertTrue(doc.isLocked());
    }

    // check we don't have a SQL-level locking error due to the lock manager
    // connection reading a row that was written but not yet committed by the
    // main connection
    public void testGetLockAfterCreate() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }
    
        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        doc1 = session.createDocument(doc1);
        session.save();
        // read lock after save (SQL INSERT)
        assertNull(doc1.getLockInfo());
    
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        doc2 = session.createDocument(doc2);
        session.save();
        // set lock after save (SQL INSERT)
        doc2.setLock();
    }

    protected CountDownLatch threadStartLatch;

    protected CountDownLatch lockingLatch;

    protected volatile boolean locked;

    public void testLockingWithMultipleThreads() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();

        nextTX();

        doc = session.getChild(root.getRef(), "doc");
        assertFalse(doc.isLocked());

        // start other thread
        threadStartLatch = new CountDownLatch(1);
        lockingLatch = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                CoreSession session2 = null;
                try {
                    session2 = openSessionAs(ADMINISTRATOR);
                    DocumentModel root2 = session2.getRootDocument();
                    DocumentModel doc2 = session2.getChild(root2.getRef(),
                            "doc");
                    // let main thread continue
                    threadStartLatch.countDown();
                    // wait main thread trigger
                    lockingLatch.await();
                    locked = doc2.isLocked();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (session2 != null) {
                        closeSession(session2);
                    }
                }
            }
        };
        t.start();
        threadStartLatch.await();

        doc.setLock();
        assertTrue(doc.isLocked());

        // trigger other thread check
        lockingLatch.countDown();
        t.join();

        assertTrue(locked);
    }

}
