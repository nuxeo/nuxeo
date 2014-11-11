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
package org.nuxeo.ecm.platform.management.statuses;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ProbeScheduler {

    public void enable() {
        EventServiceAdmin admin = Framework.getLocalService(EventServiceAdmin.class);
        admin.setListenerEnabledFlag("probeScheduleListener", true);
    }

    public void disable() {
        EventServiceAdmin admin = Framework.getLocalService(EventServiceAdmin.class);
        admin.setListenerEnabledFlag("probeScheduleListener", false);
    }

    public boolean isEnabled() {
        EventServiceAdmin admin = Framework.getLocalService(EventServiceAdmin.class);
        EventListenerDescriptor descriptor = admin.getListenerList().getDescriptor("probeScheduleListener");
        return descriptor.isEnabled();
    }

}
