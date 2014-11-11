/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests read ACLs behavior in a transactional setting.
 */
public class TestSQLRepositoryReadAcls extends TXSQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestSQLRepositoryJTAJCA.class);

    @Test
    public void testParallelPrepareUserReadAcls() throws Throwable {
        doParallelPrepareUserReadAcls(0);
    }

    // fails on SQL Server sometimes (expected:<1> but was:<0>)
    @Ignore
    @Test
    public void testParallelPrepareUserReadAclsMany() throws Throwable {
        for (int i = 0; i < 100; i++) {
            log.debug("try " + i);
            doParallelPrepareUserReadAcls(i);
        }
    }

    protected void doParallelPrepareUserReadAcls(int i) throws Throwable {
        // set ACP on root
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        String username = "user" + i;
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE(username, "Everything", true));
        acp.addACL(acl);
        String name = "doc" + i;
        DocumentModel doc = session.createDocumentModel("/", name, "File");
        doc = session.createDocument(doc);
        doc.setACP(acp, true);
        session.save();

        closeSession();
        TransactionHelper.commitOrRollbackTransaction();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch firstReady = new CountDownLatch(1);
        PrepareUserReadAclsJob r1 = new PrepareUserReadAclsJob(name, username,
                database.repositoryName, firstReady, barrier);
        PrepareUserReadAclsJob r2 = new PrepareUserReadAclsJob(name, username,
                database.repositoryName, null, barrier);
        Thread t1 = null;
        Thread t2 = null;
        try {
            t1 = new Thread(r1, "t1");
            t2 = new Thread(r2, "t2");
            t1.start();
            if (firstReady.await(60, TimeUnit.SECONDS)) {
                t2.start();

                t1.join();
                t1 = null;
                t2.join();
                t2 = null;
                if (r1.throwable != null) {
                    throw r1.throwable;
                }
                if (r2.throwable != null) {
                    throw r2.throwable;
                }
            } // else timed out
        } finally {
            // error condition recovery
            if (t1 != null) {
                t1.interrupt();
            }
            if (t2 != null) {
                t2.interrupt();
            }
        }

        // after both threads have run, check that we don't see
        // duplicate documents
        TransactionHelper.startTransaction();
        session = openSessionAs(username);
        checkOneDoc(session, name); // failed for PostgreSQL
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction();
        openSession();
    }

    protected static void checkOneDoc(CoreSession session, String name)
            throws ClientException {
        String query = "SELECT * FROM File WHERE ecm:isProxy = 0 AND ecm:name = '"
                + name + "'";
        DocumentModelList res = session.query(query, NXQL.NXQL, null, 0, 0,
                false);
        assertEquals(1, res.size());
    }

    protected static class PrepareUserReadAclsJob implements Runnable {

        private String name;

        private String username;

        private String repositoryName;

        public CountDownLatch ready;

        public CyclicBarrier barrier;

        public Throwable throwable;

        public PrepareUserReadAclsJob(String name, String username,
                String repositoryName, CountDownLatch ready,
                CyclicBarrier barrier) {
            this.name = name;
            this.username = username;
            this.repositoryName = repositoryName;
            this.ready = ready;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            TransactionHelper.startTransaction();
            try (CoreSession session = CoreInstance.openCoreSession(
                    repositoryName, username)) {
                if (ready != null) {
                    ready.countDown();
                    ready = null;
                }
                barrier.await(30, TimeUnit.SECONDS); // (throws on timeout)
                barrier = null;
                checkOneDoc(session, name); // fails for Oracle
            } catch (Throwable t) {
                t.printStackTrace();
                throwable = t;
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                // error recovery
                // still count down as main thread is awaiting us
                if (ready != null) {
                    ready.countDown();
                }
                // break barrier for other thread
                if (barrier != null) {
                    barrier.reset(); // break barrier
                }
            }
        }
    }

}
