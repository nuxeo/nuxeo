/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.event.tx.PostCommitSynchronousRunner;

/**
 * This implementation is always recording the event even if no transaction was started.
 * If the transaction was not started, the SAVE event is used to flush the event bundle.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
public class EventServiceImpl implements EventService {

    public static final VMID VMID = new VMID();

    private static final Log log = LogFactory.getLog(EventServiceImpl.class);

    protected static final ThreadLocal<EventBundleImpl> bundle = new ThreadLocal<EventBundleImpl>() {
        @Override
        protected EventBundleImpl initialValue() {
            return new EventBundleImpl();
        }
    };

    protected final ListenerList listeners;
    protected final ListenerList postCommitListeners;
    protected final AsyncEventExecutor asyncExec;

    public EventServiceImpl() {
        EntryComparator cmp = new EntryComparator();
        listeners = new ListenerList(cmp);
        postCommitListeners = new ListenerList(cmp);
        asyncExec = AsyncEventExecutor.create();

    }

    public int getActiveAsyncTaskCount() {
        return asyncExec.getNbTasksRunning();
    }

    public void addEventListener(EventListenerDescriptor listener) {
        try {
            EventListener el = listener.asEventListener();
            if (el != null) {
                listeners.add(
                        new Entry<EventListener>(el, listener.getPriority(), listener.isAsync, listener.getEvents(), listener.getName()));
            } else {
                PostCommitEventListener pcel = listener.asPostCommitListener();
                if (pcel != null) {
                    postCommitListeners.add(
                            new Entry<PostCommitEventListener>(pcel, listener.getPriority(), listener.isAsync, listener.getEvents(), listener.getName()));
                } else {
                    log.error("Invalid event listener: class: " + listener.clazz + "; script: " + listener.script + "; context: " + listener.rc);
                }
            }
        } catch (Exception e) {
            log.error("Failed to register event listener", e);
        }
    }

    public void removeEventListener(EventListenerDescriptor listener) {
        try {
            EventListener el = listener.asEventListener();
            if (el != null) {
                listeners.remove(
                        new Entry<EventListener>(el, listener.getPriority(), listener.isAsync, listener.getEvents(), listener.getName()));
            } else {
                PostCommitEventListener pcel = listener.asPostCommitListener();
                postCommitListeners.remove(
                        new Entry<PostCommitEventListener>(pcel, listener.getPriority(), listener.isAsync, listener.getEvents(), listener.getName()));
            }
        } catch (Exception e) {
            log.error("Failed to register event listener", e);
        }
    }

    public void fireEvent(String name, EventContext context) throws ClientException {
        fireEvent(new EventImpl(name, context));
    }

    @SuppressWarnings("unchecked")
    public void fireEvent(Event event) throws ClientException {
        Object[] ar = listeners.getListeners();
        if (!event.isInline()) { // record the event
            EventBundleImpl b = bundle.get();
            b.push(event);
            // check for commit events to flush the event bundle
            if (!b.isTransacted() && event.isCommitEvent()) {
                transactionCommited();
            }
        } else {
            EventBundleImpl b = new EventBundleImpl();
            b.push(event);
            fireEventBundle(b);
        }
        String ename = event.getName();
        for (Object obj : ar) {
            Entry<EventListener> entry = (Entry<EventListener>) obj;
            if (entry.acceptEvent(ename)) {
                try {
                    entry.listener.handleEvent(event);
                }
                catch (Throwable t) {
                    log.error("Error during sync listener execution", t);
                }
                finally {
                    if (event.isMarkedForRollBack()) {
                        throw new RuntimeException("Exception during sync listener execution, rollingback");
                    }
                    if (event.isCanceled()) {
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void fireEventBundle(EventBundle event) throws ClientException {
        boolean comesFromJMS = false;

        Object[] ar = postCommitListeners.getListeners();

        if (event instanceof ReconnectedEventBundle) {
            if (((ReconnectedEventBundle) event).comesFromJMS()) {
                comesFromJMS = true;
            }
        }

        // XXX : this sorting should be done at registration time
        List<PostCommitEventListener> syncListeners = new ArrayList<PostCommitEventListener>();
        List<PostCommitEventListener> asyncListeners = new ArrayList<PostCommitEventListener>();
        for (Object obj : ar) {
            Entry<PostCommitEventListener> entry = (Entry<PostCommitEventListener>) obj;
            if (entry.async) {
                asyncListeners.add(entry.listener);
            } else {
                syncListeners.add(entry.listener);
            }
        }

        // run sync listeners
        if (comesFromJMS) {
            // when called from JMS we must skip sync listeners
            // - postComit listerers should be on the core
            // - there is no transaction started by JMS listener
            log.debug("Desactivating sync post-commit listener since we are called from JMS");
        } else {
            PostCommitSynchronousRunner syncRunner = new PostCommitSynchronousRunner(syncListeners, event);
            syncRunner.run();

        }

        // fire async listeners
        if (AsyncProcessorConfig.forceJMSUsage() && !comesFromJMS) {
            log.debug("Skipping async exec, this will be triggered via JMS");
        } else {
            asyncExec.run(asyncListeners, event);
        }
    }

    /**
     * Force sync mode. This will ignore async flags on the listeners
     *
     * @param event
     * @throws ClientException
     */
    @SuppressWarnings("unchecked")
    public void fireEventBundleSync(EventBundle event) throws ClientException {
        Object[] ar = postCommitListeners.getListeners();
        for (Object obj : ar) {
            Entry<PostCommitEventListener> entry = (Entry<PostCommitEventListener>) obj;
            entry.listener.handleEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    public List<EventListener> getEventListeners() {
        List<EventListener> result = new ArrayList<EventListener>();
        Object[] ar = postCommitListeners.getListeners();
        for (Object obj : ar) {
            result.add(((Entry<EventListener>) obj).listener);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<PostCommitEventListener> getPostCommitEventListeners() {
        List<PostCommitEventListener> result = new ArrayList<PostCommitEventListener>();
        Object[] ar = postCommitListeners.getListeners();
        for (Object obj : ar) {
            result.add(((Entry<PostCommitEventListener>) obj).listener);
        }
        return result;
    }

    public ListenerList getInternalListeners() {
        return listeners;
    }

    public ListenerList getInternalPostCommitListeners() {
        return postCommitListeners;
    }

    public void transactionStarted() {
        bundle.get().setTransacted(true);
    }

    public void transactionCommited() throws ClientException {
        EventBundleImpl b = bundle.get();
        bundle.remove();
        if (b != null && !b.isEmpty()) {
            fireEventBundle(b);
        }
    }

    public void transactionRollbacked() {
        bundle.remove();
    }

    public boolean isTransactionStarted() {
        return bundle.get().isTransacted();
    }


    protected boolean isSaveEvent(Event event) {
        return event.getName().equals(DocumentEventTypes.SESSION_SAVED);
    }

    /**
     * A listener entry having a priority
     */
    public static class Entry<T> {
        public final int priority;
        public final T listener;
        public final Set<String> events;
        public final boolean async;
        public final String name;

        public Entry(T listener, int priority, boolean async, Set<String> events, String name) {
            this.listener = listener;
            this.priority = priority;
            this.events = events;
            this.async = async;
            this.name = name;
        }

        public Entry(T listener) {
            this(listener, 0, false, null, listener.getClass().getSimpleName());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Entry<?>) {
                return listener.getClass() == ((Entry<?>) obj).listener.getClass();
            }
            return false;
        }

        public final boolean acceptEvent(String eventName) {
            return events == null || events.contains(eventName);
        }
    }

    public static class EntryComparator implements Comparator<Entry<?>> {
        public int compare(Entry<?> o1, Entry<?> o2) {
            return o1.priority - o2.priority;
        }
    }

}
