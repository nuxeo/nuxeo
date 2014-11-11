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
package org.nuxeo.ecm.platform.scheduler.core.interfaces;

/**
 * Schedule entry.
 * <p>
 * Holds information about a schedule, including the event to send,
 * the username and password to use to open the session, and the
 * periodicity for the schedule.
 *
 * @author <a href="mailto:fg@nuxeo.com">Florent Guillaume</a>
 */
public interface Schedule {

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
     * Returns the password.
     *
     * @return the password
     */
    String getPassword();

}
