/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public final class NotificationServiceHelper {

    // Utility class.
    private NotificationServiceHelper() {
    }

    private static UserManager userManager;

    /**
     * Locates the notification service using NXRuntime.
     */
    public static NotificationService getNotificationService() {
        return (NotificationService) Framework.getRuntime().getComponent(
                NotificationService.NAME);
    }

    public static PlacefulService getPlacefulService() {
        return (PlacefulService) Framework.getRuntime().getComponent(
                PlacefulService.ID);
//        return Framework.getService(PlacefulService.class);
    }

    public static PlacefulService getPlacefulServiceBean()
            throws ClientException {
        PlacefulService placefulService;
        try {
            placefulService = Framework.getService(PlacefulService.class);
        } catch (Exception e) {
            final String errMsg = "Error connecting to PlacefulService. "
                    + e.getMessage();
            // log.error(errMsg, e);
            throw new ClientException(errMsg, e);
        }

        if (null == placefulService) {
            throw new ClientException("PlacefulService service not bound");
        }
        return placefulService;
    }

    public static UserManager getUsersService() throws ClientException {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return userManager;
    }

}
