/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.tx.EventBundleTransactionHandler;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.Session.Job;
import org.nuxeo.ecm.core.storage.sql.Session.JobManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A service that enqueues jobs for asynchronous processing in the context of a
 * low-level session.
 *
 * @author Florent Guillaume
 */
public class SQLJobManager implements JobManager {

    private static final Log log = LogFactory.getLog(SQLJobManager.class);

    private final BlockingQueue<Runnable> queue;

    private final ThreadPoolExecutor executor;

    private static final int POOL_SIZE = 1;

    private static final int KEEP_ALIVE_TIME = 5; // seconds

    public SQLJobManager() {
        queue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, queue,
                new NamedThreadFactory("Nuxeo SQLJobManager"));
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Queues a job for later processing.
     *
     * @param job a runnable job
     */
    public void queueJob(Job job, Session session) throws Exception {
        executor.execute(new JobCommand(job, session.getRepositoryName()));
    }

    public static class JobCommand implements Runnable {

        private final Job job;

        private final String repositoryName;

        public JobCommand(Job job, String repositoryName) {
            this.job = job;
            this.repositoryName = repositoryName;
        }

        public void run() {
            EventBundleTransactionHandler tx = new EventBundleTransactionHandler();
            try {
                tx.beginNewTransaction();
                work();
                tx.commitOrRollbackTransaction();
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                tx.rollbackTransaction();
            }
        }

        protected void work() throws Exception {
            LoginContext loginContext = Framework.login();
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            if (repositoryManager == null) {
                return; // XXX
            }
            Repository repository = repositoryManager.getRepository(repositoryName);
            if (repository == null) {
                throw new RuntimeException("Cannot get repository: "
                        + repositoryName);
            }

            CoreSession coreSession = null;
            try {
                coreSession = repository.open();
                // run this through an Operation to be able to get to the
                // underlying AbstractSession
                coreSession.run(new JobOperation(job));
            } finally {
                try {
                    if (coreSession != null) {
                        Repository.close(coreSession);
                    }
                } finally {
                    if (loginContext != null) {
                        loginContext.logout();
                    }
                }
            }
        }
    }

    /**
     * Operation running the job in the context of the core session object.
     */
    public static class JobOperation extends Operation<String> {

        private static final long serialVersionUID = 1L;

        private final Job job;

        public JobOperation(Job job) {
            super("Nuxeo SQL Job");
            this.job = job;
        }

        @Override
        public String doRun(ProgressMonitor monitor) throws Exception {
            // many casts needed...
            AbstractSession abstractSession = (AbstractSession) getSession();
            SQLSession ses = (SQLSession) abstractSession.getSession();
            // do the actual work
            job.run(ses.getUnderlyingSession(), true); // save
            return null;
        }
    }

    /**
     * Creates non-daemon threads at minimum priority.
     */
    public static class NamedThreadFactory implements ThreadFactory {

        protected static final AtomicInteger poolNumber = new AtomicInteger();

        protected final ThreadGroup group;

        protected final AtomicInteger threadNumber = new AtomicInteger();

        protected final String namePrefix;

        public NamedThreadFactory(String prefix) {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup()
                    : sm.getThreadGroup();
            namePrefix = prefix + ' ' + poolNumber.incrementAndGet() + '-';
        }

        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.incrementAndGet();
            Thread t = new Thread(group, r, name);
            t.setDaemon(false);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    }

}
