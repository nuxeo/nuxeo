/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.web.common.admin;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

import java.util.Calendar;

/**
 * Simple Helper class to hold {@link AdministrativeStatus} flags used by the Web Layer.
 *
 * @author tiry
 */
public class AdminStatusHelper {

    static boolean adminMessageActivated;

    static String adminMessage;

    static Calendar adminMessageModificationDate; // NOSONAR

    static boolean instanceInMaintenanceMode;

    static String maintenanceMessage;

    public static boolean isAdminMessageActivated() {
        return adminMessageActivated;
    }

    public static String getAdminMessage() {
        return adminMessage;
    }

    /**
     * @since 5.7.3
     */
    public static Calendar getAdminMessageModificationDate() {
        return adminMessageModificationDate;
    }

    public static boolean isInstanceInMaintenanceMode() {
        return instanceInMaintenanceMode;
    }

    public static String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public static boolean displayAdminMessage() {
        return adminMessageActivated && adminMessage != null && adminMessage.length() > 0;
    }

    public static boolean accessRestrictedToAdministrators() {
        return instanceInMaintenanceMode;
    }

}
