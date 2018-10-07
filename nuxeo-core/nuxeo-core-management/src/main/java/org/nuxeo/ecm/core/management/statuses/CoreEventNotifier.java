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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.statuses;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.InlineEventContext;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

public class CoreEventNotifier implements Notifier {

    @Override
    public void notifyEvent(String eventName, String instanceIdentifier, String serviceIdentifier) {

        Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();

        eventProperties.put("category", AdministrativeStatusManager.ADMINISTRATIVE_EVENT_CATEGORY);
        eventProperties.put(AdministrativeStatusManager.ADMINISTRATIVE_EVENT_INSTANCE, instanceIdentifier);
        eventProperties.put(AdministrativeStatusManager.ADMINISTRATIVE_EVENT_SERVICE, serviceIdentifier);

        EventContext ctx = new InlineEventContext(new SystemPrincipal(null), eventProperties);

        Event event = ctx.newEvent(eventName);
        Framework.getService(EventProducer.class).fireEvent(event);
    }

}
