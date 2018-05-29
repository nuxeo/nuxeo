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
package org.nuxeo.ecm.automation.core.events;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * TODO: This service should be moved in another project, and renamed since it's a service, not a simple registry...
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

        try (OperationContext ctx = open(event)) {
            EventContext ectx = event.getContext();
            ctx.put("Event", event);
            for (EventHandler handler : handlers) {
                if (handler.isEnabled(ctx, ectx, true)) {
                    return true;
                }
            }
            return false;
        }
    }

    protected OperationContext open(Event event) {
        EventContext ectx = event.getContext();
        if (ectx instanceof DocumentEventContext) {
            OperationContext ctx = new OperationContext(ectx.getCoreSession());
            ctx.setInput(((DocumentEventContext) ectx).getSourceDocument());
            return ctx;
        }
        return new OperationContext();
    }

    // TODO: impl remove handlers method? or should refactor runtime to be able
    // to redeploy only using clear() method

    public void handleEvent(Event event, List<EventHandler> handlers, boolean saveSession) {
        if (handlers == null || handlers.isEmpty()) {
            return; // ignore
        }

        EventContext ectx = event.getContext();
        for (EventHandler handler : handlers) {
            try (OperationContext ctx = getContext(ectx)) {
                ctx.put("Event", event);
                ctx.setCommit(saveSession); // avoid reentrant events
                if (handler.isEnabled(ctx, ectx, false)) {
                    // TODO this will save the session at each iteration!
                    svc.run(ctx, handler.getChainId());
                }
            } catch (OperationException e) {
                log.error("Failed to handle event " + event.getName() + " using chain: " + handler.getChainId(), e);
                throw new NuxeoException(e);
            } catch (NuxeoException e) {
                log.error("Failed to handle event " + event.getName() + " using chain: " + handler.getChainId(), e);
                throw e;
            }
        }
    }

    protected OperationContext getContext(EventContext ectx) {
        if (ectx instanceof DocumentEventContext) {
            OperationContext ctx = new OperationContext(ectx.getCoreSession());
            ctx.setInput(((DocumentEventContext) ectx).getSourceDocument());
            return ctx;
        }
        // not a document event .. the chain must begin with void
        // operation - session is not available.
        return new OperationContext();
    }

}
