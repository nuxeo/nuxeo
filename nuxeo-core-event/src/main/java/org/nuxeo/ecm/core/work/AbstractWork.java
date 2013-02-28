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
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;
import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDED;
import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDING;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * A base implementation for a {@link Work} instance, dealing with most of the
 * details around state change.
 * <p>
 * Actual implementations must at a minimum implement the {@link #work()}
 * method. A method {@link #cleanUp} is available.
 * <p>
 * To deal with suspension, the method {@link #suspendFromQueue()} should be
 * implemented, and {@link #work()} should periodically check for
 * {@link #isSuspending()}.
 * <p>
 * Specific information about the work can be returned by
 * {@link #getPrincipal()} and {@link #getDocuments()}.
 *
 * @since 5.6
 */
public abstract class AbstractWork implements Work {

    private static final Log log = LogFactory.getLog(AbstractWork.class);

    protected volatile State state;

    protected volatile Progress progress;

    protected volatile String status;

    /**
     * Monitor held while runningState is changed.
     */
    protected Object stateMonitor = new Object();

    protected long schedulingTime;

    protected volatile long startTime;

    protected volatile long completionTime;

    protected volatile Map<String, Serializable> data;

    // Subclasses can update this field during the lifecycle of the work execution
    // when they want to manage their transaction manually (e.g. for long running tasks
    // that only need access to transactional resources at specific times)
    protected volatile boolean isTransactionStarted = false;

    protected LoginContext loginContext;

    protected CoreSession session;

    // @since 5.7
    protected final Timer workTimer = Metrics.defaultRegistry().newTimer(
            AbstractWork.class, "work",
            TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    private TimerContext timerContext;

    public AbstractWork() {
        state = SCHEDULED;
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

    // before this, state is QUEUED or SUSPENDED
    // after this, state is RUNNING or SUSPENDED
    @Override
    public void beforeRun() {
        timerContext = workTimer.time();
        startTime = System.currentTimeMillis();
        setProgress(PROGRESS_0_PC);
        synchronized (stateMonitor) {
            if (state == SCHEDULED) {
                state = RUNNING;
            }
            // else may already be SUSPENDED
            // cannot be SUSPENDING because we switch to that state
            // only if state is RUNNING (in suspend())
        }
    }

    @Override
    public void run() {
        if (state == SUSPENDED) {
            return;
        }
        boolean tx = isTransactional();
        if (tx) {
            isTransactionStarted = TransactionHelper.startTransaction();
        }
        boolean ok = false;
        Exception err = null;
        try {
            work();
            ok = true;
        } catch (Exception e) {
            err = e;
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            try {
                cleanUp(ok, err);
            } finally {
                try {
                    if (tx && isTransactionStarted) {
                        if (!ok) {
                            TransactionHelper.setTransactionRollbackOnly();
                        }
                        TransactionHelper.commitOrRollbackTransaction();
                        isTransactionStarted = false;
                    }
                } finally {
                    if (err instanceof InterruptedException) {
                        // restore interrupted status for the thread pool worker
                        Thread.currentThread().interrupt();
                    }
                }
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
     * periodically call {@link #isSuspending()}, and if true call
     * {@link #suspended()} with saved state data and return early.
     */
    public abstract void work() throws Exception;

    // before this, state is RUNNING, SUSPENDED or SUSPENDING (and error)
    // after this, state is COMPLETED, SUSPENDED or FAILED
    @Override
    public void afterRun(boolean ok) {
        synchronized (stateMonitor) {
            if (!ok) {
                state = FAILED;
            } else if (state == RUNNING || state == SUSPENDING) {
                state = COMPLETED;
            }
            // else SUSPENDED
        }
        if (state == COMPLETED) {
            setProgress(PROGRESS_100_PC);
        }
        completionTime = System.currentTimeMillis();
        timerContext.stop();
    }

    /**
     * May be called by implementing classes to open a session on the given
     * repository.
     *
     * @param repositoryName the repository name
     * @return the session (also available in {@code session} field)
     */
    public CoreSession initSession(String repositoryName) throws Exception {
        try {
            loginContext = Framework.login();
        } catch (LoginException e) {
            log.error("Cannot log in", e);
        }
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        if (repositoryManager == null) {
            // would happen if only low-level repo is initialized
            throw new RuntimeException("RepositoryManager service not available");
        }
        Repository repository;
        if (repositoryName != null) {
            repository = repositoryManager.getRepository(repositoryName);
        } else {
            repository = repositoryManager.getDefaultRepository();
            repositoryName = repository.getName();
        }
        session = repository.open();
        return session;
    }

    /**
     * This method is called after {@link #work} is done in a finally block,
     * whether work completed normally or was in error or was interrupted.
     *
     * @param ok {@code true} if the work completed normally
     * @param e the exception, if available
     */
    public void cleanUp(boolean ok, Exception e) {
        if (!ok && !(e instanceof InterruptedException)) {
            log.error("Exception during work: " + this, e);
        }
        try {
            if (session != null) {
                CoreInstance.getInstance().close(session);
                session = null;
            }
        } finally {
            if (loginContext != null) {
                try {
                    loginContext.logout();
                    loginContext = null;
                } catch (LoginException le) {
                    log.error("Error while logging out", le);
                }
            }
        }
    }

    /**
     * Subclass this to return {@code false} if the work instance should not run
     * in a transaction.
     */
    protected boolean isTransactional() {
        return true;
    }

    // after this, state may be
    // COMPLETED, FAILED
    // SUSPENDING, SUSPENDED
    @Override
    public boolean suspend() {
        synchronized (stateMonitor) {
            if (state != COMPLETED && state != FAILED) {
                if (state != SUSPENDED) {
                    if (state == SCHEDULED) {
                        suspendFromQueue();
                        state = SUSPENDED;
                    } else { // RUNNING
                        state = SUSPENDING;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Called when suspension occurs while the work instance is queued.
     * <p>
     * Implementations should call {@link #suspended} with their saved state
     * data.
     */
    protected void suspendFromQueue() {
    }

    /**
     * Checks if a suspend has been requested for this work instance by the work
     * manager.
     * <p>
     * If yes, {@link #suspended} should be called and the {@link #work} method
     * should return early.
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
        synchronized (stateMonitor) {
            if (state == SUSPENDING) {
                state = SUSPENDED;
            }
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        // TODO use Condition
        long delay = unit.toMillis(timeout);
        long t0 = System.currentTimeMillis();
        for (;;) {
            if (state != RUNNING && state != SUSPENDING) {
                return true;
            }
            if (System.currentTimeMillis() - t0 > delay) {
                return false;
            }
            Thread.sleep(50);
        }
    }

    @Override
    public void setCanceled() {
        synchronized (stateMonitor) {
            if (state == SCHEDULED) {
                state = State.CANCELED;
            } else {
                throw new IllegalStateException(String.valueOf(state));
            }
        }
    }

    @Override
    public Map<String, Serializable> getData() {
        return data;
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
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getState() + ", "
                + getProgress() + ", " + getStatus() + ")";
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public Collection<DocumentLocation> getDocuments() {
        return Collections.emptyList();
    }

    /**
     * Release the transaction resources by committing the existing transaction
     * (if any). This is recommended before running a long process such as a
     * video conversion for instance.
     */
    protected void commitOrRollbackTransaction() {
        if (isTransactional() && isTransactionStarted) {
            TransactionHelper.commitOrRollbackTransaction();
            isTransactionStarted = false;
        }
    }

    /**
     * Start a new transaction if {@code commitOrRollbackTransaction()} was
     * called previously, for instance for saving back the results of a long
     * process such as a video conversion back as blob property of a nuxeo
     * document in the repository.
     *
     * @return true if a new transaction has started
     */
    protected boolean startTransaction() {
        if (isTransactional() && !isTransactionStarted) {
            isTransactionStarted = TransactionHelper.startTransaction();
        }
        return isTransactionStarted;
    }

}
