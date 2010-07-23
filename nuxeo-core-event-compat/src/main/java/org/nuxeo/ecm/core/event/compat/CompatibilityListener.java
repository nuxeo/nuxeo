/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.compat;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompatibilityListener implements EventListener {

    private CoreEventListenerService service;

    public CoreEventListenerService getEventService() {
        if (service == null) {
            service = Framework.getLocalService(CoreEventListenerService.class);
        }
        return service;
    }

    public static CoreEvent toCoreEvent(Event event) {
        EventContext ctx = event.getContext();
        Object[] args = ctx.getArguments();
        Object src = args.length > 0 ? args[0] : null;
        // category and comment are passed through the context properties
        return new CoreEventImpl(event.getName(), src, ctx.getProperties(), ctx.getPrincipal(), null, null);
    }

    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        if (eventId.startsWith("!OPERATION_")) {
            Object[] args = event.getContext().getArguments();
            if (args.length == 1 && args[0] instanceof Operation<?>) {
                if (eventId.equals("!OPERATION_START!")) {
                    getEventService().fireOperationStarted((Operation<?>)args[0]);
                } else if (eventId.equals("!OPERATION_END!")) {
                    getEventService().fireOperationTerminated((Operation<?>)args[0]);
                }
            }
        } else {
            getEventService().notifyEventListeners(toCoreEvent(event));
        }
    }


    /* old notify implementation - TODO must delete this after commiting new event impl.
    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Object> options, String category, String comment,
            boolean withLifeCycle) {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (options == null) {
            options = new HashMap<String, Object>();
        }

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, repositoryName);

        // Document life cycle
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = null;
            try {
                currentLifeCycleState = source.getCurrentLifeCycleState();
            } catch (ClientException err) {
                // FIXME no lifecycle -- this shouldn't generated an
                // exception (and ClientException logs the spurious error)
            }
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
        }
        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, sessionId);

        CoreEvent coreEvent = new CoreEventImpl(eventId, source, options,
                getPrincipal(), category, comment);

        CoreEventListenerService service = NXCore.getCoreEventListenerService();

        if (service != null) {
            service.notifyEventListeners(coreEvent);
        } else {
            log.debug("No CoreEventListenerService, cannot notify event "
                    + eventId);
        }
    }
    */
}
