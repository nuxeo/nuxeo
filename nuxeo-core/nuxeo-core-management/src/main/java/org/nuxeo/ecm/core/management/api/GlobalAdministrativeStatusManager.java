/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Retrieve the {@link AdministrativeStatusManager} for a given Nuxeo instance.
     */
    AdministrativeStatusManager getStatusManager(String instanceIdentifier);

    /**
     * Updates the status of a service for all registered Nuxeo instances.
     */
    void setStatus(String serviceIdentifier, String state, String message, String login);

    /**
     * Lists services that are declared to be administrable.
     */
    List<AdministrableServiceDescriptor> listRegistredServices();

    /**
     * Gets the XMAP descriptor for one service.
     */
    AdministrableServiceDescriptor getServiceDescriptor(String serviceIdentifier);

    /**
     * Registers a service given its descriptor.
     */
    void registerService(AdministrableServiceDescriptor desc);

}
