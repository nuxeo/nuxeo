/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (!useTX()) {
            Environment.getDefault().setHostApplicationName(null);
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
    @Test
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

    @Test
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
    @Test
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

    @Test
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
                TransactionHelper.startTransaction();
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
                    TransactionHelper.commitOrRollbackTransaction();
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
