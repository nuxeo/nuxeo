/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management;

import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.probes.ProbeDescriptor;
import org.nuxeo.ecm.core.management.probes.ProbeManagerImpl;
import org.nuxeo.ecm.core.management.statuses.AdministrableServiceDescriptor;
import org.nuxeo.ecm.core.management.statuses.AdministrativeStatusManagerImpl;
import org.nuxeo.ecm.core.management.statuses.GlobalAdministrativeStatusManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class CoreManagementComponent extends DefaultComponent  {

    public static final ComponentName NAME = new ComponentName(
            CoreManagementComponent.class.getCanonicalName());

    public static final String PROBES_EP = "probes";

    public static final String SERVICE_DEF_EP = "serviceDefinition";


    public CoreManagementComponent() {
        super(); // enables breaking
    }

    protected GlobalAdministrativeStatusManagerImpl globalManager = new GlobalAdministrativeStatusManagerImpl();
    protected ProbeManagerImpl probeRunner = new ProbeManagerImpl();

    protected AdministrativeStatusManagerImpl getLocalManager() {
        return (AdministrativeStatusManagerImpl) globalManager.getStatusManager(globalManager.getLocalNuxeoInstanceIdentifier());
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
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
        }
        else if (extensionPoint.equals(SERVICE_DEF_EP)) {
            globalManager.registerService((AdministrableServiceDescriptor) contribution);
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

    @Override
    public void activate(ComponentContext context) throws Exception {
        context.getRuntimeContext().getBundle().getBundleContext()
                .addFrameworkListener(new FrameworkListener() {
                    public void frameworkEvent(FrameworkEvent event) {
                        if (event.getType() != FrameworkEvent.STARTED) {
                            return;
                        }
                        event.getBundle().getBundleContext().removeFrameworkListener(this);
                        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
                        ClassLoader nuxeoCL = Framework.class.getClassLoader();
                        try{
                            Thread.currentThread().setContextClassLoader(nuxeoCL);
                            getLocalManager().onNuxeoServerStartup();
                            probeRunner.runAllProbes();
                        }
                        finally{
                            Thread.currentThread().setContextClassLoader(jbossCL);
                        }// contributed
                    }
                });
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        getLocalManager().onNuxeoServerShutdown();
    }

}
