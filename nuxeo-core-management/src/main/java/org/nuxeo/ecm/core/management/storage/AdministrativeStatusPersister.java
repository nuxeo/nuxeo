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

package org.nuxeo.ecm.core.management.storage;

import java.util.List;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

public interface AdministrativeStatusPersister {

    /**
     * Lists all instance identifiers persisted in the backend
     *
     * @return
     */
    List<String> getAllInstanceIds();

    /**
     * Saves the {@link AdministrativeStatus} of a service of a given instance
     *
     * @param instanceId
     * @param serviceIdentifier
     * @param status
     * @return
     */
    AdministrativeStatus saveStatus(AdministrativeStatus status);

    /**
     * Reads the {@link AdministrativeStatus} of a service for a given instance
     *
     * @param instanceId
     * @param serviceIdentifier
     * @return
     */
    AdministrativeStatus getStatus(String instanceId, String serviceIdentifier);

    /**
     * Retrieves the {@link AdministrativeStatus} for all services of a given
     * instance
     *
     * @param instanceId
     * @return
     */
    List<AdministrativeStatus> getAllStatuses(String instanceId);

    /**
     * Removes all persisted states for a given instance
     *
     * @param instanceId
     */
    void remove(String instanceId);

}
