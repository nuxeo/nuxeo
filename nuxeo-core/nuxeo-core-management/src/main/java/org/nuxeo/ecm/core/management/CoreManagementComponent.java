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
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
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

    protected static CoreManagementComponent defaultComponent;

    protected final GlobalAdministrativeStatusManager globalManager = new GlobalAdministrativeStatusManagerImpl();

    protected final EventStats eventStats = new EventStatsImpl();

    protected final ProbeManagerImpl probeRunner = new ProbeManagerImpl();

    protected final DocumentStoreManager storageManager = new DocumentStoreManager();

    public CoreManagementComponent() {
        super(); // enables breaking
    }

    public AdministrativeStatusManagerImpl getLocalManager() {
        return (AdministrativeStatusManagerImpl) globalManager.getStatusManager(globalManager.getLocalNuxeoInstanceIdentifier());
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

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(PROBES_EP)) {
            probeRunner.registerProbe((ProbeDescriptor) contribution);
        } else if (extensionPoint.equals(SERVICE_DEF_EP)) {
            globalManager.registerService((AdministrableServiceDescriptor) contribution);
        } else if (extensionPoint.equals(STORAGE_HANDLERS_EP)) {
            storageManager.registerHandler((DocumentStoreHandlerDescriptor) contribution);
        } else if (extensionPoint.equals(STORAGE_CONFIG_EP)) {
            storageManager.registerConfig((DocumentStoreConfigurationDescriptor) contribution);
        } else if (extensionPoint.equals(HEALTH_CHECK_EP)) {
            probeRunner.registerProbeForHealthCheck((HealthCheckProbesDescriptor) contribution);
        } else {
            super.registerContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("probes")) {
            probeRunner.unregisterProbe((ProbeDescriptor) contribution);
        }
    }

    public static CoreManagementComponent getDefault() {
        return defaultComponent;
    }

    @Override
    public void activate(ComponentContext context) {
        defaultComponent = this;
        storageManager.install();
        EventStatsHolder.clearStats();
    }

    @Override
    public void deactivate(ComponentContext context) {
        defaultComponent = null;
        storageManager.uninstall();
        getLocalManager().onNuxeoServerShutdown();
        EventStatsHolder.clearStats();
    }

    public void onNuxeoServerStartup() {
        getLocalManager().onNuxeoServerStartup();
    }

}
