/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.types.TypeFamily;

public class EventManager implements Registrable {

    private static final Log log = LogFactory.getLog(EventManager.class);

    private static final Map<EventType, List<EventListener>> listeners = new HashMap<EventType, List<EventListener>>();

    public synchronized void addListener(final EventListener listener) {
        EventType eventType = listener.getEventType();
        getListenersFor(eventType).add(listener);
        log.info("Registered EVENT LISTENER: "
                + listener.getClass().getCanonicalName() + " for EVENT: "
                + eventType.getTypeName());
    }

    public synchronized void removeListener(final AbstractEventListener listener) {
        listeners.get(listener.getEventType()).remove(listener);
    }

    public List<EventListener> getListenersFor(final EventType eventType) {
        if (!listeners.containsKey(eventType)) {
            listeners.put(eventType, new ArrayList<EventListener>());
        }
        return listeners.get(eventType);
    }

    public void notify(final String eventTypeName, final EventContext context) {
        for (EventListener listener : getListenersFor(getEventType(eventTypeName))) {
            listener.handle(context);
        }
    }

    public EventType getEventType(final String eventTypeName) {
        return (EventType) Manager.getTypeRegistry().lookup(TypeFamily.EVENT,
                eventTypeName);
    }

    public synchronized void clear() {
        listeners.clear();
    }

}
