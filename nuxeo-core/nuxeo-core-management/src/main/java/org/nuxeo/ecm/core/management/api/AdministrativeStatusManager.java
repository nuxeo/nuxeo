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
package org.nuxeo.ecm.core.management.api;

import java.util.List;

/**
 * Service interface used to manage AdministrativeStatus of Nuxeo's services on
 * a given Nuxeo Instance (node)
 *
 * @author tiry
 */
public interface AdministrativeStatusManager {

    String ADMINISTRATIVE_INSTANCE_ID = "org.nuxeo.ecm.instance.administrative.id";
    String GLOBAL_INSTANCE_AVAILABILITY = "org.nuxeo.ecm.instance.availability";
    String ADMINISTRATIVE_EVENT_CATEGORY = "administrativeCategory";
    String ADMINISTRATIVE_EVENT_INSTANCE = "instanceIdentifier";
    String ADMINISTRATIVE_EVENT_SERVICE = "serviceIdentifier";
    String ACTIVATED_EVENT = "serviceActivated";
    String PASSIVATED_EVENT = "servicePassivated";

    /**
     * List {@link AdministrativeStatus} for all tracked resources (Servers or
     * Services).
     */
    List<AdministrativeStatus> getAllStatuses();

    /**
     * Get the {@link AdministrativeStatus} of a given resource.
     */
    AdministrativeStatus getStatus(String serviceIdentifier);

    /**
     * Get the {@link AdministrativeStatus} of a the local Nuxeo Instance.
     */
    AdministrativeStatus getNuxeoInstanceStatus();

    /**
     * Sets the {@link AdministrativeStatus} of a given resource.
     */
    AdministrativeStatus setStatus(String serviceIdentifier, String state,
            String message, String login);

    /**
     * Sets the {@link AdministrativeStatus} of the Local Nuxeo Instance.
     */
    AdministrativeStatus setNuxeoInstanceStatus(String state, String message,
            String login);

    /**
     * Mark a given resource as active.
     */
    AdministrativeStatus activate(String serviceIdentifier, String message,
            String login);

    /**
     * Mark local Nuxeo instance as active.
     */
    AdministrativeStatus activateNuxeoInstance(String message, String login);

    /**
     * Mark a given resource as non active.
     */
    AdministrativeStatus deactivate(String serviceIdentifier, String message,
            String login);

    /**
     * Mark local Nuxeo instance as non active.
     */
    AdministrativeStatus deactivateNuxeoInstance(String message, String login);

}
