/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
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

    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        WorkManager workManager = getWorkManager();
        if (workManager!=null) {
            return workManager.awaitCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
        } else {
            return false;
        }
    }

    public void run(final List<EventListenerDescriptor> listeners, EventBundle bundle) {
        for (EventListenerDescriptor listener : listeners) {
            EventBundle filtered = listener.filterBundle(bundle);
            if (filtered.isEmpty()) {
                continue;
            }
            // This may be called in a transaction if event.isCommitEvent() is true or at transaction commit
            // in other cases. If the transaction has been marked rollback-only, then scheduling must discard
            // so we schedule "after commit"
            getWorkManager().schedule(new ListenerWork(listener, filtered), true);
        }
    }

    public int getUnfinishedCount() {
        WorkManager workManager = getWorkManager();
        int n = 0;
        for (String queueId : workManager.getWorkQueueIds()) {
            n += workManager.getQueueSize(queueId, State.SCHEDULED) + workManager.getQueueSize(queueId, State.RUNNING);
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

        private static final int DEFAULT_RETRY_COUNT = 2;

        protected final String title;

        protected ReconnectedEventBundle bundle;

        protected String listenerName;

        protected int retryCount;

        protected transient EventListenerDescriptor listener;

        public ListenerWork(EventListenerDescriptor listener, EventBundle bundle) {
            super(); // random id, for unique job
            listenerName = listener.getName();
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
            Integer count = listener.getRetryCount();
            retryCount = count == null ? DEFAULT_RETRY_COUNT : count.intValue();
            if (retryCount < 0) {
                retryCount = DEFAULT_RETRY_COUNT;
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
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public void work() {
            EventService eventService = Framework.getLocalService(EventService.class);
            listener = eventService.getEventListener(listenerName);
            if (listener == null) {
                throw new RuntimeException("Cannot find listener: " + listenerName);
            }
            listener.asPostCommitListener().handleEvent(bundle);
        }

        @Override
        public void cleanUp(boolean ok, Exception e) {
            super.cleanUp(ok, e);
            bundle.disconnect();
            if (e != null && !(e instanceof InterruptedException) && !(e instanceof ConcurrentUpdateException)) {
                log.error("Failed to execute async event " + bundle.getName() + " on listener " + listenerName, e);
            }
            if (listener != null) {
                EventStats stats = Framework.getLocalService(EventStats.class);
                if (stats != null) {
                    stats.logAsyncExec(listener, System.currentTimeMillis() - getStartTime());
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
