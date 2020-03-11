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
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.EventService");

    private static final Log log = LogFactory.getLog(EventService.class);

    private final Map<String, ListenerList> topics;

    private final Map<String, Object[]> contributions;

    public EventService() {
        topics = new HashMap<>();
        contributions = new Hashtable<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        topics.clear();
        contributions.clear();
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] descriptors = extension.getContributions();
        if (ArrayUtils.isEmpty(descriptors)) {
            return;
        }
        String name = extension.getId();
        synchronized (this) {
            for (Object desc : descriptors) {
                ListenerDescriptor lDesc = (ListenerDescriptor) desc;
                for (String topic : lDesc.topics) {
                    addListener(topic, lDesc.listener);
                }
            }
            contributions.put(name, descriptors);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
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
            if (log.isTraceEnabled()) {
                log.trace("Event sent to topic " + event.getTopic() + ". Ingnoring");
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        return adapter == getClass() ? (T) this : null;
    }

}
