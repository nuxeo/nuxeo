/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.AbstractRegistry;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Registry for {@link EventHandler} contributions. Also executes events.
 */
public class EventHandlerRegistry extends AbstractRegistry {

    private static final Logger log = LogManager.getLogger(EventHandlerRegistry.class);

    // event id -> list of handlers
    protected Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();

    // event id -> list of post commit handlers
    protected Map<String, List<EventHandler>> postCommitHandlers = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        handlers.clear();
        postCommitHandlers.clear();
        super.initialize();
    }

    @Override
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        EventHandler eh = getInstance(ctx, xObject, element);
        if (eh.isPostCommit()) {
            eh.getEvents().forEach(e -> postCommitHandlers.computeIfAbsent(e, k -> new ArrayList<>()).add(eh));
        } else {
            eh.getEvents().forEach(e -> handlers.computeIfAbsent(e, k -> new ArrayList<>()).add(eh));
        }
        return null;
    }

    public List<EventHandler> getEventHandlers(String eventId) {
        return handlers.get(eventId);
    }

    public List<EventHandler> getPostCommitEventHandlers(String eventId) {
        return postCommitHandlers.get(eventId);
    }

    public Set<String> getPostCommitEventNames() {
        return postCommitHandlers.keySet();
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
        AutomationService svc = Framework.getService(AutomationService.class);
        for (EventHandler handler : handlers) {
            try (OperationContext ctx = getContext(ectx)) {
                ctx.put("Event", event);
                ctx.setCommit(saveSession); // avoid reentrant events
                if (handler.isEnabled(ctx, ectx, false)) {
                    // TODO this will save the session at each iteration!
                    svc.run(ctx, handler.getChainId());
                }
            } catch (OperationException e) {
                log.error("Failed to handle event '{}' using chain '{}'", event.getName(), handler.getChainId(), e);
                throw new NuxeoException(e);
            } catch (NuxeoException e) {
                log.error("Failed to handle event '{}' using chain '{}'", event.getName(), handler.getChainId(), e);
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
