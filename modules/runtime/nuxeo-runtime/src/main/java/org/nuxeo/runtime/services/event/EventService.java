/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.services.event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.EventService");

    private static final Logger log = LogManager.getLogger(EventService.class);

    protected static final String XP = "listeners";

    protected Map<String, ListenerList> topics;

    /** map for listeners registered programmatically . */
    protected Map<String, ListenerList> programmaticTopics;

    @Override
    public void activate(ComponentContext context) {
        programmaticTopics = new ConcurrentHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        programmaticTopics = null;
    }

    @Override
    public void start(ComponentContext context) {
        topics = new HashMap<>();
        this.<ListenerDescriptor> getRegistryContributions(XP).forEach(desc -> {
            try {
                EventListener l = desc.getListener();
                for (String topic : desc.topics) {
                    topics.computeIfAbsent(topic, k -> new ListenerList()).add(l);
                }
            } catch (ReflectiveOperationException e) {
                addRuntimeMessage(Level.ERROR, e.getMessage());
                log.error(e, e);
            }

        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        topics = null;
    }

    public void sendEvent(Event event) {
        String topic = event.getTopic();
        ListenerList list = topics == null ? null : topics.get(topic);
        ListenerList plist = programmaticTopics.get(topic);
        if (list == null && plist == null) {
            if (log.isTraceEnabled()) {
                log.trace("No listener for event topic {}", topic);
            }
        } else {
            sendEvent(list, event);
            sendEvent(plist, event);
        }
    }

    public void addListener(String topic, EventListener listener) {
        programmaticTopics.computeIfAbsent(topic, k -> new ListenerList()).add(listener);
    }

    public void removeListener(String topic, EventListener listener) {
        ListenerList list = programmaticTopics.get(topic);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                programmaticTopics.remove(topic);
            }
        }
    }

    private static void sendEvent(ListenerList list, Event event) {
        if (list == null) {
            return;
        }
        Object[] listeners = list.getListeners();
        for (Object listener : listeners) {
            ((EventListener) listener).handleEvent(event);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        return adapter == getClass() ? (T) this : null;
    }

}
