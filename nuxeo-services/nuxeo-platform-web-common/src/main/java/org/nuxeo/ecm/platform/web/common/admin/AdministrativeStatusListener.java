/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.web.common.admin;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.platform.api.login.RestrictedLoginHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;


/**
 * Listen for {@link AdministrativeStatus} changes and set the necessary flag in {@link AdminStatusHelper}
 * so that web infrastructure can directly use the Helper.
 *
 * @author tiry
 *
 */
public class AdministrativeStatusListener implements EventListener {

    public static final String ADM_MESSAGE_SERVICE = "org.nuxeo.ecm.administrator.message";

    protected static String localInstanceId = null;

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    protected String getLocalInstanceId() {
        if (localInstanceId==null) {
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

          if (serviceId.equals(AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY)) {
              if (eventId.equals(AdministrativeStatusManager.ACTIVATED_EVENT)) {
                  AdminStatusHelper.instanceInMaintenanceMode=false;
                  RestrictedLoginHelper.setRestrictedModeActivated(false);
              }
              if (eventId.equals(AdministrativeStatusManager.PASSIVATED_EVENT)) {
                  AdminStatusHelper.instanceInMaintenanceMode=true;
                  RestrictedLoginHelper.setRestrictedModeActivated(true);
              }

              AdminStatusHelper.maintenanceMessage = asm.getStatus(AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY).getMessage();
          }

          if (serviceId.equals(ADM_MESSAGE_SERVICE)) {
              if (eventId.equals(AdministrativeStatusManager.ACTIVATED_EVENT)) {
                  AdminStatusHelper.adminMessageActivated=true;
              }
              if (eventId.equals(AdministrativeStatusManager.PASSIVATED_EVENT)) {
                  AdminStatusHelper.adminMessageActivated=false;
              }
              AdminStatusHelper.adminMessage = asm.getStatus(ADM_MESSAGE_SERVICE).getMessage();
          }
    }

}
