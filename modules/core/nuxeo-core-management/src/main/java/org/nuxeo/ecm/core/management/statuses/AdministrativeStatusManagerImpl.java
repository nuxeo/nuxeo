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

package org.nuxeo.ecm.core.management.statuses;

import static org.nuxeo.ecm.core.management.api.AdministrativeStatus.ACTIVE;
import static org.nuxeo.ecm.core.management.api.AdministrativeStatus.PASSIVE;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.management.CoreManagementService;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.storage.AdministrativeStatusPersister;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Implementation class for the {@link AdministrativeStatusManager} service. For each Nuxeo Instance in the cluster one
 * instance of this class is created.
 *
 * @author tiry
 */
public class AdministrativeStatusManagerImpl implements AdministrativeStatusManager, CoreManagementService {

    protected final AdministrativeStatusPersister persister;

    protected final GlobalAdministrativeStatusManager globalManager;

    protected final String serverInstanceName;

    protected ScheduledExecutorService scheduler;

    protected final Notifier[] notifiers = { new CoreEventNotifier(), new RuntimeEventNotifier() };

    public AdministrativeStatusManagerImpl(GlobalAdministrativeStatusManager globalManager,
            AdministrativeStatusPersister persister) {
        this.globalManager = globalManager;
        this.persister = persister;
        serverInstanceName = NuxeoInstanceIdentifierHelper.getServerInstanceName();
    }

    public AdministrativeStatusManagerImpl(GlobalAdministrativeStatusManager globalManager,
            AdministrativeStatusPersister persister, String instanceIdentifier) {
        this.globalManager = globalManager;
        this.persister = persister;
        serverInstanceName = instanceIdentifier;
    }

    protected String getServerInstanceName() {
        return serverInstanceName;
    }

    protected void notifyEvent(String eventName, String instanceIdentifier, String serviceIdentifier) {
        for (Notifier notifier : notifiers) {
            notifier.notifyEvent(eventName, instanceIdentifier, serviceIdentifier);
        }
    }

    public void onNuxeoServerStartup() {

        List<AdministrativeStatus> savedStatuses = persister.getAllStatuses(serverInstanceName);

        // iterate throw declared services and init them if needed
        List<AdministrableServiceDescriptor> descs = globalManager.listRegistredServices();

        for (AdministrableServiceDescriptor desc : descs) {
            boolean serviceExist = false;
            for (AdministrativeStatus status : savedStatuses) {
                if (desc.getId().equals(status.getServiceIdentifier())) {
                    serviceExist = true;
                    break;
                }
            }
            if (!serviceExist) {
                AdministrativeStatus newStatus = new AdministrativeStatus(desc.getInitialState(), "",
                        Calendar.getInstance(), SecurityConstants.SYSTEM_USERNAME, serverInstanceName, desc.getId());
                persister.saveStatus(newStatus);
            }
        }

        doNotifyAllStatuses();

        scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Nuxeo-Administrative-Statuses-Notify-Scheduler");
            }
        });

        scheduler.scheduleAtFixedRate(new NotifyStatusesHandler(), 5, 5, TimeUnit.MINUTES);
    }

    protected void doNotifyAllStatuses() {
        for (AdministrativeStatus status : persister.getAllStatuses(serverInstanceName)) {
            notifyOnStatus(status);
        }
    }

    public class NotifyStatusesHandler implements Runnable {
        @Override
        public void run() {
            TransactionHelper.startTransaction();
            boolean notified = false;
            try {
                doNotifyAllStatuses();
                notified = true;
            } finally {
                if (!notified) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    public void onNuxeoServerShutdown() {
        if (scheduler == null) {
            return;
        }
        try {
            scheduler.shutdown();
        } finally {
            scheduler = null;
        }
    }

    protected void notifyOnStatus(AdministrativeStatus status) {
        if (status.isActive()) {
            notifyEvent(ACTIVATED_EVENT, status.getInstanceIdentifier(), status.getServiceIdentifier());
        } else if (status.isPassive()) {
            notifyEvent(PASSIVATED_EVENT, status.getInstanceIdentifier(), status.getServiceIdentifier());
        }
    }

    @Override
    public AdministrativeStatus activateNuxeoInstance(String message, String login) {
        return activate(GLOBAL_INSTANCE_AVAILABILITY, message, login);
    }

    @Override
    public AdministrativeStatus deactivateNuxeoInstance(String message, String login) {
        return deactivate(GLOBAL_INSTANCE_AVAILABILITY, message, login);
    }

    @Override
    public AdministrativeStatus getNuxeoInstanceStatus() {
        return getStatus(GLOBAL_INSTANCE_AVAILABILITY);
    }

    @Override
    public AdministrativeStatus setNuxeoInstanceStatus(String state, String message, String login) {
        return setStatus(GLOBAL_INSTANCE_AVAILABILITY, state, message, login);
    }

    @Override
    public AdministrativeStatus activate(String serviceIdentifier, String message, String login) {
        return setStatus(serviceIdentifier, ACTIVE, message, login);
    }

    @Override
    public AdministrativeStatus deactivate(String serviceIdentifier, String message, String login) {
        return setStatus(serviceIdentifier, PASSIVE, message, login);
    }

    @Override
    public AdministrativeStatus setStatus(String serviceIdentifier, String state, String message, String login) {
        AdministrativeStatus status = new AdministrativeStatus(state, message, Calendar.getInstance(), login,
                serverInstanceName, serviceIdentifier);
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

    protected AdministrativeStatus addLabelAndDescription(AdministrativeStatus status) {
        if (status == null) {
            return null;
        }
        String id = status.getServiceIdentifier();
        AdministrableServiceDescriptor desc = globalManager.getServiceDescriptor(id);
        if (desc != null) {
            status.setLabelAndDescription(desc.getLabel(), desc.getDescription());
        }
        return status;
    }

    @Override
    public AdministrativeStatus getStatus(String serviceIdentifier) {
        AdministrativeStatus status = persister.getStatus(serverInstanceName, serviceIdentifier);
        addLabelAndDescription(status);
        return status;
    }

}
