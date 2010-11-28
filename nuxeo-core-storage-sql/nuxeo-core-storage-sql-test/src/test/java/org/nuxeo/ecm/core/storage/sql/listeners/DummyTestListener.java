/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
