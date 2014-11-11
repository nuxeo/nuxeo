/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Event Service Component, allowing registration of contributions and doing the
 * event service shutdown upon deactivation.
 */
public class EventServiceComponent extends DefaultComponent {

    public static final String EVENT_LISTENER_XP = "listener";

    public static final long DEFAULT_SHUTDOWN_TIMEOUT = 5 * 1000; // 5 seconds

    protected EventServiceImpl service;

    @Override
    public void activate(ComponentContext context) throws Exception {
        service = new EventServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (service != null) {
            String s = Framework.getProperty("org.nuxeo.ecm.core.event.shutdown.timeoutMillis");
            long timeout = s == null ? DEFAULT_SHUTDOWN_TIMEOUT : Long.parseLong(s);
            service.shutdown(timeout);
            service = null;
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (EVENT_LISTENER_XP.equals(extensionPoint)) {
            EventListenerDescriptor descriptor = (EventListenerDescriptor) contribution;
            descriptor.setRuntimeContext(contributor.getRuntimeContext());
            service.addEventListener(descriptor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (EVENT_LISTENER_XP.equals(extensionPoint)) {
            service.removeEventListener((EventListenerDescriptor) contribution);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (EventService.class == adapter || EventProducer.class == adapter
                || EventServiceAdmin.class == adapter) {
            return (T) service;
        }
        return null;
    }

}
