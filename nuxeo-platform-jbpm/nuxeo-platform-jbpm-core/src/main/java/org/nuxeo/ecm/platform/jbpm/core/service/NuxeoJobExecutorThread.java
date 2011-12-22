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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author arussel
 */
public class NuxeoJobExecutorThread extends JobExecutorThread {

    private static final Log log = LogFactory.getLog(JobExecutorThread.class);

    protected final long maxLockTime;

    protected final JbpmConfiguration jbpmConfiguration;

    public NuxeoJobExecutorThread(String name, JobExecutor jobExecutor,
            JbpmConfiguration jbpmConfiguration, int idleInterval,
            int maxIdleInterval, long maxLockTime, int maxHistory) {
        super(name, jobExecutor, jbpmConfiguration, idleInterval,
                maxIdleInterval, maxLockTime, maxHistory);
        this.jbpmConfiguration = jbpmConfiguration;
        this.maxLockTime = maxLockTime;
    }

    @Override
    protected void executeJob(Job job) {
        TransactionHelper.startTransaction();
        getEventService().transactionStarted();
        try {
            JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
            DbPersistenceServiceFactory factory = ((DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE));
            boolean jbpmTransaction = factory.isTransactionEnabled();
            if (!jbpmTransaction) {
                jbpmContext.getSession().getTransaction().registerSynchronization(
                        new JbpmSynchronization(jbpmContext));
            }
            try {
                JobSession jobSession = jbpmContext.getJobSession();
                job = jobSession.loadJob(job.getId());

                try {
                    log.debug("executing " + job);
                    if (job.execute(jbpmContext)) {
                        jobSession.deleteJob(job);
                    }
                } catch (Exception e) {
                    log.debug("exception while executing " + job, e);
                    if (e instanceof HibernateException) {
                        StringWriter memoryWriter = new StringWriter();
                        e.printStackTrace(new PrintWriter(memoryWriter));
                        job.setException(memoryWriter.toString());
                        job.setRetries(job.getRetries() - 1);
                    } else {
                        // allowing a transaction to proceed after a persistence
                        // exception is unsafe
                        jbpmContext.setRollbackOnly();
                    }
                }

                // if this job is locked too long
                long totalLockTimeInMillis = System.currentTimeMillis()
                        - job.getLockTime().getTime();
                if (totalLockTimeInMillis > maxLockTime) {
                    jbpmContext.setRollbackOnly();
                }
            } finally {
                try {
                    if (jbpmTransaction) {
                        jbpmContext.close();
                        getEventService().transactionCommitted();
                    }
                } catch (JbpmPersistenceException e) {
                    // if this is a stale state exception, keep it quiet
                    if (Services.isCausedByStaleState(e)) {
                        log.debug("optimistic locking failed, couldn't complete job "
                                + job);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (Throwable t) {
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    private EventService getEventService() {
        try {
            return Framework.getService(EventService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection acquireJobs() {
        TransactionHelper.startTransaction();
        Collection acquiredJobs = null;
        try {
            acquiredJobs = super.acquireJobs();
        } catch (Throwable t) {
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        return acquiredJobs;
    }

    @Override
    protected Date getNextDueDate() {
        TransactionHelper.startTransaction();
        Date date = null;
        try {
            date = super.getNextDueDate();
        } catch (Throwable t) {
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        return date;
    }

}
