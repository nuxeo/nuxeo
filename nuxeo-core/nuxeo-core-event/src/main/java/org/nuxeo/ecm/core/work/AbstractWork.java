/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import static org.nuxeo.ecm.core.work.api.Work.Progress.PROGRESS_INDETERMINATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * A base implementation for a {@link Work} instance, dealing with most of the details around state change.
 * <p>
 * It also deals with transaction management, and prevents running work instances that are suspending.
 * <p>
 * Actual implementations must at a minimum implement the {@link #work()} method. A method {@link #cleanUp} is
 * available.
 * <p>
 * To deal with suspension, {@link #work()} should periodically check for {@link #isSuspending()} and if true save its
 * state and call {@link #suspended()}.
 * <p>
 * Specific information about the work can be returned by {@link #getDocument()} or {@link #getDocuments()}.
 *
 * @since 5.6
 */
public abstract class AbstractWork implements Work {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AbstractWork.class);

    protected static final Random RANDOM = new Random();

    protected String id;

    /** Suspend requested by the work manager. */
    protected transient volatile boolean suspending;

    /** Suspend acknowledged by the work instance. */
    protected transient volatile boolean suspended;

    protected State state;

    protected Progress progress;

    /** Repository name for the Work instance, if relevant. */
    protected String repositoryName;

    /**
     * Doc id for the Work instance, if relevant. This describes for the WorkManager a document on which this Work
     * instance will act.
     * <p>
     * Either docId or docIds is set. Not both.
     */
    protected String docId;

    /**
     * Doc ids for the Work instance, if relevant. This describes for the WorkManager the documents on which this Work
     * instance will act.
     * <p>
     * Either docId or docIds is set. Not both.
     */
    protected List<String> docIds;

    /**
     * If {@code true}, the docId is only the root of a set of documents on which this Work instance will act.
     */
    protected boolean isTree;

    /**
     * The originating username to use when opening the {@link CoreSession}.
     *
     * @since 8.1
     */
    protected String originatingUsername;

    protected String status;

    protected long schedulingTime;

    protected long startTime;

    protected long completionTime;

    protected transient CoreSession session;

    protected transient LoginContext loginContext;

    protected WorkSchedulePath schedulePath;

    protected String callerThread;

    /**
     * Constructs a {@link Work} instance with a unique id.
     */
    public AbstractWork() {
        // we user RANDOM to deal with these cases:
        // - several calls in the time granularity of nanoTime()
        // - several concurrent calls on different servers
        this(System.nanoTime() + "." + Math.abs(RANDOM.nextInt()));
        callerThread = SequenceTracer.getThreadName();
    }

    public AbstractWork(String id) {
        this.id = id;
        progress = PROGRESS_INDETERMINATE;
        schedulingTime = System.currentTimeMillis();
        callerThread = SequenceTracer.getThreadName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public WorkSchedulePath getSchedulePath() {
        // schedulePath is transient so will become null after deserialization
        return schedulePath == null ? WorkSchedulePath.EMPTY : schedulePath;
    }

    @Override
    public void setSchedulePath(WorkSchedulePath path) {
        schedulePath = path;
    }

    public void setDocument(String repositoryName, String docId, boolean isTree) {
        this.repositoryName = repositoryName;
        this.docId = docId;
        docIds = null;
        this.isTree = isTree;
    }

    public void setDocument(String repositoryName, String docId) {
        setDocument(repositoryName, docId, false);
    }

    public void setDocuments(String repositoryName, List<String> docIds) {
        this.repositoryName = repositoryName;
        docId = null;
        this.docIds = new ArrayList<>(docIds);
    }

    /**
     * @since 8.1
     */
    public void setOriginatingUsername(String originatingUsername) {
        this.originatingUsername = originatingUsername;
    }

    @Override
    public void setWorkInstanceSuspending() {
        suspending = true;
    }

    @Override
    public boolean isSuspending() {
        return suspending;
    }

    @Override
    public void suspended() {
        suspended = true;
    }

    @Override
    public boolean isWorkInstanceSuspended() {
        return suspended;
    }

    @Override
    public void setWorkInstanceState(State state) {
        this.state = state;
        if (log.isTraceEnabled()) {
            log.trace(this + " state=" + state);
        }
    }

    @Override
    public State getWorkInstanceState() {
        return state;
    }

    @Override
    public void setProgress(Progress progress) {
        this.progress = progress;
        if (log.isTraceEnabled()) {
            log.trace(String.valueOf(this));
        }
    }

    @Override
    public Progress getProgress() {
        return progress;
    }

    /**
     * Sets a human-readable status for this work instance.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    /**
     * May be called by implementing classes to open a session on the repository.
     *
     * @return the session (also available in {@code session} field)
     * @deprecated since 8.1. Use {@link #openSystemSession()}.
     */
    @Deprecated
    public CoreSession initSession() {
        return initSession(repositoryName);
    }

    /**
     * May be called by implementing classes to open a System session on the repository.
     *
     * @since 8.1
     */
    public void openSystemSession() {
        session = CoreInstance.openCoreSessionSystem(repositoryName, originatingUsername);
    }

    /**
     * May be called by implementing classes to open a Use session on the repository.
     * <p>
     * It uses the set {@link #originatingUsername} to open the session.
     *
     * @since 8.1
     */
    public void openUserSession() {
        if (originatingUsername == null) {
            throw new IllegalStateException("Cannot open an user session without an originatingUsername");
        }

        try {
            loginContext = Framework.loginAsUser(originatingUsername);
        } catch (LoginException e) {
            throw new NuxeoException(e);
        }

        session = CoreInstance.openCoreSession(repositoryName);
    }

    /**
     * May be called by implementing classes to open a session on the given repository.
     *
     * @param repositoryName the repository name
     * @return the session (also available in {@code session} field)
     * @deprecated since 8.1. Use {@link #openSystemSession()} to open a session on the configured repository name,
     *             otherwise use {@link CoreInstance#openCoreSessionSystem(String)}.
     */
    @Deprecated
    public CoreSession initSession(String repositoryName) {
        session = CoreInstance.openCoreSessionSystem(repositoryName, originatingUsername);
        return session;
    }

    /**
     * Closes the session that was opened by {@link #openSystemSession()} or {@link #openUserSession()}.
     *
     * @since 5.8
     */
    public void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    @Override
    public void run() {
        if (isSuspending()) {
            // don't run anything if we're being started while a suspend
            // has been requested
            suspended();
            return;
        }
        if (SequenceTracer.isEnabled()) {
            SequenceTracer.startFrom(callerThread, "Work " + getTitleOr("unknown"), " #7acde9");
        }
        Exception suppressed = null;
        int retryCount = getRetryCount(); // may be 0
        for (int i = 0; i <= retryCount; i++) {
            if (i > 0) {
                log.debug("Retrying work due to concurrent update (" + i + "): " + this);
                log.trace("Concurrent update", suppressed);
            }
            Exception e = runWorkWithTransactionAndCheckExceptions();
            if (e == null) {
                // no exception, work is done
                SequenceTracer.stop("Work done " + (completionTime - startTime) + " ms");
                return;
            }
            if (suppressed == null) {
                suppressed = e;
            } else {
                suppressed.addSuppressed(e);
            }
        }
        // all retries have been done, throw the exception
        if (suppressed != null) {
            String msg = "Work failed after " + retryCount + " " + (retryCount == 1 ? "retry" : "retries") + ", class="
                    + getClass() + " id=" + getId() + " category=" + getCategory() + " title=" + getTitle();
            SequenceTracer.destroy("Work failure " + (completionTime - startTime) + " ms");
            throw new RuntimeException(msg, suppressed);
        }
    }

    private String getTitleOr(String defaultTitle) {
        try {
            return getTitle();
        } catch (Exception e) {
            return defaultTitle;
        }
    }

    /**
     * Does work under a transaction, and collects exception and suppressed exceptions that may lead to a retry.
     *
     * @since 5.9.4
     */
    protected Exception runWorkWithTransactionAndCheckExceptions() {
        List<Exception> suppressed = Collections.emptyList();
        try {
            TransactionHelper.noteSuppressedExceptions();
            try {
                runWorkWithTransaction();
            } finally {
                suppressed = TransactionHelper.getSuppressedExceptions();
            }
        } catch (ConcurrentUpdateException e) {
            // happens typically during save()
            return e;
        } catch (TransactionRuntimeException e) {
            // error at commit time
            if (suppressed.isEmpty()) {
                return e;
            }
        }
        // reached if no catch, or if TransactionRuntimeException caught
        if (suppressed.isEmpty()) {
            return null;
        }
        // exceptions during commit caused a rollback in SessionImpl#end
        Exception e = suppressed.get(0);
        for (int i = 1; i < suppressed.size(); i++) {
            e.addSuppressed(suppressed.get(i));
        }
        return e;
    }

    /**
     * Does work under a transaction.
     *
     * @since 5.9.4
     * @throws ConcurrentUpdateException, TransactionRuntimeException
     */
    protected void runWorkWithTransaction() throws ConcurrentUpdateException {
        TransactionHelper.startTransaction();
        boolean ok = false;
        Exception exc = null;
        try {
            WorkSchedulePath.handleEnter(this);
            // --- do work
            setStartTime();
            work(); // may throw ConcurrentUpdateException
            ok = true;
            // --- end work
        } catch (Exception e) {
            exc = e;
            if (e instanceof ConcurrentUpdateException) {
                throw (ConcurrentUpdateException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof InterruptedException) {
                // restore interrupted status for the thread pool worker
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        } finally {
            WorkSchedulePath.handleReturn();
            try {
                setCompletionTime();
                cleanUp(ok, exc);
            } finally {
                if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    if (!ok || isSuspending()) {
                        log.trace(this + " is suspending, rollbacking");
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        }
    }

    @Override
    public abstract void work();

    /**
     * Gets the number of times that this Work instance can be retried in case of concurrent update exceptions.
     *
     * @return 0 for no retry, or more if some retries are possible
     * @see #work
     * @since 5.8
     */
    public int getRetryCount() {
        return 0;
    }

    /**
     * This method is called after {@link #work} is done in a finally block, whether work completed normally or was in
     * error or was interrupted.
     *
     * @param ok {@code true} if the work completed normally
     * @param e the exception, if available
     */
    @Override
    public void cleanUp(boolean ok, Exception e) {
        if (!ok) {
            if (e instanceof InterruptedException) {
                log.debug("Suspended work: " + this);
            } else {
                if (!(e instanceof ConcurrentUpdateException)) {
                    if (!isSuspending()) {
                        log.error("Exception during work: " + this, e);
                        if (WorkSchedulePath.captureStack) {
                            WorkSchedulePath.log.error("Work schedule path", getSchedulePath().getStack());
                        }
                    }
                }
            }
        }
        closeSession();

        try {
            // loginContext may be null in tests
            if (loginContext != null) {
                loginContext.logout();
            }
        } catch (LoginException le) {
            throw new NuxeoException(le);
        }
    }

    @Override
    public String getOriginatingUsername() {
        return originatingUsername;
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
    public void setStartTime() {
        startTime = System.currentTimeMillis();
    }

    protected void setCompletionTime() {
        completionTime = System.currentTimeMillis();
    }

    @Override
    public String getCategory() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        if (docId != null) {
            buf.append(docId);
            buf.append(", ");
        } else if (docIds != null && docIds.size() > 0) {
            buf.append(docIds.get(0));
            buf.append("..., ");
        }
        buf.append(getSchedulePath().getParentPath());
        buf.append(", ");
        buf.append(getProgress());
        buf.append(", ");
        buf.append(getStatus());
        buf.append(')');
        return buf.toString();
    }

    @Override
    public DocumentLocation getDocument() {
        if (docId != null) {
            return newDocumentLocation(docId);
        }
        return null;
    }

    @Override
    public List<DocumentLocation> getDocuments() {
        if (docIds != null) {
            List<DocumentLocation> res = new ArrayList<>(docIds.size());
            for (String docId : docIds) {
                res.add(newDocumentLocation(docId));
            }
            return res;
        }
        if (docId != null) {
            return Collections.singletonList(newDocumentLocation(docId));
        }
        return Collections.emptyList();
    }

    protected DocumentLocation newDocumentLocation(String docId) {
        return new DocumentLocationImpl(repositoryName, new IdRef(docId));
    }

    @Override
    public boolean isDocumentTree() {
        return isTree;
    }

    /**
     * Releases the transaction resources by committing the existing transaction (if any). This is recommended before
     * running a long process.
     */
    public void commitOrRollbackTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /**
     * Starts a new transaction.
     * <p>
     * Usually called after {@code commitOrRollbackTransaction()}, for instance for saving back the results of a long
     * process.
     *
     * @return true if a new transaction was started
     */
    public boolean startTransaction() {
        return TransactionHelper.startTransaction();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Work)) {
            return false;
        }
        return ((Work) other).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
