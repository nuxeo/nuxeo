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
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PostCommitOperationEventListener implements
        PostCommitEventListener {

    protected EventHandlerRegistry registry;

    public void handleEvent(EventBundle events) throws ClientException {
        if (registry == null) {
            registry = Framework.getLocalService(EventHandlerRegistry.class);
        }
        boolean processEvents = false;
        for (String name : registry.getPostCommitEventNames()) {
            if (events.containsEventName(name)) {
                processEvents = true;
                break;
            }
        }
        if (! processEvents) {
            return;
        }
        for (Event event : events) {
            List<EventHandler> handlers = registry.getPostCommitEventHandlers(event.getName());
            registry.handleEvent(event, handlers, true);
        }
    }

}
