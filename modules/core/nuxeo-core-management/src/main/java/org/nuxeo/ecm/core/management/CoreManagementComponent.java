/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management;

import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.events.EventStatsHolder;
import org.nuxeo.ecm.core.management.events.EventStatsImpl;
import org.nuxeo.ecm.core.management.probes.HealthCheckProbesDescriptor;
import org.nuxeo.ecm.core.management.probes.ProbeDescriptor;
import org.nuxeo.ecm.core.management.probes.ProbeManagerImpl;
import org.nuxeo.ecm.core.management.statuses.AdministrableServiceDescriptor;
import org.nuxeo.ecm.core.management.statuses.AdministrativeStatusManagerImpl;
import org.nuxeo.ecm.core.management.statuses.GlobalAdministrativeStatusManagerImpl;
import org.nuxeo.ecm.core.management.storage.DocumentStoreConfigurationDescriptor;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandlerDescriptor;
import org.nuxeo.ecm.core.management.storage.DocumentStoreManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class CoreManagementComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(CoreManagementComponent.class.getCanonicalName());

    public static final String PROBES_EP = "probes";

    public static final String SERVICE_DEF_EP = "serviceDefinition";

    public static final String STORAGE_HANDLERS_EP = "storageHandlers";

    public static final String STORAGE_CONFIG_EP = "storageConfiguration";

    public static final String HEALTH_CHECK_EP = "healthCheck";

    protected final EventStats eventStats = new EventStatsImpl();

    protected GlobalAdministrativeStatusManager globalManager;

    protected ProbeManagerImpl probeRunner;

    protected DocumentStoreManager storageManager;

    public CoreManagementComponent() {
        super(); // enables breaking
    }

    @Override
    public void activate(ComponentContext context) {
        EventStatsHolder.clearStats();
    }

    @Override
    public void deactivate(ComponentContext context) {
        EventStatsHolder.clearStats();
    }

    @Override
    public void start(ComponentContext context) {
        globalManager = new GlobalAdministrativeStatusManagerImpl();
        this.<AdministrableServiceDescriptor> getRegistryContributions(SERVICE_DEF_EP)
            .forEach(globalManager::registerService);

        probeRunner = new ProbeManagerImpl();
        this.<ProbeDescriptor> getRegistryContributions(PROBES_EP).forEach(probeRunner::registerProbe);
        // health check needs corresponding probe to be registered first
        this.<HealthCheckProbesDescriptor> getRegistryContributions(HEALTH_CHECK_EP)
            .forEach(probeRunner::registerProbeForHealthCheck);

        storageManager = new DocumentStoreManager();
        storageManager.install();
        this.<DocumentStoreHandlerDescriptor> getRegistryContributions(STORAGE_HANDLERS_EP)
            .forEach(storageManager::registerHandler);
        this.<DocumentStoreConfigurationDescriptor> getRegistryContribution(STORAGE_CONFIG_EP)
            .ifPresent(storageManager::registerConfig);

    }

    protected AdministrativeStatusManagerImpl getLocalManager() {
        return (AdministrativeStatusManagerImpl) globalManager.getStatusManager(
                globalManager.getLocalNuxeoInstanceIdentifier());
    }

    public void onNuxeoServerStartup() {
        getLocalManager().onNuxeoServerStartup();
    }

    @Override
    public void stop(ComponentContext context) {
        getLocalManager().onNuxeoServerShutdown();
        globalManager = null;
        probeRunner = null;
        storageManager.uninstall();
        storageManager = null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == EventStats.class) {
            return adapter.cast(eventStats);
        }
        if (adapter.isAssignableFrom(GlobalAdministrativeStatusManager.class)) {
            return adapter.cast(globalManager);
        }
        if (adapter.isAssignableFrom(AdministrativeStatusManager.class)) {
            return adapter.cast(getLocalManager());
        }
        if (adapter.isAssignableFrom(ProbeManager.class)) {
            return adapter.cast(probeRunner);
        }
        return super.getAdapter(adapter);
    }

    /** @deprecated since 11.4, use {@code Framework.getService(CoreManagementComponent.class)} instead */
    @Deprecated
    public static CoreManagementComponent getDefault() {
        return Framework.getService(CoreManagementComponent.class);
    }

}
