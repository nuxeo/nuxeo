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

import java.io.Serializable;

/**
 * Schedule entry.
 * <p>
 * Holds information about a schedule, including the event to send,
 * the username to use to open the session, and the
 * periodicity for the schedule.
 */
public interface Schedule extends Serializable {

    /**
     * Returns the schedule job id.
     *
     * @return the schedule job id.
     */
    String getId();

    /**
     * Returns the event id.
     *
     * @return the event id
     */
    String getEventId();

    /**
     * Returns the event category.
     *
     * @return the event category
     */
    String getEventCategory();

    /**
     * Returns the cron expression.
     *
     * @return the cron expression
     */
    String getCronExpression();

    /**
     * Returns the username.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Returns true if the scheduler is enabled and the job scheduled for
     * execution
     *
     * @since 5.7.3
     * @return
     */
    boolean isEnabled();

}
