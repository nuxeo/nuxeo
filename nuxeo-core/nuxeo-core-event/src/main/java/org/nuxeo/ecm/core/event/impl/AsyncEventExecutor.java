/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.runtime.api.Framework;

/**
 * Executor of async listeners passing them to the WorkManager.
 */
public class AsyncEventExecutor {

    private static final Log log = LogFactory.getLog(AsyncEventExecutor.class);

    public AsyncEventExecutor() {
    }

    public WorkManager getWorkManager() {
        return Framework.getLocalService(WorkManager.class);
    }

    public void init() {
        WorkManager workManager = getWorkManager();
        if (workManager != null) {
            workManager.init();
        }
    }

    public boolean shutdown(long timeoutMillis) throws InterruptedException {
        WorkManager workManager = getWorkManager();
        if (workManager == null) {
            return true;
        }
        return workManager.shutdown(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public boolean waitForCompletion(long timeoutMillis)
            throws InterruptedException {
        WorkManager workManager = getWorkManager();
        return workManager.awaitCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void run(List<EventListenerDescriptor> listeners, EventBundle bundle) {
        for (EventListenerDescriptor listener : listeners) {
            EventBundle filtered = new EventBundleImpl();
            for (Event event : bundle) {
                if (listener.getEvents() != null
                        && !listener.acceptEvent(event.getName())) {
                    continue;
                }
                PostCommitEventListener pcl = listener.asPostCommitListener();
                if (pcl instanceof PostCommitFilteringEventListener
                        && !((PostCommitFilteringEventListener) pcl).acceptEvent(event)) {
                    continue;
                }
                filtered.push(event);
            }
            if (filtered.isEmpty()) {
                continue;
            }
            getWorkManager().schedule(new ListenerWork(listener, filtered));
        }
    }

    public int getUnfinishedCount() {
        WorkManager workManager = getWorkManager();
        int n = 0;
        for (String queueId : workManager.getWorkQueueIds()) {
            n += workManager.getNonCompletedWorkSize(queueId);
        }
        return n;
    }

    public int getActiveCount() {
        WorkManager workManager = getWorkManager();
        int n = 0;
        for (String queueId : workManager.getWorkQueueIds()) {
            n += workManager.listWork(queueId, State.RUNNING).size();
        }
        return n;
    }

    protected static class ListenerWork extends AbstractWork {

        protected final String title;

        protected ReconnectedEventBundle bundle;

        protected EventListenerDescriptor listener;

        public ListenerWork(EventListenerDescriptor listener, EventBundle bundle) {
            this.listener = listener;
            if (bundle instanceof ReconnectedEventBundle) {
                this.bundle = (ReconnectedEventBundle) bundle;
            } else {
                this.bundle = new ReconnectedEventBundleImpl(bundle,
                        listener.getName());
            }
            List<String> l = new LinkedList<String>();
            for (Event event : bundle) {
                String s = event.getName();
                EventContext ctx = event.getContext();
                if (ctx instanceof DocumentEventContext) {
                    DocumentModel source = ((DocumentEventContext) ctx).getSourceDocument();
                    if (source != null) {
                        s += "/" + source.getRef();
                    }
                }
                l.add(s);
            }
            title = "Listener " + listener.getName() + " " + l;
        }

        @Override
        public String getCategory() {
            return listener.getName();
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public Collection<DocumentLocation> getDocuments() {
            // TODO
            return Collections.emptyList();
        }

        @Override
        public void work() throws Exception {
            listener.asPostCommitListener().handleEvent(bundle);
        }

        @Override
        public void cleanUp(boolean ok, Exception e) {
            bundle.disconnect();
            if (e != null && !(e instanceof InterruptedException)) {
                log.error("Failed to execute async event " + bundle.getName()
                        + " on listener " + listener.getName(), e);
            }
            EventStats stats = Framework.getLocalService(EventStats.class);
            if (stats != null) {
                stats.logAsyncExec(listener, System.currentTimeMillis() - startTime);
            }
            bundle = null;
            listener = null;
        }
    }

    // TODO still used by quota and video
    /**
     * Creates non-daemon threads at normal priority.
     */
    public static class NamedThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger();

        private final AtomicInteger threadNumber = new AtomicInteger();

        private final ThreadGroup group;

        private final String namePrefix;

        public NamedThreadFactory(String prefix) {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup()
                    : sm.getThreadGroup();
            namePrefix = prefix + ' ' + poolNumber.incrementAndGet() + '-';
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.incrementAndGet();
            Thread t = new Thread(group, r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
