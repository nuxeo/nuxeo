/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.download;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

public class DummyDownloadListener implements EventListener {

    public static final List<Event> EVENTS_RECEIVED = Collections.synchronizedList(new LinkedList<Event>());

    @Override
    public void handleEvent(Event event) {
        EVENTS_RECEIVED.add(event);
    }

    public static List<Event> getEvents() {
        return EVENTS_RECEIVED;
    }

    public static void clear() {
        EVENTS_RECEIVED.clear();
    }

}
