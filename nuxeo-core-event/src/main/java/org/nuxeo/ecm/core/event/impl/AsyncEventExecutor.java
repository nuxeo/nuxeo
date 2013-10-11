/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
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

    public void run(final List<EventListenerDescriptor> listeners,
            EventBundle bundle) {
        for (EventListenerDescriptor listener : listeners) {
            EventBundle filtered = listener.filterBundle(bundle);
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
            n += workManager.getQueueSize(queueId, null);
        }
        return n;
    }

    public int getActiveCount() {
        WorkManager workManager = getWorkManager();
        int n = 0;
        for (String queueId : workManager.getWorkQueueIds()) {
            n += workManager.getQueueSize(queueId, State.RUNNING);
        }
        return n;
    }

    protected static class ListenerWork extends AbstractWork {

        private static final long serialVersionUID = 1L;

        protected final String title;

        protected ReconnectedEventBundle bundle;

        protected String listenerName;

        protected transient EventListenerDescriptor listener;

        public ListenerWork(EventListenerDescriptor listener, EventBundle bundle) {
            super(); // random id, for unique job
            this.listenerName = listener.getName();
            if (bundle instanceof ReconnectedEventBundle) {
                this.bundle = (ReconnectedEventBundle) bundle;
            } else {
                this.bundle = new ReconnectedEventBundleImpl(bundle, listenerName);
            }
            List<String> l = new LinkedList<String>();
            List<String> docIds = new LinkedList<String>();
            String repositoryName = null;
            for (Event event : bundle) {
                String s = event.getName();
                EventContext ctx = event.getContext();
                if (ctx instanceof DocumentEventContext) {
                    DocumentModel source = ((DocumentEventContext) ctx).getSourceDocument();
                    if (source != null) {
                        s += "/" + source.getRef();
                        docIds.add(source.getId());
                        repositoryName = source.getRepositoryName();
                    }
                }
                l.add(s);
            }
            title = "Listener " + listenerName + " " + l;
            if (!docIds.isEmpty()) {
                setDocuments(repositoryName, docIds);
            }
        }

        @Override
        public String getCategory() {
            return listenerName;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public void work() throws Exception {
            EventService eventService = Framework.getLocalService(EventService.class);
            listener = eventService.getEventListener(listenerName);
            if (listener == null) {
                throw new RuntimeException("Cannot find listener: " + listenerName);
            }
            listener.asPostCommitListener().handleEvent(bundle);
        }

        @Override
        public void cleanUp(boolean ok, Exception e) {
            bundle.disconnect();
            if (e != null && !(e instanceof InterruptedException)) {
                log.error("Failed to execute async event " + bundle.getName()
                        + " on listener " + listenerName, e);
            }
            bundle = null;
            if (listener != null) {
                EventStats stats = Framework.getLocalService(EventStats.class);
                if (stats != null) {
                    stats.logAsyncExec(listener, System.currentTimeMillis()
                            - startTime);
                }
                listener = null;
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(getClass().getSimpleName());
            buf.append('(');
            buf.append(title);
            buf.append(", ");
            buf.append(getProgress());
            buf.append(", ");
            buf.append(getStatus());
            buf.append(')');
            return buf.toString();
        }
    }

}
