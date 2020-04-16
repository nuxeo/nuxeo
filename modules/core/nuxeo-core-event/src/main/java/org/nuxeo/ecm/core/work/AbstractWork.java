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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.work.WorkManagerImpl.DEAD_LETTER_QUEUE;
import static org.nuxeo.ecm.core.work.api.Work.Progress.PROGRESS_INDETERMINATE;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

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

    protected static final Random RANDOM = new SecureRandom();

    public static final String WORK_FAILED_EVENT = "workFailed";

    public static final String WORK_INSTANCE = "workInstance";

    public static final String FAILURE_MSG = "failureMsg";

    public static final String FAILURE_EXCEPTION = "failureException";

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

    protected transient NuxeoLoginContext loginContext;

    protected WorkSchedulePath schedulePath;

    protected String callerThread;

    // @since 11.1
    public static final String GLOBAL_DLQ_COUNT_REGISTRY_NAME = MetricRegistry.name("nuxeo", "works", "dlq").getKey();

    static {
        // Initialize the metric so it is reported as 0 from start.
        SharedMetricRegistries.getOrCreate(MetricsService.class.getName()).counter(GLOBAL_DLQ_COUNT_REGISTRY_NAME);
    }

    /**
     * Constructs a {@link Work} instance with a unique id.
     */
    public AbstractWork() {
        // we user RANDOM to deal with these cases:
        // - several calls in the time granularity of nanoTime()
        // - several concurrent calls on different servers
        this(System.nanoTime() + "." + (RANDOM.nextInt() & 0x7fffffff));
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
        loginContext = Framework.loginSystem(originatingUsername);
        session = CoreInstance.getCoreSessionSystem(repositoryName, originatingUsername);
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
            loginContext = Framework.loginUser(originatingUsername);
        } catch (LoginException e) {
            throw new NuxeoException(e);
        }

        session = CoreInstance.getCoreSession(repositoryName);
    }

    /**
     * May be called by implementing classes to open a session on the given repository.
     *
     * @param repositoryName the repository name
     * @return the session (also available in {@code session} field)
     * @deprecated since 8.1. Use {@link #openSystemSession()} to open a session on the configured repository name,
     *             otherwise use {@link CoreInstance#getCoreSessionSystem(String)}.
     */
    @Deprecated
    public CoreSession initSession(String repositoryName) {
        session = CoreInstance.getCoreSessionSystem(repositoryName, originatingUsername);
        return session;
    }

    /**
     * Closes the session that was opened by {@link #openSystemSession()} or {@link #openUserSession()}.
     *
     * @since 5.8
     */
    public void closeSession() {
        // loginContext may be null in tests
        if (loginContext != null) {
            loginContext.close();
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
        RuntimeException suppressed = null;
        int retryCount = getRetryCount(); // may be 0
        for (int i = 0; i <= retryCount; i++) {
            if (i > 0) {
                log.debug("Retrying work due to concurrent update (" + i + "): " + this);
                log.trace("Concurrent update", suppressed);
            }
            if (ExceptionUtils.hasInterruptedCause(suppressed)) {
                // if we're here suppressed != null so we destroy SequenceTracer
                log.debug("No need to retry the work with id=" + getId() + ", work manager is shutting down");
                break;
            }
            try {
                runWorkWithTransaction();
                SequenceTracer.stop("Work done " + (completionTime - startTime) + " ms");
                return;
            } catch (RuntimeException e) {
                if (suppressed == null) {
                    suppressed = e;
                } else {
                    suppressed.addSuppressed(e);
                }
            }
        }

        workFailed(suppressed);
    }

    /**
     * Builds failure event properties. Work implementations can override this method to inject
     * more event properties than the default.
     * @since 10.1
     */
    public Map<String, Serializable> buildWorkFailureEventProps(RuntimeException exception) {

        Map<String, Serializable> eventProps = new HashMap<>();
        eventProps.put(WORK_INSTANCE, this);  // Work objects are serializable so send the whole thing

        if (session != null) {
            eventProps.put(REPOSITORY_NAME, session.getRepositoryName());
        }

        if (exception != null) {
            eventProps.put(FAILURE_MSG, exception.getMessage());
            eventProps.put(FAILURE_EXCEPTION, exception.getClass().getName());
        }
        return eventProps;
    }

    /**
     * Called when the worker failed to run successfully even after retrying.
     * @since 10.1
     * @param exception the exception that occurred
     */
    public void workFailed(RuntimeException exception) {

        EventService service = Framework.getService(EventService.class);
        EventContext eventContext = new EventContextImpl(null, session != null ? session.getPrincipal() : null);
        eventContext.setProperties(buildWorkFailureEventProps(exception));
        Event event = new EventImpl(WORK_FAILED_EVENT, eventContext);
        event.setIsCommitEvent(true);
        service.fireEvent(event);

        if (exception != null) {
            appendWorkToDeadLetterQueue();
            String msg = "Work failed after " + getRetryCount() + " " + (getRetryCount() == 1 ? "retry" : "retries") + ", class="
                    + getClass() + " id=" + getId() + " category=" + getCategory() + " title=" + getTitle();
            SequenceTracer.destroy("Work failure " + (completionTime - startTime) + " ms");
            // all retries have been done, throw the exception
            throw new NuxeoException(msg, exception);
        }
    }

    protected void appendWorkToDeadLetterQueue() {
        if (!State.RUNNING.equals(getWorkInstanceState())) {
            // DLQ is only for Works executed by a WorkManager, in this case they are in RUNNING state.
            return;
        }
        try {
            String key = getCategory() + ":" + getId();
            StreamService service = Framework.getService(StreamService.class);
            if (service != null) {
                service.getLogManager()
                       .getAppender(DEAD_LETTER_QUEUE)
                       .append(key, Record.of(key, WorkComputation.serialize(this)));
                MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
                registry.counter(GLOBAL_DLQ_COUNT_REGISTRY_NAME).inc();
            }
        } catch (IllegalArgumentException e) {
            log.debug("No default log manager, don't save work in failure to a dead letter queue");
        } catch (Exception e) {
            String message = "Failed to save work: " + getId() + " in dead letter queue";
            if (ExceptionUtils.hasInterruptedCause(e)) {
                // During hot reload or forced shutdown the StreamService might be unavailable
                // using warn level to prevent CI build to fail
                log.warn(message, e);
            } else {
                log.error(message, e);
            }
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
     * Does work under a transaction.
     *
     * @since 5.9.4
     */
    protected void runWorkWithTransaction() {
        TransactionHelper.startTransaction();
        boolean ok = false;
        Exception exc = null;
        try {
            WorkSchedulePath.handleEnter(this);
            // --- do work
            setStartTime();
            work(); // may throw ConcurrentUpdateException
            if (isGroupJoin() && WorkStateHelper.removeGroupJoinWork(getPartitionKey())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Detecting GroupJoin %s completion Work: %s", getPartitionKey(), getId()));
                }
                onGroupJoinCompletion();
            }
            ok = true;
            // --- end work
        } catch (Exception e) {
            exc = e;
            if (e instanceof RuntimeException) {
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
            if (ExceptionUtils.hasInterruptedCause(e)) {
                log.debug("Interrupted work: " + this);
            } else {
                if (!(e instanceof ConcurrentUpdateException)) {
                    if (!isSuspending()) {
                        log.error("Exception during work: " + this, e);
                        if (WorkSchedulePath.isCaptureStackEnabled()) {
                            WorkSchedulePath.log.error("Work schedule path", getSchedulePath().getStack());
                        }
                    }
                }
            }
        }
        closeSession();
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
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        if (docId != null) {
            sb.append(docId);
            sb.append(", ");
        } else if (docIds != null && docIds.size() > 0) {
            sb.append(docIds.get(0));
            sb.append("..., ");
        }
        sb.append(getSchedulePath().getParentPath());
        sb.append(", ");
        sb.append(getProgress());
        sb.append(", ");
        sb.append(getStatus());
        sb.append(')');
        return sb.toString();
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

    @Override
    public String getPartitionKey() {
        if (docId != null) {
            return docId;
        } else if (docIds != null && !docIds.isEmpty()) {
            return docIds.get(0);
        }
        return getId();
    }
}
