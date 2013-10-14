/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    protected final List<EventListenerDescriptor> inlineListenersDescriptors = new ArrayList<EventListenerDescriptor>();

    protected final List<EventListenerDescriptor> syncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();

    protected final List<EventListenerDescriptor> asyncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();

    protected volatile List<EventListenerDescriptor> enabledInlineListenersDescriptors = null;

    protected volatile List<EventListenerDescriptor> enabledSyncPostCommitListenersDescriptors = null;

    protected volatile List<EventListenerDescriptor> enabledAsyncPostCommitListenersDescriptors = null;

    protected final Map<String, EventListenerDescriptor> descriptors = new HashMap<String, EventListenerDescriptor>();

    protected synchronized void flushCache() {
        enabledAsyncPostCommitListenersDescriptors = null;
        enabledInlineListenersDescriptors = null;
        enabledSyncPostCommitListenersDescriptors = null;
    }

    public void add(EventListenerDescriptor descriptor) throws Exception {

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
                Collections.sort(asyncPostCommitListenersDescriptors,
                        new EventListenerDescriptorComparator());
            } else {
                syncPostCommitListenersDescriptors.add(descriptor);
                Collections.sort(syncPostCommitListenersDescriptors,
                        new EventListenerDescriptorComparator());
            }

        } else {
            inlineListenersDescriptors.add(descriptor);
            Collections.sort(inlineListenersDescriptors,
                    new EventListenerDescriptorComparator());
        }

        descriptors.put(descriptor.getName(), descriptor);
    }

    protected EventListenerDescriptor mergeDescriptor(
            EventListenerDescriptor descriptor) {
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
        List<EventListener> listeners = new ArrayList<EventListener>();
        for (EventListenerDescriptor desc : getEnabledInlineListenersDescriptors()) {
            listeners.add(desc.asEventListener());
        }
        return listeners;
    }

    public List<PostCommitEventListener> getSyncPostCommitListeners() {
        List<PostCommitEventListener> listeners = new ArrayList<PostCommitEventListener>();
        for (EventListenerDescriptor desc : getEnabledSyncPostCommitListenersDescriptors()) {
            listeners.add(desc.asPostCommitListener());
        }
        return listeners;
    }

    public List<PostCommitEventListener> getAsyncPostCommitListeners() {
        List<PostCommitEventListener> listeners = new ArrayList<PostCommitEventListener>();
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
        enabledAsyncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();
        for (EventListenerDescriptor desc : asyncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledAsyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledSyncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();
        for (EventListenerDescriptor desc : syncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledSyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledInlineListenersDescriptors = new ArrayList<EventListenerDescriptor>();
        for (EventListenerDescriptor desc : inlineListenersDescriptors) {
            if (desc.isEnabled) {
                enabledInlineListenersDescriptors.add(desc);
            }
        }
    }

    public synchronized List<EventListenerDescriptor> getEnabledInlineListenersDescriptors() {
        if (enabledInlineListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(
                enabledInlineListenersDescriptors);
    }

    public synchronized List<EventListenerDescriptor> getEnabledSyncPostCommitListenersDescriptors() {
        if (enabledSyncPostCommitListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(
                enabledSyncPostCommitListenersDescriptors);
    }

    public synchronized List<EventListenerDescriptor> getEnabledAsyncPostCommitListenersDescriptors() {
        if (enabledAsyncPostCommitListenersDescriptors == null) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(
                enabledAsyncPostCommitListenersDescriptors);
    }

    public List<String> getListenerNames() {
        return new ArrayList<String>(descriptors.keySet());
    }

    public boolean hasListener(String name) {
        return descriptors.containsKey(name);
    }

}
