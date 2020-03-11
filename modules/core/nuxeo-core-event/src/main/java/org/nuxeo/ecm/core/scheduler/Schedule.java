/*
 * (C) Copyright 2007-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Munch
 */
package org.nuxeo.ecm.core.scheduler;

import java.io.Serializable;

/**
 * Schedule entry.
 * <p>
 * Holds information about a schedule, including the event to send, the username to use to open the session, and the
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
     * Returns an instance of the {@link EventJobFactory} ({@link DefaultEventJobFactory} by default).
     *
     * @since 10.2
     * @return An instance of {@link EventJobFactory}.
     */
    EventJobFactory getJobFactory();

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
     * Returns true if the scheduler is enabled and the job scheduled for execution
     *
     * @since 5.7.3
     * @return
     */
    boolean isEnabled();

    /**
     * Returns the timezone to be used for the Cron Expression.
     *
     * @since 10.2
     * @return the timezone, or {@code null} if not specified
     */
    String getTimeZone();


}
