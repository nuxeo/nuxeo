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
     * Returns the identifier of the local Nuxeo Instance.
     */
    String getLocalNuxeoInstanceIdentifier();

    /**
     * Lists the identifiers of all Nuxeo Instances.
     */
    List<String> listInstanceIds();

    /**
     * Retrieve the {@link AdministrativeStatusManager} for a given Nuxeo
     * instance.
     */
    AdministrativeStatusManager getStatusManager(String instanceIdentifier);

    /**
     * Updates the status of a service for all registered Nuxeo instances.
     */
    void setStatus(String serviceIdentifier, String state, String message,
            String login);

    /**
     * Lists services that are declared to be administrable.
     */
    List<AdministrableServiceDescriptor> listRegistredServices();

    /**
     * Gets the XMAP descriptor for one service.
     */
    AdministrableServiceDescriptor getServiceDescriptor(
            String serviceIdentifier);

    /**
     * Registers a service given its descriptor.
     */
    void registerService(AdministrableServiceDescriptor desc);

}
