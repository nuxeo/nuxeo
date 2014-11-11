/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * TODO: This service should be moved in another project.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventHandlerRegistry {

    private static final Log log = LogFactory.getLog(OperationEventListener.class);

    protected final AutomationService svc;

    protected Map<String, List<EventHandler>> handlers;

    protected Map<String, List<EventHandler>> pchandlers;

    protected volatile Map<String, List<EventHandler>> lookup;

    protected volatile Map<String, List<EventHandler>> pclookup;

    public EventHandlerRegistry(AutomationService svc) {
        this.svc = svc;
        handlers = new HashMap<String, List<EventHandler>>();
        pchandlers = new HashMap<String, List<EventHandler>>();
    }

    public List<EventHandler> getEventHandlers(String eventId) {
        return lookup().get(eventId);
    }

    public List<EventHandler> getPostCommitEventHandlers(String eventId) {
        return pclookup().get(eventId);
    }

    public void putEventHandler(EventHandler handler) {
        for (String eventId : handler.getEvents()) {
            putEventHandler(eventId, handler);
        }
    }

    public synchronized void putEventHandler(String eventId,
            EventHandler handler) {
        List<EventHandler> handlers = this.handlers.get(eventId);
        if (handlers == null) {
            handlers = new ArrayList<EventHandler>();
            this.handlers.put(eventId, handlers);
        }
        handlers.add(handler);
        lookup = null;
    }

    public void putPostCommitEventHandler(EventHandler handler) {
        for (String eventId : handler.getEvents()) {
            putPostCommitEventHandler(eventId, handler);
        }
    }

    public synchronized void putPostCommitEventHandler(String eventId,
            EventHandler handler) {
        List<EventHandler> handlers = this.pchandlers.get(eventId);
        if (handlers == null) {
            handlers = new ArrayList<EventHandler>();
            this.pchandlers.put(eventId, handlers);
        }
        handlers.add(handler);
        pclookup = null;
    }

    public synchronized void removePostCommitEventHandler(EventHandler handler) {
        for (String eventId : handler.getEvents()) {
            List<EventHandler> handlers = this.pchandlers.get(eventId);
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
        pclookup = null;
    }

    public synchronized void removeEventHandler(EventHandler handler) {
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

    public synchronized void clear() {
        handlers = new HashMap<String, List<EventHandler>>();
        pchandlers = new HashMap<String, List<EventHandler>>();
        lookup = null;
        pclookup = null;
    }

    public Map<String, List<EventHandler>> lookup() {
        Map<String, List<EventHandler>> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                if (lookup == null) {
                    lookup = new HashMap<String, List<EventHandler>>(handlers);
                }
                _lookup = lookup;
            }
        }
        return _lookup;
    }

    public Map<String, List<EventHandler>> pclookup() {
        Map<String, List<EventHandler>> _lookup = pclookup;
        if (_lookup == null) {
            synchronized (this) {
                if (pclookup == null) {
                    pclookup = new HashMap<String, List<EventHandler>>(
                            pchandlers);
                }
                _lookup = pclookup;
            }
        }
        return _lookup;
    }

    public Set<String> getPostCommitEventNames() {
        return pclookup().keySet();
    }

    // TODO: impl remove handlers method? or should refactor runtime to be able
    // to redeploy only using clear() method

    public void handleEvent(Event event, List<EventHandler> handlers,
            boolean saveSession) {
        if (handlers == null || handlers.isEmpty()) {
            return; // ignore
        }

        EventContext ectx = event.getContext();
        OperationContext ctx = null;
        if (ectx instanceof DocumentEventContext) {
            ctx = new OperationContext(ectx.getCoreSession());
            ctx.setInput(((DocumentEventContext) ectx).getSourceDocument());
        } else { // not a document event .. the chain must begin with void
            // operation - session is not available.
            ctx = new OperationContext();
        }
        ctx.put("Event", event);
        ctx.setCommit(saveSession); // avoid reentrant events

        for (EventHandler handler : handlers) {
            try {
                if (handler.isEnabled(ctx, ectx)) { // TODO this will save the
                    // session at each
                    // iteration!
                    svc.run(ctx, handler.getChainId());
                }
            } catch (Exception e) {
                log.error("Failed to handle event " + event.getName()
                        + " using chain: " + handler.getChainId(), e);
            }
        }
    }

}
