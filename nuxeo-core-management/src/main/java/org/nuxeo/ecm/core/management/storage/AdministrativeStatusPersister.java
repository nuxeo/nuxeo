/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Retrieves the {@link AdministrativeStatus} for all services of a given
     * instance
     */
    List<AdministrativeStatus> getAllStatuses(String instanceId);

    /**
     * Removes all persisted states for a given instance.
     */
    void remove(String instanceId);

}
