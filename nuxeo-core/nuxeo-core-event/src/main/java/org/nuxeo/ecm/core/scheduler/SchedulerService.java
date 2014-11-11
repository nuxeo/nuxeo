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
import java.util.Map;

/**
 * Scheduler service.
 */
public interface SchedulerService {

    /**
     * Registers a schedule.
     *
     * @param schedule the schedule
     */
    void registerSchedule(Schedule schedule);


    /**
     * Registers a schedule.
     * Add all parameters to eventContext.
     *
     * @param schedule
     * @param parameters
     */
    void registerSchedule(Schedule schedule, Map<String, Serializable> parameters);

    /**
     * UnRegisters a schedule.
     *
     * @param scheduleId the schedule id
     * @return true if schedule has been successfully removed.
     */
    boolean unregisterSchedule(String scheduleId);

    /**
     * UnRegisters a schedule.
     *
     * @param schedule to be unregistered
     * @return true if schedule has been successfully removed.
     */
    boolean unregisterSchedule(Schedule schedule);

    /**
     * Checks if the framework has fully started.
     * <p>
     * Used to delay job execution until the framework has fully started.
     *
     * @return {@code true} if the framework has started
     * @since 5.6
     */
    boolean hasApplicationStarted();

}
