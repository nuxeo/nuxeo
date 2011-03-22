/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management;

import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.events.EventStatsImpl;
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

    public static final ComponentName NAME = new ComponentName(
            CoreManagementComponent.class.getCanonicalName());

    public static final String PROBES_EP = "probes";

    public static final String SERVICE_DEF_EP = "serviceDefinition";

    public static final String STORAGE_HANDLERS_EP = "storageHandlers";

    public static final String STORAGE_CONFIG_EP = "storageConfiguration";

    protected static CoreManagementComponent defaultComponent;

    protected final GlobalAdministrativeStatusManager globalManager = new GlobalAdministrativeStatusManagerImpl();

    protected final EventStats eventStats = new EventStatsImpl();

    protected final ProbeManagerImpl probeRunner = new ProbeManagerImpl();

    protected final DocumentStoreManager storageManager = new DocumentStoreManager();

    public CoreManagementComponent() {
        super(); // enables breaking
    }

    public AdministrativeStatusManagerImpl getLocalManager() {
        return (AdministrativeStatusManagerImpl) globalManager.getStatusManager(
                globalManager.getLocalNuxeoInstanceIdentifier());
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(PROBES_EP)) {
            probeRunner.registerProbe((ProbeDescriptor) contribution);
        } else if (extensionPoint.equals(SERVICE_DEF_EP)) {
            globalManager.registerService((AdministrableServiceDescriptor) contribution);
        } else if (extensionPoint.equals(STORAGE_HANDLERS_EP)) {
            storageManager.registerHandler((DocumentStoreHandlerDescriptor) contribution);
        } else if (extensionPoint.equals(STORAGE_CONFIG_EP)) {
            storageManager.registerConfig((DocumentStoreConfigurationDescriptor) contribution);
        } else {
            super.registerContribution(contribution, extensionPoint,
                    contributor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("probes")) {
            probeRunner.unregisterProbe((ProbeDescriptor) contribution);
        }
    }

    public static CoreManagementComponent getDefault() {
        return defaultComponent;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        defaultComponent = this;
        storageManager.install();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        defaultComponent = null;
        storageManager.uninstall();
        getLocalManager().onNuxeoServerShutdown();
    }

    public void onNuxeoServerStartup() {
        getLocalManager().onNuxeoServerStartup();
    }

}
