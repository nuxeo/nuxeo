/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected static String threadName;

    @Override
    public void handleEvent(Event event) {
        if (threadName != null && !Thread.currentThread().getName().equals(threadName)) {
            return;
        }
        EVENTS_RECEIVED.add(event);
    }

    public static void clear() {
        EVENTS_RECEIVED.clear();
        threadName = null;
    }

    public static void clearForThisThread() {
        clear();
        threadName = Thread.currentThread().getName();
    }

}
