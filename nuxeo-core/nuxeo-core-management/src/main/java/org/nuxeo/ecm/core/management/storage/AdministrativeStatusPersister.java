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

package org.nuxeo.ecm.core.management.storage;

import java.util.List;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

public interface AdministrativeStatusPersister {

    /**
     * Lists all instance identifiers persisted in the backend.
     */
    List<String> getAllInstanceIds();

    /**
     * Saves the {@link AdministrativeStatus} of a service of a given instance
     */
    AdministrativeStatus saveStatus(AdministrativeStatus status);

    /**
     * Reads the {@link AdministrativeStatus} of a service for a given instance
     */
    AdministrativeStatus getStatus(String instanceId, String serviceIdentifier);

    /**
     * Retrieves the {@link AdministrativeStatus} for all services of a given instance
     */
    List<AdministrativeStatus> getAllStatuses(String instanceId);

    /**
     * Removes all persisted states for a given instance.
     */
    void remove(String instanceId);

}
