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
 */

package org.nuxeo.ecm.core.event.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Utility class used to manage event listeners descriptors.
 *
 * @author Thierry Delprat
 */
public class EventListenerList extends MapRegistry<EventListenerDescriptor> {

    private static final Logger log = LogManager.getLogger(EventListenerList.class);

    /**
     * Descriptors registered programmatically, see CapturingEventListener in tests.
     *
     * @since 11.5
     */
    protected final List<EventListenerDescriptor> programmaticDescriptors = new CopyOnWriteArrayList<>();

    protected final List<EventListenerDescriptor> inlineListenersDescriptors = new CopyOnWriteArrayList<>();

    protected final List<EventListenerDescriptor> syncPostCommitListenersDescriptors = new CopyOnWriteArrayList<>();

    protected final List<EventListenerDescriptor> asyncPostCommitListenersDescriptors = new CopyOnWriteArrayList<>();

    @Override
    public void initialize() {
        super.initialize();
        this.initCache();
    }

    @Override
    protected String computeId(Context ctx, XAnnotatedObject<EventListenerDescriptor> xObject, Element element) {
        String id = xObject.newInstance(ctx, element).getName();
        if (id == null) {
            // prevent NPE on map key
            id = "null";
        }
        return id;
    }

    @Override
    protected EventListenerDescriptor doRegister(Context ctx, XAnnotatedObject<EventListenerDescriptor> xObject, Element element, String extensionId) {
        EventListenerDescriptor desc = super.doRegister(ctx, xObject, element, extensionId);
        if (desc != null) {
            try {
                desc.initListener(ctx);
            } catch (RuntimeException e) {
                String msg = String.format(
                        "Failed to register event listener in component '%s': error initializing event listener '%s' (%s)",
                        extensionId, desc.getName(), e.toString());
                Framework.getRuntime()
                         .getMessageHandler()
                         .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.EXTENSION, extensionId));
            }
        }
        return desc;
    }

    public void setListenerEnabledFlag(String listenerName, boolean enabled) {
        EventListenerDescriptor desc = contributions.get(listenerName);
        if (desc == null) {
            return;
        }
        if (enabled) {
            if (disabled.remove(listenerName)) {
                log.debug("Enabled listener {}", listenerName);
            }
        } else if (!disabled.contains(listenerName)) {
            disabled.add(listenerName);
            log.debug("Disabled listener {}", listenerName);
        }
        initCache();
    }

    public void add(EventListenerDescriptor descriptor) {
        programmaticDescriptors.add(descriptor);
        updateOnAdd(descriptor);
    }

    public void removeDescriptor(EventListenerDescriptor descriptor) {
        programmaticDescriptors.remove(descriptor);
        updateOnRemove(descriptor);
    }

    protected void initCache() {
        inlineListenersDescriptors.clear();
        syncPostCommitListenersDescriptors.clear();
        asyncPostCommitListenersDescriptors.clear();
        getContributionValues().forEach(this::updateOnAdd);
        programmaticDescriptors.forEach(this::updateOnAdd);
    }

    protected void updateOnAdd(EventListenerDescriptor descriptor) {
        if (descriptor == null || !descriptor.isEnabled()) {
            return;
        }
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
    }

    protected void updateOnRemove(EventListenerDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }
        if (descriptor.isPostCommit) {
            if (descriptor.getIsAsync()) {
                asyncPostCommitListenersDescriptors.remove(descriptor);
            } else {
                syncPostCommitListenersDescriptors.remove(descriptor);
            }
        } else {
            inlineListenersDescriptors.remove(descriptor);
        }
    }

    public List<EventListener> getInLineListeners() {
        return getInlineListenersDescriptors().stream()
                                              .map(EventListenerDescriptor::asEventListener)
                                              .collect(Collectors.toList());
    }

    public List<PostCommitEventListener> getSyncPostCommitListeners() {
        return getSyncPostCommitListenersDescriptors().stream()
                                                      .map(EventListenerDescriptor::asPostCommitListener)
                                                      .collect(Collectors.toList());
    }

    public List<PostCommitEventListener> getAsyncPostCommitListeners() {
        return getAsyncPostCommitListenersDescriptors().stream()
                                                       .map(EventListenerDescriptor::asPostCommitListener)
                                                       .collect(Collectors.toList());
    }

    public List<EventListenerDescriptor> getInlineListenersDescriptors() {
        checkInitialized();
        return Collections.unmodifiableList(inlineListenersDescriptors);
    }

    public List<EventListenerDescriptor> getSyncPostCommitListenersDescriptors() {
        checkInitialized();
        return Collections.unmodifiableList(syncPostCommitListenersDescriptors);
    }

    public List<EventListenerDescriptor> getAsyncPostCommitListenersDescriptors() {
        checkInitialized();
        return Collections.unmodifiableList(asyncPostCommitListenersDescriptors);
    }

}
