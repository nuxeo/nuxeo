/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.management.api;

import java.util.List;

/**
 * Service interface used to manage AdministrativeStatus of Nuxeo's services on
 * a given Nuxeo Instance (node)
 *
 * @author tiry
 */
public interface AdministrativeStatusManager {

    public static final String ADMINISTRATIVE_INSTANCE_ID = "org.nuxeo.ecm.instance.administrative.id";

    public static final String GLOBAL_INSTANCE_AVAILABILITY = "org.nuxeo.ecm.instance.availability";

    public static final String ADMINISTRATIVE_EVENT_CATEGORY = "administrativeCategory";

    public static final String ADMINISTRATIVE_EVENT_INSTANCE = "instanceIdentifier";

    public static final String ADMINISTRATIVE_EVENT_SERVICE = "serviceIdentifier";

    public static final String ACTIVATED_EVENT = "serviceActivated";

    public static final String PASSIVATED_EVENT = "servicePassivated";

    /**
     * List {@link AdministrativeStatus} for all tracked resources (Servers or
     * Services)
     *
     * @return
     */
    List<AdministrativeStatus> getAllStatuses();

    /**
     * Get the {@link AdministrativeStatus} of a given resource
     *
     * @param serviceIdentifier
     * @return
     */
    AdministrativeStatus getStatus(String serviceIdentifier);

    /**
     * Get the {@link AdministrativeStatus} of a the local Nuxeo Instance
     *
     * @return
     */
    AdministrativeStatus getNuxeoInstanceStatus();

    /**
     * Sets the {@link AdministrativeStatus} of a given resource
     *
     * @param serviceIdentifier
     * @param state
     * @param message
     * @param login
     * @return
     */
    AdministrativeStatus setStatus(String serviceIdentifier, String state,
            String message, String login);

    /**
     * Sets the {@link AdministrativeStatus} of the Local Nuxeo Instance
     *
     * @param state
     * @param message
     * @param login
     * @return
     */
    AdministrativeStatus setNuxeoInstanceStatus(String state, String message,
            String login);

    /**
     * Mark a given resource as active
     *
     * @param serviceIdentifier
     * @param login
     * @return
     */
    AdministrativeStatus activate(String serviceIdentifier, String message,
            String login);

    /**
     * Mark local Nuxeo instance as active
     *
     * @param login
     * @return
     */
    AdministrativeStatus activateNuxeoInstance(String message, String login);

    /**
     * Mark a given resource as non active
     *
     * @param serviceIdentifier
     * @param message
     * @param login
     * @return
     */
    AdministrativeStatus deactivate(String serviceIdentifier, String message,
            String login);

    /**
     * Mark local Nuxeo instance as non active
     *
     * @param message
     * @param login
     * @return
     */
    AdministrativeStatus deactivateNuxeoInstance(String message, String login);

}
