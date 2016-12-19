/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.management.test.statuses;

import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class RuntimeListener implements EventListener {

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
    public void handleEvent(Event event) {
        String eventId = event.getId();
        String instanceId = (String) event.getSource();
        String serviceId = (String) event.getData();

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
