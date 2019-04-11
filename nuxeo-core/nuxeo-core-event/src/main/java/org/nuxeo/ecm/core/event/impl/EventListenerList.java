/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

/**
 * Utility class used to manage event listeners descriptors.
 *
 * @author Thierry Delprat
 */
public class EventListenerList {

    protected final List<EventListenerDescriptor> inlineListenersDescriptors = new ArrayList<>();

    protected final List<EventListenerDescriptor> syncPostCommitListenersDescriptors = new ArrayList<>();

    protected final List<EventListenerDescriptor> asyncPostCommitListenersDescriptors = new ArrayList<>();

    protected volatile List<EventListenerDescriptor> enabledInlineListenersDescriptors = null;

    protected volatile List<EventListenerDescriptor> enabledSyncPostCommitListenersDescriptors = null;

    protected volatile List<EventListenerDescriptor> enabledAsyncPostCommitListenersDescriptors = null;

    protected final Map<String, EventListenerDescriptor> descriptors = new HashMap<>();

    protected synchronized void flushCache() {
        enabledAsyncPostCommitListenersDescriptors = null;
        enabledInlineListenersDescriptors = null;
        enabledSyncPostCommitListenersDescriptors = null;
    }

    public void add(EventListenerDescriptor descriptor) {

        flushCache();
        // merge if necessary
        if (descriptors.containsKey(descriptor.getName())) {
            descriptor = mergeDescriptor(descriptor);
        }

        // checkListener
        descriptor.initListener();

        if (descriptor.isPostCommit) {
            if (descriptor.getIsAsync()) {
                asyncPostCommitListenersDescriptors.add(descriptor);
                Collections.sort(asyncPostCommitListenersDescriptors, new EventListenerDescriptorComparator());
            } else {
                syncPostCommitListenersDescriptors.add(descriptor);
                Collections.sort(syncPostCommitListenersDescriptors, new EventListenerDescriptorComparator());
            }

        } else {
            inlineListenersDescriptors.add(descriptor);
            Collections.sort(inlineListenersDescriptors, new EventListenerDescriptorComparator());
        }

        descriptors.put(descriptor.getName(), descriptor);
    }

    protected EventListenerDescriptor mergeDescriptor(EventListenerDescriptor descriptor) {
        EventListenerDescriptor existingDesc = getDescriptor(descriptor.getName());
        removeDescriptor(existingDesc);
        existingDesc.merge(descriptor);
        return existingDesc;
    }

    public void removeDescriptor(EventListenerDescriptor descriptor) {
        flushCache();
        if (descriptors.containsKey(descriptor.getName())) {
            if (descriptor.isPostCommit) {
                if (descriptor.getIsAsync()) {
                    asyncPostCommitListenersDescriptors.remove(descriptor);
                } else {
                    syncPostCommitListenersDescriptors.remove(descriptor);
                }
            } else {
                inlineListenersDescriptors.remove(descriptor);
            }
            descriptors.remove(descriptor.getName());
        }
    }

    public EventListenerDescriptor getDescriptor(String listenerName) {
        return descriptors.get(listenerName);
    }

    public List<EventListener> getInLineListeners() {
        List<EventListener> listeners = new ArrayList<>();
        for (EventListenerDescriptor desc : getEnabledInlineListenersDescriptors()) {
            listeners.add(desc.asEventListener());
        }
        return listeners;
    }

    public List<PostCommitEventListener> getSyncPostCommitListeners() {
        List<PostCommitEventListener> listeners = new ArrayList<>();
        for (EventListenerDescriptor desc : getEnabledSyncPostCommitListenersDescriptors()) {
            listeners.add(desc.asPostCommitListener());
        }
        return listeners;
    }

    public List<PostCommitEventListener> getAsyncPostCommitListeners() {
        List<PostCommitEventListener> listeners = new ArrayList<>();
        for (EventListenerDescriptor desc : getEnabledAsyncPostCommitListenersDescriptors()) {
            listeners.add(desc.asPostCommitListener());
        }
        return listeners;
    }

    public List<EventListenerDescriptor> getInlineListenersDescriptors() {
        return inlineListenersDescriptors;
    }

    public List<EventListenerDescriptor> getSyncPostCommitListenersDescriptors() {
        return syncPostCommitListenersDescriptors;
    }

    public List<EventListenerDescriptor> getAsyncPostCommitListenersDescriptors() {
        return asyncPostCommitListenersDescriptors;
    }

    public synchronized void recomputeEnabledListeners() {
        enabledAsyncPostCommitListenersDescriptors = new ArrayList<>();
        for (EventListenerDescriptor desc : asyncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledAsyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledSyncPostCommitListenersDescriptors = new ArrayList<>();
        for (EventListenerDescriptor desc : syncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledSyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledInlineListenersDescriptors = new ArrayList<>();
        for (EventListenerDescriptor desc : inlineListenersDescriptors) {
            if (desc.isEnabled) {
                enabledInlineListenersDescriptors.add(desc);
            }
        }
    }

    public List<EventListenerDescriptor> getEnabledInlineListenersDescriptors() {
        if (enabledInlineListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<>(enabledInlineListenersDescriptors);
    }

    public List<EventListenerDescriptor> getEnabledSyncPostCommitListenersDescriptors() {
        if (enabledSyncPostCommitListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<>(enabledSyncPostCommitListenersDescriptors);
    }

    public List<EventListenerDescriptor> getEnabledAsyncPostCommitListenersDescriptors() {
        if (enabledAsyncPostCommitListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<>(enabledAsyncPostCommitListenersDescriptors);
    }

    public List<String> getListenerNames() {
        return new ArrayList<>(descriptors.keySet());
    }

    public boolean hasListener(String name) {
        return descriptors.containsKey(name);
    }

}
