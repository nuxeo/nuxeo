/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.work.WorkManagerImpl.NamedThreadFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Executor that passes an event bundle to post-commit asynchronous listeners
 * (in a separated thread in order to manage transactions).
 * <p>
 * Allows a bulk mode where transaction management is not per-listener done once
 * for the whole set of listeners.
 */
public class PostCommitEventExecutor {

    private static final Log log = LogFactory.getLog(PostCommitEventExecutor.class);

    public static final int DEFAULT_TIMEOUT_MS = 300; // 300ms

    public static final String DEFAULT_BULK_TIMEOUT_S = "600"; // 10min

    public static final String BULK_TIMEOUT_PROP = "org.nuxeo.ecm.core.event.tx.BulkExecutor.timeout";

    protected final ExecutorService executor;

    public PostCommitEventExecutor() {
        // Won't grow to its max because we wait for tasks to finish
        // synchronously, but we have to be big enough for all sessions.
        int maxPoolSize = 100;
        ThreadFactory threadFactory = new NamedThreadFactory(
                "Nuxeo-Event-PostCommit-");
        executor = new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                threadFactory);
    }

    public void shutdown(long timeoutMillis) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
    }

    public void run(List<EventListenerDescriptor> listeners, EventBundle event) {
        run(listeners, event, DEFAULT_TIMEOUT_MS, false);
    }

    public void runBulk(List<EventListenerDescriptor> listeners,
            EventBundle event) {
        String timeoutSeconds = Framework.getProperty(BULK_TIMEOUT_PROP,
                DEFAULT_BULK_TIMEOUT_S);
        run(listeners, event, Long.parseLong(timeoutSeconds) * 1000, true);
    }

    public void run(List<EventListenerDescriptor> listeners,
            EventBundle bundle, long timeoutMillis, boolean bulk) {
        // check that there's at list one listener interested
        boolean some = false;
        for (EventListenerDescriptor listener : listeners) {
            if (listener.acceptBundle(bundle)) {
                some = true;
                break;
            }
        }
        if (!some) {
            if (log.isDebugEnabled()) {
                log.debug("Events postcommit execution has nothing to do");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Events postcommit execution starting with timeout %sms%s",
                    Long.valueOf(timeoutMillis), bulk ? " in bulk mode" : ""));
        }

        Callable<Boolean> callable = !bulk ? new EventBundleRunner(listeners,
                bundle) : new EventBundleBulkRunner(listeners, bundle);
        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(callable);
        executor.execute(futureTask);
        try {
            // wait for runner to be finished, with timeout
            Boolean ok = futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (Boolean.FALSE.equals(ok)) {
                log.error("Events postcommit bulk execution aborted due to previous error");
            }
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            // interrupt thread
            futureTask.cancel(true); // mayInterruptIfRunning=true
        } catch (TimeoutException e) {
            if (!bulk) {
                log.warn(String.format(
                        "Events postcommit execution exceeded timeout of %sms, leaving thread running",
                        Long.valueOf(timeoutMillis)));
                // don't cancel task, let it run
            } else {
                log.error(String.format(
                        "Events postcommit bulk execution exceeded timeout of %sms, interrupting thread",
                        Long.valueOf(timeoutMillis)));
                futureTask.cancel(true); // mayInterruptIfRunning=true
            }
        } catch (ExecutionException e) {
            log.error(
                    "Events postcommit execution encountered unexpected exception",
                    e.getCause());
        }

        if (log.isDebugEnabled()) {
            log.debug("Events postcommit execution finished");
        }
    }

    /**
     * Lets the listeners process the event bundle.
     * <p>
     * For each listener, the event bundle is reconnected to a session and a
     * transaction is started.
     * <p>
     * In case of exception in a listener, the transaction is rolled back for
     * that listener but processing continues for the other listeners.
     * <p>
     * In case of timeout, an error is logged but processing continues for the
     * other listeners (the thread is left running separately from the main
     * thread that initiated post-commit processing).
     */
    protected static class EventBundleRunner implements Callable<Boolean> {

        protected final List<EventListenerDescriptor> listeners;

        protected final EventBundle bundle;

        public EventBundleRunner(List<EventListenerDescriptor> listeners,
                EventBundle bundle) {
            this.listeners = listeners;
            this.bundle = bundle;
        }

        @Override
        public Boolean call() {
            if (log.isDebugEnabled()) {
                log.debug("Events postcommit execution starting in thread: "
                        + Thread.currentThread().getName());
            }
            long t0 = System.currentTimeMillis();
            EventStats stats = Framework.getLocalService(EventStats.class);

            for (EventListenerDescriptor listener : listeners) {
                EventBundle filtered = listener.filterBundle(bundle);
                if (filtered.isEmpty()) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Events postcommit execution start for listener: "
                            + listener.getName());
                }
                long t1 = System.currentTimeMillis();

                boolean ok = false;
                ReconnectedEventBundle reconnected = null;
                // transaction timeout is managed by the FutureTask
                boolean tx = TransactionHelper.startTransaction();
                try {
                    reconnected = new ReconnectedEventBundleImpl(filtered,
                            listeners.toString());

                    listener.asPostCommitListener().handleEvent(reconnected);

                    if (Thread.currentThread().isInterrupted()) {
                        log.error("Events postcommit execution interrupted for listener: "
                                + listener.getName());
                        ok = false;
                    } else {
                        ok = true;
                    }
                } catch (ClientException e) {
                    log.error(
                            "Events postcommit execution encountered exception for listener: "
                                    + listener.getName(), e);
                    // don't rethrow, but rollback (ok=false) and continue loop
                } catch (RuntimeException e) {
                    log.error(
                            "Events postcommit execution encountered exception for listener: "
                                    + listener.getName(), e);
                    // don't rethrow, but rollback (ok=false) and continue loop
                } finally {
                    try {
                        if (reconnected != null) {
                            reconnected.disconnect();
                        }
                    } finally {
                        if (tx) {
                            if (!ok) {
                                TransactionHelper.setTransactionRollbackOnly();
                                log.error("Rolling back transaction");
                            }
                            TransactionHelper.commitOrRollbackTransaction();
                        }
                        if (stats != null) {
                            stats.logAsyncExec(listener,
                                    System.currentTimeMillis() - t1);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Events postcommit execution end for listener: "
                                    + listener.getName()
                                    + " in "
                                    + (System.currentTimeMillis() - t1) + "ms");
                        }
                    }
                }
                // even if interrupted due to timeout, we continue the loop
            }
            if (log.isDebugEnabled()) {
                log.debug("Events postcommit execution finished in "
                        + (System.currentTimeMillis() - t0) + "ms");
            }
            return Boolean.TRUE; // no error to report
        }
    }

    /**
     * Lets the listeners process the event bundle in bulk mode.
     * <p>
     * The event bundle is reconnected to a single session and a single
     * transaction is started for all the listeners.
     * <p>
     * In case of exception in a listener, the transaction is rolled back and
     * processing stops.
     * <p>
     * In case of timeout, the transaction is rolled back and processing stops.
     */
    protected static class EventBundleBulkRunner implements Callable<Boolean> {

        protected final List<EventListenerDescriptor> listeners;

        protected final EventBundle bundle;

        public EventBundleBulkRunner(List<EventListenerDescriptor> listeners,
                EventBundle bundle) {
            this.listeners = listeners;
            this.bundle = bundle;
        }

        @Override
        public Boolean call() {
            if (log.isDebugEnabled()) {
                log.debug("Events postcommit bulk execution starting in thread: "
                        + Thread.currentThread().getName());
            }
            long t0 = System.currentTimeMillis();

            boolean ok = false;
            boolean interrupt = false;
            ReconnectedEventBundle reconnected = null;
            // transaction timeout is managed by the FutureTask
            boolean tx = TransactionHelper.startTransaction();
            try {
                reconnected = new ReconnectedEventBundleImpl(bundle,
                        listeners.toString());
                for (EventListenerDescriptor listener : listeners) {
                    EventBundle filtered = listener.filterBundle(reconnected);
                    if (filtered.isEmpty()) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Events postcommit bulk execution start for listener: "
                                + listener.getName());
                    }
                    long t1 = System.currentTimeMillis();
                    try {

                        listener.asPostCommitListener().handleEvent(filtered);

                        if (Thread.currentThread().isInterrupted()) {
                            log.error("Events postcommit bulk execution interrupted for listener: "
                                    + listener.getName()
                                    + ", will rollback and abort bulk processing");
                            interrupt = true;
                        }
                    } catch (ClientException e) {
                        log.error(
                                "Events postcommit bulk execution encountered exception for listener: "
                                        + listener.getName(), e);
                        return Boolean.FALSE; // report error
                    } catch (RuntimeException e) {
                        log.error(
                                "Events postcommit bulk execution encountered exception for listener: "
                                        + listener.getName(), e);
                        return Boolean.FALSE; // report error
                    } finally {
                        if (log.isDebugEnabled()) {
                            log.debug("Events postcommit bulk execution end for listener: "
                                    + listener.getName()
                                    + " in "
                                    + (System.currentTimeMillis() - t1) + "ms");
                        }
                    }
                    if (interrupt) {
                        break;
                    }
                }
                ok = !interrupt;
            } finally {
                try {
                    if (reconnected != null) {
                        reconnected.disconnect();
                    }
                } finally {
                    if (tx) {
                        if (!ok) {
                            TransactionHelper.setTransactionRollbackOnly();
                            log.error("Rolling back transaction");
                        }
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Events postcommit bulk execution finished in "
                            + (System.currentTimeMillis() - t0) + "ms");
                }
            }
            return Boolean.TRUE; // no error to report
        }
    }
}
