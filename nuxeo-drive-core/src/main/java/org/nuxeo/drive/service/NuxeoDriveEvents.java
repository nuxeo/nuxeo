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

public interface NuxeoDriveEvents {

    String ABOUT_TO_REGISTER_ROOT = "aboutToRegisterRoot";

    String ROOT_REGISTERED = "rootRegistered";

    String ABOUT_TO_UNREGISTER_ROOT = "aboutToUnRegisterRoot";

    String ROOT_UNREGISTERED = "rootUnregistered";

    String IMPACTED_USERNAME_PROPERTY = "impactedUserName";

    Serializable EVENT_CATEGORY = "NuxeoDrive";

}
