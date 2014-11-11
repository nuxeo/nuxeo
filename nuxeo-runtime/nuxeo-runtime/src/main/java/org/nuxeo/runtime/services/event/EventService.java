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

package org.nuxeo.runtime.services.event;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.RuntimeModelException;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.EventService");

    private static final Log log = LogFactory.getLog(EventService.class);

    private final Map<String, ListenerList> topics;

    // private final Map<String, Collection<Event>> pendingEvents;

    private final Map<String, Object[]> contributions;

    // private Executor threadPool = Executors.newCachedThreadPool();

    public EventService() {
        topics = new HashMap<String, ListenerList>();
        // pendingEvents = new HashMap<String, Collection<Event>>();
        contributions = new Hashtable<String, Object[]>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        topics.clear();
        contributions.clear();
    }

    @Override
    public void registerExtension(Extension extension) throws RuntimeModelException {
        Object[] descriptors = extension.getContributions();
        if (descriptors.length == 0) {
            return;
        }
        String name = extension.getId();
        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();
        synchronized (this) {
            for (Object desc : descriptors) {
                ListenerDescriptor lDesc = (ListenerDescriptor) desc;
                try {
                    for (String topic : lDesc.topics) {
                        addListener(topic, lDesc.listener);
                    }
                } catch (Exception e) {
                    errors.add(new RuntimeModelException("Cannot register extension " + desc, e));
                }
            }
            contributions.put(name, descriptors);
        }
        errors.throwOnError();
    }

    @Override
    public void unregisterExtension(Extension extension) throws RuntimeModelException {
        String name = extension.getId();
        synchronized (this) {
            Object[] descriptors = contributions.remove(name);
            if (descriptors != null) {
                for (Object desc : descriptors) {
                    ListenerDescriptor lDesc = (ListenerDescriptor) desc;
                    for (String topic : lDesc.topics) {
                        removeListener(topic, lDesc.listener);
                    }
                }
            }
        }
    }

    public void sendEvent(Event event) {
        ListenerList list = topics.get(event.getTopic());
        if (list == null) {
            // enqeueEvent(event);
            if (log.isTraceEnabled()) {
                log.trace("Event sent to topic " + event.getTopic()
                        + ". Ingnoring");
            }
        } else {
            sendEvent(list, event);
        }
    }

    public synchronized void addListener(String topic, EventListener listener) {
        ListenerList list = topics.get(topic);
        if (list == null) {
            list = new ListenerList();
            topics.put(topic, list);
            // check if any event is pending
            // Collection<Event> events = pendingEvents.remove(topic);
            // if (events != null) {
            // for (Event event : events) {
            // sendEvent(list, event);
            // }
            // }
        }
        list.add(listener);
    }

    public synchronized void removeListener(String topic, EventListener listener) {
        ListenerList list = topics.get(topic);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                topics.remove(topic);
            }
        }
    }

    private static void sendEvent(ListenerList list, Event event) {
        Object[] listeners = list.getListeners();
        for (Object listener : listeners) {
            ((EventListener) listener).handleEvent(event);
        }
    }

    // private void enqeueEvent(Event event) {
    // Collection<Event> events = pendingEvents.get(event.getTopic());
    // if (events != null) {
    // events.add(event);
    // } else {
    // events = new ArrayList<Event>();
    // events.add(event);
    // pendingEvents.put(event.getTopic(), events);
    // }
    // }

    // public void sendAsync(final Event event) {
    // threadPool.execute(new Runnable() {
    // public void run() {
    // sendEvent(event);
    // }
    // });
    // }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        return adapter == getClass() ? (T) this : null;
    }

}
