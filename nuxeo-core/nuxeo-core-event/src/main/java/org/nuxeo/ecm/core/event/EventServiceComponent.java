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
 * Event Service Component, allowing registration of contributions and doing the event service shutdown upon
 * deactivation.
 */
public class EventServiceComponent extends DefaultComponent {

    public static final int APPLICATION_STARTED_ORDER = -500;

    public static final String EVENT_LISTENER_XP = "listener";

    public static final long DEFAULT_SHUTDOWN_TIMEOUT = 5 * 1000; // 5 seconds

    protected EventServiceImpl service;

    @Override
    public void activate(ComponentContext context) {
        service = new EventServiceImpl();
        service.init();

    }

    @Override
    public void deactivate(ComponentContext context) {
        if (service != null) {
            String s = Framework.getProperty("org.nuxeo.ecm.core.event.shutdown.timeoutMillis");
            long timeout = s == null ? DEFAULT_SHUTDOWN_TIMEOUT : Long.parseLong(s);
            try {
                service.shutdown(timeout);
            } catch (InterruptedException e) {
                // restore interrupted status
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            service = null;
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EVENT_LISTENER_XP.equals(extensionPoint)) {
            EventListenerDescriptor descriptor = (EventListenerDescriptor) contribution;
            descriptor.setRuntimeContext(contributor.getRuntimeContext());
            service.addEventListener(descriptor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EVENT_LISTENER_XP.equals(extensionPoint)) {
            service.removeEventListener((EventListenerDescriptor) contribution);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (EventService.class == adapter || EventProducer.class == adapter || EventServiceAdmin.class == adapter) {
            return (T) service;
        }
        return null;
    }

}
