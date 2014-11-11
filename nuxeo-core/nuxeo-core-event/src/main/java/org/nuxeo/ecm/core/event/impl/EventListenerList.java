/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    protected final List<EventListenerDescriptor> enabledInlineListenersDescriptors = new ArrayList<EventListenerDescriptor>();
    protected final List<EventListenerDescriptor> enabledSyncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();
    protected final List<EventListenerDescriptor> enabledAsyncPostCommitListenersDescriptors = new ArrayList<EventListenerDescriptor>();

    protected final List<String> listenerNames = new ArrayList<String>();

    protected boolean enabledFilteringDone = false;

    public void add(EventListenerDescriptor descriptor) throws Exception {

        enabledFilteringDone=false;
        // merge if necessary
        if (listenerNames.contains(descriptor.getName())) {
            descriptor = mergeDescriptor(descriptor);
        }

        // checkListener
        descriptor.initListener();

        if (descriptor.isPostCommit) {
            if (descriptor.isAsync) {
                asyncPostCommitListenersDescriptors.add(descriptor);
                Collections.sort(asyncPostCommitListenersDescriptors, new EventListenerDescriptorComparator());
            }
            else {
                syncPostCommitListenersDescriptors.add(descriptor);
                Collections.sort(syncPostCommitListenersDescriptors, new EventListenerDescriptorComparator());
            }

        } else {
            inlineListenersDescriptors.add(descriptor);
            Collections.sort(inlineListenersDescriptors, new EventListenerDescriptorComparator());
        }

        listenerNames.add(descriptor.getName());
    }

    protected EventListenerDescriptor mergeDescriptor(EventListenerDescriptor descriptor) {
        EventListenerDescriptor existingDesc = getDescriptor(descriptor.getName());
        removeDescriptor(existingDesc);
        existingDesc.merge(descriptor);
        return existingDesc;
    }

    public void removeDescriptor(EventListenerDescriptor descriptor) {
        enabledFilteringDone=false;
        if (listenerNames.contains(descriptor.getName())) {
            if (descriptor.isPostCommit) {
                if (descriptor.isAsync) {
                    asyncPostCommitListenersDescriptors.remove(descriptor);
                } else {
                    syncPostCommitListenersDescriptors.remove(descriptor);
                }
            } else {
                inlineListenersDescriptors.remove(descriptor);
            }
            listenerNames.remove(descriptor.getName());
        }
    }

    public EventListenerDescriptor getDescriptor(String listenerName) {
        if (!listenerNames.contains(listenerName)) {
            return null;
        }
        for (EventListenerDescriptor desc : inlineListenersDescriptors) {
            if (desc.getName().equals(listenerName)) {
                return desc;
            }
        }
        for (EventListenerDescriptor desc : syncPostCommitListenersDescriptors) {
            if (desc.getName().equals(listenerName)) {
                return desc;
            }
        }
        for (EventListenerDescriptor desc : asyncPostCommitListenersDescriptors) {
            if (desc.getName().equals(listenerName)) {
                return desc;
            }
        }
        return null;
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
        enabledAsyncPostCommitListenersDescriptors.clear();
        for (EventListenerDescriptor desc : asyncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledAsyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledSyncPostCommitListenersDescriptors.clear();
        for (EventListenerDescriptor desc : syncPostCommitListenersDescriptors) {
            if (desc.isEnabled) {
                enabledSyncPostCommitListenersDescriptors.add(desc);
            }
        }
        enabledInlineListenersDescriptors.clear();
        for (EventListenerDescriptor desc : inlineListenersDescriptors) {
            if (desc.isEnabled) {
                enabledInlineListenersDescriptors.add(desc);
            }
        }
        enabledFilteringDone=true;
    }

    public synchronized List<EventListenerDescriptor> getEnabledInlineListenersDescriptors() {
        if (!enabledFilteringDone) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(enabledInlineListenersDescriptors);
    }

    public synchronized List<EventListenerDescriptor> getEnabledSyncPostCommitListenersDescriptors() {
        if (!enabledFilteringDone) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(enabledSyncPostCommitListenersDescriptors);
    }

    public synchronized List<EventListenerDescriptor> getEnabledAsyncPostCommitListenersDescriptors() {
        if (!enabledFilteringDone) {
            recomputeEnabledListeners();
        }
        return new ArrayList<EventListenerDescriptor>(enabledAsyncPostCommitListenersDescriptors);
    }

    public List<String> getListenerNames() {
        return listenerNames;
    }

}
