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

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PostCommitOperationEventListener implements PostCommitFilteringEventListener {

    @Override
    public boolean acceptEvent(Event event) {
        EventHandlerRegistry registry = Framework.getService(EventHandlerRegistry.class);
        if (!registry.getPostCommitEventNames().contains(event.getName())) {
            return false;
        }
        List<EventHandler> handlers = registry.getPostCommitEventHandlers(event.getName());
        return registry.acceptEvent(event, handlers);
    }

    @Override
    public void handleEvent(EventBundle events) {
        EventHandlerRegistry registry = Framework.getService(EventHandlerRegistry.class);
        boolean processEvents = false;
        for (String name : registry.getPostCommitEventNames()) {
            if (events.containsEventName(name)) {
                processEvents = true;
                break;
            }
        }
        if (!processEvents) {
            return;
        }
        for (Event event : events) {
            List<EventHandler> handlers = registry.getPostCommitEventHandlers(event.getName());
            registry.handleEvent(event, handlers, true);
        }
    }

}
