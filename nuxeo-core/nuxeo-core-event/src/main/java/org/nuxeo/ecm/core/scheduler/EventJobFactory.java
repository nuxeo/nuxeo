/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Munch
 */
package org.nuxeo.ecm.core.scheduler;

import java.io.Serializable;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * Factory instantiating the {@link Job} and the {@link Trigger} of a scheduled event.
 *
 * @since 10.2
 */
public interface EventJobFactory {
    /**
     * Builds the job of the scheduled event.
     * <p>
     * Returns a builder to allow extensibility.
     *
     * @param schedule Scheduled event contribution.
     * @param parameters Job parameters (might be {@code null}).
     * @return An instance of {@link JobBuilder}.
     */
    JobBuilder buildJob(Schedule schedule, Map<String, Serializable> parameters);

    /**
     * Builds the trigger of the scheduled event.
     * <p>
     * Returns a builder to allow extensibility.
     *
     * @param schedule Scheduled event contribution.
     * @return An instance of {@link TriggerBuilder}.
     */
    TriggerBuilder<?> buildTrigger(Schedule schedule);

    /**
     * Builds the schedule of the trigger (used by {@link #buildTrigger(Schedule)}).
     * <p>
     * Returns a builder to allow extensibility.
     *
     * @param schedule Scheduled event contribution.
     * @return An instance of {@link ScheduleBuilder}.
     */
    ScheduleBuilder<?> buildSchedule(Schedule schedule);
}
