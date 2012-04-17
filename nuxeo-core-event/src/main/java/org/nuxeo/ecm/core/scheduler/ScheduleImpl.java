/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.scheduler;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * ScheduleImpl extension definition.
 */
@XObject("schedule")
public class ScheduleImpl implements Schedule {

    @XNode("@id")
    public String id;

    @XNode("event")
    public String eventId;

    // BBB compat with old descriptors. use <event> now for consistency with
    // EventListenerDescriptor
    @XNode("eventId")
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @XNode("eventCategory")
    public String eventCategory;

    @XNode("cronExpression")
    public String cronExpression;

    @XNode("username")
    public String username;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventCategory() {
        return eventCategory;
    }

    @Override
    public String getCronExpression() {
        return cronExpression;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "Schedule " + id + " (cron=" + cronExpression + ')';
    }

}
