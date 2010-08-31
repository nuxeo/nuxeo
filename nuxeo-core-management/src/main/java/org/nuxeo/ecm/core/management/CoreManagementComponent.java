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

import org.nuxeo.ecm.core.management.statuses.AdministrativeStatus;
import org.nuxeo.ecm.core.management.statuses.ProbeDescriptor;
import org.nuxeo.ecm.core.management.statuses.ProbeRunner;
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

    public CoreManagementComponent() {
        super(); // enables breaking
    }

    protected AdministrativeStatus adminStatus = new AdministrativeStatus();

    protected ProbeRunner probeRunner = new ProbeRunner();

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AdministrativeStatus.class)) {
            return adapter.cast(adminStatus);
        }
        if (adapter.isAssignableFrom(ProbeRunner.class)) {
            return adapter.cast(probeRunner);
        }
        return super.getAdapter(adapter);
    }


    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("probes")) {
            probeRunner.registerProbe((ProbeDescriptor) contribution);
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
                            adminStatus.activate();
                        }
                        finally{
                            Thread.currentThread().setContextClassLoader(jbossCL);
                        }// contributed
                    }
                });
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        adminStatus.deactivate();
    }

}
