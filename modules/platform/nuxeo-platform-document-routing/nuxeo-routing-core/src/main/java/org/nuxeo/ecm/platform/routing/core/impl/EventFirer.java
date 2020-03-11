/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class EventFirer {

    static public void fireEvent(CoreSession coreSession, DocumentRouteElement element,
            Map<String, Serializable> eventProperties, String eventName) {
        if (eventProperties == null) {
            eventProperties = new HashMap<>();
        }
        eventProperties.put(DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY, element);
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
        DocumentEventContext envContext = new DocumentEventContext(coreSession, coreSession.getPrincipal(),
                element.getDocument());
        envContext.setProperties(eventProperties);
        getEventProducer().fireEvent(envContext.newEvent(eventName));
    }

    /**
     * Fires an event in the category 'routing' for every document in the list
     *
     * @since 5.7.2
     */
    static public void fireEvent(CoreSession coreSession, List<DocumentModel> docs,
            Map<String, Serializable> eventProperties, String eventName) {
        if (eventProperties == null) {
            eventProperties = new HashMap<>();
        }
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
        for (DocumentModel doc : docs) {
            DocumentEventContext envContext = new DocumentEventContext(coreSession, coreSession.getPrincipal(), doc);
            envContext.setProperties(eventProperties);
            getEventProducer().fireEvent(envContext.newEvent(eventName));
        }
    }

    static protected EventProducer getEventProducer() {
        return Framework.getService(EventProducer.class);
    }
}
