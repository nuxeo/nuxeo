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

import java.util.Calendar;

/**
 * Simple Helper class to hold {@link AdministrativeStatus} flags used by the Web Layer.
 *
 * @author tiry
 */
public class AdminStatusHelper {

    static boolean adminMessageActivated;

    static String adminMessage;

    static Calendar adminMessageModificationDate;

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
        return adminMessageActivated && adminMessage!=null && adminMessage.length()>0;
    }

    public static boolean accessRestrictedToAdministrators() {
        return instanceInMaintenanceMode;
    }

}
