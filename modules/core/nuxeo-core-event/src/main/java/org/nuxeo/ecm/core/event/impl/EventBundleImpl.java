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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventBundleImpl implements EventBundle {

    private static final long serialVersionUID = 1L;

    // not final to allow modification derived classes
    protected VMID vmid;

    protected final List<Event> events;

    protected final Set<String> eventNames;

    public EventBundleImpl(VMID sourceVMID) {
        events = new ArrayList<>();
        eventNames = new HashSet<>();
        vmid = sourceVMID;
    }

    public EventBundleImpl() {
        this(EventServiceImpl.VMID);
    }

    @Override
    public boolean hasRemoteSource() {
        return !vmid.equals(EventServiceImpl.VMID);
    }

    @Override
    public String getName() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(0).getContext().getRepositoryName();
    }

    @Override
    public boolean isEmpty() {
        return events.isEmpty();
    }

    @Override
    public Event peek() {
        return events.get(0);
    }

    @Override
    public void push(Event event) {
        events.add(event);
        String eventName = event.getName();
        if (eventName != null) {
            eventNames.add(eventName);
        }
    }

    @Override
    public int size() {
        return events.size();
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

    @Override
    public VMID getSourceVMID() {
        return vmid;
    }

    @Override
    public boolean containsEventName(String eventName) {
        if (eventName == null) {
            return false;
        }
        return eventNames.contains(eventName);
    }

}
