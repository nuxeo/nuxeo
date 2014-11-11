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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.listener.EventListenerOrderComparator;
import org.nuxeo.ecm.core.listener.TransactedEventService;
import org.nuxeo.ecm.core.listener.TransactedEventServiceImpl;
import org.nuxeo.ecm.core.listener.TransactedListener;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of the repository listener
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings({ "SuppressionAnnotation" })
public class DefaultEventService extends DefaultComponent implements
        CoreEventListenerService {

    private static final Log log = LogFactory.getLog(DefaultEventService.class);

    private final ListenerList eventListeners = new ListenerList(new EventListenerOrderComparator());

    private TransactedEventService txEventMgr;

    public DefaultEventService() {
        this(Boolean.parseBoolean(Framework.getProperty("activateCoreNotificationTransaction", "true")));
    }

    public DefaultEventService(boolean activatePostCommit) {
        if (activatePostCommit) {
            log.info("Activating transaction support for core event notification");
            txEventMgr = new TransactedEventServiceImpl();
        }
    }

    public void addEventListener(EventListener listener) {
        if (txEventMgr != null && listener instanceof TransactedListener) {
            txEventMgr.addListener((TransactedListener)listener);
        } else {
            eventListeners.add(listener);
        }
    }

    public void removeEventListener(EventListener listener) {
        if (txEventMgr != null && listener instanceof TransactedListener) {
            txEventMgr.removeListener((TransactedListener)listener);
        } else {
            eventListeners.remove(listener);
        }
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

    public void notifyEventListeners(CoreEvent coreEvent) {
        if (coreEvent == null) {
            throw new IllegalArgumentException("Cannot fire null events");
        }

        // obsolete code warn
        Object source = coreEvent.getSource();
        if (source instanceof Document) {
            log.error(String.format(
                    "NXP-666: event with id %s should send "
                    + " document model instead of document",
                    coreEvent.getEventId()));
            return;
        }

        // fire the event
        fireEvent(coreEvent);
    }

    public void fireEvent(CoreEvent event) {
        // record event for post commit notifications
        if (txEventMgr != null) {
            txEventMgr.record(event);
        }
        //logEvent(event);
        // send the event to all listeners that accept it
        String eventId = event.getEventId();
        for (Object object : eventListeners.getListeners()) {
            EventListener listener = (EventListener) object;
            if (listener.accepts(eventId)) {
                try {
                    listener.handleEvent(event);
                } catch (Throwable e) {
                    // TODO declare specific exception that a core event might
                    // throw so that we can chose if the exception has to be
                    // ignored or has to interrupt the event notifications
                    // chain.
                    log.error("Error during notification for event: "
                            + event.getEventId(), e);
                }
            }
        }
    }

    public void fireOperationStarted(Operation<?> command) {
        // record event for post commit notifications
        if (txEventMgr != null) {
            txEventMgr.recordOperation(command);
        }
        for (Object object : eventListeners.getListeners()) {
            EventListener listener = (EventListener) object;
            try {
                listener.operationStarted(command);
            } catch (Throwable e) {
                // TODO declare specific exception that a core event might
                // throw so that we can chose if the exception has to be
                // ignored or has to interrupt the event notifications
                // chain.
                log.error("Error during notification for starting of command: "
                        + command.getName(), e);
            }
        }
    }

    public void fireOperationTerminated(Operation<?> command) {
        // record event for post commit notifications
        if (txEventMgr != null) {
            txEventMgr.recordOperation(command);
        }
        for (Object object : eventListeners.getListeners()) {
            EventListener listener = (EventListener) object;
            try {
                listener.operationTerminated(command);
            } catch (Throwable e) {
                // TODO declare specific exception that a core event might
                // throw so that we can chose if the exception has to be
                // ignored or has to interrupt the event notifications
                // chain.
                log.error("Error during notification for termination of command: "
                        + command.getName(), e);
            }
        }
    }

    public void transactionCommited() {
        if (txEventMgr != null) {
            if (log.isDebugEnabled()) {
                log.debug("commiting events ...");
            }
            txEventMgr.transactionCommitted();
        }
    }

    public void transactionRollbacked() {
        if (txEventMgr != null) {
            txEventMgr.transactionRollbacked();
        }
    }

    public void transactionStarted() {
        if (txEventMgr != null) {
            txEventMgr.transactionStarted();
        }
    }

    public void transactionAboutToCommit() {
        if (txEventMgr != null) {
            txEventMgr.transactionAboutToCommit();
        }
    }

    public boolean isPostCommitEnabled() {
        return txEventMgr != null;
    }


//    private void logEvent(CoreEvent event) {
//      if (log.isInfoEnabled()) {
//          String path = null;
//          String id = null;
//          if (event.getSource() instanceof DocumentModel) {
//              path = ((DocumentModel)event.getSource()).getPathAsString();
//              id = ((DocumentModel)event.getSource()).getId();
//          }
//          log.info("@@@@@@@@@@ sending event: "+event.getEventId()+" - "+event.getCategory()+" - "+id+" : "+path);
//      }
//    }

}
