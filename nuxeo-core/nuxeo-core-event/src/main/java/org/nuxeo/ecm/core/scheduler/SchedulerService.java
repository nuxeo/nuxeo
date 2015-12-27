/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Registers a schedule. Add all parameters to eventContext.
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
