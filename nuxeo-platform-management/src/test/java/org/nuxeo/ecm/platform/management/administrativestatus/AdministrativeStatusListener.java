/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.management.administrativestatus;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus;

public class AdministrativeStatusListener implements EventListener {

    public static boolean serverActivatedEventTriggered = false;

    public static boolean serverPassivatedEventTriggered = false;

    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        if (eventId.equals(AdministrativeStatus.ACTIVATED_EVENT)) {
            serverActivatedEventTriggered = true;
        }
        if (eventId.equals(AdministrativeStatus.PASSIVATED_EVENT)) {
            serverPassivatedEventTriggered = true;
        }
    }


}
