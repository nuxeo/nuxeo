/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.management.statuses;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ProbeScheduler {

    public void enable() {
        EventServiceAdmin admin = Framework.getService(EventServiceAdmin.class);
        admin.setListenerEnabledFlag("probeScheduleListener", true);
    }

    public void disable() {
        EventServiceAdmin admin = Framework.getService(EventServiceAdmin.class);
        admin.setListenerEnabledFlag("probeScheduleListener", false);
    }

    public boolean isEnabled() {
        EventServiceAdmin admin = Framework.getService(EventServiceAdmin.class);
        EventListenerDescriptor descriptor = admin.getListenerList().getDescriptor("probeScheduleListener");
        return descriptor.isEnabled();
    }

}
