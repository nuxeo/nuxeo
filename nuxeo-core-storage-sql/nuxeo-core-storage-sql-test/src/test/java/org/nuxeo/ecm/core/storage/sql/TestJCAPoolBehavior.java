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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test JTAJCA pool behavior.
 */
public class TestJCAPoolBehavior extends TXSQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestJCAPoolBehavior.class);

    public static final int MIN_POOL_SIZE = 2;

    public static final int MAX_POOL_SIZE = 5;

    public static final int BLOCKING_TIMEOUT = 200;

    public volatile Exception threadException;

    private RepositoryDescriptor desc;

    private ConnectionManagerWrapper cm;

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        desc = Framework.getLocalService(SQLRepositoryService.class).getRepositoryDescriptor(
                database.repositoryName);
        desc.pool.setMinPoolSize(MIN_POOL_SIZE);
        desc.pool.setMaxPoolSize(MAX_POOL_SIZE);
        desc.pool.setBlockingTimeoutMillis(BLOCKING_TIMEOUT);
    }

    @Before
    public void lookupCM() {
        cm = (ConnectionManagerWrapper) NuxeoContainer.getConnectionManager(desc.pool.getName());
        assertNotNull("no pooling", cm);
    }


    @Test
    public void testOpenAllConnections() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }

        threadException = null;

        // main thread already uses 1 session
        Thread[] threads = new Thread[MAX_POOL_SIZE - 1];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new SessionHolder(2000));
            threads[i].start();
        }
        try {
            Thread.sleep(500);
            assertNull(threadException);
        } finally {
            // finish all threads
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        }
        assertNull(threadException);
    }

    @Test
    public void testOpenMoreConnectionsThanMax() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }
        if (useSingleConnectionMode()) {
            // there's not actual pool in this mode
            return;
        }

        threadException = null;

        // main thread already uses 1 session
        Thread[] threads = new Thread[MAX_POOL_SIZE - 1];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new SessionHolder(2000));
            threads[i].start();
        }
        Thread.sleep(500);
        assertNull(threadException);

        // all connections are used, but try yet another one
        Thread t = new Thread(new SessionHolder(2000));
        t.start();
        Thread.sleep(BLOCKING_TIMEOUT + 500);
        try {
            assertNotNull(threadException);
        } finally {
            // finish all threads
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        }
        threadException = null;
        assertNull(threadException);

        // re-test full threads use
        testOpenAllConnections();
    }

    /** Creates a session and holds it open for a while. */
    public class SessionHolder implements Runnable {

        public final int sleepMillis;

        public SessionHolder(int sleepMillis) {
            this.sleepMillis = sleepMillis;
        }

        @Override
        public void run() {
            log.info("start of thread " + Thread.currentThread().getName());
            try {
                TransactionHelper.startTransaction();
                CoreSession s = null;
                try {
                    s = openSessionAs(SecurityConstants.ADMINISTRATOR);
                    Thread.sleep(sleepMillis);
                } finally {
                    try {
                        if (s != null) {
                            closeSession(s);
                        }
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                }
            } catch (Exception e) {
                if (threadException == null) {
                    threadException = e;
                }
            }
            log.info("end of thread " + Thread.currentThread().getName());
        }
    }

    /**
     * Check that for two different repositories we get the connections from two
     * different pools. If not, TransactionCachingInterceptor will return a
     * session from the first repository when asked for a new session for the
     * second repository.
     */
    @Test
    public void testMultipleRepositoriesPerTransaction() throws Exception {
        // config for second repo available only for H2
        if (!(database instanceof DatabaseH2)) {
            return;
        }
        DatabaseH2 db = (DatabaseH2) database;
        db.setUp2();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                "OSGI-INF/test-pooling-h2-repo2-contrib.xml");
        // open a second repository
        try (CoreSession session2 = CoreInstance.openCoreSession(
                database.repositoryName + "2", SecurityConstants.ADMINISTRATOR)) {
            doTestMultipleRepositoriesPerTransaction(session2);
        }
    }

    protected void doTestMultipleRepositoriesPerTransaction(CoreSession session2)
            throws Exception {
        assertEquals(database.repositoryName, session.getRepositoryName());
        assertEquals(database.repositoryName + "2",
                session2.getRepositoryName());
        assertTrue(TransactionHelper.isTransactionActive());
        assertNotSame("Sessions from two different repos",
                session.getRootDocument().getId(),
                session2.getRootDocument().getId());
    }

    @Test
    public void doesntLeakWithTx() throws ClientException {
        checkSessionLeak();
    }

    @Test
    public void doesntLeakWithoutTx() throws ClientException {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            checkSessionLeak();
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    protected void checkSessionLeak() throws ClientException {
        closeSession();
        int count = threadAllocatedConnectionsCount();
        try (CoreSession session = openSessionAs("jdoe")) {
            assertEquals(count + 1, threadAllocatedConnectionsCount());
        }
        assertEquals(count, threadAllocatedConnectionsCount());
    }



    @Test
    public void doesntReleaseBeforeCommit() throws ClientException {
        TransactionHelper.commitOrRollbackTransaction();
        assertEquals(0, activeConnectionCount());
        assertEquals(0, threadAllocatedConnectionsCount());
        closeSession();
        TransactionHelper.startTransaction();
        try {
            try (CoreSession first = openSessionAs("jdoe")) {
                assertEquals(1, threadAllocatedConnectionsCount());
                assertEquals(1, activeConnectionCount());
                try (CoreSession second = openSessionAs("jdoe")) {
                    assertEquals(2, threadAllocatedConnectionsCount());
                    assertEquals(1, activeConnectionCount());
                    TransactionHelper.commitOrRollbackTransaction();
                    assertEquals(0, threadAllocatedConnectionsCount());
                    assertEquals(0, activeConnectionCount());
                }
            }
            assertEquals(0, threadAllocatedConnectionsCount());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    protected int threadAllocatedConnectionsCount() {
        return cm.getCurrentThreadAllocations().size();
    }

    protected int activeConnectionCount() {
        return cm.getPooling().getConnectionCount()
                - cm.getPooling().getIdleConnectionCount();
    }
}
