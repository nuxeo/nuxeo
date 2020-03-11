/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Fire events for Route audit logs (used by previous route content view)
 *
 * @since 5.6
 */
public class AuditEventFirer {

    static public void fireEvent(CoreSession coreSession, DocumentRouteElement element,
            Map<String, Serializable> eventProperties, String eventName, DocumentModel doc) {
        if (eventProperties == null) {
            eventProperties = new HashMap<>();
        }
        eventProperties.put(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY,
                element.getDocumentRoute(coreSession).getDocument().getId());
        eventProperties.put(DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY, element);
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        DocumentEventContext envContext = new DocumentEventContext(coreSession, coreSession.getPrincipal(), doc);
        envContext.setProperties(eventProperties);
        getEventProducer().fireEvent(envContext.newEvent(eventName));
    }

    static protected EventProducer getEventProducer() {
        return Framework.getService(EventProducer.class);
    }

}
