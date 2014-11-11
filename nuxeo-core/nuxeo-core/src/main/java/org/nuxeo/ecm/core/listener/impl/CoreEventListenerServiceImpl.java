/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.core.listener.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.listener.EventListenerOrderComparator;
import org.nuxeo.ecm.core.listener.extensions.CoreEventListenerDescriptor;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Repository listener service.
 * <p>
 * Service dedicated to the processing of repository events. This service exists
 * for evident performance reasons.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@SuppressWarnings({ "SuppressionAnnotation" })
public class CoreEventListenerServiceImpl extends DefaultComponent implements
        CoreEventListenerService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.listener.CoreEventListenerService");

    private static final Log log = LogFactory.getLog(CoreEventListenerServiceImpl.class);

    private final ListenerList eventListeners = new ListenerList();

    public void addEventListener(EventListener listener) {
        eventListeners.add(listener);
    }

    public void notifyEventListeners(CoreEvent coreEvent) {
        if (coreEvent != null) {
            Object source = coreEvent.getSource();
            if (source instanceof Document) {
                log.error(String.format(
                        "NXP-666: event with id %s should send "
                                + " document model instead of document",
                        coreEvent.getEventId()));
                return;
            }
            String eventId = coreEvent.getEventId();
            List<EventListener> listeners = new ArrayList<EventListener>();
            for (Object object : eventListeners.getListeners()) {
                EventListener listener = (EventListener) object;
                if (listener.accepts(eventId)) {
                    listeners.add(listener);
                }
            }
            // sort by order
            Collections.sort(listeners, new EventListenerOrderComparator());
            for (EventListener listener : listeners) {
                try {
                    listener.handleEvent(coreEvent);
                } catch (Exception e) {
                    // TODO declare specific exception that a core event might
                    // throw so that we can chose if the exception has to be
                    // ignored or has to interrupt the event notifications
                    // chain.
                    log.error("Error during notification for event: "
                            + coreEvent.getEventId(), e);
                }
            }
        }
    }

    public void removeEventListener(EventListener listener) {
        eventListeners.remove(listener);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        super.registerExtension(extension);
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("listener")) {
                for (Object contribution : contributions) {
                    CoreEventListenerDescriptor desc = (CoreEventListenerDescriptor) contribution;
                    EventListener listener = (EventListener) extension.getContext().loadClass(
                            desc.getClassName()).newInstance();
                    listener.setName(desc.getName());
                    listener.setOrder(desc.getOrder());
                    String[] eventIds = desc.getEventIds();
                    if (eventIds != null) {
                        for (String eventId : eventIds) {
                            listener.addEventId(eventId);
                        }
                    }
                    addEventListener(listener);
                    log.info("Repository listener with name=" + desc.getName()
                            + " has  been registered !");
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("listener")) {
                for (Object contribution : contributions) {
                    CoreEventListenerDescriptor desc = (CoreEventListenerDescriptor) contribution;
                    removeEventListener(getEventListenerByName(desc.getName()));
                    log.info("Repository listener with name=" + desc.getName()
                            + " has been unregistered");

                }
            }
        }
        super.unregisterExtension(extension);
    }

    public EventListener getEventListenerByName(String name) {
        EventListener listener = null;
        for (EventListener elistener : getEventListeners()) {
            if (elistener.getName().equals(name)) {
                listener = elistener;
                break;
            }
        }
        return listener;
    }

    public Collection<EventListener> getEventListeners() {
        Collection<EventListener> listeners = new ArrayList<EventListener>();
        for (Object object : eventListeners.getListenersCopy()) {
            listeners.add((EventListener) object);
        }
        return listeners;
    }

    public void fireOperationStarted(Operation<?> command) {
        throw new UnsupportedOperationException("operation not supported");
    }

    public void fireOperationTerminated(Operation<?> command) {
        throw new UnsupportedOperationException("operation not supported");
    }

}
