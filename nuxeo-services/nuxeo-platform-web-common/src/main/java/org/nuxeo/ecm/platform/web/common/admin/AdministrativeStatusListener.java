/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.web.common.admin;

import static org.nuxeo.ecm.core.management.api.AdministrativeStatusManager.ACTIVATED_EVENT;
import static org.nuxeo.ecm.core.management.api.AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY;
import static org.nuxeo.ecm.core.management.api.AdministrativeStatusManager.PASSIVATED_EVENT;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.platform.api.login.RestrictedLoginHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Listen for {@link AdministrativeStatus} changes and set the necessary flag in {@link AdminStatusHelper} so that web
 * infrastructure can directly use the Helper.
 *
 * @author tiry
 */
public class AdministrativeStatusListener implements EventListener {

    public static final String ADM_MESSAGE_SERVICE = "org.nuxeo.ecm.administrator.message";

    protected static String localInstanceId;

    protected static String getLocalInstanceId() {
        if (localInstanceId == null) {
            GlobalAdministrativeStatusManager gasm = Framework.getLocalService(GlobalAdministrativeStatusManager.class);
            localInstanceId = gasm.getLocalNuxeoInstanceIdentifier();
        }
        return localInstanceId;
    }

    @Override
    public void handleEvent(Event event) {
        String eventId = event.getId();
        String instanceId = (String) event.getSource();
        String serviceId = (String) event.getData();

        if (!getLocalInstanceId().equals(instanceId)) {
            return;
        }

        AdministrativeStatusManager asm = Framework.getLocalService(AdministrativeStatusManager.class);

        if (serviceId.equals(GLOBAL_INSTANCE_AVAILABILITY)) {
            if (eventId.equals(ACTIVATED_EVENT)) {
                AdminStatusHelper.instanceInMaintenanceMode = false;
                RestrictedLoginHelper.setRestrictedModeActivated(false);
            }
            if (eventId.equals(PASSIVATED_EVENT)) {
                AdminStatusHelper.instanceInMaintenanceMode = true;
                RestrictedLoginHelper.setRestrictedModeActivated(true);
            }

            AdminStatusHelper.maintenanceMessage = asm.getStatus(GLOBAL_INSTANCE_AVAILABILITY).getMessage();
        }

        if (serviceId.equals(ADM_MESSAGE_SERVICE)) {
            if (eventId.equals(ACTIVATED_EVENT)) {
                AdminStatusHelper.adminMessageActivated = true;
            }
            if (eventId.equals(PASSIVATED_EVENT)) {
                AdminStatusHelper.adminMessageActivated = false;
            }
            AdministrativeStatus status = asm.getStatus(ADM_MESSAGE_SERVICE);
            AdminStatusHelper.adminMessage = status.getMessage();
            AdminStatusHelper.adminMessageModificationDate = status.getModificationDate();

        }
    }

}
