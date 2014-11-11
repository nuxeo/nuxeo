/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.statuses;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.InlineEventContext;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

public class CoreEventNotifier implements Notifier {

    @Override
    public void notifyEvent(String eventName, String instanceIdentifier,
            String serviceIdentifier) {

        Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();

        eventProperties.put("category",
                AdministrativeStatusManager.ADMINISTRATIVE_EVENT_CATEGORY);
        eventProperties.put(
                AdministrativeStatusManager.ADMINISTRATIVE_EVENT_INSTANCE,
                instanceIdentifier);
        eventProperties.put(
                AdministrativeStatusManager.ADMINISTRATIVE_EVENT_SERVICE,
                serviceIdentifier);

        EventContext ctx = new InlineEventContext(new SimplePrincipal(
                SecurityConstants.SYSTEM_USERNAME), eventProperties);

        Event event = ctx.newEvent(eventName);
        try {
            Framework.getService(EventProducer.class).fireEvent(event);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

}
