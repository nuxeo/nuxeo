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

import java.util.Comparator;
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

/**
 * This implementation is always recording the event even if no transaction was started.
 * If the transaction was not started the SAVE event is used to flush the event bundle.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventServiceImpl implements EventService {

    private static final Log log  = LogFactory.getLog(EventServiceImpl.class);

    protected final static ThreadLocal<EventBundleImpl> bundle = new ThreadLocal<EventBundleImpl>() {
        protected EventBundleImpl initialValue() { return new EventBundleImpl(); };
    };

    protected ListenerList listeners;
    protected ListenerList postCommitListeners;
    protected AssocMap eventDups;


    public EventServiceImpl() {
        EntryComparator cmp = new EntryComparator();
        this.listeners = new ListenerList(cmp);
        this.postCommitListeners = new ListenerList(cmp);
        eventDups = new AssocMap();
    }

    public void addEventListener(EventListenerDescriptor listener) {
        try {
            EventListener el = listener.asEventListener();
            if (el != null) {
                listeners.add(new Entry<EventListener>(el, listener.getPriority(), listener.getEvents()));
            } else {
                PostCommitEventListener pcel = listener.asPostCommitListener();
                if (pcel != null) {
                    postCommitListeners.add(
                            new Entry<PostCommitEventListener>(pcel, listener.getPriority(), listener.getEvents()));
                } else {
                    log.error("Invalid event listener: class: "+listener.clazz + "; script: "+listener.script+"; context: "+listener.rc);
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
                listeners.remove(new Entry<EventListener>(el, listener.getPriority(), listener.getEvents()));
            } else {
                PostCommitEventListener pcel = listener.asPostCommitListener();
                postCommitListeners.remove(
                        new Entry<PostCommitEventListener>(pcel, listener.getPriority(), listener.getEvents()));
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
        }
        String ename = event.getName();
        for (Object obj : ar) {
            Entry<EventListener> entry = (Entry<EventListener>)obj;
            if (entry.acceptEvent(ename)) {
                entry.listener.handleEvent(event);
                if (event.isCanceled()) {
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void fireEventBundle(EventBundle event) throws ClientException {
        Object[] ar = postCommitListeners.getListeners();
        for (Object obj : ar) {
            ((Entry<PostCommitEventListener>) obj).listener.handleEvent(event);
        }
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
    static class Entry<T> {
        int priority;
        T listener;
        Set<String> events;
        Entry(T listener) {
            this (listener, 0, null);
        }
        Entry(T listener, int priority, Set<String> events) {
            this.listener = listener;
            this.priority = priority;
            this.events = events;
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

    static class EntryComparator implements Comparator<Entry<?>> {
        public int compare(Entry<?> o1, Entry<?> o2) {
            return o1.priority - o2.priority;
        }
    }

}
