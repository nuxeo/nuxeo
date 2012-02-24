/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.tx;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Runs synchronous Listeners in a separated thread in order to enable TX
 * management
 *
 * @author Thierry Delprat
 */
public class PostCommitSynchronousRunner {

    public static final int DEFAULT_TIME_OUT_MS = 300;

    private static final Log log = LogFactory.getLog(PostCommitSynchronousRunner.class);

    protected final List<EventListenerDescriptor> listeners;

    protected final ReconnectedEventBundle event;

    protected long timeout = 0;

    public PostCommitSynchronousRunner(List<EventListenerDescriptor> listeners,
            EventBundle event, long timeout) {
        this.listeners = listeners;
        if (event instanceof ReconnectedEventBundle) {
            this.event = (ReconnectedEventBundle) event;
        } else {
            this.event = new ReconnectedEventBundleImpl(event,
                    listeners.toString());
        }
        this.timeout = timeout;
    }

    public PostCommitSynchronousRunner(List<EventListenerDescriptor> listeners,
            EventBundle event) {
        this(listeners, event, DEFAULT_TIME_OUT_MS);
    }

    public void run() {
        runSync();
    }

    protected void handleUnfinishedThread(Thread runner) {
        log.warn("PostCommitListeners are too slow, check debug log ...");
        log.warn("Exit before the end of processing");
    }

    protected void runSync() {
        log.debug("Starting sync executor from Thread "
                + Thread.currentThread().getId());
        Thread runner = new Thread(getExecutor());
        runner.start();
        try {
            runner.join(timeout);
            if (runner.isAlive()) {
                handleUnfinishedThread(runner);
            }
        } catch (InterruptedException e) {
            log.error("Exit before the end of processing", e);
        }
        log.debug("Terminated sync executor from Thread "
                + Thread.currentThread().getId());
    }

    protected Runnable getExecutor() {
        return new MonoThreadExecutor();
    }

    protected class MonoThreadExecutor implements Runnable, Thread.UncaughtExceptionHandler {

        protected EventBundleTransactionHandler txh = new EventBundleTransactionHandler();

        protected EventStats getEventStats() {
            try {
                return Framework.getService(EventStats.class);
            } catch (Exception e) {
                log.warn("Failed to lookup event stats service", e);
            }
            return null;
        }

        @Override
        public void run() {
            long t0 = System.currentTimeMillis();
            log.debug("Start post commit sync execution in Thread "
                    + Thread.currentThread().getId());
            EventStats stats = getEventStats();
            for (EventListenerDescriptor listener : listeners) {
                try {
                    long t1 = System.currentTimeMillis();
                    txh.beginNewTransaction();
                    listener.asPostCommitListener().handleEvent(event);
                    event.disconnect();
                    txh.commitOrRollbackTransaction();
                    if (stats != null) {
                        stats.logAsyncExec(listener,
                                System.currentTimeMillis() - t1);
                    }
                    log.debug("End of post commit sync execution for listener "
                            + listener.getName() + " "
                            + (System.currentTimeMillis() - t1) + "ms");
                } catch (Throwable t) {
                    log.error(
                            "Exception during post commit sync execution for listener "
                                    + listener.getName(), t);
                    event.disconnect();
                    txh.rollbackTransaction();
                }
            }
            log.debug("End of all post commit sync executions : "
                    + (System.currentTimeMillis() - t0) + "ms");
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            event.disconnect();
            txh.rollbackTransaction();
        }

    }

}
