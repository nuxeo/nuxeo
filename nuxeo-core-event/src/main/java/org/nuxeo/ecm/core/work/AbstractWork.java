/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.work;

import static org.nuxeo.ecm.core.work.api.Work.Progress.PROGRESS_0_PC;
import static org.nuxeo.ecm.core.work.api.Work.Progress.PROGRESS_100_PC;
import static org.nuxeo.ecm.core.work.api.Work.Progress.PROGRESS_INDETERMINATE;
import static org.nuxeo.ecm.core.work.api.Work.State.COMPLETED;
import static org.nuxeo.ecm.core.work.api.Work.State.FAILED;
import static org.nuxeo.ecm.core.work.api.Work.State.QUEUED;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDED;
import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDING;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * A base implementation for a {@link Work} instance. Actual implementations
 * should implement the {@link #work()} method, and deal with saved state data
 * when a suspension is required.
 *
 * @since 5.6
 */
public abstract class AbstractWork implements Work {

    private static final Log log = LogFactory.getLog(AbstractWork.class);

    protected volatile State state;

    protected volatile Progress progress;

    protected volatile String status;

    /**
     * Monitor held while runningState is conditionally changed.
     */
    protected Object runningStateMonitor = new Object();

    protected long schedulingTime;

    protected volatile long startTime;

    protected volatile long completionTime;

    protected volatile Map<String, Serializable> data;

    public AbstractWork() {
        state = QUEUED;
        progress = PROGRESS_INDETERMINATE;
        schedulingTime = System.currentTimeMillis();
    }

    @Override
    public void setData(Map<String, Serializable> data) {
        this.data = data;
    }

    /**
     * This method should be called periodically by the actual work method when
     * it knows of its progress.
     *
     * @param progress the progress
     */
    protected void setProgress(Progress progress) {
        this.progress = progress;
    }

    @Override
    public Progress getProgress() {
        return progress;
    }

    protected void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        synchronized (runningStateMonitor) {
            state = RUNNING;
        }
        setProgress(PROGRESS_0_PC);
        boolean tx = isTransactional();
        boolean ok = false;
        if (tx) {
            tx = TransactionHelper.startTransaction();
        }
        try {
            try {
                work();
                ok = true;
            } finally {
                cleanUp(ok);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                // restore interrupted status for the thread pool worker
                Thread.currentThread().interrupt();
            } else {
                log.error("Work exception", e);
            }
        } finally {
            try {
                if (tx) {
                    if (!ok) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } finally {
                synchronized (runningStateMonitor) {
                    if (!ok) {
                        state = FAILED;
                    } else if (state == RUNNING || state == SUSPENDING) {
                        state = COMPLETED;
                    }
                    // else SUSPENDED or FAILED
                }
                if (state == COMPLETED) {
                    setProgress(PROGRESS_100_PC);
                }
                completionTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * This method should implement the actual work done by the {@link Work}
     * instance.
     * <p>
     * It should periodically call {@link #setProgress()} to report its
     * progress.
     * <p>
     * To allow for suspension by the {@link WorkManager}, it should
     * periodically check if {@link #isSuspending()} is {@code true}, and if
     * yes, call {@link #suspended()} with saved state data and return early.
     */
    public abstract void work() throws Exception;

    /**
     * This method is called after {@link #work} is done in a {@code finally}
     * block.
     *
     * @param ok {@code true} if there was no exception in the {@link #work}
     *            method
     */
    public void cleanUp(boolean ok) {
    }

    /**
     * Subclass this to return {@code false} if the work instance should not run
     * in a transaction.
     */
    protected boolean isTransactional() {
        return true;
    }

    @Override
    public boolean suspend() {
        synchronized (runningStateMonitor) {
            if (state != COMPLETED && state != FAILED) {
                if (state != SUSPENDED) {
                    // QUEUED or RUNNING
                    state = SUSPENDING;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a suspend has been requested for this work instance by the work
     * manager.
     *
     * @return
     */
    protected boolean isSuspending() {
        return state == SUSPENDING;
    }

    /**
     * Should be called by work implementations to advertise that suspension is
     * done and the saved state data is available.
     */
    protected void suspended(Map<String, Serializable> data) {
        this.data = data;
        synchronized (runningStateMonitor) {
            if (state == SUSPENDING) {
                state = SUSPENDED;
            }
        }
    }

    @Override
    public Map<String, Serializable> getData(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (state == QUEUED) {
            return data;
        }
        long delay = unit.toMillis(timeout);
        long t0 = System.currentTimeMillis();
        for (;;) {
            if (state == SUSPENDED) {
                return data;
            }
            if (System.currentTimeMillis() - t0 > delay) {
                return null;
            }
            Thread.sleep(50);
        }
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public long getSchedulingTime() {
        return schedulingTime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getCompletionTime() {
        return completionTime;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public Collection<DocumentLocation> getDocuments() {
        return Collections.emptyList();
    }

}
