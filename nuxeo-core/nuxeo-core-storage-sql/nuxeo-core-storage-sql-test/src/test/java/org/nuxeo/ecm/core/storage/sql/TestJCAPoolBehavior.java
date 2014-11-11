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

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer.TransactionManagerConfiguration;
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

    @Override
    protected void setUpContainer() throws Exception {
        TransactionManagerConfiguration tmconfig = new TransactionManagerConfiguration();
        ConnectionManagerConfiguration cmconfig = new ConnectionManagerConfiguration();
        cmconfig.setMinPoolSize(MIN_POOL_SIZE);
        cmconfig.setMaxPoolSize(MAX_POOL_SIZE);
        cmconfig.setBlockingTimeoutMillis(BLOCKING_TIMEOUT);
        NuxeoContainer.install(tmconfig, cmconfig);
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
        Thread.sleep(500);
        assertNull(threadException);

        // finish all threads
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        assertNull(threadException);
    }

    @Test
    public void testOpenMoreConnectionsThanMax() throws Exception {
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
        Thread.sleep(500);
        assertNull(threadException);

        // all connections are used, but try yet another one
        Thread t = new Thread(new SessionHolder(2000));
        t.start();
        Thread.sleep(BLOCKING_TIMEOUT + 500);
        assertNotNull(threadException);
        threadException = null;

        // finish all threads
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
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

}
