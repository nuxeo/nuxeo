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
