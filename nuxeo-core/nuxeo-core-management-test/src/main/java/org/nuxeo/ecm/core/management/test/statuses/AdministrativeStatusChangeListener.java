/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.test.statuses;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;

public class AdministrativeStatusChangeListener implements EventListener {

    protected static boolean serverActivatedEventTriggered = false;

    protected static boolean serverPassivatedEventTriggered = false;

    public static void init() {
        serverActivatedEventTriggered = false;
        serverPassivatedEventTriggered = false;
    }

    public static boolean isServerActivatedEventTriggered() {
        return serverActivatedEventTriggered;
    }

    public static boolean isServerPassivatedEventTriggered() {
        return serverPassivatedEventTriggered;
    }

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        String serviceId = (String) event.getContext().getProperty(
                AdministrativeStatusManager.ADMINISTRATIVE_EVENT_SERVICE);
        String instanceId = (String) event.getContext().getProperty(
                AdministrativeStatusManager.ADMINISTRATIVE_EVENT_INSTANCE);

        if (serviceId.equals(AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY)) {
            if (eventId.equals(AdministrativeStatusManager.ACTIVATED_EVENT)) {
                serverActivatedEventTriggered = true;
            }
            if (eventId.equals(AdministrativeStatusManager.PASSIVATED_EVENT)) {
                serverPassivatedEventTriggered = true;
            }
        }
    }

}
