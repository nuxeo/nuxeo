/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;

/**
 * Core event related constants for Nuxeo Drive.
 * 
 * @author Antoine Taillefer
 */
public final class NuxeoDriveEvents {

    private NuxeoDriveEvents() {
        // Utility class
    }

    public static final String ABOUT_TO_REGISTER_ROOT = "aboutToRegisterRoot";

    public static final String ROOT_REGISTERED = "rootRegistered";

    public static final String ABOUT_TO_UNREGISTER_ROOT = "aboutToUnRegisterRoot";

    public static final String ROOT_UNREGISTERED = "rootUnregistered";

    public static final String IMPACTED_USERNAME_PROPERTY = "impactedUserName";

    public static final Serializable EVENT_CATEGORY = "NuxeoDrive";

    public static final String VIRTUAL_EVENT_CREATED = "virtualEventCreated";

    public static final String DELETED_EVENT = "deleted";

    public static final String SECURITY_UPDATED_EVENT = "securityUpdated";

    public static final String MOVED_EVENT = "moved";

}
