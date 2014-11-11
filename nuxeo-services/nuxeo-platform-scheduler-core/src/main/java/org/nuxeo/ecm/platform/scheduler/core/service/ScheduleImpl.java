/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.scheduler.core.interfaces.Schedule;

/**
 * ScheduleImpl extension definition.
 *
 * @author <a href="mailto:fg@nuxeo.com">Florent Guillaume</a>
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

    @XNode("password")
    public String password;

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "Schedule " + id + " (cron=" + cronExpression + ')';
    }

}
