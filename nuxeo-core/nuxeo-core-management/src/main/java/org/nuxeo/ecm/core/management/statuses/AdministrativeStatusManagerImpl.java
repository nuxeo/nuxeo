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

package org.nuxeo.ecm.core.management.statuses;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.management.CoreManagementService;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.storage.AdministrativeStatusPersister;

/**
 * Implementation class for the {@link AdministrativeStatusManager} service.
 * For each Nuxeo Instance in the cluster one instance of this class is
 * created.
 *
 * @author tiry
 */
public class AdministrativeStatusManagerImpl implements
        AdministrativeStatusManager, CoreManagementService {

    protected final AdministrativeStatusPersister persister;

    protected final GlobalAdministrativeStatusManager globalManager;

    protected final String serverInstanceName;

    protected Notifier[] notifiers = { new CoreEventNotifier(),
            new RuntimeEventNotifier() };

    public AdministrativeStatusManagerImpl(
            GlobalAdministrativeStatusManager globalManager,
            AdministrativeStatusPersister persister) {
        this.globalManager = globalManager;
        this.persister = persister;
        this.serverInstanceName = NuxeoInstanceIdentifierHelper.getServerInstanceName();
    }

    public AdministrativeStatusManagerImpl(
            GlobalAdministrativeStatusManager globalManager,
            AdministrativeStatusPersister persister, String instanceIdentifier) {
        this.globalManager = globalManager;
        this.persister = persister;
        this.serverInstanceName = instanceIdentifier;
    }

    protected String getServerInstanceName() {
        return serverInstanceName;
    }

    protected void notifyEvent(String eventName, String instanceIdentifier,
            String serviceIdentifier) {
        for (Notifier notifier : notifiers) {
            notifier.notifyEvent(eventName, instanceIdentifier,
                    serviceIdentifier);
        }
    }

    public void onNuxeoServerStartup() {

        List<AdministrativeStatus> savedSatuses = persister.getAllStatuses(getServerInstanceName());

        // iterate throw declared services and init them if needed
        List<AdministrableServiceDescriptor> descs = globalManager.listRegistredServices();

        for (AdministrableServiceDescriptor desc : descs) {
            boolean serviceExist = false;
            for (AdministrativeStatus status : savedSatuses) {
                if (desc.getId().equals(status.getServiceIdentifier())) {
                    serviceExist = true;
                    break;
                }
            }
            if (!serviceExist) {
                AdministrativeStatus newStatus = new AdministrativeStatus(
                        desc.getInitialState(), "", Calendar.getInstance(),
                        "system", getServerInstanceName(), desc.getId());
                persister.saveStatus(newStatus);
            }
        }

        savedSatuses = persister.getAllStatuses(getServerInstanceName());
        for (AdministrativeStatus status : savedSatuses) {
            notifyOnStatus(status);
        }
    }

    public void onNuxeoServerShutdown() {

    }

    protected void notifyOnStatus(AdministrativeStatus status) {
        if (status.isActive()) {
            notifyEvent(AdministrativeStatusManager.ACTIVATED_EVENT,
                    status.getInstanceIdentifier(),
                    status.getServiceIdentifier());
        } else if (status.isPassive()) {
            notifyEvent(AdministrativeStatusManager.PASSIVATED_EVENT,
                    status.getInstanceIdentifier(),
                    status.getServiceIdentifier());
        }
    }

    @Override
    public AdministrativeStatus activateNuxeoInstance(String message,
            String login) {
        return activate(
                AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY,
                message, login);
    }

    @Override
    public AdministrativeStatus deactivateNuxeoInstance(String message,
            String login) {
        return deactivate(
                AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY,
                message, login);
    }

    @Override
    public AdministrativeStatus getNuxeoInstanceStatus() {
        return getStatus(AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY);
    }

    @Override
    public AdministrativeStatus setNuxeoInstanceStatus(String state,
            String message, String login) {
        return setStatus(
                AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY,
                state, message, login);
    }

    @Override
    public AdministrativeStatus activate(String serviceIdentifier,
            String message, String login) {
        return setStatus(serviceIdentifier, AdministrativeStatus.ACTIVE,
                message, login);
    }

    @Override
    public AdministrativeStatus deactivate(String serviceIdentifier,
            String message, String login) {
        return setStatus(serviceIdentifier, AdministrativeStatus.PASSIVE,
                message, login);
    }

    @Override
    public AdministrativeStatus setStatus(String serviceIdentifier,
            String state, String message, String login) {
        AdministrativeStatus status = new AdministrativeStatus(state, message,
                Calendar.getInstance(), login, serverInstanceName,
                serviceIdentifier);
        status = persister.saveStatus(status);
        notifyOnStatus(status);
        return addLabelAndDescription(status);
    }

    @Override
    public List<AdministrativeStatus> getAllStatuses() {
        List<AdministrativeStatus> statuses = persister.getAllStatuses(serverInstanceName);
        for (AdministrativeStatus status : statuses) {
            addLabelAndDescription(status);
        }
        return statuses;
    }

    protected AdministrativeStatus addLabelAndDescription(
            AdministrativeStatus status) {
        if (status == null) {
            return null;
        }
        String id = status.getServiceIdentifier();
        AdministrableServiceDescriptor desc = globalManager.getServiceDescriptor(id);
        if (desc != null) {
            status.setLabelAndDescription(desc.getLabel(),
                    desc.getDescription());
        }
        return status;
    }

    @Override
    public AdministrativeStatus getStatus(String serviceIdentifier) {
        AdministrativeStatus status = persister.getStatus(serverInstanceName,
                serviceIdentifier);
        addLabelAndDescription(status);
        return status;
    }

}
