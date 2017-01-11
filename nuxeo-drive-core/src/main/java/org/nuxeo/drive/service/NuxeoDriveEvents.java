/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Mincong Huang <mhuang@nuxeo.com>
 */
package org.nuxeo.drive.service;

/**
 * Core event related constants for Nuxeo Drive.
 *
 * @author Antoine Taillefer
 * @author Mincong Huang
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

    public static final String EVENT_CATEGORY = "NuxeoDrive";

    public static final String VIRTUAL_EVENT_CREATED = "virtualEventCreated";

    public static final String DELETED_EVENT = "deleted";

    public static final String SECURITY_UPDATED_EVENT = "securityUpdated";

    public static final String MOVED_EVENT = "moved";

}
