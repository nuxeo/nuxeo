/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class EventRegistry extends ContributionFragmentRegistry<EventHandler> {

    protected Map<String, List<EventHandler>> handlers = new HashMap<>();

    protected volatile Map<String, List<EventHandler>> lookup;

    @Override
    public String getContributionId(EventHandler contrib) {
        // useless, never used
        return contrib.getChainId();
    }

    @Override
    public void contributionUpdated(String id, EventHandler handler, EventHandler newOrigContrib) {
        for (String eventId : handler.getEvents()) {
            putEventHandler(eventId, handler);
        }
        lookup = null;
    }

    protected void putEventHandler(String eventId, EventHandler handler) {
        List<EventHandler> handlers = this.handlers.get(eventId);
        if (handlers == null) {
            handlers = new ArrayList<>();
        }
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
        this.handlers.put(eventId, handlers);
    }

    @Override
    public void contributionRemoved(String id, EventHandler handler) {
        for (String eventId : handler.getEvents()) {
            List<EventHandler> handlers = this.handlers.get(eventId);
            if (handlers != null) {
                Iterator<EventHandler> it = handlers.iterator();
                while (it.hasNext()) {
                    EventHandler h = it.next();
                    // TODO chainId is not really an unique ID for the event
                    // handler...
                    if (h.chainId.equals(handler.chainId)) {
                        it.remove();
                        break;
                    }
                }
            }
        }
        lookup = null;
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public EventHandler clone(EventHandler orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(EventHandler src, EventHandler dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public Map<String, List<EventHandler>> lookup() {
        Map<String, List<EventHandler>> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                if (lookup == null) {
                    lookup = new HashMap<>(handlers);
                }
                _lookup = lookup;
            }
        }
        return _lookup;
    }

}
