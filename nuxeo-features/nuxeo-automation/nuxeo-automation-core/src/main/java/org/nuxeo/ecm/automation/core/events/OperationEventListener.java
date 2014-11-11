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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationEventListener implements EventListener {

    protected EventHandlerRegistry registry;

    public void handleEvent(Event event) throws ClientException {
        if (registry == null) {
            registry = Framework.getLocalService(EventHandlerRegistry.class);
        }
        List<EventHandler> handlers = registry.getEventHandlers(event.getName());
        registry.handleEvent(event, handlers, false);
    }

}
