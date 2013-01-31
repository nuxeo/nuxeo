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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test JTAJCA pool behavior.
 */
public class TestJCAPoolBehavior extends TXSQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestJCAPoolBehavior.class);

    public static final int MIN_POOL_SIZE = 0;

    public static final int MAX_POOL_SIZE = 5;

    public static final int BLOCKING_TIMEOUT = 200;

    public volatile Exception threadException;



    /** Creates a session and holds it open for a while. */
    protected class SessionHolder extends Thread {

        protected final CountDownLatch fillSignal;

        protected final CountDownLatch closeSignal;

        public SessionHolder(String name, CountDownLatch fillSignal, CountDownLatch closeSignal) {
            super("session-holder-" + name);
            this.fillSignal = fillSignal;
            this.closeSignal = closeSignal;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            log.info("start of thread " + Thread.currentThread().getName());
            TransactionHelper.startTransaction();
            CoreSession s = null;
            try {
                try {
                    s = openSessionAs(SecurityConstants.ADMINISTRATOR);
                } catch (Exception e) {
                    threadException = e;
                }
            } finally {
                try {
                    fillSignal.countDown();
                    try {
                        closeSignal.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (s != null) {
                        closeSession(s);
                    }
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
            log.info("end of thread " + Thread.currentThread().getName());
        }
    }

    @Override
    protected void setUpContainer() throws Exception {
        NuxeoConnectionManagerConfiguration cmconfig = new NuxeoConnectionManagerConfiguration();
        cmconfig.setMinPoolSize(MIN_POOL_SIZE);
        cmconfig.setMaxPoolSize(MAX_POOL_SIZE);
        cmconfig.setBlockingTimeoutMillis(BLOCKING_TIMEOUT);
        NuxeoContainer.install();
        NuxeoContainer.installConnectionManager(database.repositoryName,
                cmconfig);
    }

    @Test
    public void testOpenAllConnections() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }

        threadException = null;

        CountDownLatch fillSignal = new CountDownLatch(MAX_POOL_SIZE - 1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        // main thread already uses 1 session
        Thread[] threads = new Thread[MAX_POOL_SIZE - 1];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new SessionHolder(Integer.toString(i), fillSignal, closeSignal);
            threads[i].start();
        }
        fillSignal.await(); // wait for pool being filled
        try {
            assertNull(threadException);
        } finally {
            // finish all threads
            closeSignal.countDown();
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        }
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

        CountDownLatch fillSignal = new CountDownLatch(MAX_POOL_SIZE - 1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        // main thread already uses 1 session
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < MAX_POOL_SIZE-1; i++) {
            Thread t  = new SessionHolder(Integer.toString(i), fillSignal, closeSignal);
            t.start();
            threads.add(t);
        }
        fillSignal.await();
        try {
            assertNull(threadException);
        } finally {
            fillSignal = new CountDownLatch(1);
            // all connections are used, but try yet another one
            {
                Thread t = new SessionHolder("limit-reached", fillSignal, closeSignal);
                t.start();
                threads.add(t);
            }
            fillSignal.await();
            try {
                assertNotNull(threadException);
                threadException = null;
            } finally {
                // finish all threads
                closeSignal.countDown();
                Iterator<Thread> it =  threads.iterator();
                while (it.hasNext()) {
                    Thread t = it.next();
                    t.join();
                    it.remove();
                }
            }
        }
        // re-test full threads use
        testOpenAllConnections();
    }


    /**
     * Check that for two different repositories we get the connections from two
     * different pools. If not, TransactionCachingInterceptor will return a
     * session from the first repository when asked for a new session for the
     * second repository.
     */
    @SuppressWarnings("deprecation")
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
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", SecurityConstants.ADMINISTRATOR);
        CoreSession session2 = CoreInstance.getInstance().open(
                database.repositoryName + "2", context);
        try {
            doTestMultipleRepositoriesPerTransaction(session2);
        } finally {
            CoreInstance.getInstance().close(session2);
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

}
