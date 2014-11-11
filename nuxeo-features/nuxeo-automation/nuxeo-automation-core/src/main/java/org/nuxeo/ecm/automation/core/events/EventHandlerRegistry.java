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

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * TODO: This service should be moved in another project, and renamed since
 * it's a service, not a simple registry...
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventHandlerRegistry {

    private static final Log log = LogFactory.getLog(OperationEventListener.class);

    protected final AutomationService svc;

    protected EventRegistry handlers;

    protected EventRegistry pchandlers;

    public EventHandlerRegistry(AutomationService svc) {
        this.svc = svc;
        handlers = new EventRegistry();
        pchandlers = new EventRegistry();
    }

    public List<EventHandler> getEventHandlers(String eventId) {
        return handlers.lookup().get(eventId);
    }

    public List<EventHandler> getPostCommitEventHandlers(String eventId) {
        return pchandlers.lookup().get(eventId);
    }

    public void putEventHandler(EventHandler handler) {
        handlers.addContribution(handler);
    }

    public synchronized void putPostCommitEventHandler(EventHandler handler) {
        pchandlers.addContribution(handler);
    }

    public synchronized void removePostCommitEventHandler(EventHandler handler) {
        pchandlers.removeContribution(handler);
    }

    public synchronized void removeEventHandler(EventHandler handler) {
        handlers.removeContribution(handler);
    }

    public synchronized void clear() {
        handlers = new EventRegistry();
        pchandlers = new EventRegistry();
    }

    public Set<String> getPostCommitEventNames() {
        return pchandlers.lookup().keySet();
    }

    public boolean acceptEvent(Event event, List<EventHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }
        EventContext ectx = event.getContext();
        OperationContext ctx;
        if (ectx instanceof DocumentEventContext) {
            ctx = new OperationContext(ectx.getCoreSession());
            ctx.setInput(((DocumentEventContext) ectx).getSourceDocument());
        } else {
            ctx = new OperationContext();
        }
        ctx.put("Event", event);
        for (EventHandler handler : handlers) {
            try {
                if (handler.isEnabled(ctx, ectx)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to check event " + event.getName()
                        + " using chain: " + handler.getChainId(), e);
            }
        }
        return false;
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
        for (EventHandler handler : handlers) {
            if (ectx instanceof DocumentEventContext) {
                ctx = new OperationContext(ectx.getCoreSession());
                ctx.setInput(((DocumentEventContext) ectx).getSourceDocument());
            } else { // not a document event .. the chain must begin with void
                // operation - session is not available.
                ctx = new OperationContext();
            }
            ctx.put("Event", event);
            ctx.setCommit(saveSession); // avoid reentrant events
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
