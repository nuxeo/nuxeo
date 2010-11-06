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

import org.nuxeo.ecm.core.management.statuses.AdministrableServiceDescriptor;

public interface GlobalAdministrativeStatusManager {

    /**
     * Return the identifier of the local Nuxeo Instance
     *
     * @return
     */
    String getLocalNuxeoInstanceIdentifier();

    /**
     * List the identifiers of all Nuxeo Instances
     *
     * @return
     */
    List<String> listInstanceIds();

    /**
     * Retrive the {@link AdministrativeStatusManager} for a given Nuxeo
     * Instance
     *
     * @param instanceIdentifier
     * @return
     */
    AdministrativeStatusManager getStatusManager(String instanceIdentifier);

    /**
     * Update the status of a service for all refistred Nuxeo Instances
     *
     * @param serviceIdentifier
     * @param state
     * @param message
     * @param login
     */
    void setStatus(String serviceIdentifier, String state, String message,
            String login);

    /**
     * List services that are declared to be administrable
     *
     * @return
     */
    List<AdministrableServiceDescriptor> listRegistredServices();

    /**
     * Get the XMAP descriptor for one service
     *
     * @param serviceIndentifier
     * @return
     */
    AdministrableServiceDescriptor getServiceDescriptor(
            String serviceIndentifier);
}
