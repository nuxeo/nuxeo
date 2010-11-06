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
 *     "Stephane Lacoin at Nuxeo (aka matic)"
 */
package org.nuxeo.ecm.core.management.guards;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;

/**
 * Listen to administrative status changes and cache
 * state in a map.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public class GuardsCacheUpdater implements EventListener {
    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        String category = (String) ctx.getProperty("category");
        if (!AdministrativeStatusManager.ADMINISTRATIVE_EVENT_CATEGORY.equals(category)) {
            return;
        }
        String id = (String) ctx.getProperty(AdministrativeStatusManager.ADMINISTRATIVE_EVENT_SERVICE);
        if (AdministrativeStatusManager.ACTIVATED_EVENT.equals(event.getName())) {
            GuardedServiceProvider.INSTANCE.activeStatuses.put(id, Boolean.TRUE);
        } else if (AdministrativeStatusManager.PASSIVATED_EVENT.equals(event.getName())) {
            GuardedServiceProvider.INSTANCE.activeStatuses.put(id, Boolean.FALSE);
        }
    }
}
