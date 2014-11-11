/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.storage.sql.listeners;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

public class DummyTestListener implements EventListener {

    public static final List<Event> EVENTS_RECEIVED = Collections.synchronizedList(new LinkedList<Event>());

    @Override
    public void handleEvent(Event event) {
        EVENTS_RECEIVED.add(event);
    }

    public static void clear() {
        EVENTS_RECEIVED.clear();
    }

}
