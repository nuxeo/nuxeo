/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock;

import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * Default implementation of the lock service.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class LockComponent extends DefaultComponent {

    protected LockCoordinatorImpl coordinator;

    protected ThreadedLockRecordProvider provider;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        final BundleContext bundleContext = context.getRuntimeContext().getBundle().getBundleContext();
        bundleContext.addFrameworkListener(new FrameworkListener() {

            public void frameworkEvent(FrameworkEvent event) {
                if (FrameworkEvent.STARTED != event.getType()) {
                    return;
                }
                bundleContext.removeFrameworkListener(this);
                coordinator = new LockCoordinatorImpl();
                provider = new ThreadedLockRecordProvider();
                coordinator.activate(LockComponent.this);
                provider.activate(LockComponent.this);
            }
        });
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        coordinator.disactivate();
        provider.disactivate();
        coordinator = null;
        provider = null;
        super.deactivate(context);
    }

    public LockCoordinator getLockCoordinator() {
        return coordinator;
    }

    @Override
    public <T> T getAdapter(Class<T> clazz) {
        if (LockCoordinator.class.isAssignableFrom(clazz)) {
            return clazz.cast(coordinator);
        }
        return super.getAdapter(clazz);
    }

}
